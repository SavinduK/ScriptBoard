package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.model.LanguagePack
import com.example.ui.keyboard.model.LanguagePackDownloader
import com.example.ui.keyboard.model.LanguagePackRegistry
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.launch

@Composable
fun LanguagesScreen(
    colors: KeyboardColors,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardPrefs = remember { KeyboardPreferences.getInstance(context) }
    val downloader = remember { LanguagePackDownloader(context) }

    val installedLangIds by keyboardPrefs.installedLanguages.collectAsState()
    val activeLangId by keyboardPrefs.currentLanguage.collectAsState()
    val githubOwner by keyboardPrefs.githubRepoOwner.collectAsState()
    val githubRepo by keyboardPrefs.githubRepoName.collectAsState()
    val githubBranch by keyboardPrefs.githubRepoBranch.collectAsState()

    var downloadingPackId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }
    var showRepoConfigDialog by remember { mutableStateOf(false) }

    fun startDownload(pack: LanguagePack) {
        scope.launch {
            downloadingPackId = pack.id
            downloadProgress = 0f
            downloadStatusText = "Connecting to GitHub..."

            val result = downloader.downloadLanguagePack(pack) { progress, status ->
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
                Toast.makeText(context, "${pack.name} installed from $sourceLabel!", Toast.LENGTH_LONG).show()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown error"
                Toast.makeText(context, "Download failed: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    val installedPacks = LanguagePackRegistry.ALL_PACKAGES.filter {
        it.isBuiltIn || installedLangIds.contains(it.id)
    }

    val availablePacks = LanguagePackRegistry.ALL_PACKAGES.filter {
        !it.isBuiltIn && !installedLangIds.contains(it.id)
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
                modifier = Modifier.testTag("btn_languages_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.letterKeyTextColor
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "Language Packages",
                    color = colors.letterKeyTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "GitHub remote repository & offline language packs",
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: GitHub Remote Repository Source Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("card_github_repo_source"),
                    colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.enterKeyBackground.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = colors.enterKeyBackground,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "GitHub Remote Storage",
                                        color = colors.letterKeyTextColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$githubOwner/$githubRepo ($githubBranch)",
                                        color = colors.letterKeySecondaryTextColor,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showRepoConfigDialog = true },
                                modifier = Modifier.testTag("btn_configure_github_repo")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Configure GitHub Repo",
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.functionKeyBackground.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = colors.letterKeySecondaryTextColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Repo Folder: /languages/ (si.json, ta.json, es.json, hi.json)",
                                        color = colors.letterKeyTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Raw URL: https://raw.githubusercontent.com/$githubOwner/$githubRepo/$githubBranch/languages/<pack>.json",
                                    color = colors.letterKeySecondaryTextColor,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Active / Installed Languages Section
            item {
                Text(
                    text = "INSTALLED LANGUAGES (${installedPacks.size})",
                    color = colors.enterKeyBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            items(installedPacks, key = { it.id }) { pack ->
                val isActive = activeLangId == pack.id
                val isDownloadedFilePresent = downloader.isPackageDownloaded(pack.id)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { keyboardPrefs.setCurrentLanguage(pack.id) }
                        .testTag("card_installed_lang_${pack.id}"),
                    colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            RadioButton(
                                selected = isActive,
                                onClick = { keyboardPrefs.setCurrentLanguage(pack.id) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.enterKeyBackground,
                                    unselectedColor = colors.letterKeySecondaryTextColor
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = pack.flag,
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${pack.name} (${pack.nativeName})",
                                        color = colors.letterKeyTextColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(colors.enterKeyBackground)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = colors.enterKeyTextColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (pack.isBuiltIn) {
                                        "Built-in English QWERTY"
                                    } else if (isDownloadedFilePresent) {
                                        "Installed from GitHub (${pack.id}.json in storage)"
                                    } else {
                                        "Installed package (${pack.sizeDisplay})"
                                    },
                                    color = colors.letterKeySecondaryTextColor,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (!pack.isBuiltIn) {
                            IconButton(
                                onClick = {
                                    downloader.deletePackageFile(pack.id)
                                    keyboardPrefs.uninstallLanguage(pack.id)
                                    Toast.makeText(context, "${pack.name} uninstalled", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("btn_delete_${pack.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Uninstall ${pack.name}",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Available to Download Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE TO DOWNLOAD (${availablePacks.size})",
                        color = colors.enterKeyBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "From GitHub Repo",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 11.sp
                    )
                }
            }

            if (availablePacks.isEmpty()) {
                item {
                    Text(
                        text = "All language packages are installed!",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 13.sp
                    )
                }
            } else {
                items(availablePacks, key = { it.id }) { pack ->
                    val isDownloading = downloadingPackId == pack.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("card_download_lang_${pack.id}"),
                        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pack.flag,
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "${pack.name} (${pack.nativeName})",
                                            color = colors.letterKeyTextColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "GitHub file: languages/${pack.id}.json (${pack.sizeDisplay})",
                                            color = colors.letterKeySecondaryTextColor,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                if (!isDownloading) {
                                    Button(
                                        onClick = { startDownload(pack) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground),
                                        modifier = Modifier.testTag("btn_download_${pack.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Download",
                                            color = colors.enterKeyTextColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pack.description,
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            if (isDownloading) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = colors.enterKeyBackground,
                                    trackColor = colors.functionKeyBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = downloadStatusText.ifEmpty { "Downloading from GitHub... ${(downloadProgress * 100).toInt()}%" },
                                    color = colors.enterKeyBackground,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Configure GitHub Repository details
    if (showRepoConfigDialog) {
        var tempOwner by remember { mutableStateOf(githubOwner) }
        var tempRepo by remember { mutableStateOf(githubRepo) }
        var tempBranch by remember { mutableStateOf(githubBranch) }

        AlertDialog(
            onDismissRequest = { showRepoConfigDialog = false },
            title = {
                Text(
                    text = "Configure GitHub Repository",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Specify the GitHub repository where your language files (/languages/*.json) are hosted:",
                        fontSize = 12.sp,
                        color = colors.letterKeySecondaryTextColor
                    )
                    OutlinedTextField(
                        value = tempOwner,
                        onValueChange = { tempOwner = it },
                        label = { Text("GitHub Username / Owner") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempRepo,
                        onValueChange = { tempRepo = it },
                        label = { Text("Repository Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempBranch,
                        onValueChange = { tempBranch = it },
                        label = { Text("Branch (default: main)") },
                        singleLine = true,
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
                    Text("Cancel")
                }
            }
        )
    }
}
