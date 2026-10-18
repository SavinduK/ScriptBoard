import React, { useRef } from 'react';
import {
  KeyActionType,
  KeyboardColors,
  KeyboardThemeType,
  SnippetEntity,
  StickerEntity,
  WordFrequencyEntity,
} from '../../types';
import { ArrowLeft, Trash2 } from 'lucide-react';
import { KeyProKeyboardView } from '../keyboard/KeyProKeyboardView';

interface TestKeyboardScreenProps {
  colors: KeyboardColors;
  currentThemeType: KeyboardThemeType;
  editorText: string;
  onEditorTextChange: (text: string) => void;
  allSnippets: SnippetEntity[];
  stickers: StickerEntity[];
  isSoundEnabled: boolean;
  isHapticEnabled: boolean;
  isLaptopBarVisible: boolean;
  isHoldForSymbolsEnabled: boolean;
  isAutoSuggestEnabled: boolean;
  keyFontSize: number;
  currentLanguage: string;
  installedLanguages: string[];
  installedEmojiPacks: string[];
  recentEmojis: string[];
  wordFrequencies: WordFrequencyEntity[];
  lastActionStatus: string;
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
  onBack: () => void;
}

export const TestKeyboardScreen: React.FC<TestKeyboardScreenProps> = ({
  colors,
  currentThemeType,
  editorText,
  onEditorTextChange,
  allSnippets,
  stickers,
  isSoundEnabled,
  isHapticEnabled,
  isLaptopBarVisible,
  isHoldForSymbolsEnabled,
  isAutoSuggestEnabled,
  keyFontSize,
  currentLanguage,
  installedLanguages,
  installedEmojiPacks,
  recentEmojis,
  wordFrequencies,
  lastActionStatus,
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
  onBack,
}) => {
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  const wordCount = editorText.trim() ? editorText.trim().split(/\s+/).length : 0;
  const charCount = editorText.length;

  return (
    <div
      style={{ backgroundColor: colors.background }}
      className="flex-1 flex flex-col h-screen select-none overflow-hidden"
    >
      {/* Top App Bar */}
      <div
        style={{ backgroundColor: colors.toolbarBackground }}
        className="w-full h-14 px-3 flex items-center justify-between border-b border-black/20 shrink-0"
      >
        <div className="flex items-center space-x-2">
          <button
            type="button"
            onClick={onBack}
            style={{ color: colors.letterKeyTextColor }}
            className="p-1.5 rounded-lg hover:bg-white/10 active:scale-95 transition-all cursor-pointer"
            title="Back to Home"
          >
            <ArrowLeft size={20} />
          </button>
          <div className="flex flex-col">
            <h2
              style={{ color: colors.letterKeyTextColor }}
              className="text-sm font-bold leading-tight"
            >
              Test Keyboard & Layout
            </h2>
            <span
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-[11px]"
            >
              Interactive typing pad & live preview
            </span>
          </div>
        </div>

        <button
          type="button"
          onClick={() => onEditorTextChange('')}
          style={{ color: colors.toolbarIconTint }}
          className="p-2 rounded-lg hover:bg-white/10 active:scale-90 transition-all cursor-pointer"
          title="Clear Editor"
        >
          <Trash2 size={18} />
        </button>
      </div>

      {/* Status Bar Indicator */}
      <div
        style={{ backgroundColor: `${colors.functionKeyBackground}99` }}
        className="w-full px-4 py-1 flex items-center justify-between text-[11px] font-mono shrink-0 border-b border-black/10"
      >
        <span
          style={{ color: colors.letterKeyTextColor }}
          className="truncate max-w-[65%]"
        >
          {lastActionStatus || 'Ready to type'}
        </span>
        <span style={{ color: colors.letterKeySecondaryTextColor }}>
          {charCount} chars | {wordCount} words
        </span>
      </div>

      {/* Live Textarea Editor */}
      <div className="flex-1 w-full max-w-5xl 2xl:max-w-6xl mx-auto p-2 sm:p-4 min-h-0 flex flex-col">
        <textarea
          ref={textareaRef}
          value={editorText}
          onChange={(e) => onEditorTextChange(e.target.value)}
          placeholder="Tap any key below to test typing, hold to delete, PC keys (Ctrl, Alt, Tab, Esc), emojis, and clipboard..."
          style={{
            backgroundColor: colors.letterKeyBackground,
            color: colors.letterKeyTextColor,
            borderColor: colors.functionKeyBackground,
          }}
          className="w-full flex-1 rounded-2xl p-3 sm:p-5 text-sm sm:text-base md:text-lg outline-none border focus:border-blue-400/80 resize-none font-sans leading-relaxed select-text shadow-inner transition-colors"
        />
      </div>

      {/* KeyPro Virtual Keyboard Component */}
      <div className="w-full max-w-5xl 2xl:max-w-6xl mx-auto shrink-0 pb-1 sm:pb-2">
        <KeyProKeyboardView
          colors={colors}
          currentThemeType={currentThemeType}
          allSnippets={allSnippets}
          stickers={stickers}
          isSoundEnabled={isSoundEnabled}
          isHapticEnabled={isHapticEnabled}
          initialLaptopBarVisible={isLaptopBarVisible}
          isHoldForSymbolsEnabled={isHoldForSymbolsEnabled}
          isAutoSuggestEnabled={isAutoSuggestEnabled}
          keyFontSize={keyFontSize}
          currentLanguage={currentLanguage}
          installedLanguages={installedLanguages}
          installedEmojiPacks={installedEmojiPacks}
          recentEmojis={recentEmojis}
          wordFrequencies={wordFrequencies}
          onAction={onAction}
          onTogglePin={onTogglePin}
          onDeleteSnippet={onDeleteSnippet}
          onSaveSnippet={onSaveSnippet}
          onClearHistory={onClearHistory}
          onDeleteSticker={onDeleteSticker}
          onInstallEmojiPack={onInstallEmojiPack}
          onThemeSelected={onThemeSelected}
          onToggleSound={onToggleSound}
          onToggleHaptic={onToggleHaptic}
          onToggleLaptopBar={onToggleLaptopBar}
          onToggleHoldForSymbols={onToggleHoldForSymbols}
          onToggleAutoSuggest={onToggleAutoSuggest}
          onKeyFontSizeChanged={onKeyFontSizeChanged}
          onCycleLanguage={onCycleLanguage}
          onRecordWordUsed={onRecordWordUsed}
          onOpenStickerStudio={onOpenStickerStudio}
        />
      </div>
    </div>
  );
};
