export type MainScreenSection =
  | 'HUB'
  | 'TEST_KEYBOARD'
  | 'SETTINGS_PAGE'
  | 'CLIPBOARD_HISTORY'
  | 'STICKERS'
  | 'LANGUAGES'
  | 'EMOJI_PACKS';

export type KeyboardLayoutMode =
  | 'TEXT'
  | 'SYMBOLS_1'
  | 'SYMBOLS_2'
  | 'NUMPAD'
  | 'EXTENDED_PC'
  | 'EMOJI'
  | 'CLIPBOARD'
  | 'SETTINGS'
  | 'STICKERS'
  | 'VOICE_TYPING';

export type ShiftState = 'OFF' | 'ONCE' | 'CAPS_LOCKED';

export type KeyboardThemeType =
  | 'GBOARD_DARK'
  | 'GBOARD_LIGHT'
  | 'CYBER_NAVY'
  | 'CUSTOM';

export interface KeyboardColors {
  background: string;
  toolbarBackground: string;
  toolbarIconTint: string;
  toolbarIconActiveTint: string;
  letterKeyBackground: string;
  letterKeyTextColor: string;
  letterKeySecondaryTextColor: string;
  functionKeyBackground: string;
  functionKeyTextColor: string;
  enterKeyBackground: string;
  enterKeyTextColor: string;
  laptopRowBackground: string;
  laptopKeyBackground: string;
  laptopKeyTextColor: string;
  laptopKeyActiveBackground: string;
  laptopKeyActiveTextColor: string;
  popupBackground: string;
  popupTextColor: string;
  chipBackground: string;
  chipTextColor: string;
  isDark: boolean;
}

export interface CustomThemeConfig {
  background: string;
  letterKeyBackground: string;
  letterKeyTextColor: string;
  enterKeyBackground: string;
}

export interface SnippetEntity {
  id: number;
  title: string;
  content: string;
  isPinned: boolean;
  category: string;
  shortcut: string;
  timestamp: number;
  imageUri?: string | null;
  isImage?: boolean;
  mimeType?: string | null;
}

export interface StickerEntity {
  id: number;
  filePath: string;
  name: string;
  source: 'whatsapp' | 'gallery' | 'imported';
  dateAdded: number;
  isFavorite: boolean;
  isAnimated: boolean;
  usageCount: number;
}

export interface WordFrequencyEntity {
  word: string;
  frequency: number;
  lastUsed: number;
}

export type KeyActionType =
  | { type: 'INSERT_TEXT'; text: string }
  | { type: 'INSERT_STICKER'; filePath: string; name: string }
  | { type: 'INSERT_SINHALA_PILLAM'; baseWithPillam: string; modifier: string }
  | { type: 'SWITCH_LANGUAGE' }
  | { type: 'BACKSPACE' }
  | { type: 'DELETE_FORWARD' }
  | { type: 'ENTER' }
  | { type: 'TOGGLE_SHIFT' }
  | { type: 'SWITCH_TO_SYMBOLS' }
  | { type: 'SWITCH_TO_LETTERS' }
  | { type: 'SWITCH_TO_MORE_SYMBOLS' }
  | { type: 'SWITCH_TO_NUMPAD' }
  | { type: 'SWITCH_TO_EXTENDED_PC' }
  | { type: 'SWITCH_TO_EMOJI' }
  | { type: 'SWITCH_TO_CLIPBOARD' }
  | { type: 'SPACE' }
  | { type: 'ESCAPE' }
  | { type: 'TAB' }
  | { type: 'TOGGLE_CTRL' }
  | { type: 'TOGGLE_ALT' }
  | { type: 'CURSOR_LEFT' }
  | { type: 'CURSOR_RIGHT' }
  | { type: 'CURSOR_UP' }
  | { type: 'CURSOR_DOWN' }
  | { type: 'HOME' }
  | { type: 'END' }
  | { type: 'PAGE_UP' }
  | { type: 'PAGE_DOWN' }
  | { type: 'SELECT_ALL' }
  | { type: 'COPY' }
  | { type: 'CUT' }
  | { type: 'PASTE' }
  | { type: 'UNDO' }
  | { type: 'REDO' }
  | { type: 'FUNCTION_KEY'; fNumber: number }
  | { type: 'EXPAND_SHORTCUT'; shortcutText: string; fullContent: string }
  | { type: 'APPLY_SUGGESTION'; typedWord: string; suggestedWord: string };

export interface KeyItem {
  primaryText: string;
  secondaryText?: string | null;
  action: KeyActionType;
  weight?: number;
  isFunctional?: boolean;
  isAccent?: boolean;
  isActiveModifier?: boolean;
  testTag: string;
}

export interface LanguagePack {
  id: string;
  name: string;
  nativeName: string;
  flag: string;
  version: string;
  description: string;
  file: string;
  sizeBytes: number;
  sizeDisplay: string;
  wordCount: number;
  isBuiltIn?: boolean;
}

export interface EmojiPack {
  id: string;
  name: string;
  nativeName: string;
  icon: string;
  version: string;
  description: string;
  file: string;
  sizeBytes: number;
  sizeDisplay: string;
  itemCount: number;
  category: string;
}

export interface EmojiPackItem {
  symbol: string;
  name: string;
  meaning: string;
  category?: string;
}
