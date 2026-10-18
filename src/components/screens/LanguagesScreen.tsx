import React, { useState } from 'react';
import { KeyboardColors, LanguagePack } from '../../types';
import { LanguagePackRegistry } from '../../data/languageData';
import {
  ArrowLeft,
  Download,
  Trash2,
  Check,
  Globe2,
} from 'lucide-react';

interface LanguagesScreenProps {
  colors: KeyboardColors;
  activeLanguageId: string;
  installedLanguageIds: string[];
  githubOwner?: string;
  githubRepo?: string;
  githubBranch?: string;
  onBack: () => void;
  onSelectActiveLanguage: (id: string) => void;
  onInstallLanguage: (id: string) => void;
  onUninstallLanguage: (id: string) => void;
  onUpdateGithubConfig?: (owner: string, repo: string, branch: string) => void;
}

export const LanguagesScreen: React.FC<LanguagesScreenProps> = ({
  colors,
  activeLanguageId,
  installedLanguageIds,
  onBack,
  onSelectActiveLanguage,
  onInstallLanguage,
  onUninstallLanguage,
}) => {
  const [downloadingPackId, setDownloadingPackId] = useState<string | null>(null);
  const [downloadProgress, setDownloadProgress] = useState<number>(0);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 2500);
  };

  const installedPacks = LanguagePackRegistry.ALL_PACKAGES.filter(
    (p) => p.isBuiltIn || installedLanguageIds.includes(p.id)
  );

  const availablePacks = LanguagePackRegistry.ALL_PACKAGES.filter(
    (p) => !p.isBuiltIn && !installedLanguageIds.includes(p.id)
  );

  const handleStartDownload = (pack: LanguagePack) => {
    setDownloadingPackId(pack.id);
    setDownloadProgress(20);

    const interval = setInterval(() => {
      setDownloadProgress((prev) => {
        if (prev >= 90) {
          clearInterval(interval);
          setTimeout(() => {
            onInstallLanguage(pack.id);
            setDownloadingPackId(null);
            setDownloadProgress(0);
            showToast(`${pack.name} (${pack.nativeName}) installed successfully!`);
          }, 300);
          return 100;
        }
        return prev + 35;
      });
    }, 200);
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
            Language Packages
          </h2>
          <span
            style={{ color: colors.letterKeySecondaryTextColor }}
            className="text-[11px]"
          >
            Manage active typing layouts &amp; offline language packages
          </span>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 max-w-xl mx-auto w-full p-4 space-y-5">
        {/* SECTION: INSTALLED LANGUAGES */}
        <div className="space-y-2.5">
          <div className="flex items-center justify-between">
            <span
              className="text-[11px] font-bold uppercase tracking-wider"
              style={{ color: colors.enterKeyBackground }}
            >
              INSTALLED LANGUAGES ({installedPacks.length})
            </span>
            <span
              className="text-xs"
              style={{ color: colors.letterKeySecondaryTextColor }}
            >
              Tap to switch active keyboard
            </span>
          </div>

          <div className="space-y-2.5">
            {installedPacks.map((pack) => {
              const isActive = activeLanguageId === pack.id;
              return (
                <div
                  key={pack.id}
                  onClick={() => onSelectActiveLanguage(pack.id)}
                  style={{ backgroundColor: colors.letterKeyBackground }}
                  className={`p-4 rounded-2xl border transition-all cursor-pointer flex items-center justify-between shadow-sm ${
                    isActive
                      ? 'border-blue-500/60 ring-1 ring-blue-500/30'
                      : 'border-black/15 hover:brightness-105'
                  }`}
                >
                  <div className="flex items-center space-x-3.5 min-w-0 pr-2">
                    <div
                      className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors ${
                        isActive
                          ? 'border-blue-400 bg-blue-500'
                          : 'border-white/30'
                      }`}
                    >
                      {isActive && <Check size={12} className="text-white" />}
                    </div>

                    <span className="text-3xl leading-none">{pack.flag}</span>

                    <div className="min-w-0">
                      <div className="flex items-center space-x-2">
                        <h4
                          style={{ color: colors.letterKeyTextColor }}
                          className="text-sm font-bold truncate"
                        >
                          {pack.name} ({pack.nativeName})
                        </h4>
                        {isActive && (
                          <span
                            style={{
                              backgroundColor: colors.enterKeyBackground,
                              color: colors.enterKeyTextColor,
                            }}
                            className="px-2 py-0.5 rounded-md text-[9px] font-extrabold uppercase tracking-wide"
                          >
                            Active
                          </span>
                        )}
                      </div>
                      <span
                        style={{ color: colors.letterKeySecondaryTextColor }}
                        className="text-[11px]"
                      >
                        {pack.isBuiltIn
                          ? 'Built-in English QWERTY Layout'
                          : `Wijesekara 5-Row Layout (${pack.sizeDisplay})`}
                      </span>
                    </div>
                  </div>

                  {!pack.isBuiltIn && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        onUninstallLanguage(pack.id);
                        showToast(`${pack.name} package uninstalled`);
                      }}
                      className="p-2 rounded-xl hover:bg-red-500/20 text-red-400 cursor-pointer transition-colors"
                      title={`Uninstall ${pack.name}`}
                    >
                      <Trash2 size={18} />
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* SECTION: AVAILABLE TO DOWNLOAD (Only Sinhala) */}
        <div className="space-y-2.5 pt-2">
          <div className="flex items-center justify-between">
            <span
              className="text-[11px] font-bold uppercase tracking-wider"
              style={{ color: colors.enterKeyBackground }}
            >
              AVAILABLE TO DOWNLOAD ({availablePacks.length})
            </span>
          </div>

          {availablePacks.length === 0 ? (
            <div
              style={{ backgroundColor: colors.letterKeyBackground }}
              className="p-4 rounded-2xl border border-black/15 text-center flex flex-col items-center justify-center space-y-1.5 shadow-sm"
            >
              <div
                style={{
                  backgroundColor: `${colors.enterKeyBackground}26`,
                  color: colors.enterKeyBackground,
                }}
                className="w-8 h-8 rounded-full flex items-center justify-center"
              >
                <Check size={16} />
              </div>
              <p
                className="text-xs font-medium"
                style={{ color: colors.letterKeyTextColor }}
              >
                All language packages are installed!
              </p>
              <p
                className="text-[11px]"
                style={{ color: colors.letterKeySecondaryTextColor }}
              >
                You can switch between English and Sinhala on the keyboard or above.
              </p>
            </div>
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
                      <div className="flex items-center space-x-3.5">
                        <span className="text-3xl leading-none">{pack.flag}</span>
                        <div>
                          <h4
                            style={{ color: colors.letterKeyTextColor }}
                            className="text-sm font-bold"
                          >
                            {pack.name} ({pack.nativeName})
                          </h4>
                          <span
                            style={{ color: colors.letterKeySecondaryTextColor }}
                            className="text-[11px]"
                          >
                            Size: {pack.sizeDisplay}
                          </span>
                        </div>
                      </div>

                      {!isDownloading && (
                        <button
                          type="button"
                          onClick={() => handleStartDownload(pack)}
                          style={{
                            backgroundColor: colors.enterKeyBackground,
                            color: colors.enterKeyTextColor,
                          }}
                          className="px-4 py-2 rounded-xl text-xs font-bold flex items-center space-x-1.5 shadow-md hover:opacity-90 active:scale-95 cursor-pointer transition-all"
                        >
                          <Download size={14} />
                          <span>Download</span>
                        </button>
                      )}
                    </div>

                    <p
                      style={{ color: colors.letterKeySecondaryTextColor }}
                      className="text-xs leading-relaxed"
                    >
                      {pack.description}
                    </p>

                    {isDownloading && (
                      <div className="space-y-1.5 pt-1">
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
                          Downloading language pack... {downloadProgress}%
                        </span>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-2xl bg-black/90 text-white text-xs font-semibold shadow-2xl border border-white/15 animate-bounce">
          {toastMessage}
        </div>
      )}
    </div>
  );
};
