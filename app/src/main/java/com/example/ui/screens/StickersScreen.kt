package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.StickerEntity
import com.example.data.StickerRepository
import com.example.ui.keyboard.model.KeyboardColors
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
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
    var processingMessage by remember { mutableStateOf("Processing...") }

    // Detailed Sticker View Dialog
    var selectedStickerForView by remember { mutableStateOf<StickerEntity?>(null) }
    // Long hold delete confirmation
    var stickerToDelete by remember { mutableStateOf<StickerEntity?>(null) }

    var selectedStickerTab by remember { mutableStateOf(0) } // 0 = Normal, 1 = Animated

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

    // ZIP Backup launcher using SAF CreateDocument
    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                isProcessing = true
                processingMessage = "Creating stickers ZIP backup..."
                try {
                    var backedUpCount = 0
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(outputStream)).use { zipOut ->
                            val addedNames = mutableSetOf<String>()
                            stickers.forEachIndexed { index, sticker ->
                                val file = File(sticker.filePath)
                                if (file.exists() && file.isFile) {
                                    val ext = file.extension.ifEmpty { "webp" }
                                    var entryName = "${sticker.name.replace(" ", "_")}_$index.$ext"
                                    if (addedNames.contains(entryName)) {
                                        entryName = "sticker_${sticker.id}_$index.$ext"
                                    }
                                    addedNames.add(entryName)
                                    zipOut.putNextEntry(java.util.zip.ZipEntry(entryName))
                                    file.inputStream().use { it.copyTo(zipOut) }
                                    zipOut.closeEntry()
                                    backedUpCount++
                                }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        Toast.makeText(context, "Successfully backed up $backedUpCount stickers as ZIP!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top App Bar (Note: NO search bar per user request)
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
                    text = "Stickers",
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

        val normalStickers = remember(stickers) { stickers.filter { !it.isAnimated } }
        val animatedStickers = remember(stickers) { stickers.filter { it.isAnimated } }
        val displayedStickers = if (selectedStickerTab == 0) normalStickers else animatedStickers

        // Main Content (Import Action Card + Stickers Grid)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // ACTION: Import from WhatsApp / other apps
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("card_action_import_whatsapp"),
                    colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    text = "Import Stickers",
                                    color = colors.letterKeyTextColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Import .webp sticker packs from WhatsApp or pick files from your device folders",
                                    color = colors.letterKeySecondaryTextColor,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { openDocumentLauncher.launch(arrayOf("image/webp", "image/png", "image/gif", "image/*", "*/*")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_browse_saf_files"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.enterKeyTextColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Folder", color = colors.enterKeyTextColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (stickers.isEmpty()) {
                                        Toast.makeText(context, "No stickers to backup yet!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        createZipLauncher.launch("stickers_backup_${System.currentTimeMillis()}.zip")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_backup_stickers_zip"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.letterKeyTextColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup as ZIP", color = colors.letterKeyTextColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Horizontal Tab Bar: Normal Stickers vs Animated Stickers
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedStickerTab,
                        containerColor = colors.toolbarBackground,
                        contentColor = colors.enterKeyBackground,
                        indicator = { tabPositions ->
                            if (selectedStickerTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedStickerTab]),
                                    color = colors.enterKeyBackground,
                                    height = 3.dp
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedStickerTab == 0,
                            onClick = { selectedStickerTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedStickerTab == 0) colors.enterKeyBackground else colors.letterKeySecondaryTextColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Normal Stickers (${normalStickers.size})",
                                        color = if (selectedStickerTab == 0) colors.enterKeyBackground else colors.letterKeySecondaryTextColor,
                                        fontWeight = if (selectedStickerTab == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.5.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_normal_stickers")
                        )

                        Tab(
                            selected = selectedStickerTab == 1,
                            onClick = { selectedStickerTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedStickerTab == 1) colors.enterKeyBackground else colors.letterKeySecondaryTextColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Animated Stickers (${animatedStickers.size})",
                                        color = if (selectedStickerTab == 1) colors.enterKeyBackground else colors.letterKeySecondaryTextColor,
                                        fontWeight = if (selectedStickerTab == 1) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.5.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_animated_stickers")
                        )
                    }
                }
            }

            // Empty State if no stickers in selected tab
            if (displayedStickers.isEmpty()) {
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
                                    imageVector = if (selectedStickerTab == 0) Icons.Default.Image else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (selectedStickerTab == 0) "No Normal Stickers Yet" else "No Animated Stickers Yet",
                                color = colors.letterKeyTextColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedStickerTab == 0)
                                    "Tap 'Open Folder' above to import static .webp, .png, or .jpg images from your device."
                                else
                                    "Tap 'Open Folder' above to import animated .webp or .gif stickers from your device.",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                // Sticker grid items (NO delete button on card; delete ONLY via long-hold)
                items(displayedStickers, key = { it.id }) { sticker ->
                    StickerGridCard(
                        sticker = sticker,
                        colors = colors,
                        onClick = { selectedStickerForView = sticker },
                        onLongClick = { stickerToDelete = sticker }
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



    // Long Hold Delete Confirmation Dialog (Requirement: "To delete stickers only use the long hold menu(remove the on sticker delete button)")
    stickerToDelete?.let { sticker ->
        AlertDialog(
            onDismissRequest = { stickerToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF5350)
                )
            },
            title = {
                Text(
                    text = "Delete Sticker?",
                    fontWeight = FontWeight.Bold,
                    color = colors.letterKeyTextColor
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${sticker.name}\"? This action cannot be undone.",
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = stickerToDelete
                        stickerToDelete = null
                        if (s != null) {
                            scope.launch {
                                stickerRepo.deleteSticker(s)
                                Toast.makeText(context, "Sticker deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_sticker")
                ) {
                    Text("Delete", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { stickerToDelete = null }) {
                    Text("Cancel", color = colors.letterKeySecondaryTextColor)
                }
            },
            containerColor = colors.toolbarBackground
        )
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Source: ${sticker.source.replaceFirstChar { it.uppercase() }}",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (sticker.isAnimated) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.enterKeyBackground.copy(alpha = 0.9f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ANIMATED .WEBP",
                                        color = colors.enterKeyTextColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (sticker.usageCount > 0) {
                                Text(
                                    text = "Used: ${sticker.usageCount}",
                                    color = colors.enterKeyBackground,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val s = selectedStickerForView
                                selectedStickerForView = null
                                stickerToDelete = s
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
                                Toast.makeText(context, "Available in KeyPro Keyboard emoji/sticker section!", Toast.LENGTH_SHORT).show()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StickerGridCard(
    sticker: StickerEntity,
    colors: KeyboardColors,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Note: NO delete button on the card! Only long hold menu deletes.
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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

            if (sticker.isAnimated) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.enterKeyBackground.copy(alpha = 0.85f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ANIM",
                        color = colors.enterKeyTextColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
