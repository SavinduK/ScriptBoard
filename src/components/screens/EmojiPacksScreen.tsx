import React, { useState } from 'react';
import { EmojiPack, EmojiPackItem, KeyboardColors } from '../../types';
import { EmojiPackRegistry, BuiltInPackItems } from '../../data/emojiData';
import {
  ArrowLeft,
  Eye,
  Check,
  CloudDownload,
  Trash2,
  X,
} from 'lucide-react';

interface EmojiPacksScreenProps {
  colors: KeyboardColors;
  installedPackIds: string[];
  onBack: () => void;
  onInstallPack: (id: string) => void;
  onUninstallPack: (id: string) => void;
}

export const EmojiPacksScreen: React.FC<EmojiPacksScreenProps> = ({
  colors,
  installedPackIds,
  onBack,
  onInstallPack,
  onUninstallPack,
}) => {
  const [downloadingPackId, setDownloadingPackId] = useState<string | null>(null);
  const [downloadProgress, setDownloadProgress] = useState<number>(0);
  const [previewPack, setPreviewPack] = useState<EmojiPack | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 2500);
  };

  const installedPacks = EmojiPackRegistry.ALL_PACKS.filter((p) =>
    installedPackIds.includes(p.id)
  );

  const availablePacks = EmojiPackRegistry.ALL_PACKS.filter(
    (p) => !installedPackIds.includes(p.id)
  );

  const handleStartDownload = (pack: EmojiPack) => {
    setDownloadingPackId(pack.id);
    setDownloadProgress(25);

    const interval = setInterval(() => {
      setDownloadProgress((prev) => {
        if (prev >= 90) {
          clearInterval(interval);
          setTimeout(() => {
            onInstallPack(pack.id);
            setDownloadingPackId(null);
            setDownloadProgress(0);
            showToast(`${pack.name} added to emoji drawer!`);
          }, 250);
          return 100;
        }
        return prev + 35;
      });
    }, 180);
  };

  const previewItems: EmojiPackItem[] = previewPack ? BuiltInPackItems[previewPack.id] || [] : [];

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
            Emoji &amp; Symbol Packages
          </h2>
          <span
            style={{ color: colors.letterKeySecondaryTextColor }}
            className="text-[11px]"
          >
            Download Runic, Hieroglyphs &amp; Kaomoji
          </span>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 max-w-xl mx-auto w-full p-4 space-y-4">
        {/* SECTION: INSTALLED PACKAGES */}
        {installedPacks.length > 0 && (
          <div className="space-y-2">
            <span
              className="text-[11px] font-bold uppercase tracking-wider"
              style={{ color: colors.enterKeyBackground }}
            >
              INSTALLED IN EMOJI SECTION ({installedPacks.length})
            </span>

            <div className="space-y-3">
              {installedPacks.map((pack) => (
                <div
                  key={pack.id}
                  style={{ backgroundColor: colors.letterKeyBackground }}
                  className="p-4 rounded-2xl border border-black/15 shadow-sm space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <div
                        style={{
                          backgroundColor: `${colors.enterKeyBackground}26`,
                          color: colors.enterKeyBackground,
                        }}
                        className="w-11 h-11 rounded-full flex items-center justify-center text-xl shrink-0 font-bold"
                      >
                        {pack.icon}
                      </div>

                      <div>
                        <div className="flex items-center space-x-2">
                          <h4
                            style={{ color: colors.letterKeyTextColor }}
                            className="text-sm font-bold"
                          >
                            {pack.name}
                          </h4>
                          <span
                            style={{
                              backgroundColor: `${colors.enterKeyBackground}26`,
                              color: colors.enterKeyBackground,
                            }}
                            className="px-2 py-0.5 rounded-md text-[10px] font-bold"
                          >
                            {pack.category}
                          </span>
                        </div>
                        <span
                          style={{ color: colors.letterKeySecondaryTextColor }}
                          className="text-[11px]"
                        >
                          {pack.nativeName} • {pack.itemCount} items • {pack.sizeDisplay}
                        </span>
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={() => setPreviewPack(pack)}
                      style={{ color: colors.enterKeyBackground }}
                      className="p-2 rounded-lg hover:bg-white/10 cursor-pointer"
                      title="Preview glyphs"
                    >
                      <Eye size={18} />
                    </button>
                  </div>

                  <p
                    style={{ color: colors.letterKeySecondaryTextColor }}
                    className="text-xs leading-relaxed"
                  >
                    {pack.description}
                  </p>

                  <div className="flex items-center justify-between pt-1 border-t border-black/10">
                    <span className="flex items-center space-x-1 text-xs font-bold text-green-400">
                      <Check size={14} />
                      <span>In Emoji Drawer</span>
                    </span>

                    <button
                      type="button"
                      onClick={() => {
                        onUninstallPack(pack.id);
                        showToast(`${pack.name} removed from emoji drawer`);
                      }}
                      className="px-3 py-1 rounded-lg text-xs font-semibold text-red-400 hover:bg-red-500/10 active:scale-95 transition-all flex items-center space-x-1 cursor-pointer border border-red-500/30"
                    >
                      <Trash2 size={13} />
                      <span>Remove</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* SECTION: AVAILABLE PACKAGES */}
        <div className="space-y-2 pt-2">
          <span
            className="text-[11px] font-bold uppercase tracking-wider"
            style={{ color: colors.letterKeySecondaryTextColor }}
          >
            AVAILABLE TO DOWNLOAD ({availablePacks.length})
          </span>

          {availablePacks.length === 0 ? (
            <p
              className="text-xs p-2 text-center"
              style={{ color: colors.letterKeySecondaryTextColor }}
            >
              All emoji &amp; symbol packages are installed!
            </p>
          ) : (
            <div className="space-y-3">
              {availablePacks.map((pack) => {
                const isDownloading = downloadingPackId === pack.id;
                return (
                  <div
                    key={pack.id}
                    style={{ backgroundColor: colors.letterKeyBackground }}
                    className="p-4 rounded-2xl border border-black/15 shadow-sm space-y-3"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-3">
                        <div
                          style={{
                            backgroundColor: `${colors.enterKeyBackground}26`,
                            color: colors.enterKeyBackground,
                          }}
                          className="w-11 h-11 rounded-full flex items-center justify-center text-xl shrink-0 font-bold"
                        >
                          {pack.icon}
                        </div>

                        <div>
                          <div className="flex items-center space-x-2">
                            <h4
                              style={{ color: colors.letterKeyTextColor }}
                              className="text-sm font-bold"
                            >
                              {pack.name}
                            </h4>
                            <span
                              style={{
                                backgroundColor: `${colors.enterKeyBackground}26`,
                                color: colors.enterKeyBackground,
                              }}
                              className="px-2 py-0.5 rounded-md text-[10px] font-bold"
                            >
                              {pack.category}
                            </span>
                          </div>
                          <span
                            style={{ color: colors.letterKeySecondaryTextColor }}
                            className="text-[11px]"
                          >
                            {pack.nativeName} • {pack.itemCount} items • {pack.sizeDisplay}
                          </span>
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() => setPreviewPack(pack)}
                        style={{ color: colors.enterKeyBackground }}
                        className="p-2 rounded-lg hover:bg-white/10 cursor-pointer"
                        title="Preview glyphs"
                      >
                        <Eye size={18} />
                      </button>
                    </div>

                    <p
                      style={{ color: colors.letterKeySecondaryTextColor }}
                      className="text-xs leading-relaxed"
                    >
                      {pack.description}
                    </p>

                    {isDownloading ? (
                      <div className="space-y-1 pt-1">
                        <div className="w-full h-2 rounded-full bg-black/30 overflow-hidden">
                          <div
                            style={{
                              width: `${downloadProgress}%`,
                              backgroundColor: colors.enterKeyBackground,
                            }}
                            className="h-full transition-all duration-200"
                          />
                        </div>
                        <span
                          className="text-[10px] font-mono"
                          style={{ color: colors.enterKeyBackground }}
                        >
                          Downloading package... {downloadProgress}%
                        </span>
                      </div>
                    ) : (
                      <div className="flex justify-end pt-1">
                        <button
                          type="button"
                          onClick={() => handleStartDownload(pack)}
                          style={{
                            backgroundColor: colors.enterKeyBackground,
                            color: colors.enterKeyTextColor,
                          }}
                          className="px-4 py-1.5 rounded-xl text-xs font-bold flex items-center space-x-1.5 shadow-sm hover:opacity-90 active:scale-95 transition-all cursor-pointer"
                        >
                          <CloudDownload size={14} />
                          <span>Download to Emoji Drawer</span>
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Symbol Glyphs Preview Dialog */}
      {previewPack && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div
            style={{ backgroundColor: colors.toolbarBackground }}
            className="w-full max-w-sm rounded-3xl p-5 shadow-2xl border border-white/10 flex flex-col space-y-3.5 max-h-[85vh]"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <span className="text-2xl leading-none">{previewPack.icon}</span>
                <div>
                  <h3
                    style={{ color: colors.letterKeyTextColor }}
                    className="text-sm font-bold"
                  >
                    {previewPack.name}
                  </h3>
                  <span
                    style={{ color: colors.letterKeySecondaryTextColor }}
                    className="text-[10px]"
                  >
                    {previewItems.length} glyphs in pack
                  </span>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setPreviewPack(null)}
                style={{ color: colors.letterKeySecondaryTextColor }}
                className="p-1 rounded-lg hover:bg-white/10"
              >
                <X size={18} />
              </button>
            </div>

            <p
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-xs leading-relaxed"
            >
              {previewPack.description}
            </p>

            <div className="flex-1 overflow-y-auto pr-1">
              <div className="grid grid-cols-4 gap-2">
                {previewItems.map((item, idx) => (
                  <div
                    key={idx}
                    style={{ backgroundColor: colors.letterKeyBackground }}
                    className="p-2 rounded-xl border border-black/15 flex flex-col items-center justify-center text-center shadow-sm"
                  >
                    <span
                      style={{ color: colors.letterKeyTextColor }}
                      className="text-lg font-medium leading-tight"
                    >
                      {item.symbol}
                    </span>
                    <span
                      style={{ color: colors.letterKeySecondaryTextColor }}
                      className="text-[9px] font-semibold truncate max-w-full mt-0.5"
                    >
                      {item.name}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                type="button"
                onClick={() => setPreviewPack(null)}
                style={{
                  backgroundColor: colors.enterKeyBackground,
                  color: colors.enterKeyTextColor,
                }}
                className="px-5 py-2 rounded-xl text-xs font-bold cursor-pointer hover:opacity-90 active:scale-95 transition-all"
              >
                Close Preview
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
