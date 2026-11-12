package com.example.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Permanent, immutable application ownership identity.
 * These constants are compiled into source code and cannot be modified
 * via UI, local storage, or remote configuration.
 */
object DeveloperIdentity {
    const val APP_NAME: String = "LUDO"
    const val DEVELOPER_NAME: String = "Xadi Ahmed"
    const val DEVELOPER_EMAIL: String = "xadiahmed23@gmail.com"
    const val DEVELOPER_PHONE: String = "01878864913"
    const val APP_VERSION: String = "1.0.0"

    /**
     * Integrity verification token derived from permanent developer constants.
     */
    fun ownershipFingerprint(): String {
        val raw = "$APP_NAME|$DEVELOPER_NAME|$DEVELOPER_EMAIL|$DEVELOPER_PHONE"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.take(8).joinToString("") { "%02X".format(it) }
    }
}

/**
 * Runtime application security & input validation helpers.
 */
object AppSecurityManager {

    private val SAFE_NAME_REGEX = Regex("^[\\p{L}\\p{N} _.-]{1,20}$")
    private val ROOM_CODE_REGEX = Regex("^[A-Z0-9]{6}$")

    fun sanitizeNickname(input: String, fallback: String = "Player"): String {
        val trimmed = input.trim().take(20)
        return if (trimmed.isNotEmpty() && SAFE_NAME_REGEX.matches(trimmed)) {
            trimmed
        } else {
            val cleaned = trimmed.replace(Regex("[^\\p{L}\\p{N} _.-]"), "").trim()
            cleaned.ifEmpty { fallback }
        }
    }

    fun isValidRoomCode(code: String): Boolean {
        return ROOM_CODE_REGEX.matches(code.trim().uppercase())
    }

    /**
     * Reads the active APK signing certificate SHA-256 digest from PackageManager
     * for runtime verification and About Developer integrity display.
     */
    @Suppress("DEPRECATION")
    fun getSigningCertificateSha256(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo
                if (signingInfo == null) emptyArray()
                else if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
                else signingInfo.signingCertificateHistory
            } else {
                packageInfo.signatures ?: emptyArray()
            }

            val firstSig = signatures.firstOrNull() ?: return "UNAVAILABLE"
            val digest = MessageDigest.getInstance("SHA-256").digest(firstSig.toByteArray())
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            "UNAVAILABLE"
        }
    }

    fun verifyDeveloperIdentityIntegrity(): Boolean {
        return DeveloperIdentity.DEVELOPER_NAME == "Xadi Ahmed" &&
            DeveloperIdentity.DEVELOPER_EMAIL == "xadiahmed23@gmail.com" &&
            DeveloperIdentity.DEVELOPER_PHONE == "01878864913"
    }
}
