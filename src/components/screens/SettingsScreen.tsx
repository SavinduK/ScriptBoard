import React, { useState } from 'react';
import { CustomThemeConfig, KeyboardColors, KeyboardThemeType } from '../../types';
import {
  ArrowLeft,
  Palette,
  Wand2,
  Check,
  Type,
  Volume2,
  Smartphone,
  Laptop,
  Hash,
  Sparkles,
  ExternalLink,
} from 'lucide-react';
import { getTheme, GboardDark, GboardLight, CyberNavy } from '../../data/themes';

interface SettingsScreenProps {
  colors: KeyboardColors;
  currentThemeType: KeyboardThemeType;
  customThemeConfig: CustomThemeConfig;
  isSoundEnabled: boolean;
  isHapticEnabled: boolean;
  isLaptopBarEnabled: boolean;
  isHoldForSymbolsEnabled: boolean;
  isAutoSuggestEnabled: boolean;
  keyFontSize: number;
  onBack: () => void;
  onThemeSelected: (theme: KeyboardThemeType) => void;
  onCustomThemeChanged: (config: CustomThemeConfig) => void;
  onKeyFontSizeChanged: (size: number) => void;
  onToggleSound: (enabled: boolean) => void;
  onToggleHaptic: (enabled: boolean) => void;
  onToggleLaptopBar: (enabled: boolean) => void;
  onToggleHoldForSymbols: (enabled: boolean) => void;
  onToggleAutoSuggest: (enabled: boolean) => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  colors,
  currentThemeType,
  customThemeConfig,
  isSoundEnabled,
  isHapticEnabled,
  isLaptopBarEnabled,
  isHoldForSymbolsEnabled,
  isAutoSuggestEnabled,
  keyFontSize,
  onBack,
  onThemeSelected,
  onCustomThemeChanged,
  onKeyFontSizeChanged,
  onToggleSound,
  onToggleHaptic,
  onToggleLaptopBar,
  onToggleHoldForSymbols,
  onToggleAutoSuggest,
}) => {
  const [showRgbStudio, setShowRgbStudio] = useState(false);
  const [tempCustomBg, setTempCustomBg] = useState(customThemeConfig.background);
  const [tempCustomKeyBg, setTempCustomKeyBg] = useState(customThemeConfig.letterKeyBackground);
  const [tempCustomText, setTempCustomText] = useState(customThemeConfig.letterKeyTextColor);
  const [tempCustomAccent, setTempCustomAccent] = useState(customThemeConfig.enterKeyBackground);

  const themeList: { type: KeyboardThemeType; name: string; colors: KeyboardColors }[] = [
    { type: 'GBOARD_DARK', name: 'Classic Dark', colors: GboardDark },
    { type: 'GBOARD_LIGHT', name: 'Classic Light', colors: GboardLight },
    { type: 'CYBER_NAVY', name: 'Cyber Navy', colors: CyberNavy },
    { type: 'CUSTOM', name: 'Custom RGB', colors: getTheme('CUSTOM', customThemeConfig) },
  ];

  const handleApplyRgb = () => {
    onCustomThemeChanged({
      background: tempCustomBg,
      letterKeyBackground: tempCustomKeyBg,
      letterKeyTextColor: tempCustomText,
      enterKeyBackground: tempCustomAccent,
    });
    onThemeSelected('CUSTOM');
    setShowRgbStudio(false);
  };

  return (
    <div
      style={{ backgroundColor: colors.background }}
      className="flex-1 flex flex-col min-h-screen select-none pb-12"
    >
      {/* Top App Bar */}
      <div
        style={{ backgroundColor: colors.toolbarBackground }}
        className="w-full h-14 px-4 flex items-center border-b border-black/20 shrink-0"
      >
        <button
          type="button"
          onClick={onBack}
          style={{ color: colors.letterKeyTextColor }}
          className="p-1.5 rounded-lg hover:bg-white/10 active:scale-95 transition-all cursor-pointer mr-3"
          title="Back to Home"
        >
          <ArrowLeft size={20} />
        </button>
        <div className="flex flex-col">
          <h2
            style={{ color: colors.letterKeyTextColor }}
            className="text-base font-bold leading-tight"
          >
            Keyboard Settings
          </h2>
          <span
            style={{ color: colors.letterKeySecondaryTextColor }}
            className="text-[11px]"
          >
            Preferences, audio, and visual themes
          </span>
        </div>
      </div>

      {/* Main Settings Content */}
      <div className="flex-1 max-w-xl mx-auto w-full p-4 space-y-4">
        {/* SECTION 1: KEYBOARD THEMES */}
        <div
          style={{ backgroundColor: colors.letterKeyBackground }}
          className="p-4 rounded-2xl border border-black/15 shadow-md space-y-3"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <Palette size={20} style={{ color: colors.enterKeyBackground }} />
              <h3
                style={{ color: colors.letterKeyTextColor }}
                className="text-sm font-bold"
              >
                Keyboard Theme
              </h3>
            </div>
            <button
              type="button"
              onClick={() => {
                setTempCustomBg(customThemeConfig.background);
                setTempCustomKeyBg(customThemeConfig.letterKeyBackground);
                setTempCustomText(customThemeConfig.letterKeyTextColor);
                setTempCustomAccent(customThemeConfig.enterKeyBackground);
                setShowRgbStudio(true);
              }}
              style={{
                backgroundColor: colors.enterKeyBackground,
                color: colors.enterKeyTextColor,
              }}
              className="px-3 py-1 rounded-xl text-xs font-bold flex items-center space-x-1 shadow-sm hover:opacity-90 active:scale-95 cursor-pointer"
            >
              <Wand2 size={13} />
              <span>RGB Studio</span>
            </button>
          </div>

          <div className="grid grid-cols-2 gap-3 pt-1">
            {themeList.map((t) => {
              const isSelected = currentThemeType === t.type;
              return (
                <div
                  key={t.type}
                  onClick={() => {
                    onThemeSelected(t.type);
                    if (t.type === 'CUSTOM') {
                      setShowRgbStudio(true);
                    }
                  }}
                  style={{
                    backgroundColor: t.colors.background,
                    borderColor: isSelected ? colors.enterKeyBackground : `${t.colors.letterKeyBackground}80`,
                  }}
                  className={`p-3 rounded-xl border-2 flex flex-col items-center justify-center space-y-2 cursor-pointer transition-transform hover:scale-[1.02] active:scale-[0.98] shadow-sm relative`}
                >
                  <div className="flex items-center space-x-2">
                    <div
                      className="w-5 h-5 rounded-md"
                      style={{ backgroundColor: t.colors.letterKeyBackground }}
                    />
                    <div
                      className="w-5 h-5 rounded-md flex items-center justify-center"
                      style={{
                        backgroundColor: t.colors.enterKeyBackground,
                        color: t.colors.enterKeyTextColor,
                      }}
                    >
                      {isSelected && <Check size={12} />}
                    </div>
                  </div>
                  <span
                    className="text-xs font-bold"
                    style={{ color: t.colors.letterKeyTextColor }}
                  >
                    {t.name}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* SECTION 2: KEY CHARACTER FONT SIZE */}
        <div
          style={{ backgroundColor: colors.letterKeyBackground }}
          className="p-4 rounded-2xl border border-black/15 shadow-md space-y-3"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <Type size={20} style={{ color: colors.enterKeyBackground }} />
              <div>
                <h3
                  style={{ color: colors.letterKeyTextColor }}
                  className="text-sm font-bold"
                >
                  Key Character Font Size
                </h3>
                <p
                  style={{ color: colors.letterKeySecondaryTextColor }}
                  className="text-[11px]"
                >
                  Customize character size on keyboard keys
                </p>
              </div>
            </div>
            <span
              style={{
                backgroundColor: colors.enterKeyBackground,
                color: colors.enterKeyTextColor,
              }}
              className="px-2.5 py-0.5 rounded-lg text-xs font-mono font-bold"
            >
              {keyFontSize}sp
            </span>
          </div>

          {/* Preset Buttons */}
          <div className="grid grid-cols-4 gap-2 pt-1">
            {[
              { label: 'Small\n(20sp)', size: 20 },
              { label: 'Normal\n(24sp)', size: 24 },
              { label: 'Large\n(27sp)', size: 27 },
              { label: 'X-Large\n(30sp)', size: 30 },
            ].map((p) => {
              const isSelected = keyFontSize === p.size;
              return (
                <button
                  key={p.size}
                  type="button"
                  onClick={() => onKeyFontSizeChanged(p.size)}
                  style={{
                    backgroundColor: isSelected ? colors.enterKeyBackground : colors.background,
                    color: isSelected ? colors.enterKeyTextColor : colors.letterKeyTextColor,
                  }}
                  className="py-2 px-1 rounded-xl text-[11px] font-semibold whitespace-pre-line text-center transition-all cursor-pointer shadow-sm active:scale-95"
                >
                  {p.label}
                </button>
              );
            })}
          </div>

          {/* Slider */}
          <div className="pt-2">
            <input
              type="range"
              min={18}
              max={32}
              step={1}
              value={keyFontSize}
              onChange={(e) => onKeyFontSizeChanged(Number(e.target.value))}
              className="w-full accent-blue-400 cursor-pointer"
            />
          </div>

          {/* LIVE KEYCAP PREVIEW */}
          <div className="pt-2 space-y-1.5">
            <span
              className="text-[10px] font-bold uppercase tracking-wider"
              style={{ color: colors.letterKeySecondaryTextColor }}
            >
              LIVE KEYCAP PREVIEW
            </span>
            <div
              style={{ backgroundColor: colors.background }}
              className="p-2.5 rounded-xl flex items-center justify-between space-x-2"
            >
              {['Q', 'W', 'E', 'R', 'T'].map((char) => (
                <div
                  key={char}
                  style={{
                    backgroundColor: colors.letterKeyBackground,
                    color: colors.letterKeyTextColor,
                  }}
                  className="flex-1 h-11 rounded-lg flex items-center justify-center font-medium shadow-sm border border-black/10"
                >
                  <span style={{ fontSize: `${keyFontSize}px`, lineHeight: 1 }}>
                    {char}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* SECTION 3: FEEDBACK & ACTIONS */}
        <div
          style={{ backgroundColor: colors.letterKeyBackground }}
          className="p-4 rounded-2xl border border-black/15 shadow-md space-y-3.5"
        >
          <h3
            style={{ color: colors.letterKeyTextColor }}
            className="text-sm font-bold"
          >
            Feedback & Actions
          </h3>

          {/* Hold Key for Symbols */}
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3 pr-2">
              <Hash size={18} style={{ color: colors.enterKeyBackground }} />
              <div>
                <h4 style={{ color: colors.letterKeyTextColor }} className="text-xs font-bold">
                  Hold Key for Symbols
                </h4>
                <p style={{ color: colors.letterKeySecondaryTextColor }} className="text-[11px]">
                  Long press keys for numbers, symbols & toggleable alternates
                </p>
              </div>
            </div>
            <input
              type="checkbox"
              checked={isHoldForSymbolsEnabled}
              onChange={(e) => onToggleHoldForSymbols(e.target.checked)}
              className="w-5 h-5 accent-blue-500 rounded cursor-pointer"
            />
          </div>

          {/* Auto-suggest Words */}
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3 pr-2">
              <Sparkles size={18} style={{ color: colors.enterKeyBackground }} />
              <div>
                <h4 style={{ color: colors.letterKeyTextColor }} className="text-xs font-bold">
                  Auto-suggest Words
                </h4>
                <p style={{ color: colors.letterKeySecondaryTextColor }} className="text-[11px]">
                  Show 3 suggested word completions above the keyboard
                </p>
              </div>
            </div>
            <input
              type="checkbox"
              checked={isAutoSuggestEnabled}
              onChange={(e) => onToggleAutoSuggest(e.target.checked)}
              className="w-5 h-5 accent-blue-500 rounded cursor-pointer"
            />
          </div>

          {/* Keypress Sound */}
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3 pr-2">
              <Volume2 size={18} style={{ color: colors.enterKeyBackground }} />
              <div>
                <h4 style={{ color: colors.letterKeyTextColor }} className="text-xs font-bold">
                  Keypress Sound
                </h4>
                <p style={{ color: colors.letterKeySecondaryTextColor }} className="text-[11px]">
                  Audio click feedback when typing keys
                </p>
              </div>
            </div>
            <input
              type="checkbox"
              checked={isSoundEnabled}
              onChange={(e) => onToggleSound(e.target.checked)}
              className="w-5 h-5 accent-blue-500 rounded cursor-pointer"
            />
          </div>

          {/* Haptic Vibration */}
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3 pr-2">
              <Smartphone size={18} style={{ color: colors.enterKeyBackground }} />
              <div>
                <h4 style={{ color: colors.letterKeyTextColor }} className="text-xs font-bold">
                  Haptic Vibration
                </h4>
                <p style={{ color: colors.letterKeySecondaryTextColor }} className="text-[11px]">
                  Vibration response on key tap
                </p>
              </div>
            </div>
            <input
              type="checkbox"
              checked={isHapticEnabled}
              onChange={(e) => onToggleHaptic(e.target.checked)}
              className="w-5 h-5 accent-blue-500 rounded cursor-pointer"
            />
          </div>

          {/* Laptop Keys Bar */}
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3 pr-2">
              <Laptop size={18} style={{ color: colors.enterKeyBackground }} />
              <div>
                <h4 style={{ color: colors.letterKeyTextColor }} className="text-xs font-bold">
                  Laptop Keys Bar
                </h4>
                <p style={{ color: colors.letterKeySecondaryTextColor }} className="text-[11px]">
                  Quick Esc, Tab, Ctrl, Alt, and Arrows navigation row
                </p>
              </div>
            </div>
            <input
              type="checkbox"
              checked={isLaptopBarEnabled}
              onChange={(e) => onToggleLaptopBar(e.target.checked)}
              className="w-5 h-5 accent-blue-500 rounded cursor-pointer"
            />
          </div>
        </div>

        {/* SECTION 4: SYSTEM SETTINGS INFO */}
        <div
          style={{ backgroundColor: colors.functionKeyBackground }}
          className="p-4 rounded-2xl flex items-center justify-between cursor-pointer border border-black/10 shadow-sm hover:opacity-95"
        >
          <div>
            <h4 style={{ color: colors.letterKeyTextColor }} className="text-xs font-bold">
              KeyPro Keyboard Engine
            </h4>
            <p style={{ color: colors.letterKeySecondaryTextColor }} className="text-[11px]">
              Multi-language touch layout with full PC keys &amp; phrase expansion
            </p>
          </div>
          <ExternalLink size={18} style={{ color: colors.enterKeyBackground }} />
        </div>
      </div>

      {/* RGB Studio Custom Palette Modal */}
      {showRgbStudio && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div
            style={{ backgroundColor: colors.toolbarBackground }}
            className="w-full max-w-sm rounded-2xl p-4 shadow-2xl border border-white/10 flex flex-col space-y-3.5"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Wand2 size={18} style={{ color: colors.enterKeyBackground }} />
                <h3 className="text-sm font-bold" style={{ color: colors.letterKeyTextColor }}>
                  RGB Custom Theme Studio
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowRgbStudio(false)}
                className="text-xs opacity-60 hover:opacity-100"
                style={{ color: colors.letterKeyTextColor }}
              >
                ✕
              </button>
            </div>

            <p className="text-xs" style={{ color: colors.letterKeySecondaryTextColor }}>
              Customize your palette colors with immediate contrast checking.
            </p>

            <div className="space-y-2.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold" style={{ color: colors.letterKeyTextColor }}>
                  Keyboard Background
                </span>
                <input
                  type="color"
                  value={tempCustomBg}
                  onChange={(e) => setTempCustomBg(e.target.value)}
                  className="w-9 h-8 rounded border-none cursor-pointer bg-transparent"
                />
              </div>

              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold" style={{ color: colors.letterKeyTextColor }}>
                  Keycap Background
                </span>
                <input
                  type="color"
                  value={tempCustomKeyBg}
                  onChange={(e) => setTempCustomKeyBg(e.target.value)}
                  className="w-9 h-8 rounded border-none cursor-pointer bg-transparent"
                />
              </div>

              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold" style={{ color: colors.letterKeyTextColor }}>
                  Character Text Color
                </span>
                <input
                  type="color"
                  value={tempCustomText}
                  onChange={(e) => setTempCustomText(e.target.value)}
                  className="w-9 h-8 rounded border-none cursor-pointer bg-transparent"
                />
              </div>

              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold" style={{ color: colors.letterKeyTextColor }}>
                  Enter / Accent Key
                </span>
                <input
                  type="color"
                  value={tempCustomAccent}
                  onChange={(e) => setTempCustomAccent(e.target.value)}
                  className="w-9 h-8 rounded border-none cursor-pointer bg-transparent"
                />
              </div>
            </div>

            {/* Live Palette Preview */}
            <div
              style={{ backgroundColor: tempCustomBg }}
              className="p-3 rounded-xl border border-black/20 flex items-center justify-around space-x-2 mt-2"
            >
              {['K', 'E', 'Y'].map((letter) => (
                <div
                  key={letter}
                  style={{
                    backgroundColor: tempCustomKeyBg,
                    color: tempCustomText,
                  }}
                  className="w-10 h-10 rounded-lg flex items-center justify-center font-bold text-sm shadow-sm"
                >
                  {letter}
                </div>
              ))}
              <div
                style={{
                  backgroundColor: tempCustomAccent,
                  color: '#FFFFFF',
                }}
                className="w-12 h-10 rounded-lg flex items-center justify-center font-bold text-sm shadow-sm"
              >
                ↵
              </div>
            </div>

            <div className="flex items-center justify-end space-x-2 pt-2">
              <button
                type="button"
                onClick={() => setShowRgbStudio(false)}
                className="px-3 py-1.5 rounded-lg text-xs font-medium cursor-pointer"
                style={{
                  backgroundColor: colors.functionKeyBackground,
                  color: colors.letterKeyTextColor,
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleApplyRgb}
                className="px-4 py-1.5 rounded-lg text-xs font-bold cursor-pointer"
                style={{
                  backgroundColor: colors.enterKeyBackground,
                  color: colors.enterKeyTextColor,
                }}
              >
                Apply Custom Palette
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
