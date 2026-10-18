import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  CustomThemeConfig,
  KeyActionType,
  KeyboardThemeType,
  MainScreenSection,
  SnippetEntity,
  StickerEntity,
  WordFrequencyEntity,
} from './types';
import { DefaultCustomThemeConfig, getTheme } from './data/themes';
import { initialSnippets, initialStickers } from './data/initialData';
import { HubScreen } from './components/screens/HubScreen';
import { TestKeyboardScreen } from './components/screens/TestKeyboardScreen';
import { SettingsScreen } from './components/screens/SettingsScreen';
import { ClipboardHistoryScreen } from './components/screens/ClipboardHistoryScreen';
import { StickersScreen } from './components/screens/StickersScreen';
import { LanguagesScreen } from './components/screens/LanguagesScreen';
import { EmojiPacksScreen } from './components/screens/EmojiPacksScreen';

export function App() {
  // Navigation Section
  const [currentSection, setCurrentSection] = useState<MainScreenSection>('HUB');

  // Text Editor Value & History Stack
  const [editorText, setEditorText] = useState<string>(
    'Welcome to KeyPro Keyboard! 🚀\nTap keys below to test typing, PC shortcuts (Ctrl, Alt, Tab, Esc), emojis, and pinned snippets.'
  );
  const [historyStack, setHistoryStack] = useState<string[]>([editorText]);
  const [historyIndex, setHistoryIndex] = useState<number>(0);
  const [lastActionStatus, setLastActionStatus] = useState<string>('Ready to type');

  // Keyboard Preferences
  const [themeType, setThemeType] = useState<KeyboardThemeType>(() => {
    return (localStorage.getItem('keypro_theme') as KeyboardThemeType) || 'GBOARD_DARK';
  });

  const [customThemeConfig, setCustomThemeConfig] = useState<CustomThemeConfig>(() => {
    const saved = localStorage.getItem('keypro_custom_theme');
    return saved ? JSON.parse(saved) : DefaultCustomThemeConfig;
  });

  const [isSoundEnabled, setIsSoundEnabled] = useState<boolean>(() => {
    return localStorage.getItem('keypro_sound') !== 'false';
  });

  const [isHapticEnabled, setIsHapticEnabled] = useState<boolean>(() => {
    return localStorage.getItem('keypro_haptic') !== 'false';
  });

  const [isLaptopBarVisible, setIsLaptopBarVisible] = useState<boolean>(() => {
    return localStorage.getItem('keypro_laptop_bar') !== 'false';
  });

  const [isHoldForSymbolsEnabled, setIsHoldForSymbolsEnabled] = useState<boolean>(() => {
    return localStorage.getItem('keypro_hold_symbols') !== 'false';
  });

  const [isAutoSuggestEnabled, setIsAutoSuggestEnabled] = useState<boolean>(() => {
    return localStorage.getItem('keypro_auto_suggest') !== 'false';
  });

  const [keyFontSize, setKeyFontSize] = useState<number>(() => {
    const saved = localStorage.getItem('keypro_key_font_size');
    return saved ? parseFloat(saved) : 24;
  });

  const [currentLanguage, setCurrentLanguage] = useState<string>(() => {
    return localStorage.getItem('keypro_language') || 'en';
  });

  const [installedLanguages, setInstalledLanguages] = useState<string[]>(() => {
    const saved = localStorage.getItem('keypro_installed_langs');
    if (saved) {
      try {
        const parsed: string[] = JSON.parse(saved);
        const filtered = parsed.filter((id) => id === 'en' || id === 'si');
        return filtered.length > 0 ? filtered : ['en', 'si'];
      } catch {
        return ['en', 'si'];
      }
    }
    return ['en', 'si'];
  });

  const [installedEmojiPacks, setInstalledEmojiPacks] = useState<string[]>(() => {
    const saved = localStorage.getItem('keypro_installed_packs');
    return saved ? JSON.parse(saved) : ['runic', 'kaomoji'];
  });

  const [recentEmojis, setRecentEmojis] = useState<string[]>(() => {
    const saved = localStorage.getItem('keypro_recent_emojis');
    return saved ? JSON.parse(saved) : ['😊', '👍', '❤️', '🚀', '✨', '🔥', '🎉', '💻'];
  });

  // GitHub Repo Configuration for Languages & Emojis
  const [githubOwner, setGithubOwner] = useState<string>(() => {
    return localStorage.getItem('keypro_gh_owner') || 'savindukahandagamage2';
  });
  const [githubRepo, setGithubRepo] = useState<string>(() => {
    return localStorage.getItem('keypro_gh_repo') || 'KeyPro';
  });
  const [githubBranch, setGithubBranch] = useState<string>(() => {
    return localStorage.getItem('keypro_gh_branch') || 'main';
  });

  // Snippets Data
  const [allSnippets, setAllSnippets] = useState<SnippetEntity[]>(() => {
    const saved = localStorage.getItem('keypro_snippets');
    return saved ? JSON.parse(saved) : initialSnippets;
  });

  // Stickers Data
  const [stickers, setStickers] = useState<StickerEntity[]>(() => {
    const saved = localStorage.getItem('keypro_stickers');
    return saved ? JSON.parse(saved) : initialStickers;
  });

  // Word Frequencies for Smart Suggestion Ranking
  const [wordFrequencies, setWordFrequencies] = useState<WordFrequencyEntity[]>(() => {
    const saved = localStorage.getItem('keypro_word_freq');
    return saved
      ? JSON.parse(saved)
      : [
          { word: 'the', frequency: 28 },
          { word: 'thanks', frequency: 24 },
          { word: 'hello', frequency: 19 },
          { word: 'yes', frequency: 15 },
          { word: 'please', frequency: 12 },
          { word: 'ආයුබෝවන්', frequency: 25 },
          { word: 'ස්තූතියි', frequency: 22 },
          { word: 'හොඳයි', frequency: 18 },
          { word: 'කොහොමද', frequency: 14 },
          { word: 'සුභ', frequency: 12 },
        ];
  });

  // Persist State Changes to LocalStorage
  useEffect(() => {
    localStorage.setItem('keypro_theme', themeType);
  }, [themeType]);

  useEffect(() => {
    localStorage.setItem('keypro_custom_theme', JSON.stringify(customThemeConfig));
  }, [customThemeConfig]);

  useEffect(() => {
    localStorage.setItem('keypro_sound', String(isSoundEnabled));
  }, [isSoundEnabled]);

  useEffect(() => {
    localStorage.setItem('keypro_haptic', String(isHapticEnabled));
  }, [isHapticEnabled]);

  useEffect(() => {
    localStorage.setItem('keypro_laptop_bar', String(isLaptopBarVisible));
  }, [isLaptopBarVisible]);

  useEffect(() => {
    localStorage.setItem('keypro_hold_symbols', String(isHoldForSymbolsEnabled));
  }, [isHoldForSymbolsEnabled]);

  useEffect(() => {
    localStorage.setItem('keypro_auto_suggest', String(isAutoSuggestEnabled));
  }, [isAutoSuggestEnabled]);

  useEffect(() => {
    localStorage.setItem('keypro_key_font_size', String(keyFontSize));
  }, [keyFontSize]);

  useEffect(() => {
    localStorage.setItem('keypro_language', currentLanguage);
  }, [currentLanguage]);

  useEffect(() => {
    localStorage.setItem('keypro_installed_langs', JSON.stringify(installedLanguages));
  }, [installedLanguages]);

  useEffect(() => {
    localStorage.setItem('keypro_installed_packs', JSON.stringify(installedEmojiPacks));
  }, [installedEmojiPacks]);

  useEffect(() => {
    localStorage.setItem('keypro_recent_emojis', JSON.stringify(recentEmojis));
  }, [recentEmojis]);

  useEffect(() => {
    localStorage.setItem('keypro_snippets', JSON.stringify(allSnippets));
  }, [allSnippets]);

  useEffect(() => {
    localStorage.setItem('keypro_stickers', JSON.stringify(stickers));
  }, [stickers]);

  useEffect(() => {
    localStorage.setItem('keypro_word_freq', JSON.stringify(wordFrequencies));
  }, [wordFrequencies]);

  const activeColors = useMemo(() => {
    return getTheme(themeType, customThemeConfig);
  }, [themeType, customThemeConfig]);

  // Update Editor Text & History
  const setEditorTextWithHistory = useCallback((newText: string) => {
    setEditorText(newText);
    setHistoryStack((prev) => {
      const sliced = prev.slice(0, historyIndex + 1);
      return [...sliced, newText];
    });
    setHistoryIndex((prev) => prev + 1);
  }, [historyIndex]);

  // Record word usage in typing history for frequency suggestions (same word used more -> suggested more)
  const handleRecordWordUsed = useCallback((word: string) => {
    const clean = word.trim().replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
    if (clean.length < 2) return;

    setWordFrequencies((prev) => {
      const existing = prev.find((wf) => wf.word === clean);
      if (existing) {
        return prev.map((wf) =>
          wf.word === clean
            ? { ...wf, frequency: wf.frequency + 1, lastUsed: Date.now() }
            : wf
        );
      } else {
        return [...prev, { word: clean, frequency: 1, lastUsed: Date.now() }];
      }
    });
  }, []);

  // Add recent emoji
  const handleAddRecentEmoji = useCallback((emoji: string) => {
    setRecentEmojis((prev) => {
      const filtered = prev.filter((e) => e !== emoji);
      return [emoji, ...filtered].slice(0, 32);
    });
  }, []);

  // Cycle installed language
  const handleCycleLanguage = useCallback(() => {
    if (installedLanguages.length <= 1) return;
    const currentIdx = installedLanguages.indexOf(currentLanguage);
    const nextIdx = (currentIdx + 1) % installedLanguages.length;
    const nextLang = installedLanguages[nextIdx];
    setCurrentLanguage(nextLang);
    setLastActionStatus(`Language switched to: ${nextLang === 'si' ? 'සිංහල' : nextLang.toUpperCase()}`);
  }, [installedLanguages, currentLanguage]);

  // Handle all keyboard actions
  const handleKeyAction = useCallback(
    (action: KeyActionType) => {
      switch (action.type) {
        case 'INSERT_TEXT': {
          setEditorTextWithHistory(editorText + action.text);
          setLastActionStatus(`Typed: "${action.text}"`);
          if (action.text.length >= 1 && /[\p{Emoji}\u2000-\u3300]/u.test(action.text)) {
            handleAddRecentEmoji(action.text);
          }
          break;
        }

        case 'INSERT_SINHALA_PILLAM': {
          // Replace last consonant if applicable or append baseWithPillam
          setEditorTextWithHistory(editorText + action.modifier);
          setLastActionStatus(`Pillam: "${action.modifier}"`);
          break;
        }

        case 'INSERT_STICKER': {
          setEditorTextWithHistory(`${editorText}\n[Sticker: ${action.name}]\n`);
          setLastActionStatus(`Sticker Sent: "${action.name}"`);
          // Increment usage count
          setStickers((prev) =>
            prev.map((s) =>
              s.filePath === action.filePath
                ? { ...s, usageCount: (s.usageCount || 0) + 1 }
                : s
            )
          );
          break;
        }

        case 'BACKSPACE': {
          if (editorText.length > 0) {
            setEditorTextWithHistory(editorText.slice(0, -1));
            setLastActionStatus('Backspace');
          }
          break;
        }

        case 'DELETE_FORWARD': {
          if (editorText.length > 0) {
            setEditorTextWithHistory(editorText.slice(0, -1));
            setLastActionStatus('Delete Forward');
          }
          break;
        }

        case 'SPACE': {
          setEditorTextWithHistory(`${editorText} `);
          setLastActionStatus('Space');
          break;
        }

        case 'ENTER': {
          setEditorTextWithHistory(`${editorText}\n`);
          setLastActionStatus('Enter');
          break;
        }

        case 'TAB': {
          setEditorTextWithHistory(`${editorText}    `);
          setLastActionStatus('Tab (4 spaces)');
          break;
        }

        case 'ESCAPE': {
          setLastActionStatus('Escape (ESC)');
          break;
        }

        case 'CURSOR_LEFT': {
          setLastActionStatus('Cursor ←');
          break;
        }

        case 'CURSOR_RIGHT': {
          setLastActionStatus('Cursor →');
          break;
        }

        case 'CURSOR_UP': {
          setLastActionStatus('Cursor ↑');
          break;
        }

        case 'CURSOR_DOWN': {
          setLastActionStatus('Cursor ↓');
          break;
        }

        case 'HOME': {
          setLastActionStatus('Home (Start of Line)');
          break;
        }

        case 'END': {
          setLastActionStatus('End (End of Line)');
          break;
        }

        case 'PAGE_UP': {
          setLastActionStatus('Page Up');
          break;
        }

        case 'PAGE_DOWN': {
          setLastActionStatus('Page Down');
          break;
        }

        case 'SELECT_ALL': {
          setLastActionStatus('Ctrl+A (Select All)');
          break;
        }

        case 'COPY': {
          navigator.clipboard.writeText(editorText);
          setLastActionStatus('Ctrl+C (Copied to Clipboard)');
          break;
        }

        case 'CUT': {
          navigator.clipboard.writeText(editorText);
          setEditorTextWithHistory('');
          setLastActionStatus('Ctrl+X (Cut to Clipboard)');
          break;
        }

        case 'PASTE': {
          navigator.clipboard
            .readText()
            .then((clip) => {
              if (clip) {
                setEditorTextWithHistory(editorText + clip);
                setLastActionStatus(`Pasted: "${clip.slice(0, 16)}..."`);
              }
            })
            .catch(() => {
              // Fallback to last snippet
              const last = allSnippets[0];
              if (last) {
                setEditorTextWithHistory(editorText + last.content);
                setLastActionStatus(`Pasted Snippet: "${last.content.slice(0, 16)}..."`);
              }
            });
          break;
        }

        case 'UNDO': {
          if (historyIndex > 0) {
            setHistoryIndex(historyIndex - 1);
            setEditorText(historyStack[historyIndex - 1]);
            setLastActionStatus('Undo');
          }
          break;
        }

        case 'REDO': {
          if (historyIndex < historyStack.length - 1) {
            setHistoryIndex(historyIndex + 1);
            setEditorText(historyStack[historyIndex + 1]);
            setLastActionStatus('Redo');
          }
          break;
        }

        case 'FUNCTION_KEY': {
          setLastActionStatus(`F${action.fNumber} Key Pressed`);
          break;
        }

        case 'EXPAND_SHORTCUT': {
          // Replace shortcut prefix with expanded content
          const current = editorText;
          const shortcut = action.shortcutText;
          const full = action.fullContent;

          if (current.endsWith(shortcut)) {
            const next = current.slice(0, -shortcut.length) + full;
            setEditorTextWithHistory(next);
          } else {
            setEditorTextWithHistory(current + full);
          }
          setLastActionStatus(`Shortcut Expanded: "${shortcut} ➔ ${full.slice(0, 18)}..."`);
          break;
        }

        case 'APPLY_SUGGESTION': {
          const current = editorText;
          const typed = action.typedWord;
          const suggested = `${action.suggestedWord} `;

          if (typed && current.endsWith(typed)) {
            const next = current.slice(0, -typed.length) + suggested;
            setEditorTextWithHistory(next);
          } else {
            setEditorTextWithHistory(current + suggested);
          }
          setLastActionStatus(`Suggestion Applied: "${action.suggestedWord}"`);
          break;
        }

        default:
          break;
      }
    },
    [editorText, historyIndex, historyStack, allSnippets, setEditorTextWithHistory, handleAddRecentEmoji]
  );

  // Snippet Operations
  const handleTogglePin = useCallback((snippet: SnippetEntity) => {
    setAllSnippets((prev) =>
      prev.map((s) => (s.id === snippet.id ? { ...s, isPinned: !s.isPinned } : s))
    );
  }, []);

  const handleDeleteSnippet = useCallback((id: number) => {
    setAllSnippets((prev) => prev.filter((s) => s.id !== id));
  }, []);

  const handleSaveSnippet = useCallback(
    (title: string, content: string, isPinned: boolean, category: string, shortcut: string) => {
      const newSnippet: SnippetEntity = {
        id: Date.now(),
        title,
        content,
        isPinned,
        category,
        shortcut,
        timestamp: Date.now(),
      };
      setAllSnippets((prev) => [newSnippet, ...prev]);
    },
    []
  );

  const handleSaveImageSnippet = useCallback((uri: string, title?: string) => {
    const newSnippet: SnippetEntity = {
      id: Date.now(),
      title: title || 'Image Clip',
      content: uri,
      isPinned: true,
      category: 'Images',
      shortcut: '',
      timestamp: Date.now(),
      imageUri: uri,
      isImage: true,
      mimeType: 'image/*',
    };
    setAllSnippets((prev) => [newSnippet, ...prev]);
  }, []);

  const handleClearUnpinnedHistory = useCallback(() => {
    setAllSnippets((prev) => prev.filter((s) => s.isPinned));
  }, []);

  // Sticker Operations
  const handleImportStickers = useCallback((newStickers: StickerEntity[]) => {
    setStickers((prev) => [...newStickers, ...prev]);
  }, []);

  const handleDeleteSticker = useCallback((sticker: StickerEntity) => {
    setStickers((prev) => prev.filter((s) => s.id !== sticker.id));
  }, []);

  // Language Operations
  const handleInstallLanguage = useCallback((langId: string) => {
    setInstalledLanguages((prev) => (prev.includes(langId) ? prev : [...prev, langId]));
    setCurrentLanguage(langId);
  }, []);

  const handleUninstallLanguage = useCallback(
    (langId: string) => {
      setInstalledLanguages((prev) => prev.filter((id) => id !== langId));
      if (currentLanguage === langId) {
        setCurrentLanguage('en');
      }
    },
    [currentLanguage]
  );

  // Emoji Pack Operations
  const handleInstallEmojiPack = useCallback((packId: string) => {
    setInstalledEmojiPacks((prev) => (prev.includes(packId) ? prev : [...prev, packId]));
  }, []);

  const handleUninstallEmojiPack = useCallback((packId: string) => {
    setInstalledEmojiPacks((prev) => prev.filter((id) => id !== packId));
  }, []);

  return (
    <div className="w-full min-h-screen bg-[#1E1F24] flex flex-col font-sans">
      {currentSection === 'HUB' && (
        <HubScreen
          colors={activeColors}
          currentThemeType={themeType}
          onNavigate={(sec) => setCurrentSection(sec)}
        />
      )}

      {currentSection === 'TEST_KEYBOARD' && (
        <TestKeyboardScreen
          colors={activeColors}
          currentThemeType={themeType}
          editorText={editorText}
          onEditorTextChange={(text) => setEditorTextWithHistory(text)}
          allSnippets={allSnippets}
          stickers={stickers}
          isSoundEnabled={isSoundEnabled}
          isHapticEnabled={isHapticEnabled}
          isLaptopBarVisible={isLaptopBarVisible}
          isHoldForSymbolsEnabled={isHoldForSymbolsEnabled}
          isAutoSuggestEnabled={isAutoSuggestEnabled}
          keyFontSize={keyFontSize}
          currentLanguage={currentLanguage}
          installedLanguages={installedLanguages}
          installedEmojiPacks={installedEmojiPacks}
          recentEmojis={recentEmojis}
          wordFrequencies={wordFrequencies}
          lastActionStatus={lastActionStatus}
          onAction={handleKeyAction}
          onTogglePin={handleTogglePin}
          onDeleteSnippet={handleDeleteSnippet}
          onSaveSnippet={handleSaveSnippet}
          onClearHistory={handleClearUnpinnedHistory}
          onDeleteSticker={handleDeleteSticker}
          onInstallEmojiPack={handleInstallEmojiPack}
          onThemeSelected={(t) => setThemeType(t)}
          onToggleSound={(s) => setIsSoundEnabled(s)}
          onToggleHaptic={(h) => setIsHapticEnabled(h)}
          onToggleLaptopBar={(b) => setIsLaptopBarVisible(b)}
          onToggleHoldForSymbols={(hs) => setIsHoldForSymbolsEnabled(hs)}
          onToggleAutoSuggest={(as) => setIsAutoSuggestEnabled(as)}
          onKeyFontSizeChanged={(fs) => setKeyFontSize(fs)}
          onCycleLanguage={handleCycleLanguage}
          onRecordWordUsed={handleRecordWordUsed}
          onOpenStickerStudio={() => setCurrentSection('STICKERS')}
          onBack={() => setCurrentSection('HUB')}
        />
      )}

      {currentSection === 'SETTINGS_PAGE' && (
        <SettingsScreen
          colors={activeColors}
          currentThemeType={themeType}
          customThemeConfig={customThemeConfig}
          isSoundEnabled={isSoundEnabled}
          isHapticEnabled={isHapticEnabled}
          isLaptopBarEnabled={isLaptopBarVisible}
          isHoldForSymbolsEnabled={isHoldForSymbolsEnabled}
          isAutoSuggestEnabled={isAutoSuggestEnabled}
          keyFontSize={keyFontSize}
          onBack={() => setCurrentSection('HUB')}
          onThemeSelected={(t) => setThemeType(t)}
          onCustomThemeChanged={(cfg) => setCustomThemeConfig(cfg)}
          onKeyFontSizeChanged={(fs) => setKeyFontSize(fs)}
          onToggleSound={(s) => setIsSoundEnabled(s)}
          onToggleHaptic={(h) => setIsHapticEnabled(h)}
          onToggleLaptopBar={(b) => setIsLaptopBarVisible(b)}
          onToggleHoldForSymbols={(hs) => setIsHoldForSymbolsEnabled(hs)}
          onToggleAutoSuggest={(as) => setIsAutoSuggestEnabled(as)}
        />
      )}

      {currentSection === 'CLIPBOARD_HISTORY' && (
        <ClipboardHistoryScreen
          colors={activeColors}
          pinnedSnippets={allSnippets.filter((s) => s.isPinned)}
          historySnippets={allSnippets.filter((s) => !s.isPinned)}
          onBack={() => setCurrentSection('HUB')}
          onTogglePin={handleTogglePin}
          onDeleteSnippet={handleDeleteSnippet}
          onSaveSnippet={handleSaveSnippet}
          onSaveImage={handleSaveImageSnippet}
          onClearHistory={handleClearUnpinnedHistory}
        />
      )}

      {currentSection === 'STICKERS' && (
        <StickersScreen
          colors={activeColors}
          stickers={stickers}
          onBack={() => setCurrentSection('HUB')}
          onImportStickers={handleImportStickers}
          onDeleteSticker={handleDeleteSticker}
        />
      )}

      {currentSection === 'LANGUAGES' && (
        <LanguagesScreen
          colors={activeColors}
          activeLanguageId={currentLanguage}
          installedLanguageIds={installedLanguages}
          onBack={() => setCurrentSection('HUB')}
          onSelectActiveLanguage={(id) => setCurrentLanguage(id)}
          onInstallLanguage={handleInstallLanguage}
          onUninstallLanguage={handleUninstallLanguage}
        />
      )}

      {currentSection === 'EMOJI_PACKS' && (
        <EmojiPacksScreen
          colors={activeColors}
          installedPackIds={installedEmojiPacks}
          onBack={() => setCurrentSection('HUB')}
          onInstallPack={handleInstallEmojiPack}
          onUninstallPack={handleUninstallEmojiPack}
        />
      )}
    </div>
  );
}

export default App;
