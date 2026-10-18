import React, { useRef } from 'react';
import { KeyItem, KeyboardColors } from '../../types';

interface KeyCapProps {
  item: KeyItem;
  colors: KeyboardColors;
  heightClass?: string;
  characterFontSize?: number;
  holdForSymbols?: boolean;
  onKeyClick: (item: KeyItem) => void;
  onKeyLongClick?: (item: KeyItem) => void;
}

export const KeyCap: React.FC<KeyCapProps> = ({
  item,
  colors,
  heightClass = 'h-12 sm:h-14 md:h-16 lg:h-20',
  characterFontSize = 22,
  holdForSymbols = true,
  onKeyClick,
  onKeyLongClick,
}) => {
  const timerRef = useRef<number | null>(null);
  const didLongPressRef = useRef<boolean>(false);

  const handlePointerDown = (e: React.PointerEvent) => {
    e.preventDefault();
    didLongPressRef.current = false;
    timerRef.current = window.setTimeout(() => {
      didLongPressRef.current = true;
      if (onKeyLongClick) {
        onKeyLongClick(item);
      }
    }, 450);
  };

  const handlePointerUp = (e: React.PointerEvent) => {
    e.preventDefault();
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    if (!didLongPressRef.current) {
      onKeyClick(item);
    }
  };

  const handlePointerCancel = () => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  };

  // Determine key styling based on type and active state
  let bgColor = colors.letterKeyBackground;
  let textColor = colors.letterKeyTextColor;

  if (item.isAccent) {
    bgColor = colors.enterKeyBackground;
    textColor = colors.enterKeyTextColor;
  } else if (item.isActiveModifier) {
    bgColor = colors.enterKeyBackground;
    textColor = colors.enterKeyTextColor;
  } else if (item.isFunctional) {
    bgColor = colors.functionKeyBackground;
    textColor = colors.functionKeyTextColor;
  }

  // Weight defines flex ratio relative to 1
  const flexGrow = item.weight || 1;
  const isMultiChar = item.primaryText.length > 2;

  return (
    <button
      type="button"
      data-testid={item.testTag}
      onPointerDown={handlePointerDown}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerCancel}
      onContextMenu={(e) => e.preventDefault()}
      style={{
        flex: `${flexGrow} 1 0%`,
        backgroundColor: bgColor,
        color: textColor,
      }}
      className={`relative m-[2.5px] sm:m-1 md:m-1.5 lg:m-2 rounded-lg sm:rounded-xl md:rounded-2xl ${heightClass} flex flex-col items-center justify-center transition-all duration-75 active:scale-95 hover:brightness-110 shadow-sm md:shadow overflow-hidden select-none touch-manipulation cursor-pointer border border-black/15`}
    >
      {/* Secondary alternate character in upper corner if enabled */}
      {holdForSymbols && item.secondaryText && (
        <span
          style={{
            color: item.isAccent || item.isActiveModifier ? textColor : colors.letterKeySecondaryTextColor,
          }}
          className="absolute top-1 sm:top-1.5 right-1.5 sm:right-2 text-[9px] sm:text-[11px] md:text-xs font-semibold opacity-80 pointer-events-none leading-none"
        >
          {item.secondaryText}
        </span>
      )}

      {/* Primary Key Text with fluid scaling */}
      <span
        style={{
          fontSize: isMultiChar
            ? undefined
            : `clamp(${characterFontSize * 0.85}px, 1.8vw, ${characterFontSize * 1.45}px)`,
          lineHeight: 1,
        }}
        className={`font-medium ${
          isMultiChar
            ? 'text-xs sm:text-sm md:text-base font-semibold tracking-tight px-1'
            : ''
        }`}
      >
        {item.primaryText}
      </span>
    </button>
  );
};
