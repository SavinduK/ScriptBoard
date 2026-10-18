import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  KeyActionType,
  KeyItem,
  KeyboardColors,
  KeyboardLayoutMode,
  KeyboardThemeType,
  ShiftState,
  SnippetEntity,
  StickerEntity,
  WordFrequencyEntity,
} from '../../types';
import {
  getQwertyRows,
  getSinhalaRows,
  getSymbols1Rows,
  getSymbols2Rows,
  getNumpadRows,
  getCompleteLaptopRows,
  isSinhalaConsonant,
} from '../../data/layoutGenerator';
import {
  commonVocabulary,
  defaultStarterWords,
  sinhalaVocabulary,
  sinhalaStarterWords,
  isSinhalaText,
  applyCasing,
} from '../../data/initialData';
import { performKeyPressFeedback } from '../../utils/audioAndHaptic';
import { KeyCap } from './KeyCap';
import { LaptopKeysBar } from './LaptopKeysBar';
import { KeyboardToolbar } from './KeyboardToolbar';
import { EmojiPickerView } from './EmojiPickerView';
import { InKeyboardClipboardView } from './InKeyboardClipboardView';
import { InKeyboardSettingsView } from './InKeyboardSettingsView';
import { VoiceTypingView } from './VoiceTypingView';
import { Sparkles } from 'lucide-react';

interface KeyProKeyboardViewProps {
  colors: KeyboardColors;
  currentThemeType: KeyboardThemeType;
  allSnippets: SnippetEntity[];
  stickers: StickerEntity[];
  isSoundEnabled: boolean;
  isHapticEnabled: boolean;
  initialLaptopBarVisible?: boolean;
  isHoldForSymbolsEnabled: boolean;
  isAutoSuggestEnabled: boolean;
  keyFontSize: number;
  currentLanguage: string;
  installedLanguages: string[];
  installedEmojiPacks: string[];
  recentEmojis: string[];
  wordFrequencies: WordFrequencyEntity[];
  onAction: (action: KeyActionType) => void;
  onTogglePin: (snippet: SnippetEntity) => void;
  onDeleteSnippet: (id: number) => void;
  onSaveSnippet: (title: string, content: string, isPinned: boolean, category: string, shortcut: string) => void;
  onClearHistory: () => void;
  onDeleteSticker: (sticker: StickerEntity) => void;
  onInstallEmojiPack: (packId: string) => void;
  onThemeSelected: (theme: KeyboardThemeType) => void;
  onToggleSound: (enabled: boolean) => void;
  onToggleHaptic: (enabled: boolean) => void;
  onToggleLaptopBar: (enabled: boolean) => void;
  onToggleHoldForSymbols: (enabled: boolean) => void;
  onToggleAutoSuggest: (enabled: boolean) => void;
  onKeyFontSizeChanged: (size: number) => void;
  onCycleLanguage: () => void;
  onRecordWordUsed: (word: string) => void;
  onOpenStickerStudio: () => void;
}

