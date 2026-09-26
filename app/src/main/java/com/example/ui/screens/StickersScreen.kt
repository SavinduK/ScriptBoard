package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.StickerEntity
import com.example.data.StickerRepository
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.stickers.StickerSegmentationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StickersScreen(
    colors: KeyboardColors,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stickerRepo = remember { StickerRepository(AppDatabase.getDatabase(context).stickerDao()) }
    val stickers by stickerRepo.allStickers.collectAsState(initial = emptyList())

    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("Processing image...") }

    // Preview / Outline Dialog state
    var rawCutoutBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayedPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasOutline by remember { mutableStateOf(true) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    // Detailed Sticker View Dialog
    var selectedStickerForView by remember { mutableStateOf<StickerEntity?>(null) }

    // WhatsApp Import state
    var showWhatsAppFolderDialog by remember { mutableStateOf(false) }
    var detectedWhatsAppFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // Generic file picker (SAF / ACTION_OPEN_DOCUMENT) for browsing sticker folders
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            scope.launch {
                isProcessing = true
                processingMessage = "Importing ${uris.size} stickers..."
                val count = stickerRepo.importStickersFromUris(context, uris, "imported")
                isProcessing = false
                Toast.makeText(context, "Successfully imported $count stickers!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Photo Picker launcher (PickVisualMedia) for Generate from Gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                processingMessage = "Extracting subject with ML Kit..."
                try {
                    val loaded = StickerSegmentationHelper.loadAndDownsampleBitmap(context, uri)
                    if (loaded == null) {
                        isProcessing = false
                        Toast.makeText(context, "Unable to load selected photo", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val cutout = StickerSegmentationHelper.removeBackground(loaded)
                    rawCutoutBitmap = cutout
                    hasOutline = true
                    displayedPreviewBitmap = StickerSegmentationHelper.addStickerOutline(cutout, strokeWidthPx = 14)
                    isProcessing = false
                    showPreviewDialog = true
                } catch (e: Exception) {
                    isProcessing = false
                    Toast.makeText(context, "Failed to process photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Function to check WhatsApp folders
    fun checkAndOpenWhatsAppImport() {
        val potentialDirs = listOf(
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers"),
            File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Stickers"),
            File(Environment.getExternalStorageDirectory(), "Android/data/com.whatsapp/files/stickers")
        )

        var foundFiles: List<File> = emptyList()
        for (dir in potentialDirs) {
            if (dir.exists() && dir.canRead()) {
                val files = dir.listFiles { file ->
                    file.isFile && (file.extension.equals("webp", ignoreCase = true) || file.extension.equals("png", ignoreCase = true))
                }?.toList() ?: emptyList()
                if (files.isNotEmpty()) {
                    foundFiles = files
                    break
                }
            }
        }

        detectedWhatsAppFiles = foundFiles
        showWhatsAppFolderDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.toolbarBackground)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_stickers_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.letterKeyTextColor
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sticker Studio",
                    color = colors.letterKeyTextColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stickers.size} saved sticker${if (stickers.size == 1) "" else "s"}",
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.enterKeyBackground.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "KeyPro",
                    color = colors.enterKeyBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Main Content (Action Cards + Stickers Grid)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // Span 1: ACTION A - Import from WhatsApp / other apps
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { checkAndOpenWhatsAppImport() }
                        .testTag("card_action_import_whatsapp"),
                    colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.enterKeyBackground.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = colors.enterKeyBackground,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Import from WhatsApp / other apps",
                                color = colors.letterKeyTextColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Import .webp sticker packs directly from WhatsApp or browse device folders",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Span 2: ACTION B - Generate from Gallery
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("card_action_generate_gallery"),
                    colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF8E24AA).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFBA68C8),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Generate from Gallery",
                                color = colors.letterKeyTextColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Pick any photo, remove background with ML Kit, and add white sticker outline",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Span 3: Section Title: "SAVED STICKERS"
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAVED STICKERS (${stickers.size})",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    if (stickers.isNotEmpty()) {
                        Text(
                            text = "Tap to view • Long-press to delete",
                            color = colors.letterKeySecondaryTextColor.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Empty State if no stickers
            if (stickers.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.enterKeyBackground.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Stickers Yet",
                                color = colors.letterKeyTextColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap 'Generate from Gallery' to turn your photos into stickers, or 'Import from WhatsApp' to add your favorite sticker packs.",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                // Sticker grid items
                items(stickers, key = { it.id }) { sticker ->
                    StickerGridCard(
                        sticker = sticker,
                        colors = colors,
                        onClick = { selectedStickerForView = sticker },
                        onDelete = {
                            scope.launch {
                                stickerRepo.deleteSticker(sticker)
                                Toast.makeText(context, "Sticker deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // Processing Dialog
    if (isProcessing) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.toolbarBackground
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = colors.enterKeyBackground
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Text(
                        text = processingMessage,
                        color = colors.letterKeyTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // WhatsApp Import Dialog
    if (showWhatsAppFolderDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppFolderDialog = false },
            title = {
                Text(
                    text = "Import Stickers",
                    fontWeight = FontWeight.Bold,
                    color = colors.letterKeyTextColor
                )
            },
            text = {
                Column {
                    if (detectedWhatsAppFiles.isNotEmpty()) {
                        Text(
                            text = "Found ${detectedWhatsAppFiles.size} stickers in WhatsApp folder! Tap below to import all into KeyPro.",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "No direct WhatsApp stickers folder was found or accessible (Android Scoped Storage protects messaging app directories).",
                            color = colors.letterKeyTextColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You can easily select sticker files (.webp, .png) or exported sticker folders from any app using the Android file picker.",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (detectedWhatsAppFiles.isNotEmpty()) {
                    Button(
                        onClick = {
                            showWhatsAppFolderDialog = false
                            scope.launch {
                                isProcessing = true
                                processingMessage = "Importing WhatsApp stickers..."
                                val count = stickerRepo.importStickersFromFiles(context, detectedWhatsAppFiles, "whatsapp")
                                isProcessing = false
                                Toast.makeText(context, "Imported $count stickers!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground),
                        modifier = Modifier.testTag("btn_confirm_import_wa")
                    ) {
                        Text("Import All (${detectedWhatsAppFiles.size})", color = colors.enterKeyTextColor)
                    }
                } else {
                    Button(
                        onClick = {
                            showWhatsAppFolderDialog = false
                            openDocumentLauncher.launch(arrayOf("image/webp", "image/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground),
                        modifier = Modifier.testTag("btn_browse_saf_stickers")
                    ) {
                        Text("Browse Files (SAF)", color = colors.enterKeyTextColor)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppFolderDialog = false }) {
                    Text("Cancel", color = colors.letterKeySecondaryTextColor)
                }
            },
            containerColor = colors.toolbarBackground
        )
    }

    // Cutout Preview & Edit Dialog (White Outline Toggle + 512x512 Export)
    if (showPreviewDialog && rawCutoutBitmap != null) {
        Dialog(onDismissRequest = { showPreviewDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.toolbarBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sticker Preview",
                        color = colors.letterKeyTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Background removed • Ready for WhatsApp & chat",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checkerboard background for transparent preview
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF262626))
                            .border(1.dp, colors.functionKeyBackground, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        displayedPreviewBitmap?.let { bmp ->
                            androidx.compose.foundation.Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Cutout Result",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // White sticker outline toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.letterKeyBackground)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "White Sticker Outline",
                                color = colors.letterKeyTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Dilates alpha mask to draw sticker stroke",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = hasOutline,
                            onCheckedChange = { checked ->
                                hasOutline = checked
                                val raw = rawCutoutBitmap ?: return@Switch
                                displayedPreviewBitmap = if (checked) {
                                    StickerSegmentationHelper.addStickerOutline(raw, strokeWidthPx = 14)
                                } else {
                                    raw
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.enterKeyTextColor,
                                checkedTrackColor = colors.enterKeyBackground
                            ),
                            modifier = Modifier.testTag("switch_sticker_outline")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Format badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.functionKeyBackground.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Export Format: 512 × 512 WEBP",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPreviewDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Discard", color = colors.letterKeySecondaryTextColor)
                        }

                        Button(
                            onClick = {
                                val preview = displayedPreviewBitmap ?: return@Button
                                scope.launch {
                                    isProcessing = true
                                    processingMessage = "Exporting 512x512 WebP sticker..."
                                    val final512 = StickerSegmentationHelper.scaleTo512Sticker(preview)
                                    stickerRepo.saveStickerBitmap(context, final512, "My Sticker", "gallery")
                                    isProcessing = false
                                    showPreviewDialog = false
                                    Toast.makeText(context, "Sticker added to keyboard tray!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_save_generated_sticker"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
                        ) {
                            Text("Save Sticker", color = colors.enterKeyTextColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Detail / View Dialog for clicked sticker
    selectedStickerForView?.let { sticker ->
        Dialog(onDismissRequest = { selectedStickerForView = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.toolbarBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sticker.name,
                            color = colors.letterKeyTextColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { selectedStickerForView = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.letterKeySecondaryTextColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF262626)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = File(sticker.filePath),
                            contentDescription = sticker.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Source: ${sticker.source.replaceFirstChar { it.uppercase() }}",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val s = selectedStickerForView ?: return@OutlinedButton
                                selectedStickerForView = null
                                scope.launch {
                                    stickerRepo.deleteSticker(s)
                                    Toast.makeText(context, "Sticker removed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = Color(0xFFEF5350))
                        }

                        Button(
                            onClick = {
                                selectedStickerForView = null
                                Toast.makeText(context, "Available in KeyPro Keyboard sticker tray!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
                        ) {
                            Text("Done", color = colors.enterKeyTextColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerGridCard(
    sticker: StickerEntity,
    colors: KeyboardColors,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, colors.functionKeyBackground.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .testTag("sticker_card_${sticker.id}"),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = File(sticker.filePath),
                contentDescription = sticker.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Delete action button on top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.background.copy(alpha = 0.7f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete sticker",
                    tint = colors.letterKeySecondaryTextColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
