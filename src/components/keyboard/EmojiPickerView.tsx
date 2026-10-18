import React, { useState, useMemo, useRef } from 'react';
import { EmojiPack, EmojiPackItem, KeyboardColors, StickerEntity } from '../../types';
import {
  allEmojiCategories,
  EmojiPackRegistry,
  BuiltInPackItems,
} from '../../data/emojiData';
import { ArrowLeft, Delete, Plus, Check, CloudDownload, Sparkles, Image as ImageIcon } from 'lucide-react';

interface EmojiPickerViewProps {
  colors: KeyboardColors;
  stickers: StickerEntity[];
  initialSection?: 'EMOJIS' | 'STICKERS';
  installedPackIds: string[];
  recentEmojis: string[];
  onEmojiSelected: (emoji: string) => void;
  onStickerSelected: (sticker: StickerEntity) => void;
  onDeleteSticker: (sticker: StickerEntity) => void;
  onInstallPack: (packId: string) => void;
  onOpenStickerStudio: () => void;
  onBackToLetters: () => void;
  onBackspace: () => void;
}

export const EmojiPickerView: React.FC<EmojiPickerViewProps> = ({
  colors,
  stickers,
  initialSection = 'EMOJIS',
  installedPackIds,
  recentEmojis,
  onEmojiSelected,
  onStickerSelected,
  onDeleteSticker,
  onInstallPack,
  onOpenStickerStudio,
  onBackToLetters,
  onBackspace,
}) => {
  const [currentSection, setCurrentSection] = useState<'EMOJIS' | 'STICKERS'>(initialSection);
  const [selectedCategoryIndex, setSelectedCategoryIndex] = useState<number>(0); // 0 = Recents, 1..N = standard, N+1.. = installed packs
  const [stickerTab, setStickerTab] = useState<0 | 1>(0); // 0 = Normal, 1 = Animated
  const [showPacksModal, setShowPacksModal] = useState<boolean>(false);
  const [stickerToDelete, setStickerToDelete] = useState<StickerEntity | null>(null);

  const longPressTimerRef = useRef<number | null>(null);

  const installedPacks = useMemo(
    () => EmojiPackRegistry.ALL_PACKS.filter((p) => installedPackIds.includes(p.id)),
    [installedPackIds]
  );

  const totalStandardCategories = allEmojiCategories.length;
  const isRecentsSelected = selectedCategoryIndex === 0;
  const isStandardCategory = selectedCategoryIndex >= 1 && selectedCategoryIndex <= totalStandardCategories;
  const currentStandardCategory = isStandardCategory ? allEmojiCategories[selectedCategoryIndex - 1] : null;

  const currentPackIndex = selectedCategoryIndex > totalStandardCategories ? selectedCategoryIndex - totalStandardCategories - 1 : -1;
  const currentPack = currentPackIndex >= 0 && currentPackIndex < installedPacks.length ? installedPacks[currentPackIndex] : null;

  const packItems: EmojiPackItem[] = useMemo(() => {
    if (!currentPack) return [];
    return BuiltInPackItems[currentPack.id] || [];
  }, [currentPack]);

  const normalStickers = useMemo(() => stickers.filter((s) => !s.isAnimated), [stickers]);
  const animatedStickers = useMemo(() => stickers.filter((s) => s.isAnimated), [stickers]);
  const displayedStickers = stickerTab === 0 ? normalStickers : animatedStickers;

  const handleStickerPointerDown = (sticker: StickerEntity) => {
    longPressTimerRef.current = window.setTimeout(() => {
      setStickerToDelete(sticker);
    }, 500);
  };

  const handleStickerPointerUp = (sticker: StickerEntity) => {
    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
      if (!stickerToDelete) {
        onStickerSelected(sticker);
      }
    }
  };

  return (
    <div
      style={{ backgroundColor: colors.background }}
      className="w-full h-[290px] flex flex-col justify-between p-2 select-none"
    >
      {/* Top Bar */}
      <div className="flex items-center justify-between pb-1.5 border-b border-black/15">
        <button
          type="button"
          onClick={onBackToLetters}
          className="p-1.5 rounded-lg flex items-center justify-center cursor-pointer"
          style={{
            backgroundColor: colors.functionKeyBackground,
            color: colors.functionKeyTextColor,
          }}
          title="Back to Keyboard"
        >
          <ArrowLeft size={16} />
        </button>

        {/* Section Switcher: Emojis vs Stickers */}
        <div
          className="flex items-center rounded-full p-0.5 border border-black/10"
          style={{ backgroundColor: colors.functionKeyBackground }}
        >
          <button
            type="button"
            onClick={() => setCurrentSection('EMOJIS')}
            style={{
              backgroundColor: currentSection === 'EMOJIS' ? colors.enterKeyBackground : 'transparent',
              color: currentSection === 'EMOJIS' ? colors.enterKeyTextColor : colors.letterKeySecondaryTextColor,
            }}
            className="px-3.5 py-1 rounded-full text-xs font-bold transition-all cursor-pointer"
          >
            😊 Emojis
          </button>
          <button
            type="button"
            onClick={() => setCurrentSection('STICKERS')}
            style={{
              backgroundColor: currentSection === 'STICKERS' ? colors.enterKeyBackground : 'transparent',
              color: currentSection === 'STICKERS' ? colors.enterKeyTextColor : colors.letterKeySecondaryTextColor,
            }}
            className="px-3.5 py-1 rounded-full text-xs font-bold transition-all cursor-pointer"
          >
            ✨ Stickers
          </button>
        </div>

        {/* Backspace Button */}
        <button
          type="button"
          onClick={onBackspace}
          className="p-1.5 rounded-lg flex items-center justify-center cursor-pointer"
          style={{
            backgroundColor: colors.functionKeyBackground,
            color: colors.functionKeyTextColor,
          }}
          title="Backspace"
        >
          <Delete size={16} />
        </button>
      </div>

      {/* Main Drawer Body */}
      {currentSection === 'EMOJIS' ? (
        <div className="flex-1 flex flex-col min-h-0">
          {/* Categories Tab Strip */}
          <div className="flex items-center space-x-1 overflow-x-auto py-1 scrollbar-none border-b border-black/10">
            {/* Recents tab 🕒 */}
            <button
              type="button"
              onClick={() => setSelectedCategoryIndex(0)}
              style={{
                backgroundColor: isRecentsSelected ? `${colors.enterKeyBackground}33` : 'transparent',
                borderColor: isRecentsSelected ? colors.enterKeyBackground : 'transparent',
              }}
              className="p-1.5 rounded-lg border text-sm font-medium shrink-0 cursor-pointer"
              title="Recent Emojis"
            >
              🕒
            </button>

            {/* Standard Categories */}
            {allEmojiCategories.map((cat, idx) => {
              const isSelected = selectedCategoryIndex === idx + 1;
              return (
                <button
                  key={cat.title}
                  type="button"
                  onClick={() => setSelectedCategoryIndex(idx + 1)}
                  style={{
                    backgroundColor: isSelected ? `${colors.enterKeyBackground}33` : 'transparent',
                    borderColor: isSelected ? colors.enterKeyBackground : 'transparent',
                    color: colors.letterKeyTextColor,
                  }}
                  className="px-2 py-1 rounded-lg border text-sm shrink-0 cursor-pointer font-medium"
                  title={cat.title}
                >
                  {cat.icon}
                </button>
              );
            })}

            {/* Installed Packs Tabs */}
            {installedPacks.map((pack, pIdx) => {
              const tabIdx = totalStandardCategories + 1 + pIdx;
              const isSelected = selectedCategoryIndex === tabIdx;
              return (
                <button
                  key={pack.id}
                  type="button"
                  onClick={() => setSelectedCategoryIndex(tabIdx)}
                  style={{
                    backgroundColor: isSelected ? `${colors.enterKeyBackground}33` : 'transparent',
                    borderColor: isSelected ? colors.enterKeyBackground : 'transparent',
                    color: colors.letterKeyTextColor,
                  }}
                  className="px-2 py-1 rounded-lg border text-xs font-bold shrink-0 cursor-pointer"
                  title={pack.name}
                >
                  {pack.icon}
                </button>
              );
            })}

            {/* + Packs Button */}
            <button
              type="button"
              onClick={() => setShowPacksModal(true)}
              style={{
                backgroundColor: `${colors.enterKeyBackground}22`,
                color: colors.enterKeyBackground,
              }}
              className="px-2 py-1 rounded-lg text-xs font-bold shrink-0 flex items-center space-x-1 cursor-pointer"
            >
              <Plus size={12} />
              <span>Packs</span>
            </button>
          </div>

          {/* Grid Area */}
          <div className="flex-1 overflow-y-auto p-1">
            {currentPack ? (
              // Pack Grid (e.g. Runic, Hieroglyphs, Kaomoji, Alchemy)
              currentPack.id === 'kaomoji' ? (
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-1.5">
                  {packItems.map((item, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => onEmojiSelected(item.symbol)}
                      title={item.name}
                      style={{
                        backgroundColor: colors.letterKeyBackground,
                        color: colors.letterKeyTextColor,
                      }}
                      className="p-2 rounded-lg text-xs font-medium border border-black/10 hover:brightness-110 active:scale-95 text-center truncate cursor-pointer shadow-sm"
                    >
                      {item.symbol}
                    </button>
                  ))}
                </div>
              ) : (
                <div className="grid grid-cols-6 sm:grid-cols-8 gap-1.5">
                  {packItems.map((item, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => onEmojiSelected(item.symbol)}
                      title={`${item.name}: ${item.meaning}`}
                      style={{
                        backgroundColor: colors.letterKeyBackground,
                        color: colors.letterKeyTextColor,
                      }}
                      className="h-10 rounded-lg flex flex-col items-center justify-center border border-black/10 hover:brightness-110 active:scale-95 cursor-pointer shadow-sm"
                    >
                      <span className="text-lg leading-none">{item.symbol}</span>
                      <span
                        className="text-[8px] truncate max-w-full px-0.5 opacity-60 leading-none mt-0.5"
                        style={{ color: colors.letterKeySecondaryTextColor }}
                      >
                        {item.name}
                      </span>
                    </button>
                  ))}
                </div>
              )
            ) : isRecentsSelected ? (
              recentEmojis.length === 0 ? (
                <div className="h-full flex items-center justify-center text-center p-4">
                  <span className="text-xs" style={{ color: colors.letterKeySecondaryTextColor }}>
                    No recent emojis yet. Tap any emoji to type and save here!
                  </span>
                </div>
              ) : (
                <div className="grid grid-cols-8 gap-1.5">
                  {recentEmojis.map((emoji, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => onEmojiSelected(emoji)}
                      className="h-9 rounded-lg flex items-center justify-center text-xl hover:bg-white/10 active:scale-90 cursor-pointer transition-transform"
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
              )
            ) : currentStandardCategory ? (
              <div className="grid grid-cols-8 gap-1">
                {currentStandardCategory.emojis.map((emoji, idx) => {
                  const isSymbolOrLetter = currentStandardCategory.title === 'Math' || currentStandardCategory.title === 'Greek';
                  return (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => onEmojiSelected(emoji)}
                      style={{
                        color: colors.letterKeyTextColor,
                        backgroundColor: isSymbolOrLetter ? colors.letterKeyBackground : 'transparent',
                      }}
                      className={`h-9 rounded-lg flex items-center justify-center active:scale-90 cursor-pointer transition-transform ${
                        isSymbolOrLetter ? 'text-sm font-semibold shadow-sm' : 'text-xl'
                      }`}
                    >
                      {emoji}
                    </button>
                  );
                })}
              </div>
            ) : null}
          </div>
        </div>
      ) : (
        /* Stickers Section */
        <div className="flex-1 flex flex-col min-h-0">
          {/* Normal vs Animated switch + + Studio button */}
          <div className="flex items-center justify-between py-1 border-b border-black/10">
            <div className="flex items-center space-x-1.5">
              <button
                type="button"
                onClick={() => setStickerTab(0)}
                style={{
                  backgroundColor: stickerTab === 0 ? colors.enterKeyBackground : colors.functionKeyBackground,
                  color: stickerTab === 0 ? colors.enterKeyTextColor : colors.letterKeyTextColor,
                }}
                className="px-2.5 py-0.5 rounded-lg text-xs font-bold transition-colors cursor-pointer"
              >
                Normal ({normalStickers.length})
              </button>
              <button
                type="button"
                onClick={() => setStickerTab(1)}
                style={{
                  backgroundColor: stickerTab === 1 ? colors.enterKeyBackground : colors.functionKeyBackground,
                  color: stickerTab === 1 ? colors.enterKeyTextColor : colors.letterKeyTextColor,
                }}
                className="px-2.5 py-0.5 rounded-lg text-xs font-bold transition-colors cursor-pointer"
              >
                Animated ({animatedStickers.length})
              </button>
            </div>

            <button
              type="button"
              onClick={onOpenStickerStudio}
              style={{
                backgroundColor: colors.functionKeyBackground,
                color: colors.functionKeyTextColor,
              }}
              className="px-2 py-0.5 rounded-lg text-[11px] font-bold cursor-pointer hover:opacity-80"
            >
              + Studio
            </button>
          </div>

          {/* Sticker Grid */}
          <div className="flex-1 overflow-y-auto p-1">
            {displayedStickers.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center p-4">
                <span className="text-xs font-medium" style={{ color: colors.letterKeySecondaryTextColor }}>
                  {stickerTab === 0 ? 'No static stickers available.' : 'No animated stickers available.'}
                </span>
                <button
                  type="button"
                  onClick={onOpenStickerStudio}
                  className="mt-2 text-xs font-bold underline"
                  style={{ color: colors.enterKeyBackground }}
                >
                  Open Sticker Studio to import
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-4 sm:grid-cols-5 gap-2">
                {displayedStickers.map((sticker) => (
                  <div
                    key={sticker.id}
                    onPointerDown={() => handleStickerPointerDown(sticker)}
                    onPointerUp={() => handleStickerPointerUp(sticker)}
                    style={{
                      backgroundColor: colors.letterKeyBackground,
                      borderColor: colors.functionKeyBackground,
                    }}
                    className="relative aspect-square rounded-xl border flex items-center justify-center p-1.5 cursor-pointer hover:scale-105 active:scale-95 transition-transform shadow-sm"
                    title={`${sticker.name} (Hold to delete)`}
                  >
                    <img
                      src={sticker.filePath}
                      alt={sticker.name}
                      className="w-full h-full object-contain pointer-events-none"
                    />
                    {sticker.isAnimated && (
                      <span
                        className="absolute bottom-1 right-1 px-1 rounded text-[7px] font-bold"
                        style={{
                          backgroundColor: colors.enterKeyBackground,
                          color: colors.enterKeyTextColor,
                        }}
                      >
                        ANIM
                      </span>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Bottom Bar: ABC Key and Label */}
      <div className="flex items-center justify-between pt-1 border-t border-black/15">
        <button
          type="button"
          onClick={onBackToLetters}
          className="px-4 py-1 rounded-lg text-xs font-bold cursor-pointer shadow-sm"
          style={{
            backgroundColor: colors.enterKeyBackground,
            color: colors.enterKeyTextColor,
          }}
        >
          ABC
        </button>

        <span className="text-[11px] opacity-75 truncate max-w-[240px]" style={{ color: colors.letterKeySecondaryTextColor }}>
          {currentSection === 'EMOJIS'
            ? currentPack
              ? `${currentPack.name} (${packItems.length} items)`
              : isRecentsSelected
              ? 'Recent Emojis'
              : currentStandardCategory?.title || 'Emojis'
            : `${displayedStickers.length} stickers available • Hold to delete`}
        </span>
      </div>

      {/* Quick Packs Download / Management Modal */}
      {showPacksModal && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div
            style={{ backgroundColor: colors.toolbarBackground }}
            className="w-full max-w-sm rounded-2xl p-4 shadow-2xl border border-white/10 flex flex-col space-y-3"
          >
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold" style={{ color: colors.letterKeyTextColor }}>
                Download Symbol & Emoji Packs
              </h3>
              <button
                type="button"
                onClick={() => setShowPacksModal(false)}
                className="text-xs opacity-60 hover:opacity-100"
                style={{ color: colors.letterKeyTextColor }}
              >
                ✕
              </button>
            </div>

            <p className="text-xs" style={{ color: colors.letterKeySecondaryTextColor }}>
              Elder Futhark runes, Egyptian hieroglyphs, Japanese Kaomoji emoticons, and medieval alchemy symbols.
            </p>

            <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
              {EmojiPackRegistry.ALL_PACKS.map((pack) => {
                const isInstalled = installedPackIds.includes(pack.id);
                return (
                  <div
                    key={pack.id}
                    style={{ backgroundColor: colors.letterKeyBackground }}
                    className="p-2.5 rounded-xl border border-black/10 flex items-center justify-between"
                  >
                    <div className="flex items-center space-x-2.5">
                      <span className="text-2xl leading-none">{pack.icon}</span>
                      <div>
                        <h4 className="text-xs font-bold" style={{ color: colors.letterKeyTextColor }}>
                          {pack.name}
                        </h4>
                        <span className="text-[10px]" style={{ color: colors.letterKeySecondaryTextColor }}>
                          {pack.nativeName} • {pack.itemCount} items
                        </span>
                      </div>
                    </div>

                    {isInstalled ? (
                      <span className="flex items-center space-x-1 text-xs font-bold text-green-400">
                        <Check size={14} />
                        <span>Added</span>
                      </span>
                    ) : (
                      <button
                        type="button"
                        onClick={() => {
                          onInstallPack(pack.id);
                        }}
                        style={{
                          backgroundColor: colors.enterKeyBackground,
                          color: colors.enterKeyTextColor,
                        }}
                        className="px-2.5 py-1 rounded-lg text-xs font-bold flex items-center space-x-1 cursor-pointer"
                      >
                        <CloudDownload size={13} />
                        <span>Add</span>
                      </button>
                    )}
                  </div>
                );
              })}
            </div>

            <div className="pt-2 flex justify-end">
              <button
                type="button"
                onClick={() => setShowPacksModal(false)}
                style={{
                  backgroundColor: colors.functionKeyBackground,
                  color: colors.letterKeyTextColor,
                }}
                className="px-4 py-1.5 rounded-lg text-xs font-semibold cursor-pointer"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Sticker Confirmation Modal (Requirement: Delete only via long-hold) */}
      {stickerToDelete && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div
            style={{ backgroundColor: colors.toolbarBackground }}
            className="w-full max-w-xs rounded-2xl p-4 shadow-2xl border border-white/10 flex flex-col space-y-3"
          >
            <h3 className="text-sm font-bold text-red-400">Delete Sticker?</h3>
            <p className="text-xs" style={{ color: colors.letterKeySecondaryTextColor }}>
              Are you sure you want to remove &quot;{stickerToDelete.name}&quot; from your stickers?
            </p>
            <div className="flex items-center justify-end space-x-2 pt-2">
              <button
                type="button"
                onClick={() => setStickerToDelete(null)}
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
                onClick={() => {
                  onDeleteSticker(stickerToDelete);
                  setStickerToDelete(null);
                }}
                className="px-3 py-1.5 rounded-lg text-xs font-bold bg-red-600 text-white cursor-pointer hover:bg-red-700"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
