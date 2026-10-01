package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.keyboard.model.EmojiPack
import com.example.ui.keyboard.model.EmojiPackDownloader
import com.example.ui.keyboard.model.EmojiPackItem
import com.example.ui.keyboard.model.EmojiPackRegistry
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.launch

@Composable
fun EmojiPacksScreen(
    colors: KeyboardColors,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardPrefs = remember { KeyboardPreferences.getInstance(context) }
    val downloader = remember { EmojiPackDownloader(context) }

    val installedPackIds by keyboardPrefs.installedEmojiPacks.collectAsState()
    val githubOwner by keyboardPrefs.githubRepoOwner.collectAsState()
    val githubRepo by keyboardPrefs.githubRepoName.collectAsState()
    val githubBranch by keyboardPrefs.githubRepoBranch.collectAsState()

    var downloadingPackId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }
    var showRepoConfigDialog by remember { mutableStateOf(false) }
    var previewPack by remember { mutableStateOf<EmojiPack?>(null) }
    var previewItems by remember { mutableStateOf<List<EmojiPackItem>>(emptyList()) }

    fun startDownload(pack: EmojiPack) {
        scope.launch {
            downloadingPackId = pack.id
            downloadProgress = 0f
            downloadStatusText = "Connecting to GitHub..."

            val result = downloader.downloadEmojiPack(pack) { progress, status ->
                downloadProgress = progress
                downloadStatusText = status
            }

            downloadingPackId = null
            if (result.isSuccess) {
                val data = result.getOrNull()
                val sourceLabel = if (data?.downloadedFromRemote == true) {
                    "GitHub ($githubOwner/$githubRepo)"
                } else {
                    "Repository Package Bundle"
                }
                Toast.makeText(context, "${pack.name} added to emoji drawer from $sourceLabel!", Toast.LENGTH_LONG).show()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown error"
                Toast.makeText(context, "Download failed: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    val installedPacks = EmojiPackRegistry.ALL_PACKS.filter {
        installedPackIds.contains(it.id)
    }

    val availablePacks = EmojiPackRegistry.ALL_PACKS.filter {
        !installedPackIds.contains(it.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.toolbarBackground)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_emoji_packs_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.letterKeyTextColor
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Emoji & Symbol Packages",
                    color = colors.letterKeyTextColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Download Runic, Hieroglyphs & Kaomoji",
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { showRepoConfigDialog = true },
                modifier = Modifier.testTag("btn_emoji_repo_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Configure GitHub Repo",
                    tint = colors.enterKeyBackground
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info Card: GitHub Repository connection info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.functionKeyBackground.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = colors.enterKeyBackground,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GitHub Repository Remote Storage",
                                color = colors.letterKeyTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Emoji and symbol packages are stored in your GitHub repository under emojis/ and downloaded on demand to save app storage space. If offline, local bundled package backups are used.",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.letterKeyBackground)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Repository: $githubOwner/$githubRepo ($githubBranch)",
                                    color = colors.letterKeyTextColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Endpoint: https://raw.githubusercontent.com/.../emojis/{id}.json",
                                    color = colors.letterKeySecondaryTextColor,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Section: Installed Emoji Packs
            if (installedPacks.isNotEmpty()) {
                item {
                    Text(
                        text = "INSTALLED PACKAGES IN EMOJI SECTION (${installedPacks.size})",
                        color = colors.enterKeyBackground,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                items(installedPacks, key = { it.id }) { pack ->
                    EmojiPackCard(
                        pack = pack,
                        isInstalled = true,
                        isDownloading = downloadingPackId == pack.id,
                        downloadProgress = downloadProgress,
                        downloadStatusText = downloadStatusText,
                        colors = colors,
                        onDownload = { startDownload(pack) },
                        onUninstall = {
                            keyboardPrefs.uninstallEmojiPack(pack.id)
                            downloader.deletePack(pack.id)
                            Toast.makeText(context, "${pack.name} removed from emoji drawer", Toast.LENGTH_SHORT).show()
                        },
                        onPreview = {
                            val items = downloader.loadPackItems(pack.id)
                            previewPack = pack
                            previewItems = items
                        }
                    )
                }
            }

            // Section: Available for Download
            item {
                Text(
                    text = "AVAILABLE TO DOWNLOAD (${availablePacks.size})",
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            if (availablePacks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All emoji packages are installed!",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(availablePacks, key = { it.id }) { pack ->
                    EmojiPackCard(
                        pack = pack,
                        isInstalled = false,
                        isDownloading = downloadingPackId == pack.id,
                        downloadProgress = downloadProgress,
                        downloadStatusText = downloadStatusText,
                        colors = colors,
                        onDownload = { startDownload(pack) },
                        onUninstall = {},
                        onPreview = {
                            val items = downloader.loadPackItems(pack.id)
                            previewPack = pack
                            previewItems = items
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Symbol Preview Dialog
    previewPack?.let { pack ->
        AlertDialog(
            onDismissRequest = { previewPack = null },
            icon = {
                Text(text = pack.icon, fontSize = 28.sp)
            },
            title = {
                Text(
                    text = "${pack.name} (${pack.itemCount} items)",
                    fontWeight = FontWeight.Bold,
                    color = colors.letterKeyTextColor,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = pack.description,
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (previewItems.isEmpty()) {
                        Text(
                            text = "Loading preview glyphs...",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 12.sp
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 56.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(previewItems) { item ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.letterKeyBackground)
                                        .border(0.5.dp, colors.functionKeyBackground, RoundedCornerShape(8.dp))
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = item.symbol,
                                            fontSize = if (item.symbol.length > 2) 11.sp else 18.sp,
                                            color = colors.letterKeyTextColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (item.name.isNotBlank()) {
                                            Text(
                                                text = item.name,
                                                fontSize = 8.sp,
                                                color = colors.letterKeySecondaryTextColor,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewPack = null }) {
                    Text("Close", color = colors.enterKeyBackground, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.toolbarBackground
        )
    }

    // GitHub Repo Configuration Dialog
    if (showRepoConfigDialog) {
        var tempOwner by remember { mutableStateOf(githubOwner) }
        var tempRepo by remember { mutableStateOf(githubRepo) }
        var tempBranch by remember { mutableStateOf(githubBranch) }

        AlertDialog(
            onDismissRequest = { showRepoConfigDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = colors.enterKeyBackground
                )
            },
            title = {
                Text(
                    text = "Configure GitHub Remote Repo",
                    fontWeight = FontWeight.Bold,
                    color = colors.letterKeyTextColor,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Specify your GitHub repository where emojis/ and languages/ are hosted.",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = tempOwner,
                        onValueChange = { tempOwner = it },
                        label = { Text("Owner / Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempRepo,
                        onValueChange = { tempRepo = it },
                        label = { Text("Repository Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempBranch,
                        onValueChange = { tempBranch = it },
                        label = { Text("Branch (default: main)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardPrefs.setGithubRepoConfig(tempOwner, tempRepo, tempBranch)
                        showRepoConfigDialog = false
                        Toast.makeText(context, "GitHub repository configuration saved!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
                ) {
                    Text("Save", color = colors.enterKeyTextColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepoConfigDialog = false }) {
                    Text("Cancel", color = colors.letterKeySecondaryTextColor)
                }
            },
            containerColor = colors.toolbarBackground
        )
    }
}

@Composable
private fun EmojiPackCard(
    pack: EmojiPack,
    isInstalled: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadStatusText: String,
    colors: KeyboardColors,
    onDownload: () -> Unit,
    onUninstall: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_emoji_pack_${pack.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.functionKeyBackground)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.enterKeyBackground.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pack.icon,
                            fontSize = if (pack.icon.length > 2) 13.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.letterKeyTextColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pack.name,
                                color = colors.letterKeyTextColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.enterKeyBackground.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = pack.category,
                                    color = colors.enterKeyBackground,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "${pack.nativeName} • ${pack.itemCount} items • ${pack.sizeDisplay}",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 11.sp
                        )
                    }
                }

                // Preview Button
                IconButton(
                    onClick = onPreview,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_preview_${pack.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Preview symbols",
                        tint = colors.enterKeyBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = pack.description,
                color = colors.letterKeySecondaryTextColor,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // Live Download Progress Bar
            AnimatedVisibility(visible = isDownloading) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = downloadStatusText.ifEmpty { "Downloading from GitHub..." },
                            color = colors.enterKeyBackground,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            color = colors.enterKeyBackground,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = colors.enterKeyBackground,
                        trackColor = colors.letterKeyBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInstalled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "In Emoji Drawer",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = onUninstall,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEF5350)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_uninstall_emoji_${pack.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", fontSize = 11.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = onDownload,
                        enabled = !isDownloading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.enterKeyBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_download_emoji_${pack.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = colors.enterKeyTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDownloading) "Downloading..." else "Download to Emoji Drawer",
                            color = colors.enterKeyTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
