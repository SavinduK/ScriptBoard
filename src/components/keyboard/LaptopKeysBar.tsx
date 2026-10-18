import React from 'react';
import { KeyItem, KeyboardColors } from '../../types';
import { getLaptopKeyRows } from '../../data/layoutGenerator';
import { KeyCap } from './KeyCap';

interface LaptopKeysBarProps {
  colors: KeyboardColors;
  isCtrlActive: boolean;
  isAltActive: boolean;
  onKeyClick: (item: KeyItem) => void;
}

export const LaptopKeysBar: React.FC<LaptopKeysBarProps> = ({
  colors,
  isCtrlActive,
  isAltActive,
  onKeyClick,
}) => {
  const laptopRows = getLaptopKeyRows(isCtrlActive, isAltActive);

  return (
    <div
      style={{ backgroundColor: colors.laptopRowBackground }}
      className="w-full px-1.5 sm:px-3 md:px-5 py-1 sm:py-1.5 border-b border-black/20 flex flex-col gap-0.5 sm:gap-1"
    >
      {laptopRows.map((row, rIdx) => (
        <div key={rIdx} className="flex flex-row w-full">
          {row.map((key) => (
            <KeyCap
              key={key.testTag}
              item={key}
              colors={colors}
              heightClass="h-8 sm:h-10 md:h-11 lg:h-13"
              characterFontSize={14}
              holdForSymbols={false}
              onKeyClick={onKeyClick}
            />
          ))}
        </div>
      ))}
    </div>
  );
};
