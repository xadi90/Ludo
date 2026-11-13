package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.i18n.LocalAppStrings
import com.example.ui.components.GlossyButton
import com.example.ui.components.GlowingGameCard
import com.example.ui.components.LudoTopBar
import com.example.ui.components.RoyalArenaBackground
import com.example.ui.theme.ArenaCardSurface
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoBlueDark
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoGreenDark
import com.example.ui.theme.LudoPurple
import com.example.ui.theme.LudoRed
import com.example.vault.SecretVaultManager
import com.example.vault.VaultItem
import com.example.vault.VaultMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SecretVaultScreen(
    darkTheme: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val vaultManager = remember { SecretVaultManager(context) }

    var isPinConfigured by remember { mutableStateOf(vaultManager.isPinConfigured()) }
    var isUnlocked by remember { mutableStateOf(false) }
    var firstStepPin by remember { mutableStateOf<String?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var lockoutRemainingSec by remember { mutableIntStateOf(0) }
    var isChangingPin by remember { mutableStateOf(false) }

    var items by remember { mutableStateOf<List<VaultItem>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf<VaultMediaType?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var previewItem by remember { mutableStateOf<VaultItem?>(null) }
    var deleteCandidate by remember { mutableStateOf<VaultItem?>(null) }
    var exportCandidate by remember { mutableStateOf<VaultItem?>(null) }
    var deleteSafOriginal by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isUnlocked, refreshTrigger) {
        if (isUnlocked) {
            items = withContext(Dispatchers.IO) { vaultManager.listItems() }
        }
    }

    LaunchedEffect(lockoutRemainingSec) {
        while (true) {
            val rem = ((vaultManager.getLockoutUntilMs() - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
            lockoutRemainingSec = rem
            if (rem <= 0) break
            delay(1000L)
        }
    }

    BackHandler {
        when {
            previewItem != null -> previewItem = null
            isChangingPin -> {
                isChangingPin = false
                firstStepPin = null
                enteredPin = ""
                pinError = null
            }
            else -> {
                isUnlocked = false
                enteredPin = ""
                onBack()
            }
        }
    }

    // Android Zero-Permission Photo Picker for Photos & Videos
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            isBusy = true
            scope.launch {
                val importedCount = withContext(Dispatchers.IO) {
                    var count = 0
                    uris.forEach { uri ->
                        val (item, _) = vaultManager.importFromUri(uri, deleteOriginalIfSupported = false)
                        if (item != null) count++
                    }
                    count
                }
                isBusy = false
                refreshTrigger = System.currentTimeMillis()
                statusNotice = "$importedCount item(s) encrypted inside Vault. Delete original from Gallery if desired."
            }
        }
    }

    // Storage Access Framework Document Picker (PDFs, audio, docs, or files)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isBusy = true
            scope.launch {
                val (imported, deleted) = withContext(Dispatchers.IO) {
                    var count = 0
                    var delCount = 0
                    uris.forEach { uri ->
                        val (item, removed) = vaultManager.importFromUri(uri, deleteOriginalIfSupported = deleteSafOriginal)
                        if (item != null) count++
                        if (removed) delCount++
                    }
                    count to delCount
                }
                isBusy = false
                refreshTrigger = System.currentTimeMillis()
                statusNotice = if (deleted > 0) {
                    "$imported file(s) moved securely into Vault ($deleted original removed)."
                } else {
                    "$imported file(s) encrypted inside Vault."
                }
            }
        }
    }

    // Export / Restore hidden item back to device storage
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { targetUri ->
        val candidate = exportCandidate
        exportCandidate = null
        if (targetUri != null && candidate != null) {
            isBusy = true
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    vaultManager.exportToUri(candidate, targetUri)
                }
                isBusy = false
                statusNotice = if (ok) "Restored '${candidate.displayName}' to device." else "Restore failed."
            }
        }
    }

    fun handlePinSubmit() {
        if (lockoutRemainingSec > 0) return
        if (!isPinConfigured || isChangingPin) {
            if (enteredPin.length < 4) {
                pinError = "Enter at least 4 digits"
                return
            }
            val first = firstStepPin
            if (first == null) {
                firstStepPin = enteredPin
                enteredPin = ""
                pinError = null
            } else if (first == enteredPin) {
                vaultManager.setOrChangePin(enteredPin)
                isPinConfigured = true
                isChangingPin = false
                isUnlocked = true
                firstStepPin = null
                enteredPin = ""
                pinError = null
                statusNotice = "Secret PIN saved!"
            } else {
                pinError = "PINs did not match. Try again."
                firstStepPin = null
                enteredPin = ""
            }
        } else {
            if (vaultManager.verifyPin(enteredPin)) {
                isUnlocked = true
                enteredPin = ""
                pinError = null
            } else {
                val rem = ((vaultManager.getLockoutUntilMs() - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
                lockoutRemainingSec = rem
                pinError = if (rem > 0) {
                    "Too many attempts. Wait ${rem}s."
                } else {
                    "Incorrect PIN (${vaultManager.getRemainingAttempts()} tries left)"
                }
                enteredPin = ""
            }
        }
    }

    RoyalArenaBackground(darkTheme = darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
        ) {
            if (!isUnlocked || isChangingPin) {
                LudoTopBar(
                    title = strings.secretVaultTitle,
                    onBack = {
                        if (isChangingPin) {
                            isChangingPin = false
                            firstStepPin = null
                            enteredPin = ""
                            pinError = null
                        } else {
                            onBack()
                        }
                    }
                )

                val headline = when {
                    (!isPinConfigured || isChangingPin) && firstStepPin == null -> "Create Secret Password (4–8 Digits)"
                    (!isPinConfigured || isChangingPin) && firstStepPin != null -> "Confirm Secret Password"
                    else -> "Enter Secret Password"
                }

                PinPadLockContent(
                    headline = headline,
                    pinLength = enteredPin.length,
                    errorText = pinError,
                    lockoutSec = lockoutRemainingSec,
                    onDigit = { d ->
                        if (lockoutRemainingSec <= 0 && enteredPin.length < 8) {
                            enteredPin += d
                            pinError = null
                        }
                    },
                    onBackspace = {
                        if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                    },
                    onSubmit = ::handlePinSubmit
                )
            } else {
                // Unlocked Vault Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ArenaCardSurface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isUnlocked = false
                                    onBack()
                                },
                                modifier = Modifier.testTag("vault_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back & Lock", tint = Color.White)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.secretVaultTitle,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = GoldAccent
                            )
                            Text(
                                text = "${items.size} encrypted items",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = ArenaCardSurface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isChangingPin = true
                                    firstStepPin = null
                                    enteredPin = ""
                                    pinError = null
                                },
                                modifier = Modifier.testTag("vault_change_pin_button")
                            ) {
                                Icon(Icons.Filled.Key, contentDescription = "Change PIN", tint = GoldAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = LudoRed.copy(alpha = 0.9f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isUnlocked = false
                                    enteredPin = ""
                                },
                                modifier = Modifier.testTag("vault_lock_now_button")
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = "Lock Vault", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Import Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlossyButton(
                        text = "Photos / Video",
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        topColor = LudoBlue,
                        bottomColor = LudoBlueDark,
                        icon = Icons.Filled.AddPhotoAlternate,
                        height = 48.dp,
                        testTag = "vault_add_media_button",
                        modifier = Modifier.weight(1f)
                    )
                    GlossyButton(
                        text = "Hide Files",
                        onClick = { documentPickerLauncher.launch(arrayOf("*/*")) },
                        topColor = LudoGreen,
                        bottomColor = LudoGreenDark,
                        icon = Icons.Filled.UploadFile,
                        height = 48.dp,
                        testTag = "vault_add_files_button",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Optional Remove Source File toggle for Document Picker + Open Gallery helper
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Switch(
                            checked = deleteSafOriginal,
                            onCheckedChange = { deleteSafOriginal = it }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Move files (remove source)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val galleryIntent = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            runCatching { context.startActivity(galleryIntent) }
                        }
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gallery", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (statusNotice != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LudoGreen.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, LudoGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusNotice ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { statusNotice = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        null to "All (${items.size})",
                        VaultMediaType.IMAGE to "Photos",
                        VaultMediaType.VIDEO to "Videos",
                        VaultMediaType.FILE to "Files"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = { selectedFilter = type },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LudoPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                val filtered = remember(items, selectedFilter) {
                    if (selectedFilter == null) items else items.filter { it.mediaType == selectedFilter }
                }

                if (isBusy) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldAccent)
                    }
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlowingGameCard(accentColor = LudoBlue) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LockOpen,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Your Encrypted Vault is Empty",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Photos / Video' or 'Hide Files' above to encrypt & lock private items inside the app. Tip: Long-press the LUDO banner on Home anytime for secret access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            VaultGridCard(
                                item = item,
                                vaultManager = vaultManager,
                                onOpen = { previewItem = item },
                                onRestore = {
                                    exportCandidate = item
                                    exportLauncher.launch(item.displayName)
                                },
                                onDelete = { deleteCandidate = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full-screen Preview Dialog for encrypted Photos/Items
    if (previewItem != null) {
        val current = previewItem!!
        val fullBitmap by produceState<Bitmap?>(initialValue = null, current.id) {
            value = withContext(Dispatchers.IO) { vaultManager.decodeDecryptedBitmap(current, maxDim = 1600) }
        }
        Dialog(onDismissRequest = { previewItem = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0A1432),
                border = BorderStroke(1.5.dp, GoldAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = current.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { previewItem = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (fullBitmap != null) {
                        Image(
                            bitmap = fullBitmap!!.asImageBitmap(),
                            contentDescription = current.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (current.mediaType == VaultMediaType.VIDEO) Icons.Filled.VideoFile else Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Encrypted ${current.mediaType.name} (${formatBytes(current.sizeBytes)})\nUse Restore to export.",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                previewItem = null
                                exportCandidate = current
                                exportLauncher.launch(current.displayName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LudoGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restore", color = Color.White)
                        }
                        Button(
                            onClick = {
                                previewItem = null
                                deleteCandidate = current
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LudoRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (deleteCandidate != null) {
        val target = deleteCandidate!!
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete from Vault?", fontWeight = FontWeight.Bold) },
            text = { Text("Permanently remove '${target.displayName}' from the encrypted Vault?") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCandidate = null
                        scope.launch {
                            withContext(Dispatchers.IO) { vaultManager.deleteItem(target.id) }
                            refreshTrigger = System.currentTimeMillis()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LudoRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteCandidate = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun PinPadLockContent(
    headline: String,
    pinLength: Int,
    errorText: String?,
    lockoutSec: Int,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(LudoBlue)
                .border(2.dp, GoldAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))

        // PIN dot display
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val dots = pinLength.coerceAtLeast(4).coerceAtMost(8)
            for (i in 0 until dots) {
                val filled = i < pinLength
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (filled) GoldAccent else Color.White.copy(alpha = 0.2f))
                        .border(1.5.dp, if (filled) GoldAccent else Color.White.copy(alpha = 0.45f), CircleShape)
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        if (errorText != null) {
            Text(
                text = errorText,
                color = LudoRed,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        } else if (lockoutSec > 0) {
            Text(
                text = "Locked for ${lockoutSec}s",
                color = LudoRed,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "AES-256 Encrypted Internal Vault",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("DEL", "0", "OK")
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { key ->
                        val isOk = key == "OK"
                        val isDel = key == "DEL"
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isOk -> LudoGreen
                                isDel -> ArenaCardSurface.copy(alpha = 0.8f)
                                else -> ArenaCardSurface.copy(alpha = 0.92f)
                            },
                            border = BorderStroke(1.5.dp, if (isOk) GoldAccent else Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .clickable(enabled = lockoutSec <= 0) {
                                    when (key) {
                                        "DEL" -> onBackspace()
                                        "OK" -> onSubmit()
                                        else -> onDigit(key)
                                    }
                                }
                                .testTag("vault_key_$key")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when {
                                    isDel -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", tint = Color.White)
                                    isOk -> Icon(Icons.Filled.Check, contentDescription = "Unlock", tint = Color.White, modifier = Modifier.size(24.dp))
                                    else -> Text(key, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun VaultGridCard(
    item: VaultItem,
    vaultManager: SecretVaultManager,
    onOpen: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val thumb by produceState<Bitmap?>(initialValue = null, item.id) {
        if (item.mediaType == VaultMediaType.IMAGE) {
            value = withContext(Dispatchers.IO) { vaultManager.decodeDecryptedBitmap(item, maxDim = 360) }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ArenaCardSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(Color(0xFF0A1432)),
                contentAlignment = Alignment.Center
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb!!.asImageBitmap(),
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (item.mediaType == VaultMediaType.VIDEO) Icons.Filled.VideoFile else Icons.Filled.Description,
                        contentDescription = item.displayName,
                        tint = GoldAccent,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatBytes(item.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Row {
                        IconButton(onClick = onRestore, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Download, contentDescription = "Restore", tint = LudoGreen, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = LudoRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.toInt()}KB"
    return String.format("%.1fMB", kb / 1024.0)
}