export const KeyProKeyboardView: React.FC<KeyProKeyboardViewProps> = ({
  colors,
  currentThemeType,
  allSnippets,
  stickers,
  isSoundEnabled,
  isHapticEnabled,
  initialLaptopBarVisible = true,
  isHoldForSymbolsEnabled,
  isAutoSuggestEnabled,
  keyFontSize,
  currentLanguage,
  installedLanguages,
  installedEmojiPacks,
  recentEmojis,
  wordFrequencies,
  onAction,
  onTogglePin,
  onDeleteSnippet,
  onSaveSnippet,
  onClearHistory,
  onDeleteSticker,
  onInstallEmojiPack,
  onThemeSelected,
  onToggleSound,
  onToggleHaptic,
  onToggleLaptopBar,
  onToggleHoldForSymbols,
  onToggleAutoSuggest,
  onKeyFontSizeChanged,
  onCycleLanguage,
  onRecordWordUsed,
  onOpenStickerStudio,
}) => {
  const [layoutMode, setLayoutMode] = useState<KeyboardLayoutMode>('TEXT');
  const [shiftState, setShiftState] = useState<ShiftState>('OFF');
  const [isLaptopBarVisible, setIsLaptopBarVisible] = useState<boolean>(initialLaptopBarVisible);
  const [isCtrlActive, setIsCtrlActive] = useState<boolean>(false);
  const [isAltActive, setIsAltActive] = useState<boolean>(false);
  const [currentWordBuffer, setCurrentWordBuffer] = useState<string>('');
  const [suggestedWords, setSuggestedWords] = useState<string[]>([]);
  const [selectedSinhalaLetter, setSelectedSinhalaLetter] = useState<string>('ව');

  const pinnedSnippets = useMemo(() => allSnippets.filter((s) => s.isPinned), [allSnippets]);
  const historySnippets = useMemo(() => allSnippets.filter((s) => !s.isPinned), [allSnippets]);

  // Is current keyboard in Sinhala mode
  const isSinhala = currentLanguage === 'si' && layoutMode === 'TEXT';
  const hasMultipleLanguages = installedLanguages.length > 1;

  // Language-specific auto suggestions: strictly suggests words in the current keyboard language
  useEffect(() => {
    if (!isAutoSuggestEnabled) {
      setSuggestedWords([]);
      return;
    }

    const cleanPrefix = currentWordBuffer.trim();

    if (isSinhala) {
      // SINHALA AUTO-SUGGEST
      if (!cleanPrefix) {
        // Show top Sinhala words from user history or starter Sinhala words
        const topHistorySinhala = wordFrequencies
          .filter((wf) => isSinhalaText(wf.word))
          .sort((a, b) => b.frequency - a.frequency)
          .slice(0, 3)
          .map((wf) => wf.word);

        const suggestions = [...topHistorySinhala];
        for (const sw of sinhalaStarterWords) {
          if (suggestions.length >= 3) break;
          if (!suggestions.includes(sw)) {
            suggestions.push(sw);
          }
        }
        setSuggestedWords(suggestions.slice(0, 3));
        return;
      }

      // Match Sinhala prefix
      const scoreMap = new Map<string, number>();

      wordFrequencies.forEach((item) => {
        if (isSinhalaText(item.word) && item.word.startsWith(cleanPrefix)) {
          const score = 1000 + item.frequency * 50 + (item.word === cleanPrefix ? 200 : 0);
          scoreMap.set(item.word, score);
        }
      });

      sinhalaVocabulary.forEach((vocab) => {
        if (vocab.startsWith(cleanPrefix)) {
          if (!scoreMap.has(vocab)) {
            scoreMap.set(vocab, 100 + (vocab === cleanPrefix ? 50 : 0));
          }
        }
      });

      const sortedCandidates = Array.from(scoreMap.entries())
        .sort((a, b) => b[1] - a[1])
        .map((entry) => entry[0]);

      if (sortedCandidates.length < 3) {
        if (!sortedCandidates.includes(cleanPrefix)) {
          sortedCandidates.push(cleanPrefix);
        }
        for (const vocab of sinhalaVocabulary) {
          if (sortedCandidates.length >= 3) break;
          if (vocab.includes(cleanPrefix) && !sortedCandidates.includes(vocab)) {
            sortedCandidates.push(vocab);
          }
        }
      }

      setSuggestedWords(sortedCandidates.slice(0, 3));
    } else {
      // ENGLISH AUTO-SUGGEST
      if (!cleanPrefix) {
        const topHistoryEn = wordFrequencies
          .filter((wf) => !isSinhalaText(wf.word))
          .sort((a, b) => b.frequency - a.frequency)
          .slice(0, 3)
          .map((wf) => wf.word);

        const suggestions = [...topHistoryEn];
        for (const dw of defaultStarterWords) {
          if (suggestions.length >= 3) break;
          if (!suggestions.some((s) => s.toLowerCase() === dw.toLowerCase())) {
            suggestions.push(dw);
          }
        }
        setSuggestedWords(suggestions.slice(0, 3));
        return;
      }

      const prefixLower = cleanPrefix.toLowerCase();
      const scoreMap = new Map<string, number>();

      wordFrequencies.forEach((item) => {
        if (!isSinhalaText(item.word) && item.word.toLowerCase().startsWith(prefixLower)) {
          const score = 1000 + item.frequency * 50 + (item.word.toLowerCase() === prefixLower ? 200 : 0);
          scoreMap.set(item.word, score);
        }
      });

      commonVocabulary.forEach((vocab) => {
        if (vocab.startsWith(prefixLower)) {
          if (!scoreMap.has(vocab)) {
            scoreMap.set(vocab, 100 + (vocab === prefixLower ? 50 : 0));
          }
        }
      });

      const sortedCandidates = Array.from(scoreMap.entries())
        .sort((a, b) => b[1] - a[1])
        .map((entry) => entry[0]);

      if (sortedCandidates.length < 3) {
        if (!sortedCandidates.includes(prefixLower)) {
          sortedCandidates.push(prefixLower);
        }
        for (const vocab of commonVocabulary) {
          if (sortedCandidates.length >= 3) break;
          if (vocab.includes(prefixLower) && !sortedCandidates.includes(vocab)) {
            sortedCandidates.push(vocab);
          }
        }
      }

      const formatted = sortedCandidates.slice(0, 3).map((w) => applyCasing(cleanPrefix, w));
      setSuggestedWords(formatted);
    }
  }, [currentWordBuffer, isAutoSuggestEnabled, wordFrequencies, isSinhala]);

  // Matching shortcuts for phrase expansion
  const matchingShortcuts = useMemo(() => {
    if (!currentWordBuffer.trim()) return [];
    const q = currentWordBuffer.trim().toLowerCase();
    return pinnedSnippets.filter((snippet) => {
      const s = snippet.shortcut.trim().toLowerCase();
      if (!s) return false;
      return (
        s === q ||
        s.startsWith(q) ||
        q.startsWith(s) ||
        (q.startsWith('@') && s.includes(q.replace('@', '')))
      );
    });
  }, [currentWordBuffer, pinnedSnippets]);

  // Main Key Action Handler
  const handleKeyAction = useCallback(
    (action: KeyActionType) => {
      // Play audio & haptic feedback
      const soundType =
        action.type === 'SPACE'
          ? 'space'
          : action.type === 'ENTER'
          ? 'enter'
          : action.type === 'BACKSPACE' || action.type === 'DELETE_FORWARD'
          ? 'backspace'
          : action.type.startsWith('TOGGLE_') || action.type.startsWith('SWITCH_')
          ? 'fn'
          : 'letter';

      performKeyPressFeedback(isSoundEnabled, isHapticEnabled, soundType);

      // Track typed word buffer
      if (action.type === 'INSERT_TEXT') {
        const text = action.text;
        const isWordChar = /^[a-zA-Z0-9@/#_.-]$/.test(text) || isSinhalaText(text);
        if (text.length >= 1 && isWordChar) {
          setCurrentWordBuffer((prev) => prev + text);
        } else {
          if (currentWordBuffer.trim()) {
            onRecordWordUsed(currentWordBuffer.trim());
          }
          setCurrentWordBuffer('');
        }
      } else if (action.type === 'INSERT_SINHALA_PILLAM') {
        setCurrentWordBuffer((prev) => prev + action.modifier);
      } else if (action.type === 'BACKSPACE') {
        setCurrentWordBuffer((prev) => (prev.length > 0 ? Array.from(prev).slice(0, -1).join('') : ''));
      } else if (action.type === 'SPACE') {
        // Check exact shortcut match
        const matched = pinnedSnippets.find(
          (s) => s.shortcut.trim().toLowerCase() === currentWordBuffer.trim().toLowerCase()
        );
        if (matched) {
          onAction({
            type: 'EXPAND_SHORTCUT',
            shortcutText: currentWordBuffer,
            fullContent: `${matched.content} `,
          });
          setCurrentWordBuffer('');
          return;
        }

        if (currentWordBuffer.trim()) {
          onRecordWordUsed(currentWordBuffer.trim());
        }
        setCurrentWordBuffer('');
      } else if (action.type === 'ENTER') {
        if (currentWordBuffer.trim()) {
          onRecordWordUsed(currentWordBuffer.trim());
        }
        setCurrentWordBuffer('');
      }

      // Handle specific action logic
      switch (action.type) {
        case 'TOGGLE_SHIFT':
          setShiftState((prev) => {
            if (prev === 'OFF') return 'ONCE';
            if (prev === 'ONCE') return 'CAPS_LOCKED';
            return 'OFF';
          });
          break;
        case 'SWITCH_TO_SYMBOLS':
          setLayoutMode('SYMBOLS_1');
          setCurrentWordBuffer('');
          break;
        case 'SWITCH_TO_LETTERS':
          setLayoutMode('TEXT');
          setCurrentWordBuffer('');
          break;
        case 'SWITCH_TO_MORE_SYMBOLS':
          setLayoutMode('SYMBOLS_2');
          setCurrentWordBuffer('');
          break;
        case 'SWITCH_TO_NUMPAD':
          setLayoutMode('NUMPAD');
          setCurrentWordBuffer('');
          break;
        case 'SWITCH_TO_EXTENDED_PC':
          setLayoutMode('EXTENDED_PC');
          setCurrentWordBuffer('');
          break;
        case 'SWITCH_TO_EMOJI':
          setLayoutMode('EMOJI');
          setCurrentWordBuffer('');
          break;
        case 'SWITCH_TO_CLIPBOARD':
          setLayoutMode('CLIPBOARD');
          setCurrentWordBuffer('');
          break;
        case 'TOGGLE_CTRL':
          setIsCtrlActive((prev) => !prev);
          break;
        case 'TOGGLE_ALT':
          setIsAltActive((prev) => !prev);
          break;
        case 'SWITCH_LANGUAGE':
          setSelectedSinhalaLetter('ව');
          setCurrentWordBuffer('');
          onCycleLanguage();
          break;
        case 'INSERT_TEXT':
          if (action.text && isSinhalaConsonant(action.text)) {
            setSelectedSinhalaLetter(action.text);
          }
          onAction(action);
          if (shiftState === 'ONCE') {
            setShiftState('OFF');
          }
          break;
        case 'INSERT_SINHALA_PILLAM':
          onAction(action);
          break;
        default:
          onAction(action);
          break;
      }
    },
    [
      isSoundEnabled,
      isHapticEnabled,
      currentWordBuffer,
      pinnedSnippets,
      shiftState,
      onAction,
      onRecordWordUsed,
      onCycleLanguage,
    ]
  );

  // Keyboard Rows resolution based on current mode & language
  const currentRows = useMemo(() => {
    switch (layoutMode) {
      case 'TEXT':
        if (isSinhala) {
          return getSinhalaRows(selectedSinhalaLetter, hasMultipleLanguages);
        }
        return getQwertyRows(shiftState, isHoldForSymbolsEnabled, 'English', false, hasMultipleLanguages);
      case 'SYMBOLS_1':
        return getSymbols1Rows(isSinhala ? 'සිංහල' : 'English', isHoldForSymbolsEnabled);
      case 'SYMBOLS_2':
        return getSymbols2Rows(isSinhala ? 'සිංහල' : 'English', isHoldForSymbolsEnabled);
      case 'NUMPAD':
        return getNumpadRows(isHoldForSymbolsEnabled);
      case 'EXTENDED_PC':
        return getCompleteLaptopRows(shiftState, isCtrlActive, isAltActive, isSinhala ? 'සිංහල' : 'English');
      default:
        return getQwertyRows(shiftState, isHoldForSymbolsEnabled, 'English', false, hasMultipleLanguages);
    }
  }, [
    layoutMode,
    isSinhala,
    selectedSinhalaLetter,
    hasMultipleLanguages,
    shiftState,
    isHoldForSymbolsEnabled,
    isCtrlActive,
    isAltActive,
  ]);

  const isLaptopLayout = layoutMode === 'EXTENDED_PC';
  const heightClass = isLaptopLayout
    ? 'h-8 sm:h-10 md:h-11 lg:h-13'
    : isSinhala
    ? 'h-11 sm:h-13 md:h-15 lg:h-18'
    : 'h-12 sm:h-14 md:h-16 lg:h-20';

  const calculatedFontSize = isLaptopLayout ? 13 : isSinhala ? 20 : keyFontSize;

  return (
    <div
      style={{ backgroundColor: colors.background }}
      className="w-full flex flex-col select-none transition-colors duration-150 rounded-b-2xl overflow-hidden shadow-2xl border-t border-black/20"
    >
      {/* Laptop Keys Bar (Collapsible top row) */}
      {isLaptopBarVisible &&
        !['EMOJI', 'CLIPBOARD', 'SETTINGS', 'STICKERS', 'VOICE_TYPING', 'EXTENDED_PC'].includes(
          layoutMode
        ) && (
          <LaptopKeysBar
            colors={colors}
            isCtrlActive={isCtrlActive}
            isAltActive={isAltActive}
            onKeyClick={(key) => handleKeyAction(key.action)}
          />
        )}

      {/* Keyboard Toolbar (Laptop toggle, Extended PC switch, Direct Paste, Clipboard, Emoji/Sticker, Voice, Settings) */}
      {layoutMode !== 'EXTENDED_PC' && (
        <KeyboardToolbar
          colors={colors}
          isLaptopBarVisible={isLaptopBarVisible}
          isExtendedPcActive={false}
          isEmojiActive={layoutMode === 'EMOJI' || layoutMode === 'STICKERS'}
          isVoiceTypeActive={layoutMode === 'VOICE_TYPING'}
          isClipboardActive={layoutMode === 'CLIPBOARD'}
          onToggleLaptopBar={() => setIsLaptopBarVisible((prev) => !prev)}
          onToggleExtendedPcKeys={() =>
            setLayoutMode((prev) => (prev === 'EXTENDED_PC' ? 'TEXT' : 'EXTENDED_PC'))
          }
          onPasteCopiedContent={() => handleKeyAction({ type: 'PASTE' })}
          onOpenClipboard={() =>
            setLayoutMode((prev) => (prev === 'CLIPBOARD' ? 'TEXT' : 'CLIPBOARD'))
          }
          onOpenEmoji={() =>
            setLayoutMode((prev) => (prev === 'EMOJI' ? 'TEXT' : 'EMOJI'))
          }
          onTriggerVoiceType={() =>
            setLayoutMode((prev) => (prev === 'VOICE_TYPING' ? 'TEXT' : 'VOICE_TYPING'))
          }
          onOpenSettings={() =>
            setLayoutMode((prev) => (prev === 'SETTINGS' ? 'TEXT' : 'SETTINGS'))
          }
        />
      )}

      {/* Shortcut Expansion Bar (shows shortcut preview chip when typed query matches pinned shortcut) */}
      {matchingShortcuts.length > 0 && layoutMode === 'TEXT' && (
        <div
          style={{ backgroundColor: `${colors.functionKeyBackground}cc` }}
          className="w-full h-9 sm:h-11 px-3 sm:px-6 flex items-center space-x-2 border-b border-black/15 overflow-x-auto scrollbar-none"
        >
          <span
            style={{ color: colors.enterKeyBackground }}
            className="text-[11px] sm:text-xs font-bold uppercase tracking-wider flex items-center space-x-1 shrink-0"
          >
            <Sparkles size={13} />
            <span>Shortcut:</span>
          </span>
          {matchingShortcuts.map((snip) => (
            <button
              key={snip.id}
              type="button"
              onClick={() => {
                onAction({
                  type: 'EXPAND_SHORTCUT',
                  shortcutText: currentWordBuffer,
                  fullContent: `${snip.content} `,
                });
                setCurrentWordBuffer('');
              }}
              style={{
                backgroundColor: colors.letterKeyBackground,
                color: colors.letterKeyTextColor,
                borderColor: colors.enterKeyBackground,
              }}
              className="px-2.5 sm:px-3.5 py-1 rounded-xl text-xs sm:text-sm font-semibold border flex items-center space-x-1.5 shrink-0 shadow-sm hover:brightness-105 active:scale-95 cursor-pointer transition-all"
            >
              <span className="font-mono text-blue-400 font-bold">{snip.shortcut}</span>
              <span className="opacity-40">➔</span>
              <span className="max-w-[160px] sm:max-w-[240px] truncate">{snip.content}</span>
            </button>
          ))}
        </div>
      )}

      {/* Suggested Words Bar (Auto-Suggest with language matching) */}
      {isAutoSuggestEnabled &&
        layoutMode === 'TEXT' &&
        matchingShortcuts.length === 0 && (
          <div
            style={{ backgroundColor: colors.toolbarBackground }}
            className="w-full h-10 sm:h-12 md:h-13 px-2 sm:px-4 md:px-6 flex items-center justify-between border-b border-black/15 select-none"
          >
            {suggestedWords.map((word, idx) => {
              const isCenter = idx === 1;
              return (
                <React.Fragment key={idx}>
                  {idx > 0 && (
                    <div
                      style={{ backgroundColor: colors.functionKeyBackground }}
                      className="w-[1px] h-5 opacity-60"
                    />
                  )}
                  <button
                    type="button"
                    onClick={() => {
                      if (word) {
                        performKeyPressFeedback(isSoundEnabled, isHapticEnabled, 'letter');
                        onAction({
                          type: 'APPLY_SUGGESTION',
                          typedWord: currentWordBuffer,
                          suggestedWord: word,
                        });
                        onRecordWordUsed(word);
                        setCurrentWordBuffer('');
                      }
                    }}
                    disabled={!word}
                    className="flex-1 h-full mx-1 rounded-xl flex items-center justify-center px-2 cursor-pointer hover:bg-white/5 active:scale-95 transition-all"
                  >
                    <span
                      style={{
                        color: isCenter ? colors.enterKeyBackground : colors.letterKeyTextColor,
                        fontWeight: isCenter ? 700 : 500,
                      }}
                      className="truncate tracking-tight text-xs sm:text-sm md:text-base"
                    >
                      {word}
                    </span>
                  </button>
                </React.Fragment>
              );
            })}
          </div>
        )}

      {/* Main Drawer or Keypad Switch */}
      {layoutMode === 'EMOJI' ? (
        <EmojiPickerView
          colors={colors}
          stickers={stickers}
          initialSection="EMOJIS"
          installedPackIds={installedEmojiPacks}
          recentEmojis={recentEmojis}
          onEmojiSelected={(emoji) => handleKeyAction({ type: 'INSERT_TEXT', text: emoji })}
          onStickerSelected={(sticker) =>
            handleKeyAction({ type: 'INSERT_STICKER', filePath: sticker.filePath, name: sticker.name })
          }
          onDeleteSticker={onDeleteSticker}
          onInstallPack={onInstallEmojiPack}
          onOpenStickerStudio={onOpenStickerStudio}
          onBackToLetters={() => setLayoutMode('TEXT')}
          onBackspace={() => handleKeyAction({ type: 'BACKSPACE' })}
        />
      ) : layoutMode === 'STICKERS' ? (
        <EmojiPickerView
          colors={colors}
          stickers={stickers}
          initialSection="STICKERS"
          installedPackIds={installedEmojiPacks}
          recentEmojis={recentEmojis}
          onEmojiSelected={(emoji) => handleKeyAction({ type: 'INSERT_TEXT', text: emoji })}
          onStickerSelected={(sticker) =>
            handleKeyAction({ type: 'INSERT_STICKER', filePath: sticker.filePath, name: sticker.name })
          }
          onDeleteSticker={onDeleteSticker}
          onInstallPack={onInstallEmojiPack}
          onOpenStickerStudio={onOpenStickerStudio}
          onBackToLetters={() => setLayoutMode('TEXT')}
          onBackspace={() => handleKeyAction({ type: 'BACKSPACE' })}
        />
      ) : layoutMode === 'CLIPBOARD' ? (
        <InKeyboardClipboardView
          colors={colors}
          pinnedSnippets={pinnedSnippets}
          historySnippets={historySnippets}
          onSnippetSelected={(text) => handleKeyAction({ type: 'INSERT_TEXT', text })}
          onTogglePin={onTogglePin}
          onDeleteSnippet={onDeleteSnippet}
          onSaveSnippet={onSaveSnippet}
          onClearHistory={onClearHistory}
          onBackToLetters={() => setLayoutMode('TEXT')}
        />
      ) : layoutMode === 'SETTINGS' ? (
        <InKeyboardSettingsView
          colors={colors}
          currentThemeType={currentThemeType}
          isSoundEnabled={isSoundEnabled}
          isHapticEnabled={isHapticEnabled}
          isLaptopBarEnabled={isLaptopBarVisible}
          isHoldForSymbolsEnabled={isHoldForSymbolsEnabled}
          isAutoSuggestEnabled={isAutoSuggestEnabled}
          keyFontSize={keyFontSize}
          onThemeSelected={onThemeSelected}
          onToggleSound={onToggleSound}
          onToggleHaptic={onToggleHaptic}
          onToggleLaptopBar={onToggleLaptopBar}
          onToggleHoldForSymbols={onToggleHoldForSymbols}
          onToggleAutoSuggest={onToggleAutoSuggest}
          onKeyFontSizeChanged={onKeyFontSizeChanged}
          onBackToLetters={() => setLayoutMode('TEXT')}
        />
      ) : layoutMode === 'VOICE_TYPING' ? (
        <VoiceTypingView
          colors={colors}
          onInsertText={(text) => handleKeyAction({ type: 'INSERT_TEXT', text })}
          onBackspace={() => handleKeyAction({ type: 'BACKSPACE' })}
          onEnter={() => handleKeyAction({ type: 'ENTER' })}
          onSpace={() => handleKeyAction({ type: 'SPACE' })}
          onBackToLetters={() => setLayoutMode('TEXT')}
        />
      ) : (
        /* Standard Key Rows */
        <div className="w-full px-1.5 sm:px-3 md:px-5 lg:px-6 py-1.5 sm:py-2.5 md:py-3 flex flex-col gap-0.5 sm:gap-1 md:gap-1.5">
          {currentRows.map((row, rIdx) => (
            <div key={rIdx} className="flex flex-row w-full justify-center">
              {row.map((key) => (
                <KeyCap
                  key={key.testTag}
                  item={key}
                  colors={colors}
                  heightClass={heightClass}
                  characterFontSize={calculatedFontSize}
                  holdForSymbols={isHoldForSymbolsEnabled}
                  onKeyClick={(clicked) => handleKeyAction(clicked.action)}
                  onKeyLongClick={(clicked) => {
                    if (clicked.action.type === 'SPACE' && hasMultipleLanguages) {
                      handleKeyAction({ type: 'SWITCH_LANGUAGE' });
                    } else if (
                      isSinhala &&
                      clicked.action.type === 'INSERT_TEXT' &&
                      isSinhalaConsonant(clicked.action.text)
                    ) {
                      setSelectedSinhalaLetter(clicked.action.text);
                    }
                  }}
                />
              ))}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
