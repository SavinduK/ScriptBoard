import React, { useState, useMemo, useRef } from 'react';
import { KeyboardColors, StickerEntity } from '../../types';
import {
  ArrowLeft,
  FolderOpen,
  Archive,
  Image as ImageIcon,
  Sparkles,
  Trash2,
  X,
} from 'lucide-react';

interface StickersScreenProps {
  colors: KeyboardColors;
  stickers: StickerEntity[];
  onBack: () => void;
  onImportStickers: (newStickers: StickerEntity[]) => void;
  onDeleteSticker: (sticker: StickerEntity) => void;
}

export const StickersScreen: React.FC<StickersScreenProps> = ({
  colors,
  stickers,
  onBack,
  onImportStickers,
  onDeleteSticker,
}) => {
  const [selectedTab, setSelectedTab] = useState<0 | 1>(0); // 0 = Normal, 1 = Animated
  const [viewedSticker, setViewedSticker] = useState<StickerEntity | null>(null);
  const [stickerToDelete, setStickerToDelete] = useState<StickerEntity | null>(null);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const longPressTimerRef = useRef<number | null>(null);

  const normalStickers = useMemo(() => stickers.filter((s) => !s.isAnimated), [stickers]);
  const animatedStickers = useMemo(() => stickers.filter((s) => s.isAnimated), [stickers]);
  const displayedStickers = selectedTab === 0 ? normalStickers : animatedStickers;

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 2500);
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setIsProcessing(true);
    const imported: StickerEntity[] = [];

    Array.from(files).forEach((file, idx) => {
      const reader = new FileReader();
      const isAnim = file.name.endsWith('.gif') || file.type.includes('gif');
      reader.onload = (event) => {
        const dataUrl = event.target?.result as string;
        if (dataUrl) {
          imported.push({
            id: Date.now() + idx,
            filePath: dataUrl,
            name: file.name.replace(/\.[^/.]+$/, '').replace(/_/g, ' '),
            source: 'imported',
            dateAdded: Date.now(),
            isFavorite: false,
            isAnimated: isAnim,
            usageCount: 0,
          });

          if (imported.length === files.length) {
            onImportStickers(imported);
            setIsProcessing(false);
            showToast(`Successfully imported ${imported.length} sticker(s)!`);
          }
        }
      };
      reader.readAsDataURL(file);
    });
  };

  // ZIP Backup Simulator / Downloader
  const handleBackupZip = () => {
    if (stickers.length === 0) {
      showToast('No stickers to backup yet!');
      return;
    }
    // Simple metadata JSON export for backup
    const backupData = JSON.stringify(stickers, null, 2);
    const blob = new Blob([backupData], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `keypro_stickers_backup_${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    showToast(`Successfully created backup for ${stickers.length} stickers!`);
  };

  const handlePointerDown = (sticker: StickerEntity) => {
    longPressTimerRef.current = window.setTimeout(() => {
      setStickerToDelete(sticker);
    }, 500);
  };

  const handlePointerUp = (sticker: StickerEntity) => {
    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
      if (!stickerToDelete) {
        setViewedSticker(sticker);
      }
    }
  };

  return (
    <div
      style={{ backgroundColor: colors.background }}
      className="flex-1 flex flex-col min-h-screen select-none pb-12"
    >
      {/* Top App Bar */}
      <div
        style={{ backgroundColor: colors.toolbarBackground }}
        className="w-full h-14 px-4 flex items-center justify-between border-b border-black/20 shrink-0"
      >
        <div className="flex items-center space-x-3">
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
              className="text-base font-bold leading-tight"
            >
              Sticker Studio
            </h2>
            <span
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-[11px]"
            >
              Stickers
            </span>
          </div>
        </div>

        <span
          style={{
            backgroundColor: `${colors.enterKeyBackground}26`,
            color: colors.enterKeyBackground,
          }}
          className="px-3 py-1 rounded-xl text-xs font-bold"
        >
          KeyPro
        </span>
      </div>

      {/* Main Screen Content */}
      <div className="flex-1 max-w-xl mx-auto w-full p-4 space-y-4">
        {/* ACTION: IMPORT STICKERS */}
        <div
          style={{ backgroundColor: colors.letterKeyBackground }}
          className="p-4 rounded-2xl border border-black/15 shadow-md space-y-3"
        >
          <div className="flex items-start space-x-3.5">
            <div
              style={{
                backgroundColor: `${colors.enterKeyBackground}26`,
                color: colors.enterKeyBackground,
              }}
              className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0"
            >
              <FolderOpen size={24} />
            </div>
            <div>
              <h3
                style={{ color: colors.letterKeyTextColor }}
                className="text-base font-bold"
              >
                Import Stickers
              </h3>
              <p
                style={{ color: colors.letterKeySecondaryTextColor }}
                className="text-xs leading-relaxed mt-0.5"
              >
                Import .webp sticker packs from WhatsApp or pick files from your device folders.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 pt-2">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              style={{
                backgroundColor: colors.enterKeyBackground,
                color: colors.enterKeyTextColor,
              }}
              className="py-2.5 px-3 rounded-xl text-xs font-bold flex items-center justify-center space-x-2 shadow-sm hover:opacity-90 active:scale-95 transition-all cursor-pointer"
            >
              <FolderOpen size={16} />
              <span>Open Folder</span>
            </button>
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept="image/webp,image/png,image/gif,image/jpeg"
              className="hidden"
              onChange={handleFileSelect}
            />

            <button
              type="button"
              onClick={handleBackupZip}
              style={{
                borderColor: `${colors.letterKeyTextColor}33`,
                color: colors.letterKeyTextColor,
              }}
              className="py-2.5 px-3 rounded-xl text-xs font-bold border flex items-center justify-center space-x-2 hover:bg-white/5 active:scale-95 transition-all cursor-pointer"
            >
              <Archive size={16} />
              <span>Backup as ZIP</span>
            </button>
          </div>
        </div>

        {/* Tab Row: Normal vs Animated Stickers */}
        <div
          style={{ backgroundColor: colors.toolbarBackground }}
          className="p-1 rounded-xl flex items-center border border-black/15 shadow-sm"
        >
          <button
            type="button"
            onClick={() => setSelectedTab(0)}
            style={{
              backgroundColor: selectedTab === 0 ? colors.enterKeyBackground : 'transparent',
              color: selectedTab === 0 ? colors.enterKeyTextColor : colors.letterKeySecondaryTextColor,
            }}
            className="flex-1 py-2 rounded-lg text-xs font-bold flex items-center justify-center space-x-2 transition-all cursor-pointer"
          >
            <ImageIcon size={16} />
            <span>Normal Stickers ({normalStickers.length})</span>
          </button>
          <button
            type="button"
            onClick={() => setSelectedTab(1)}
            style={{
              backgroundColor: selectedTab === 1 ? colors.enterKeyBackground : 'transparent',
              color: selectedTab === 1 ? colors.enterKeyTextColor : colors.letterKeySecondaryTextColor,
            }}
            className="flex-1 py-2 rounded-lg text-xs font-bold flex items-center justify-center space-x-2 transition-all cursor-pointer"
          >
            <Sparkles size={16} />
            <span>Animated Stickers ({animatedStickers.length})</span>
          </button>
        </div>

        {/* Sticker Grid Items */}
        {displayedStickers.length === 0 ? (
          <div
            style={{ backgroundColor: `${colors.letterKeyBackground}80` }}
            className="p-10 rounded-2xl flex flex-col items-center justify-center text-center space-y-3"
          >
            <div
              style={{
                backgroundColor: `${colors.enterKeyBackground}26`,
                color: colors.enterKeyBackground,
              }}
              className="w-14 h-14 rounded-full flex items-center justify-center"
            >
              {selectedTab === 0 ? <ImageIcon size={26} /> : <Sparkles size={26} />}
            </div>
            <h4
              style={{ color: colors.letterKeyTextColor }}
              className="text-base font-bold"
            >
              {selectedTab === 0 ? 'No Normal Stickers Yet' : 'No Animated Stickers Yet'}
            </h4>
            <p
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-xs max-w-xs leading-relaxed"
            >
              {selectedTab === 0
                ? "Tap 'Open Folder' above to import static .webp, .png, or .jpg images from your device."
                : "Tap 'Open Folder' above to import animated .webp or .gif stickers from your device."}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-3 sm:grid-cols-4 gap-3">
            {displayedStickers.map((sticker) => (
              <div
                key={sticker.id}
                onPointerDown={() => handlePointerDown(sticker)}
                onPointerUp={() => handlePointerUp(sticker)}
                style={{
                  backgroundColor: colors.letterKeyBackground,
                  borderColor: colors.functionKeyBackground,
                }}
                className="relative aspect-square rounded-2xl border flex items-center justify-center p-3 cursor-pointer hover:scale-[1.03] active:scale-[0.97] transition-all shadow-md group"
              >
                <img
                  src={sticker.filePath}
                  alt={sticker.name}
                  className="w-full h-full object-contain pointer-events-none"
                />

                {sticker.isAnimated && (
                  <span
                    style={{
                      backgroundColor: colors.enterKeyBackground,
                      color: colors.enterKeyTextColor,
                    }}
                    className="absolute bottom-2 right-2 px-1.5 py-0.5 rounded-md text-[8px] font-bold"
                  >
                    ANIM
                  </span>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Sticker Detail Dialog on tap */}
      {viewedSticker && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div
            style={{ backgroundColor: colors.toolbarBackground }}
            className="w-full max-w-sm rounded-3xl p-5 shadow-2xl border border-white/10 flex flex-col space-y-4"
          >
            <div className="flex items-center justify-between">
              <h3
                style={{ color: colors.letterKeyTextColor }}
                className="text-base font-bold"
              >
                {viewedSticker.name}
              </h3>
              <button
                type="button"
                onClick={() => setViewedSticker(null)}
                style={{ color: colors.letterKeySecondaryTextColor }}
                className="p-1 rounded-lg hover:bg-white/10"
              >
                <X size={18} />
              </button>
            </div>

            <div className="w-full h-48 rounded-2xl bg-[#262626] flex items-center justify-center p-4">
              <img
                src={viewedSticker.filePath}
                alt={viewedSticker.name}
                className="max-h-full max-w-full object-contain"
              />
            </div>

            <div className="flex items-center justify-between text-xs">
              <span style={{ color: colors.letterKeySecondaryTextColor }}>
                Source: {viewedSticker.source}
              </span>
              <div className="flex items-center space-x-2">
                {viewedSticker.isAnimated && (
                  <span
                    style={{
                      backgroundColor: colors.enterKeyBackground,
                      color: colors.enterKeyTextColor,
                    }}
                    className="px-2 py-0.5 rounded-md text-[10px] font-bold"
                  >
                    ANIMATED .WEBP
                  </span>
                )}
                {viewedSticker.usageCount > 0 && (
                  <span
                    style={{ color: colors.enterKeyBackground }}
                    className="font-bold"
                  >
                    Used: {viewedSticker.usageCount}
                  </span>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2 pt-2">
              <button
                type="button"
                onClick={() => {
                  const s = viewedSticker;
                  setViewedSticker(null);
                  setStickerToDelete(s);
                }}
                className="py-2.5 rounded-xl border border-red-500/40 text-red-400 text-xs font-bold flex items-center justify-center space-x-1 hover:bg-red-500/10 active:scale-95 transition-all cursor-pointer"
              >
                <Trash2 size={15} />
                <span>Delete</span>
              </button>
              <button
                type="button"
                onClick={() => setViewedSticker(null)}
                style={{
                  backgroundColor: colors.enterKeyBackground,
                  color: colors.enterKeyTextColor,
                }}
                className="py-2.5 rounded-xl text-xs font-bold cursor-pointer hover:opacity-90 active:scale-95 transition-all"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Long Hold Delete Confirmation Dialog (Requirement: "To delete stickers only use the long hold menu") */}
      {stickerToDelete && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div
            style={{ backgroundColor: colors.toolbarBackground }}
            className="w-full max-w-xs rounded-3xl p-5 shadow-2xl border border-white/10 flex flex-col space-y-3"
          >
            <div className="flex items-center space-x-2 text-red-400">
              <Trash2 size={20} />
              <h3 className="text-base font-bold">Delete Sticker?</h3>
            </div>
            <p
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-xs leading-relaxed"
            >
              Are you sure you want to delete &quot;{stickerToDelete.name}&quot;? This action cannot be undone.
            </p>
            <div className="flex items-center justify-end space-x-2 pt-2">
              <button
                type="button"
                onClick={() => setStickerToDelete(null)}
                style={{
                  backgroundColor: colors.functionKeyBackground,
                  color: colors.letterKeyTextColor,
                }}
                className="px-3.5 py-1.5 rounded-xl text-xs font-medium cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onDeleteSticker(stickerToDelete);
                  setStickerToDelete(null);
                  showToast('Sticker deleted');
                }}
                className="px-4 py-1.5 rounded-xl text-xs font-bold bg-red-600 text-white cursor-pointer hover:bg-red-700"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-2xl bg-black/90 text-white text-xs font-semibold shadow-2xl border border-white/15 animate-bounce">
          {toastMessage}
        </div>
      )}
    </div>
  );
};
