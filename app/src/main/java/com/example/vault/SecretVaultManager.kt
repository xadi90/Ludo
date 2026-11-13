package com.example.vault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

enum class VaultMediaType {
    IMAGE,
    VIDEO,
    FILE
}

data class VaultItem(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val mediaType: VaultMediaType,
    val sizeBytes: Long,
    val addedAtMs: Long,
    val encryptedFileName: String
)

/**
 * Private AES-256-GCM encrypted media & file vault backed by app-private internal storage
 * (not indexed by Gallery / MediaStore) with salted SHA-256 PIN verification.
 */
class SecretVaultManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val vaultDir: File = File(appContext.filesDir, VAULT_DIR_NAME).apply { mkdirs() }
    private val metaFile: File = File(vaultDir, "vault_index.json")
    private val random = SecureRandom()

    fun isPinConfigured(): Boolean = !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()

    fun setOrChangePin(newPin: String) {
        val clean = newPin.trim()
        require(clean.length in 4..8 && clean.all { it.isDigit() }) {
            "PIN must be 4 to 8 digits"
        }
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val hash = hashPin(clean, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, hash)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
            .apply()
    }

    fun verifyPin(candidatePin: String): Boolean {
        val now = System.currentTimeMillis()
        if (now < getLockoutUntilMs()) return false

        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val expectedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val actualHash = hashPin(candidatePin.trim(), salt)

        return if (MessageDigest.isEqual(actualHash.toByteArray(), expectedHash.toByteArray())) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
                .apply()
            true
        } else {
            val fails = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val lockoutUntil = if (fails >= MAX_ATTEMPTS) now + LOCKOUT_DURATION_MS else 0L
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, if (fails >= MAX_ATTEMPTS) 0 else fails)
                .putLong(KEY_LOCKOUT_UNTIL_MS, lockoutUntil)
                .apply()
            false
        }
    }

    fun getRemainingAttempts(): Int =
        (MAX_ATTEMPTS - prefs.getInt(KEY_FAILED_ATTEMPTS, 0)).coerceIn(1, MAX_ATTEMPTS)

    fun getLockoutUntilMs(): Long = prefs.getLong(KEY_LOCKOUT_UNTIL_MS, 0L)

    fun listItems(): List<VaultItem> {
        if (!metaFile.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(metaFile.readText())
            val list = ArrayList<VaultItem>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val encName = obj.getString("encryptedFileName")
                if (File(vaultDir, encName).exists()) {
                    list.add(
                        VaultItem(
                            id = obj.getString("id"),
                            displayName = obj.getString("displayName"),
                            mimeType = obj.getString("mimeType"),
                            mediaType = runCatching {
                                VaultMediaType.valueOf(obj.getString("mediaType"))
                            }.getOrDefault(VaultMediaType.FILE),
                            sizeBytes = obj.optLong("sizeBytes", 0L),
                            addedAtMs = obj.optLong("addedAtMs", 0L),
                            encryptedFileName = encName
                        )
                    )
                }
            }
            list.sortedByDescending { it.addedAtMs }
        }.getOrDefault(emptyList())
    }

    fun importFromUri(uri: Uri, deleteOriginalIfSupported: Boolean = false): Pair<VaultItem?, Boolean> {
        val resolver = appContext.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val name = queryDisplayName(uri) ?: "item_${System.currentTimeMillis()}"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null to false

        val mediaType = when {
            mime.startsWith("image/") -> VaultMediaType.IMAGE
            mime.startsWith("video/") -> VaultMediaType.VIDEO
            else -> VaultMediaType.FILE
        }

        val item = importRawBytes(name, mime, mediaType, bytes)
        var originalRemoved = false
        if (deleteOriginalIfSupported) {
            originalRemoved = runCatching {
                DocumentsContract.deleteDocument(resolver, uri)
            }.getOrDefault(false)
        }
        return item to originalRemoved
    }

    fun importRawBytes(
        displayName: String,
        mimeType: String,
        mediaType: VaultMediaType,
        plainBytes: ByteArray
    ): VaultItem {
        val id = UUID.randomUUID().toString()
        val encFileName = "$id.vlt"
        val encryptedPayload = encryptBytes(plainBytes)
        File(vaultDir, encFileName).writeBytes(encryptedPayload)

        val item = VaultItem(
            id = id,
            displayName = displayName,
            mimeType = mimeType,
            mediaType = mediaType,
            sizeBytes = plainBytes.size.toLong(),
            addedAtMs = System.currentTimeMillis(),
            encryptedFileName = encFileName
        )
        val current = listItems().toMutableList()
        current.add(0, item)
        saveMetadata(current)
        return item
    }

    fun readDecryptedBytes(item: VaultItem): ByteArray? {
        val file = File(vaultDir, item.encryptedFileName)
        if (!file.exists()) return null
        return runCatching { decryptBytes(file.readBytes()) }.getOrNull()
    }

    fun decodeDecryptedBitmap(item: VaultItem, maxDim: Int = 720): Bitmap? {
        if (item.mediaType != VaultMediaType.IMAGE) return null
        val plain = readDecryptedBytes(item) ?: return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(plain, 0, plain.size, opts)
        var sample = 1
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(plain, 0, plain.size, decodeOpts)
    }

    fun exportToUri(item: VaultItem, targetUri: Uri): Boolean {
        val plain = readDecryptedBytes(item) ?: return false
        return runCatching {
            appContext.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(plain)
            } ?: return false
            true
        }.getOrDefault(false)
    }

    fun deleteItem(itemId: String) {
        val items = listItems().toMutableList()
        val target = items.firstOrNull { it.id == itemId } ?: return
        File(vaultDir, target.encryptedFileName).delete()
        items.removeAll { it.id == itemId }
        saveMetadata(items)
    }

    fun encryptBytes(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain)
        return ByteArrayOutputStream(1 + iv.size + cipherText.size).apply {
            write(iv.size)
            write(iv)
            write(cipherText)
        }.toByteArray()
    }

    fun decryptBytes(payload: ByteArray): ByteArray {
        require(payload.size > 13) { "Corrupted vault payload" }
        val ivLen = payload[0].toInt() and 0xFF
        val iv = payload.copyOfRange(1, 1 + ivLen)
        val cipherText = payload.copyOfRange(1 + ivLen, payload.size)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        // Prefer hardware-backed AndroidKeyStore on real devices; fall back gracefully in JVM unit tests
        val keyStoreKey = runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = ks.getKey(KEYSTORE_ALIAS, null) as? SecretKey
            if (existing != null) return@runCatching existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }.getOrNull()

        if (keyStoreKey != null) return keyStoreKey

        val storedB64 = prefs.getString(KEY_FALLBACK_AES, null)
        val raw = if (storedB64 != null) {
            Base64.decode(storedB64, Base64.NO_WRAP)
        } else {
            ByteArray(32).also {
                random.nextBytes(it)
                prefs.edit().putString(KEY_FALLBACK_AES, Base64.encodeToString(it, Base64.NO_WRAP)).apply()
            }
        }
        return SecretKeySpec(raw, "AES")
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var current = salt + pin.toByteArray(Charsets.UTF_8)
        repeat(2000) {
            digest.reset()
            digest.update(salt)
            current = digest.digest(current)
        }
        return Base64.encodeToString(current, Base64.NO_WRAP)
    }

    private fun saveMetadata(items: List<VaultItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("displayName", item.displayName)
                    put("mimeType", item.mimeType)
                    put("mediaType", item.mediaType.name)
                    put("sizeBytes", item.sizeBytes)
                    put("addedAtMs", item.addedAtMs)
                    put("encryptedFileName", item.encryptedFileName)
                }
            )
        }
        metaFile.writeText(arr.toString())
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    companion object {
        private const val PREFS_NAME = "ludo_secret_vault_prefs"
        private const val VAULT_DIR_NAME = "secret_vault_store"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "ludo_secret_vault_aes_key"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"

        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL_MS = "lockout_until_ms"
        private const val KEY_FALLBACK_AES = "fallback_aes_key"

        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
    }
}
