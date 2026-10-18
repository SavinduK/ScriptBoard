import React, { useState } from 'react';
import { KeyboardColors, KeyboardThemeType, MainScreenSection } from '../../types';
import {
  Keyboard,
  Settings,
  ClipboardList,
  Sparkles,
  Languages,
  ChevronDown,
  CheckCircle2,
} from 'lucide-react';

interface HubScreenProps {
  colors: KeyboardColors;
  currentThemeType: KeyboardThemeType;
  onNavigate: (section: MainScreenSection) => void;
}

export const HubScreen: React.FC<HubScreenProps> = ({
  colors,
  currentThemeType,
  onNavigate,
}) => {
  const [showSetupMenu, setShowSetupMenu] = useState(false);
  const [setupStep1Done, setSetupStep1Done] = useState(true);
  const [setupStep2Done, setSetupStep2Done] = useState(true);

  const themeDisplayNames: Record<KeyboardThemeType, string> = {
    GBOARD_DARK: 'Classic Dark',
    GBOARD_LIGHT: 'Classic Light',
    CYBER_NAVY: 'Cyber Navy',
    CUSTOM: 'Custom RGB Studio',
  };

  return (
    <div
      style={{ backgroundColor: colors.background }}
      className="flex-1 flex flex-col min-h-screen text-slate-100 select-none pb-12"
    >
      {/* Top App Bar with Setup Dropdown */}
      <div
        style={{ backgroundColor: colors.toolbarBackground }}
        className="w-full h-14 px-4 flex items-center justify-between border-b border-black/20 shadow-md relative z-30"
      >
        <div className="flex items-center space-x-3">
          <div
            style={{
              backgroundColor: colors.enterKeyBackground,
              color: colors.enterKeyTextColor,
            }}
            className="w-9 h-9 rounded-xl flex items-center justify-center shadow-md font-bold"
          >
            <Keyboard size={20} />
          </div>
          <div className="flex flex-col">
            <h1
              style={{ color: colors.letterKeyTextColor }}
              className="text-base font-bold tracking-tight leading-tight"
            >
              KeyPro Keyboard
            </h1>
            <span
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-[11px] font-medium"
            >
              {themeDisplayNames[currentThemeType]}
            </span>
          </div>
        </div>

        {/* Setup Dropdown */}
        <div className="relative">
          <button
            type="button"
            onClick={() => setShowSetupMenu((prev) => !prev)}
            style={{
              backgroundColor: colors.enterKeyBackground,
              color: colors.enterKeyTextColor,
            }}
            className="px-3 py-1.5 rounded-xl text-xs font-bold flex items-center space-x-1.5 shadow-sm hover:opacity-90 active:scale-95 transition-all cursor-pointer"
          >
            <span>Setup</span>
            <ChevronDown size={14} className={showSetupMenu ? 'rotate-180 transition-transform' : 'transition-transform'} />
          </button>

          {showSetupMenu && (
            <>
              <div
                className="fixed inset-0 z-40"
                onClick={() => setShowSetupMenu(false)}
              />
              <div
                style={{ backgroundColor: colors.toolbarBackground }}
                className="absolute right-0 mt-2 w-64 rounded-2xl shadow-2xl border border-white/10 p-2 z-50 flex flex-col space-y-1"
              >
                <button
                  type="button"
                  onClick={() => {
                    setSetupStep1Done((prev) => !prev);
                  }}
                  className="w-full p-2.5 rounded-xl hover:bg-white/5 flex items-start space-x-2.5 text-left transition-colors cursor-pointer"
                >
                  <CheckCircle2
                    size={16}
                    className={`mt-0.5 shrink-0 ${
                      setupStep1Done ? 'text-green-400' : 'text-gray-400'
                    }`}
                  />
                  <div>
                    <h4
                      style={{ color: colors.letterKeyTextColor }}
                      className="text-xs font-bold"
                    >
                      1. Enable in Settings
                    </h4>
                    <p
                      style={{ color: colors.letterKeySecondaryTextColor }}
                      className="text-[11px] leading-tight mt-0.5"
                    >
                      Turn on KeyPro in your virtual keyboards list
                    </p>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setSetupStep2Done((prev) => !prev);
                  }}
                  className="w-full p-2.5 rounded-xl hover:bg-white/5 flex items-start space-x-2.5 text-left transition-colors cursor-pointer"
                >
                  <CheckCircle2
                    size={16}
                    className={`mt-0.5 shrink-0 ${
                      setupStep2Done ? 'text-green-400' : 'text-gray-400'
                    }`}
                  />
                  <div>
                    <h4
                      style={{ color: colors.letterKeyTextColor }}
                      className="text-xs font-bold"
                    >
                      2. Select KeyPro Keyboard
                    </h4>
                    <p
                      style={{ color: colors.letterKeySecondaryTextColor }}
                      className="text-[11px] leading-tight mt-0.5"
                    >
                      Set KeyPro as active input method
                    </p>
                  </div>
                </button>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Hub Cards Container */}
      <div className="flex-1 max-w-xl mx-auto w-full p-4 flex flex-col space-y-4">
        {/* CARD A: TEST KEYBOARD */}
        <button
          type="button"
          onClick={() => onNavigate('TEST_KEYBOARD')}
          style={{ backgroundColor: colors.letterKeyBackground }}
          className="w-full p-4 rounded-2xl border border-black/15 shadow-md flex items-start space-x-4 text-left transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer group"
        >
          <div
            style={{
              backgroundColor: `${colors.enterKeyBackground}26`,
              color: colors.enterKeyBackground,
            }}
            className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0"
          >
            <Keyboard size={24} />
          </div>
          <div className="flex-1">
            <h3
              style={{ color: colors.letterKeyTextColor }}
              className="text-base font-bold group-hover:text-blue-400 transition-colors"
            >
              Test Keyboard & Layout
            </h3>
            <p
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-xs leading-relaxed mt-1"
            >
              Test standard typing layout, hold-to-delete backspace, PC keys (Ctrl, Alt, Tab, Esc), emojis, and the quick paste button.
            </p>
          </div>
        </button>

        {/* CARD B: SETTINGS */}
        <button
          type="button"
          onClick={() => onNavigate('SETTINGS_PAGE')}
          style={{ backgroundColor: colors.letterKeyBackground }}
          className="w-full p-4 rounded-2xl border border-black/15 shadow-md flex items-start space-x-4 text-left transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer group"
        >
          <div
            style={{
              backgroundColor: `${colors.enterKeyBackground}26`,
              color: colors.enterKeyBackground,
            }}
            className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0"
          >
            <Settings size={24} />
          </div>
          <div className="flex-1">
            <h3
              style={{ color: colors.letterKeyTextColor }}
              className="text-base font-bold group-hover:text-blue-400 transition-colors"
            >
              Settings
            </h3>
            <p
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-xs leading-relaxed mt-1"
            >
              Customize keyboard color themes, typing audio sounds, haptic feedback vibration, and laptop quick access bar.
            </p>
          </div>
        </button>

        {/* CARD C: SAVED CLIPBOARD HISTORY */}
        <button
          type="button"
          onClick={() => onNavigate('CLIPBOARD_HISTORY')}
          style={{ backgroundColor: colors.letterKeyBackground }}
          className="w-full p-4 rounded-2xl border border-black/15 shadow-md flex items-start space-x-4 text-left transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer group"
        >
          <div
            style={{
              backgroundColor: `${colors.enterKeyBackground}26`,
              color: colors.enterKeyBackground,
            }}
            className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0"
          >
            <ClipboardList size={24} />
          </div>
          <div className="flex-1">
            <h3
              style={{ color: colors.letterKeyTextColor }}
              className="text-base font-bold group-hover:text-blue-400 transition-colors"
            >
              Clipboard History
            </h3>
            <p
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-xs leading-relaxed mt-1"
            >
              Browse auto-saved clipboard clips, search frequently used texts, and manage your pinned quick snippets.
            </p>
          </div>
        </button>

        {/* CARD D: STICKERS */}
        <button
          type="button"
          onClick={() => onNavigate('STICKERS')}
          style={{ backgroundColor: colors.letterKeyBackground }}
          className="w-full p-4 rounded-2xl border border-black/15 shadow-md flex items-start space-x-4 text-left transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer group"
        >
          <div
            style={{
              backgroundColor: `${colors.enterKeyBackground}26`,
              color: colors.enterKeyBackground,
            }}
            className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0"
          >
            <Sparkles size={24} />
          </div>
          <div className="flex-1">
            <h3
              style={{ color: colors.letterKeyTextColor }}
              className="text-base font-bold group-hover:text-blue-400 transition-colors"
            >
              Stickers
            </h3>
            <p
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-xs leading-relaxed mt-1"
            >
              Import sticker packs from WhatsApp or storage folders, and send them directly in your chats.
            </p>
          </div>
        </button>

        {/* CARD E: LANGUAGE PACKAGES */}
        <button
          type="button"
          onClick={() => onNavigate('LANGUAGES')}
          style={{ backgroundColor: colors.letterKeyBackground }}
          className="w-full p-4 rounded-2xl border border-black/15 shadow-md flex items-start space-x-4 text-left transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer group"
        >
          <div
            style={{
              backgroundColor: `${colors.enterKeyBackground}26`,
              color: colors.enterKeyBackground,
            }}
            className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0"
          >
            <Languages size={24} />
          </div>
          <div className="flex-1">
            <h3
              style={{ color: colors.letterKeyTextColor }}
              className="text-base font-bold group-hover:text-blue-400 transition-colors"
            >
              Language Packages
            </h3>
            <p
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-xs leading-relaxed mt-1"
            >
              Download optional language layouts, switch active languages, and manage multi-language typing.
            </p>
          </div>
        </button>

        {/* CARD F: EMOJI & SYMBOL PACKAGES */}
        <button
          type="button"
          onClick={() => onNavigate('EMOJI_PACKS')}
          style={{ backgroundColor: colors.letterKeyBackground }}
          className="w-full p-4 rounded-2xl border border-black/15 shadow-md flex items-start space-x-4 text-left transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer group"
        >
          <div
            style={{
              backgroundColor: `${colors.enterKeyBackground}26`,
              color: colors.enterKeyBackground,
            }}
            className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0 font-serif text-xl"
          >
            ᚱ
          </div>
          <div className="flex-1">
            <h3
              style={{ color: colors.letterKeyTextColor }}
              className="text-base font-bold group-hover:text-blue-400 transition-colors"
            >
              Emoji & Symbol Packages (Runic, Hieroglyphs, etc.)
            </h3>
            <p
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-xs leading-relaxed mt-1"
            >
              Download ancient rune sets, Egyptian hieroglyphs, Kaomoji, and alchemical symbols to expand your keyboard emoji drawer.
            </p>
          </div>
        </button>
      </div>
    </div>
  );
};
