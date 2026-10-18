import React from 'react';
import { KeyboardColors } from '../../types';
import {
  Laptop,
  Monitor,
  ClipboardPaste,
  ClipboardList,
  Smile,
  Mic,
  Settings,
} from 'lucide-react';

interface KeyboardToolbarProps {
  colors: KeyboardColors;
  isLaptopBarVisible: boolean;
  isExtendedPcActive: boolean;
  isEmojiActive: boolean;
  isVoiceTypeActive: boolean;
  isClipboardActive: boolean;
  onToggleLaptopBar: () => void;
  onToggleExtendedPcKeys: () => void;
  onPasteCopiedContent: () => void;
  onOpenClipboard: () => void;
  onOpenEmoji: () => void;
  onTriggerVoiceType: () => void;
  onOpenSettings: () => void;
}

export const KeyboardToolbar: React.FC<KeyboardToolbarProps> = ({
  colors,
  isLaptopBarVisible,
  isExtendedPcActive,
  isEmojiActive,
  isVoiceTypeActive,
  isClipboardActive,
  onToggleLaptopBar,
  onToggleExtendedPcKeys,
  onPasteCopiedContent,
  onOpenClipboard,
  onOpenEmoji,
  onTriggerVoiceType,
  onOpenSettings,
}) => {
  return (
    <div
      style={{ backgroundColor: colors.toolbarBackground }}
      className="w-full h-10 sm:h-12 md:h-13 px-2 sm:px-4 md:px-6 flex items-center justify-between border-b border-black/15 select-none"
    >
      <div className="flex items-center space-x-1 sm:space-x-2 md:space-x-3">
        {/* Toggle Laptop Row */}
        <button
          type="button"
          onClick={onToggleLaptopBar}
          title="Toggle Laptop Keys Bar (Esc, Tab, Ctrl, Arrows)"
          className={`p-1.5 sm:p-2 rounded-lg sm:rounded-xl transition-colors flex items-center justify-center cursor-pointer ${
            isLaptopBarVisible ? 'bg-black/20' : 'hover:bg-white/10'
          }`}
          style={{
            color: isLaptopBarVisible ? colors.toolbarIconActiveTint : colors.toolbarIconTint,
          }}
        >
          <Laptop className="w-4 h-4 sm:w-5 sm:h-5 md:w-5 md:h-5" />
        </button>

        {/* Toggle Extended Full PC/Desktop Keyboard */}
        <button
          type="button"
          onClick={onToggleExtendedPcKeys}
          title="Switch to Complete Laptop Layout (F1-F12, Esc, Win)"
          className={`p-1.5 sm:p-2 rounded-lg sm:rounded-xl transition-colors flex items-center justify-center cursor-pointer ${
            isExtendedPcActive ? 'bg-black/20' : 'hover:bg-white/10'
          }`}
          style={{
            color: isExtendedPcActive ? colors.toolbarIconActiveTint : colors.toolbarIconTint,
          }}
        >
          <Monitor className="w-4 h-4 sm:w-5 sm:h-5 md:w-5 md:h-5" />
        </button>

        {/* Direct Paste */}
        <button
          type="button"
          onClick={onPasteCopiedContent}
          title="Quick Paste Last Copied Text"
          className="p-1.5 sm:p-2 rounded-lg sm:rounded-xl transition-colors flex items-center justify-center hover:bg-white/10 cursor-pointer"
          style={{ color: colors.toolbarIconTint }}
        >
          <ClipboardPaste className="w-4 h-4 sm:w-5 sm:h-5 md:w-5 md:h-5" />
        </button>

        {/* In-Keyboard Clipboard Drawer */}
        <button
          type="button"
          onClick={onOpenClipboard}
          title="Open Saved Clipboard History"
          className={`p-1.5 sm:p-2 rounded-lg sm:rounded-xl transition-colors flex items-center justify-center cursor-pointer ${
            isClipboardActive ? 'bg-black/20' : 'hover:bg-white/10'
          }`}
          style={{
            color: isClipboardActive ? colors.toolbarIconActiveTint : colors.toolbarIconTint,
          }}
        >
          <ClipboardList className="w-4 h-4 sm:w-5 sm:h-5 md:w-5 md:h-5" />
        </button>
      </div>

      <div className="flex items-center space-x-1 sm:space-x-2 md:space-x-3">
        {/* Emojis & Stickers Drawer */}
        <button
          type="button"
          onClick={onOpenEmoji}
          title="Emojis, Kaomoji, Ancient Symbols & Stickers"
          className={`p-1.5 sm:p-2 rounded-lg sm:rounded-xl transition-colors flex items-center justify-center cursor-pointer ${
            isEmojiActive ? 'bg-black/20' : 'hover:bg-white/10'
          }`}
          style={{
            color: isEmojiActive ? colors.toolbarIconActiveTint : colors.toolbarIconTint,
          }}
        >
          <Smile className="w-4 h-4 sm:w-5 sm:h-5 md:w-5 md:h-5" />
        </button>

        {/* Voice Typing */}
        <button
          type="button"
          onClick={onTriggerVoiceType}
          title="Voice Typing Speech Recognition"
          className={`p-1.5 sm:p-2 rounded-lg sm:rounded-xl transition-colors flex items-center justify-center cursor-pointer ${
            isVoiceTypeActive ? 'bg-black/20' : 'hover:bg-white/10'
          }`}
          style={{
            color: isVoiceTypeActive ? colors.toolbarIconActiveTint : colors.toolbarIconTint,
          }}
        >
          <Mic className="w-4 h-4 sm:w-5 sm:h-5 md:w-5 md:h-5" />
        </button>

        {/* In-Keyboard Settings */}
        <button
          type="button"
          onClick={onOpenSettings}
          title="In-Keyboard Rapid Settings"
          className="p-1.5 sm:p-2 rounded-lg sm:rounded-xl transition-colors flex items-center justify-center hover:bg-white/10 cursor-pointer"
          style={{ color: colors.toolbarIconTint }}
        >
          <Settings className="w-4 h-4 sm:w-5 sm:h-5 md:w-5 md:h-5" />
        </button>
      </div>
    </div>
  );
};
