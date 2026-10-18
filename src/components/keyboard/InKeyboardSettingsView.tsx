import React from 'react';
import { KeyboardColors, KeyboardThemeType } from '../../types';
import { ArrowLeft, Check, Volume2, VolumeX, Smartphone, Laptop, Hash, Sparkles } from 'lucide-react';

interface InKeyboardSettingsViewProps {
  colors: KeyboardColors;
  currentThemeType: KeyboardThemeType;
  isSoundEnabled: boolean;
  isHapticEnabled: boolean;
  isLaptopBarEnabled: boolean;
  isHoldForSymbolsEnabled: boolean;
  isAutoSuggestEnabled: boolean;
  keyFontSize: number;
  onThemeSelected: (theme: KeyboardThemeType) => void;
  onToggleSound: (enabled: boolean) => void;
  onToggleHaptic: (enabled: boolean) => void;
  onToggleLaptopBar: (enabled: boolean) => void;
  onToggleHoldForSymbols: (enabled: boolean) => void;
  onToggleAutoSuggest: (enabled: boolean) => void;
  onKeyFontSizeChanged: (size: number) => void;
  onBackToLetters: () => void;
}

export const InKeyboardSettingsView: React.FC<InKeyboardSettingsViewProps> = ({
  colors,
  currentThemeType,
  isSoundEnabled,
  isHapticEnabled,
  isLaptopBarEnabled,
  isHoldForSymbolsEnabled,
  isAutoSuggestEnabled,
  keyFontSize,
  onThemeSelected,
  onToggleSound,
  onToggleHaptic,
  onToggleLaptopBar,
  onToggleHoldForSymbols,
  onToggleAutoSuggest,
  onKeyFontSizeChanged,
  onBackToLetters,
}) => {
  return (
    <div
      style={{ backgroundColor: colors.background }}
      className="w-full h-[290px] flex flex-col justify-between p-2 select-none overflow-y-auto"
    >
      {/* Top Header */}
      <div className="flex items-center justify-between pb-1.5 border-b border-black/15">
        <button
          type="button"
          onClick={onBackToLetters}
          className="flex items-center space-x-1.5 px-3 py-1 rounded-lg text-xs font-semibold cursor-pointer"
          style={{
            backgroundColor: colors.functionKeyBackground,
            color: colors.functionKeyTextColor,
          }}
        >
          <ArrowLeft size={14} />
          <span>Keyboard</span>
        </button>

        <span
          className="text-xs font-bold"
          style={{ color: colors.letterKeyTextColor }}
        >
          In-Keyboard Rapid Settings
        </span>

        <button
          type="button"
          onClick={onBackToLetters}
          className="px-3 py-1 rounded-lg text-xs font-bold cursor-pointer"
          style={{
            backgroundColor: colors.enterKeyBackground,
            color: colors.enterKeyTextColor,
          }}
        >
          Done
        </button>
      </div>

      {/* Main Settings List */}
      <div className="flex-1 py-1 space-y-2 overflow-y-auto pr-1">
        {/* Theme Picker Strip */}
        <div className="space-y-1">
          <span className="text-[10px] font-bold uppercase tracking-wider" style={{ color: colors.letterKeySecondaryTextColor }}>
            Color Theme
          </span>
          <div className="grid grid-cols-4 gap-1.5">
            {[
              { id: 'GBOARD_DARK' as KeyboardThemeType, label: 'Dark', bg: '#1E1F24', accent: '#8AB4F8' },
              { id: 'GBOARD_LIGHT' as KeyboardThemeType, label: 'Light', bg: '#ECEFF1', accent: '#0B57D0' },
              { id: 'CYBER_NAVY' as KeyboardThemeType, label: 'Cyber', bg: '#0A111E', accent: '#00E5FF' },
              { id: 'CUSTOM' as KeyboardThemeType, label: 'Custom', bg: '#1B263B', accent: '#00B4D8' },
            ].map((theme) => {
              const isSelected = currentThemeType === theme.id;
              return (
                <button
                  key={theme.id}
                  type="button"
                  onClick={() => onThemeSelected(theme.id)}
                  style={{
                    backgroundColor: theme.bg,
                    borderColor: isSelected ? theme.accent : 'transparent',
                  }}
                  className={`py-1.5 px-1 rounded-lg border-2 flex items-center justify-center space-x-1 transition-all cursor-pointer shadow-sm`}
                >
                  <span
                    className="w-2.5 h-2.5 rounded-full"
                    style={{ backgroundColor: theme.accent }}
                  />
                  <span
                    className="text-[11px] font-bold"
                    style={{ color: theme.id === 'GBOARD_LIGHT' ? '#1F1F1F' : '#E8EAED' }}
                  >
                    {theme.label}
                  </span>
                  {isSelected && <Check size={11} style={{ color: theme.accent }} />}
                </button>
              );
            })}
          </div>
        </div>

        {/* Font Size Selector */}
        <div className="space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold uppercase tracking-wider" style={{ color: colors.letterKeySecondaryTextColor }}>
              Key Font Size
            </span>
            <span className="text-[10px] font-mono font-bold" style={{ color: colors.enterKeyBackground }}>
              {keyFontSize}sp
            </span>
          </div>
          <div className="grid grid-cols-4 gap-1.5">
            {[20, 24, 27, 30].map((size) => {
              const isSelected = keyFontSize === size;
              return (
                <button
                  key={size}
                  type="button"
                  onClick={() => onKeyFontSizeChanged(size)}
                  style={{
                    backgroundColor: isSelected ? colors.enterKeyBackground : colors.letterKeyBackground,
                    color: isSelected ? colors.enterKeyTextColor : colors.letterKeyTextColor,
                  }}
                  className="py-1 rounded-lg text-xs font-semibold flex items-center justify-center cursor-pointer transition-colors shadow-sm"
                >
                  {size}sp
                </button>
              );
            })}
          </div>
        </div>

        {/* Fast Toggles Grid */}
        <div className="grid grid-cols-2 gap-1.5 pt-1">
          {/* Laptop Bar */}
          <button
            type="button"
            onClick={() => onToggleLaptopBar(!isLaptopBarEnabled)}
            style={{
              backgroundColor: isLaptopBarEnabled ? colors.letterKeyBackground : colors.functionKeyBackground,
              borderColor: isLaptopBarEnabled ? colors.enterKeyBackground : 'transparent',
            }}
            className="p-1.5 rounded-lg border flex items-center justify-between cursor-pointer shadow-sm text-left"
          >
            <div className="flex items-center space-x-1.5">
              <Laptop size={14} style={{ color: isLaptopBarEnabled ? colors.enterKeyBackground : colors.letterKeySecondaryTextColor }} />
              <span className="text-[11px] font-medium" style={{ color: colors.letterKeyTextColor }}>
                Laptop Keys Bar
              </span>
            </div>
            <span
              className={`w-3.5 h-3.5 rounded-full flex items-center justify-center text-[9px] font-bold ${
                isLaptopBarEnabled ? 'bg-green-500 text-white' : 'bg-gray-500/30 text-transparent'
              }`}
            >
              ✓
            </span>
          </button>

          {/* Auto-suggest words */}
          <button
            type="button"
            onClick={() => onToggleAutoSuggest(!isAutoSuggestEnabled)}
            style={{
              backgroundColor: isAutoSuggestEnabled ? colors.letterKeyBackground : colors.functionKeyBackground,
              borderColor: isAutoSuggestEnabled ? colors.enterKeyBackground : 'transparent',
            }}
            className="p-1.5 rounded-lg border flex items-center justify-between cursor-pointer shadow-sm text-left"
          >
            <div className="flex items-center space-x-1.5">
              <Sparkles size={14} style={{ color: isAutoSuggestEnabled ? colors.enterKeyBackground : colors.letterKeySecondaryTextColor }} />
              <span className="text-[11px] font-medium" style={{ color: colors.letterKeyTextColor }}>
                Auto-Suggest Words
              </span>
            </div>
            <span
              className={`w-3.5 h-3.5 rounded-full flex items-center justify-center text-[9px] font-bold ${
                isAutoSuggestEnabled ? 'bg-green-500 text-white' : 'bg-gray-500/30 text-transparent'
              }`}
            >
              ✓
            </span>
          </button>

          {/* Keypress Sound */}
          <button
            type="button"
            onClick={() => onToggleSound(!isSoundEnabled)}
            style={{
              backgroundColor: isSoundEnabled ? colors.letterKeyBackground : colors.functionKeyBackground,
              borderColor: isSoundEnabled ? colors.enterKeyBackground : 'transparent',
            }}
            className="p-1.5 rounded-lg border flex items-center justify-between cursor-pointer shadow-sm text-left"
          >
            <div className="flex items-center space-x-1.5">
              {isSoundEnabled ? (
                <Volume2 size={14} style={{ color: colors.enterKeyBackground }} />
              ) : (
                <VolumeX size={14} style={{ color: colors.letterKeySecondaryTextColor }} />
              )}
              <span className="text-[11px] font-medium" style={{ color: colors.letterKeyTextColor }}>
                Key Audio Clicks
              </span>
            </div>
            <span
              className={`w-3.5 h-3.5 rounded-full flex items-center justify-center text-[9px] font-bold ${
                isSoundEnabled ? 'bg-green-500 text-white' : 'bg-gray-500/30 text-transparent'
              }`}
            >
              ✓
            </span>
          </button>

          {/* Haptic Vibration */}
          <button
            type="button"
            onClick={() => onToggleHaptic(!isHapticEnabled)}
            style={{
              backgroundColor: isHapticEnabled ? colors.letterKeyBackground : colors.functionKeyBackground,
              borderColor: isHapticEnabled ? colors.enterKeyBackground : 'transparent',
            }}
            className="p-1.5 rounded-lg border flex items-center justify-between cursor-pointer shadow-sm text-left"
          >
            <div className="flex items-center space-x-1.5">
              <Smartphone size={14} style={{ color: isHapticEnabled ? colors.enterKeyBackground : colors.letterKeySecondaryTextColor }} />
              <span className="text-[11px] font-medium" style={{ color: colors.letterKeyTextColor }}>
                Haptic Vibration
              </span>
            </div>
            <span
              className={`w-3.5 h-3.5 rounded-full flex items-center justify-center text-[9px] font-bold ${
                isHapticEnabled ? 'bg-green-500 text-white' : 'bg-gray-500/30 text-transparent'
              }`}
            >
              ✓
            </span>
          </button>

          {/* Hold for symbols */}
          <button
            type="button"
            onClick={() => onToggleHoldForSymbols(!isHoldForSymbolsEnabled)}
            style={{
              backgroundColor: isHoldForSymbolsEnabled ? colors.letterKeyBackground : colors.functionKeyBackground,
              borderColor: isHoldForSymbolsEnabled ? colors.enterKeyBackground : 'transparent',
            }}
            className="p-1.5 rounded-lg border flex items-center justify-between cursor-pointer shadow-sm text-left col-span-2"
          >
            <div className="flex items-center space-x-1.5">
              <Hash size={14} style={{ color: isHoldForSymbolsEnabled ? colors.enterKeyBackground : colors.letterKeySecondaryTextColor }} />
              <span className="text-[11px] font-medium" style={{ color: colors.letterKeyTextColor }}>
                Hold Key for Symbols / Numbers
              </span>
            </div>
            <span
              className={`w-3.5 h-3.5 rounded-full flex items-center justify-center text-[9px] font-bold ${
                isHoldForSymbolsEnabled ? 'bg-green-500 text-white' : 'bg-gray-500/30 text-transparent'
              }`}
            >
              ✓
            </span>
          </button>
        </div>
      </div>
    </div>
  );
};
