package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.screens.ClipboardHistoryPageView
import com.example.ui.screens.SettingsPageView
import com.example.ui.screens.TestKeyboardPageView

enum class MainScreenSection {
    HUB,
    TEST_KEYBOARD,
    SETTINGS_PAGE,
    CLIPBOARD_HISTORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current

    val editorValue by viewModel.editorValue.collectAsState()
    val allSnippets by viewModel.allSnippets.collectAsState()
    val pinnedSnippets by viewModel.pinnedSnippets.collectAsState()
    val historySnippets by viewModel.historySnippets.collectAsState()
    val currentThemeType by viewModel.themeType.collectAsState()
    val isLaptopBarVisible by viewModel.isLaptopBarVisible.collectAsState()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()
    val isHoldForSymbolsEnabled by viewModel.isHoldForSymbolsEnabled.collectAsState()
    val lastActionStatus by viewModel.lastActionStatus.collectAsState()

    val colors = viewModel.currentColors

    var currentSection by remember { mutableStateOf(MainScreenSection.HUB) }
    var showSetupDropdown by remember { mutableStateOf(false) }

    fun openImeSettings() {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openImePicker() {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        } catch (_: Exception) {}
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            when (currentSection) {
                MainScreenSection.HUB -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background)
                    ) {
                        // Top App Bar with Setup dropdown
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(colors.toolbarBackground)
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.enterKeyBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Keyboard,
                                        contentDescription = "App Icon",
                                        tint = colors.enterKeyTextColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "KeyPro Keyboard",
                                        color = colors.letterKeyTextColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${currentThemeType.displayName} • Gboard Standard",
                                        color = colors.letterKeySecondaryTextColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Setup Dropdown Menu
                            Box {
                                FilledTonalButton(
                                    onClick = { showSetupDropdown = true },
                                    modifier = Modifier.testTag("btn_setup_keyboard"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = colors.enterKeyBackground,
                                        contentColor = colors.enterKeyTextColor
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Setup",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Setup Options",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showSetupDropdown,
                                    onDismissRequest = { showSetupDropdown = false },
                                    modifier = Modifier
                                        .background(colors.toolbarBackground)
                                        .testTag("menu_setup_dropdown")
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "1. Enable in Settings",
                                                    color = colors.letterKeyTextColor,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "Turn on KeyPro in Android settings",
                                                    color = colors.letterKeySecondaryTextColor,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            showSetupDropdown = false
                                            openImeSettings()
                                        },
                                        modifier = Modifier.testTag("item_enable_in_settings")
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "2. Select KeyPro Keyboard",
                                                    color = colors.letterKeyTextColor,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "Set KeyPro as current input method",
                                                    color = colors.letterKeySecondaryTextColor,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            showSetupDropdown = false
                                            openImePicker()
                                        },
                                        modifier = Modifier.testTag("item_select_keyboard")
                                    )
                                }
                            }
                        }

                        // Hub Cards Content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // CARD A: TEST KEYBOARD
                            HubNavigationCard(
                                icon = Icons.Default.Keyboard,
                                title = "Test Keyboard",
                                description = "Test standard Gboard typing layout, hold-to-delete backspace, PC keys (Ctrl, Alt, Tab, Esc), emojis, and the quick paste button.",
                                colors = colors,
                                testTag = "card_test_keyboard",
                                onClick = { currentSection = MainScreenSection.TEST_KEYBOARD }
                            )

                            // CARD B: SETTINGS PAGE
                            HubNavigationCard(
                                icon = Icons.Default.Settings,
                                title = "Settings Page",
                                description = "Customize keyboard color themes, typing audio sounds, haptic feedback vibration, and laptop quick access bar.",
                                colors = colors,
                                testTag = "card_settings_page",
                                onClick = { currentSection = MainScreenSection.SETTINGS_PAGE }
                            )

                            // CARD C: SAVED CLIPBOARD HISTORY
                            HubNavigationCard(
                                icon = Icons.Default.ContentPaste,
                                title = "Saved Clipboard History",
                                description = "Browse auto-saved clipboard clips, search frequently used texts, and manage your pinned quick snippets.",
                                colors = colors,
                                testTag = "card_saved_clipboard_history",
                                onClick = { currentSection = MainScreenSection.CLIPBOARD_HISTORY }
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                MainScreenSection.TEST_KEYBOARD -> {
                    TestKeyboardPageView(
                        editorValue = editorValue,
                        onEditorValueChange = { viewModel.updateEditorValue(it) },
                        colors = colors,
                        allSnippets = allSnippets,
                        pinnedSnippets = pinnedSnippets,
                        historySnippets = historySnippets,
                        isSoundEnabled = isSoundEnabled,
                        isHapticEnabled = isHapticEnabled,
                        isLaptopBarVisible = isLaptopBarVisible,
                        lastActionStatus = lastActionStatus ?: "",
                        onAction = { viewModel.handleKeyAction(it) },
                        onTogglePin = { viewModel.toggleSnippetPin(it) },
                        onDeleteSnippet = { viewModel.deleteSnippet(it) },
                        onSaveSnippet = { title, content, isPinned, category, shortcut ->
                            viewModel.insertSnippet(title, content, isPinned, category, shortcut)
                        },
                        onClearHistory = { viewModel.clearUnpinnedHistory() },
                        onClearEditor = { viewModel.clearEditor() },
                        onBack = { currentSection = MainScreenSection.HUB }
                    )
                }

                MainScreenSection.SETTINGS_PAGE -> {
                    SettingsPageView(
                        colors = colors,
                        currentThemeType = currentThemeType,
                        customColors = viewModel.keyboardPrefs.getCustomKeyboardColors(),
                        isSoundEnabled = isSoundEnabled,
                        isHapticEnabled = isHapticEnabled,
                        isLaptopBarEnabled = isLaptopBarVisible,
                        isHoldForSymbolsEnabled = isHoldForSymbolsEnabled,
                        onBack = { currentSection = MainScreenSection.HUB },
                        onThemeSelected = { viewModel.setTheme(it) },
                        onToggleSound = { viewModel.setSoundEnabled(it) },
                        onToggleHaptic = { viewModel.setHapticEnabled(it) },
                        onToggleLaptopBar = { viewModel.setLaptopBarVisible(it) },
                        onToggleHoldForSymbols = { viewModel.setHoldForSymbolsEnabled(it) },
                        onCustomColorsChanged = { bg, keyBg, text, accent ->
                            viewModel.keyboardPrefs.setCustomThemeColors(bg, keyBg, text, accent)
                        }
                    )
                }

                MainScreenSection.CLIPBOARD_HISTORY -> {
                    ClipboardHistoryPageView(
                        pinnedSnippets = pinnedSnippets,
                        historySnippets = historySnippets,
                        colors = colors,
                        onBack = { currentSection = MainScreenSection.HUB },
                        onTogglePin = { viewModel.toggleSnippetPin(it) },
                        onDeleteSnippet = { viewModel.deleteSnippet(it) },
                        onSaveSnippet = { title, content, isPinned, category, shortcut ->
                            viewModel.insertSnippet(title, content, isPinned, category, shortcut)
                        },
                        onClearHistory = { viewModel.clearUnpinnedHistory() }
                    )
                }
            }
        }
    }
}

@Composable
private fun HubNavigationCard(
    icon: ImageVector,
    title: String,
    description: String,
    colors: KeyboardColors,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.enterKeyBackground.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.enterKeyBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = colors.letterKeyTextColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                color = colors.letterKeySecondaryTextColor,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
