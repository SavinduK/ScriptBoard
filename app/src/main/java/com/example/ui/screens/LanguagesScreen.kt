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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.model.LanguagePack
import com.example.ui.keyboard.model.LanguagePackRegistry
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LanguagesScreen(
    colors: KeyboardColors,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardPrefs = remember { KeyboardPreferences.getInstance(context) }

    val installedLangIds by keyboardPrefs.installedLanguages.collectAsState()
    val activeLangId by keyboardPrefs.currentLanguage.collectAsState()

    var downloadingPackId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    fun startDownload(pack: LanguagePack) {
        scope.launch {
            downloadingPackId = pack.id
            downloadProgress = 0f
            // Realistic download & extraction simulation
            for (step in 1..10) {
                delay(120)
                downloadProgress = step / 10f
            }
            keyboardPrefs.installLanguage(pack.id)
            keyboardPrefs.setCurrentLanguage(pack.id)
            downloadingPackId = null
            Toast.makeText(context, "${pack.name} package installed and activated!", Toast.LENGTH_SHORT).show()
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
                    text = "Download layouts for Sinhala and other languages",
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
                                colors = RadioButtonDefaults.colors(selectedColor = colors.enterKeyBackground)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${pack.flag}  ${pack.nativeName}",
                                        color = colors.letterKeyTextColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.enterKeyBackground.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = colors.enterKeyBackground,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = pack.description,
                                    color = colors.letterKeySecondaryTextColor,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (!pack.isBuiltIn) {
                            IconButton(
                                onClick = { keyboardPrefs.uninstallLanguage(pack.id) },
                                modifier = Modifier.testTag("btn_uninstall_${pack.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Uninstall",
                                    tint = Color(0xFFEF5350).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Available Language Packages Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DOWNLOADABLE PACKAGES",
                    color = colors.enterKeyBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
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
                                            text = "Size: ${pack.sizeDisplay}",
                                            color = colors.letterKeySecondaryTextColor,
                                            fontSize = 11.sp
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
                                    text = "Downloading & installing package... ${(downloadProgress * 100).toInt()}%",
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
}
