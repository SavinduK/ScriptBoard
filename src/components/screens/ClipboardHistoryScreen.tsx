import React, { useState, useMemo, useRef } from 'react';
import { KeyboardColors, SnippetEntity } from '../../types';
import {
  ArrowLeft,
  Search,
  Plus,
  ImagePlus,
  Pin,
  Trash2,
  Copy,
  Check,
} from 'lucide-react';

interface ClipboardHistoryScreenProps {
  colors: KeyboardColors;
  pinnedSnippets: SnippetEntity[];
  historySnippets: SnippetEntity[];
  onBack: () => void;
  onTogglePin: (snippet: SnippetEntity) => void;
  onDeleteSnippet: (id: number) => void;
  onSaveSnippet: (title: string, content: string, isPinned: boolean, category: string, shortcut: string) => void;
  onSaveImage: (uri: string, title?: string) => void;
  onClearHistory: () => void;
}

export const ClipboardHistoryScreen: React.FC<ClipboardHistoryScreenProps> = ({
  colors,
  pinnedSnippets,
  historySnippets,
  onBack,
  onTogglePin,
  onDeleteSnippet,
  onSaveSnippet,
  onSaveImage,
  onClearHistory,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFilter, setSelectedFilter] = useState<'PINNED' | 'HISTORY' | 'IMAGES'>('PINNED');
  const [showAddModal, setShowAddModal] = useState(false);
  const [copiedId, setCopiedId] = useState<number | null>(null);

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // New snippet modal fields
  const [newTitle, setNewTitle] = useState('');
  const [newContent, setNewContent] = useState('');
  const [newShortcut, setNewShortcut] = useState('');
  const [newPinned, setNewPinned] = useState(true);

  const allSnippets = useMemo(
    () => [...pinnedSnippets, ...historySnippets],
    [pinnedSnippets, historySnippets]
  );

  const filteredPinned = useMemo(() => {
    if (!searchQuery.trim()) return pinnedSnippets;
    const q = searchQuery.toLowerCase();
    return pinnedSnippets.filter(
      (s) =>
        s.title.toLowerCase().includes(q) ||
        s.content.toLowerCase().includes(q) ||
        s.shortcut.toLowerCase().includes(q)
    );
  }, [pinnedSnippets, searchQuery]);

  const filteredHistory = useMemo(() => {
    if (!searchQuery.trim()) return historySnippets;
    const q = searchQuery.toLowerCase();
    return historySnippets.filter(
      (s) =>
        s.title.toLowerCase().includes(q) ||
        s.content.toLowerCase().includes(q) ||
        s.shortcut.toLowerCase().includes(q)
    );
  }, [historySnippets, searchQuery]);

  const filteredImages = useMemo(() => {
    const images = allSnippets.filter((s) => s.isImage || !!s.imageUri);
    if (!searchQuery.trim()) return images;
    const q = searchQuery.toLowerCase();
    return images.filter((s) => s.title.toLowerCase().includes(q));
  }, [allSnippets, searchQuery]);

  const handleCopy = (snippet: SnippetEntity) => {
    navigator.clipboard.writeText(snippet.content);
    setCopiedId(snippet.id);
    setTimeout(() => setCopiedId(null), 1500);
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files[0]) {
      const file = files[0];
      const reader = new FileReader();
      reader.onload = (event) => {
        const uri = event.target?.result as string;
        if (uri) {
          onSaveImage(uri, file.name.replace(/\.[^/.]+$/, ''));
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSaveModal = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newContent.trim()) return;
    const title = newTitle.trim() || newContent.trim().slice(0, 20);
    onSaveSnippet(title, newContent.trim(), newPinned, 'Pinned', newShortcut.trim());
    setNewTitle('');
    setNewContent('');
    setNewShortcut('');
    setShowAddModal(false);
  };

  return (
    <div
      style={{ backgroundColor: colors.background }}
      className="flex-1 flex flex-col min-h-screen select-none pb-12"
    >
      {/* Top App Bar */}
      <div
        style={{ backgroundColor: colors.toolbarBackground }}
        className="w-full h-14 px-3 flex items-center justify-between border-b border-black/20 shrink-0"
      >
        <div className="flex items-center space-x-2">
          <button
            type="button"
            onClick={onBack}
            style={{ color: colors.letterKeyTextColor }}
            className="p-1.5 rounded-lg hover:bg-white/10 active:scale-95 transition-all cursor-pointer mr-1"
            title="Back to Home"
          >
            <ArrowLeft size={20} />
          </button>
          <div className="flex flex-col">
            <h2
              style={{ color: colors.letterKeyTextColor }}
              className="text-base font-bold leading-tight"
            >
              Saved Clipboard &amp; Pinned
            </h2>
            <span
              style={{ color: colors.letterKeySecondaryTextColor }}
              className="text-[11px]"
            >
              {pinnedSnippets.length} pinned • {historySnippets.length} history clips • {filteredImages.length} images
            </span>
          </div>
        </div>

        <div className="flex items-center space-x-1">
          {/* Add Image Button */}
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            title="Add Image to Clipboard"
            className="p-2 rounded-lg hover:bg-white/10 active:scale-90 transition-all cursor-pointer"
            style={{ color: colors.enterKeyBackground }}
          >
            <ImagePlus size={20} />
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={handleImageUpload}
          />

          {/* Add Text Snippet Button */}
          <button
            type="button"
            onClick={() => setShowAddModal(true)}
            title="Add New Snippet"
            className="p-2 rounded-lg hover:bg-white/10 active:scale-90 transition-all cursor-pointer"
            style={{ color: colors.enterKeyBackground }}
          >
            <Plus size={22} />
          </button>
        </div>
      </div>

      {/* Search Bar & Filter Chips */}
      <div className="max-w-xl mx-auto w-full p-4 space-y-3 shrink-0">
        <div className="relative">
          <Search
            size={16}
            className="absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none opacity-60"
            style={{ color: colors.letterKeySecondaryTextColor }}
          />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search text, shortcuts, or images..."
            className="w-full pl-9 pr-3 py-2 rounded-xl text-xs outline-none border transition-colors select-text"
            style={{
              backgroundColor: colors.letterKeyBackground,
              color: colors.letterKeyTextColor,
              borderColor: colors.functionKeyBackground,
            }}
          />
        </div>

        {/* Filter Pills */}
        <div className="flex items-center space-x-2">
          <button
            type="button"
            onClick={() => setSelectedFilter('PINNED')}
            style={{
              backgroundColor: selectedFilter === 'PINNED' ? colors.enterKeyBackground : colors.letterKeyBackground,
              color: selectedFilter === 'PINNED' ? colors.enterKeyTextColor : colors.letterKeyTextColor,
            }}
            className="px-3.5 py-1.5 rounded-full text-xs font-bold transition-colors cursor-pointer shadow-sm"
          >
            Pinned ({pinnedSnippets.length})
          </button>
          <button
            type="button"
            onClick={() => setSelectedFilter('HISTORY')}
            style={{
              backgroundColor: selectedFilter === 'HISTORY' ? colors.enterKeyBackground : colors.letterKeyBackground,
              color: selectedFilter === 'HISTORY' ? colors.enterKeyTextColor : colors.letterKeyTextColor,
            }}
            className="px-3.5 py-1.5 rounded-full text-xs font-bold transition-colors cursor-pointer shadow-sm"
          >
            History ({historySnippets.length})
          </button>
          <button
            type="button"
            onClick={() => setSelectedFilter('IMAGES')}
            style={{
              backgroundColor: selectedFilter === 'IMAGES' ? colors.enterKeyBackground : colors.letterKeyBackground,
              color: selectedFilter === 'IMAGES' ? colors.enterKeyTextColor : colors.letterKeyTextColor,
            }}
            className="px-3.5 py-1.5 rounded-full text-xs font-bold transition-colors cursor-pointer shadow-sm"
          >
            Images ({filteredImages.length})
          </button>

          {selectedFilter === 'HISTORY' && historySnippets.length > 0 && (
            <button
              type="button"
              onClick={onClearHistory}
              className="ml-auto text-xs text-red-400 hover:underline cursor-pointer font-semibold"
            >
              Clear History
            </button>
          )}
        </div>
      </div>

      {/* Main Snippets List */}
      <div className="flex-1 max-w-xl mx-auto w-full px-4 space-y-3 overflow-y-auto">
        {selectedFilter === 'IMAGES' ? (
          filteredImages.length === 0 ? (
            <div
              style={{ backgroundColor: `${colors.letterKeyBackground}80` }}
              className="p-6 rounded-2xl text-center"
            >
              <p className="text-xs" style={{ color: colors.letterKeySecondaryTextColor }}>
                No image clips yet. Tap the photo icon in the top right to upload photos from your device!
              </p>
            </div>
          ) : (
            filteredImages.map((snippet) => (
              <SnippetItemCard
                key={snippet.id}
                snippet={snippet}
                colors={colors}
                isCopied={copiedId === snippet.id}
                onCopy={() => handleCopy(snippet)}
                onTogglePin={() => onTogglePin(snippet)}
                onDelete={() => onDeleteSnippet(snippet.id)}
              />
            ))
          )
        ) : selectedFilter === 'PINNED' ? (
          filteredPinned.length === 0 ? (
            <div
              style={{ backgroundColor: `${colors.letterKeyBackground}80` }}
              className="p-6 rounded-2xl text-center"
            >
              <p className="text-xs" style={{ color: colors.letterKeySecondaryTextColor }}>
                {searchQuery
                  ? `No pinned items match "${searchQuery}"`
                  : 'No pinned messages yet. Tap the + icon or pin from history to save frequent texts & shortcuts!'}
              </p>
            </div>
          ) : (
            filteredPinned.map((snippet) => (
              <SnippetItemCard
                key={snippet.id}
                snippet={snippet}
                colors={colors}
                isCopied={copiedId === snippet.id}
                onCopy={() => handleCopy(snippet)}
                onTogglePin={() => onTogglePin(snippet)}
                onDelete={() => onDeleteSnippet(snippet.id)}
              />
            ))
          )
        ) : filteredHistory.length === 0 ? (
          <div
            style={{ backgroundColor: `${colors.letterKeyBackground}80` }}
            className="p-6 rounded-2xl text-center"
          >
            <p className="text-xs" style={{ color: colors.letterKeySecondaryTextColor }}>
              {searchQuery
                ? `No history clips match "${searchQuery}"`
                : 'Clipboard history is empty. Anything you copy or type is preserved here!'}
            </p>
          </div>
        ) : (
          filteredHistory.map((snippet) => (
            <SnippetItemCard
              key={snippet.id}
              snippet={snippet}
              colors={colors}
              isCopied={copiedId === snippet.id}
              onCopy={() => handleCopy(snippet)}
              onTogglePin={() => onTogglePin(snippet)}
              onDelete={() => onDeleteSnippet(snippet.id)}
            />
          ))
        )}
      </div>

      {/* Add Snippet Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div
            style={{ backgroundColor: colors.toolbarBackground }}
            className="w-full max-w-sm rounded-2xl p-5 shadow-2xl border border-white/10 flex flex-col space-y-3.5"
          >
            <h3 className="text-sm font-bold" style={{ color: colors.letterKeyTextColor }}>
              Add Pinned Message
            </h3>

            <form onSubmit={handleSaveModal} className="space-y-3">
              <div>
                <label className="text-xs font-semibold" style={{ color: colors.letterKeyTextColor }}>
                  Title / Label
                </label>
                <input
                  type="text"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="e.g. Work Address"
                  className="w-full mt-1 px-3 py-2 rounded-xl text-xs border outline-none select-text"
                  style={{
                    backgroundColor: colors.letterKeyBackground,
                    color: colors.letterKeyTextColor,
                    borderColor: colors.functionKeyBackground,
                  }}
                />
              </div>

              <div>
                <label className="text-xs font-semibold" style={{ color: colors.letterKeyTextColor }}>
                  Shortcut (Expands when typed e.g. @email, addr)
                </label>
                <input
                  type="text"
                  value={newShortcut}
                  onChange={(e) => setNewShortcut(e.target.value)}
                  placeholder="e.g. @email"
                  className="w-full mt-1 px-3 py-2 rounded-xl text-xs font-mono border outline-none select-text"
                  style={{
                    backgroundColor: colors.letterKeyBackground,
                    color: colors.letterKeyTextColor,
                    borderColor: colors.functionKeyBackground,
                  }}
                />
              </div>

              <div>
                <label className="text-xs font-semibold" style={{ color: colors.letterKeyTextColor }}>
                  Message Content *
                </label>
                <textarea
                  required
                  rows={3}
                  value={newContent}
                  onChange={(e) => setNewContent(e.target.value)}
                  placeholder="Type or paste the snippet content here..."
                  className="w-full mt-1 px-3 py-2 rounded-xl text-xs border outline-none select-text"
                  style={{
                    backgroundColor: colors.letterKeyBackground,
                    color: colors.letterKeyTextColor,
                    borderColor: colors.functionKeyBackground,
                  }}
                />
              </div>

              <div className="flex items-center space-x-2 pt-1">
                <input
                  type="checkbox"
                  id="pinCheckScreen"
                  checked={newPinned}
                  onChange={(e) => setNewPinned(e.target.checked)}
                  className="rounded"
                />
                <label htmlFor="pinCheckScreen" className="text-xs font-medium cursor-pointer" style={{ color: colors.letterKeyTextColor }}>
                  Pin to top of clipboard list
                </label>
              </div>

              <div className="flex items-center justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-3 py-1.5 rounded-lg text-xs font-medium cursor-pointer"
                  style={{
                    backgroundColor: colors.functionKeyBackground,
                    color: colors.letterKeyTextColor,
                  }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 rounded-lg text-xs font-bold cursor-pointer"
                  style={{
                    backgroundColor: colors.enterKeyBackground,
                    color: colors.enterKeyTextColor,
                  }}
                >
                  Save Snippet
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

interface SnippetItemCardProps {
  snippet: SnippetEntity;
  colors: KeyboardColors;
  isCopied: boolean;
  onCopy: () => void;
  onTogglePin: () => void;
  onDelete: () => void;
}

const SnippetItemCard: React.FC<SnippetItemCardProps> = ({
  snippet,
  colors,
  isCopied,
  onCopy,
  onTogglePin,
  onDelete,
}) => {
  const isImageClip = snippet.isImage || !!snippet.imageUri;

  return (
    <div
      style={{ backgroundColor: colors.letterKeyBackground }}
      className="p-4 rounded-2xl border border-black/15 shadow-sm space-y-2.5 transition-all"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2 min-w-0 pr-2">
          {snippet.isPinned && (
            <Pin size={14} className="fill-current text-blue-400 shrink-0" />
          )}
          <h4
            style={{ color: colors.letterKeyTextColor }}
            className="text-sm font-bold truncate"
          >
            {snippet.title}
          </h4>
          {snippet.shortcut && (
            <span
              className="px-2 py-0.5 rounded-md text-[11px] font-mono font-bold shrink-0"
              style={{
                backgroundColor: `${colors.enterKeyBackground}33`,
                color: colors.enterKeyBackground,
              }}
            >
              {snippet.shortcut}
            </span>
          )}
          {isImageClip && (
            <span
              className="px-1.5 py-0.5 rounded text-[10px] font-bold shrink-0"
              style={{
                backgroundColor: `${colors.enterKeyBackground}33`,
                color: colors.enterKeyBackground,
              }}
            >
              IMAGE
            </span>
          )}
        </div>

        <div className="flex items-center space-x-1 shrink-0">
          <button
            type="button"
            onClick={onCopy}
            title="Copy to Clipboard"
            className="p-1.5 rounded-lg hover:bg-white/10 cursor-pointer"
            style={{ color: isCopied ? colors.enterKeyBackground : colors.toolbarIconTint }}
          >
            {isCopied ? <Check size={16} /> : <Copy size={16} />}
          </button>
          <button
            type="button"
            onClick={onTogglePin}
            title={snippet.isPinned ? 'Unpin' : 'Pin'}
            className="p-1.5 rounded-lg hover:bg-white/10 cursor-pointer"
            style={{ color: snippet.isPinned ? colors.enterKeyBackground : colors.toolbarIconTint }}
          >
            <Pin size={16} className={snippet.isPinned ? 'fill-current' : ''} />
          </button>
          <button
            type="button"
            onClick={onDelete}
            title="Delete Snippet"
            className="p-1.5 rounded-lg hover:bg-red-500/20 text-red-400 cursor-pointer"
          >
            <Trash2 size={16} />
          </button>
        </div>
      </div>

      {isImageClip ? (
        <div className="w-full h-36 rounded-xl overflow-hidden bg-black/20 flex items-center justify-center border border-white/5">
          <img
            src={snippet.imageUri || snippet.content}
            alt={snippet.title}
            className="w-full h-full object-contain"
          />
        </div>
      ) : (
        <p
          style={{ color: colors.letterKeySecondaryTextColor }}
          className="text-xs leading-relaxed select-text line-clamp-3 whitespace-pre-wrap"
        >
          {snippet.content}
        </p>
      )}

      {isCopied && (
        <span
          className="text-[11px] font-bold block pt-0.5"
          style={{ color: colors.enterKeyBackground }}
        >
          ✓ Copied to clipboard!
        </span>
      )}
    </div>
  );
};
