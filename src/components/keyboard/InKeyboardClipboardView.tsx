import React, { useState, useMemo } from 'react';
import { KeyboardColors, SnippetEntity } from '../../types';
import { ArrowLeft, Search, Pin, Trash2, Plus, Copy, Image as ImageIcon, Check } from 'lucide-react';

interface InKeyboardClipboardViewProps {
  colors: KeyboardColors;
  pinnedSnippets: SnippetEntity[];
  historySnippets: SnippetEntity[];
  onSnippetSelected: (text: string) => void;
  onTogglePin: (snippet: SnippetEntity) => void;
  onDeleteSnippet: (id: number) => void;
  onSaveSnippet: (title: string, content: string, isPinned: boolean, category: string, shortcut: string) => void;
  onClearHistory: () => void;
  onBackToLetters: () => void;
}

export const InKeyboardClipboardView: React.FC<InKeyboardClipboardViewProps> = ({
  colors,
  pinnedSnippets,
  historySnippets,
  onSnippetSelected,
  onTogglePin,
  onDeleteSnippet,
  onSaveSnippet,
  onClearHistory,
  onBackToLetters,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFilter, setSelectedFilter] = useState<'PINNED' | 'HISTORY' | 'IMAGES'>('PINNED');
  const [showAddModal, setShowAddModal] = useState(false);
  const [copiedId, setCopiedId] = useState<number | null>(null);

  // New snippet modal fields
  const [newTitle, setNewTitle] = useState('');
  const [newContent, setNewContent] = useState('');
  const [newShortcut, setNewShortcut] = useState('');
  const [newCategory, setNewCategory] = useState('Quick Text');
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

  const displayedList =
    selectedFilter === 'PINNED'
      ? filteredPinned
      : selectedFilter === 'HISTORY'
      ? filteredHistory
      : filteredImages;

  const handleCopy = (e: React.MouseEvent, snippet: SnippetEntity) => {
    e.stopPropagation();
    navigator.clipboard.writeText(snippet.content);
    setCopiedId(snippet.id);
    setTimeout(() => setCopiedId(null), 1500);
  };

  const handleSaveModal = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newContent.trim()) return;
    const title = newTitle.trim() || newContent.trim().slice(0, 20);
    onSaveSnippet(title, newContent.trim(), newPinned, newCategory, newShortcut.trim());
    setNewTitle('');
    setNewContent('');
    setNewShortcut('');
    setShowAddModal(false);
  };

  return (
    <div
      style={{ backgroundColor: colors.background }}
      className="w-full h-[290px] flex flex-col justify-between p-2 select-none"
    >
      {/* Top Header Bar */}
      <div className="flex items-center justify-between pb-1.5 border-b border-black/15">
        <button
          type="button"
          onClick={onBackToLetters}
          className="flex items-center space-x-1 px-2.5 py-1 rounded-lg text-xs font-semibold cursor-pointer"
          style={{
            backgroundColor: colors.functionKeyBackground,
            color: colors.functionKeyTextColor,
          }}
        >
          <ArrowLeft size={14} />
          <span>Keyboard</span>
        </button>

        {/* Filter Pills */}
        <div className="flex items-center space-x-1">
          <button
            type="button"
            onClick={() => setSelectedFilter('PINNED')}
            style={{
              backgroundColor: selectedFilter === 'PINNED' ? colors.enterKeyBackground : colors.functionKeyBackground,
              color: selectedFilter === 'PINNED' ? colors.enterKeyTextColor : colors.letterKeyTextColor,
            }}
            className="px-2.5 py-0.5 rounded-full text-[11px] font-bold transition-colors cursor-pointer"
          >
            Pinned ({pinnedSnippets.length})
          </button>
          <button
            type="button"
            onClick={() => setSelectedFilter('HISTORY')}
            style={{
              backgroundColor: selectedFilter === 'HISTORY' ? colors.enterKeyBackground : colors.functionKeyBackground,
              color: selectedFilter === 'HISTORY' ? colors.enterKeyTextColor : colors.letterKeyTextColor,
            }}
            className="px-2.5 py-0.5 rounded-full text-[11px] font-bold transition-colors cursor-pointer"
          >
            History ({historySnippets.length})
          </button>
          <button
            type="button"
            onClick={() => setSelectedFilter('IMAGES')}
            style={{
              backgroundColor: selectedFilter === 'IMAGES' ? colors.enterKeyBackground : colors.functionKeyBackground,
              color: selectedFilter === 'IMAGES' ? colors.enterKeyTextColor : colors.letterKeyTextColor,
            }}
            className="px-2.5 py-0.5 rounded-full text-[11px] font-bold transition-colors cursor-pointer"
          >
            Images ({filteredImages.length})
          </button>
        </div>

        <button
          type="button"
          onClick={() => setShowAddModal(true)}
          title="Add Pinned Snippet"
          className="p-1.5 rounded-lg flex items-center justify-center cursor-pointer shadow-sm"
          style={{
            backgroundColor: colors.enterKeyBackground,
            color: colors.enterKeyTextColor,
          }}
        >
          <Plus size={16} />
        </button>
      </div>

      {/* Search Input */}
      <div className="relative my-1">
        <Search
          size={14}
          className="absolute left-2.5 top-1/2 -translate-y-1/2 pointer-events-none opacity-60"
          style={{ color: colors.letterKeySecondaryTextColor }}
        />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search clips, shortcuts (@email), or images..."
          className="w-full pl-8 pr-3 py-1 rounded-lg text-xs outline-none border transition-colors select-text"
          style={{
            backgroundColor: colors.letterKeyBackground,
            color: colors.letterKeyTextColor,
            borderColor: colors.functionKeyBackground,
          }}
        />
      </div>

      {/* Snippet List Items */}
      <div className="flex-1 overflow-y-auto space-y-1 pr-1">
        {displayedList.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center p-4">
            <span
              className="text-xs font-medium"
              style={{ color: colors.letterKeySecondaryTextColor }}
            >
              {searchQuery
                ? `No clips matching "${searchQuery}"`
                : selectedFilter === 'PINNED'
                ? 'No pinned snippets yet. Tap + to add shortcuts like @email!'
                : selectedFilter === 'HISTORY'
                ? 'Clipboard history is empty. Anything typed or copied appears here.'
                : 'No image clips found. Add images in the Clipboard History screen.'}
            </span>
          </div>
        ) : (
          displayedList.map((snippet) => {
            const isImageClip = snippet.isImage || !!snippet.imageUri;
            return (
              <div
                key={snippet.id}
                onClick={() => onSnippetSelected(snippet.content)}
                style={{
                  backgroundColor: colors.letterKeyBackground,
                  borderColor: colors.functionKeyBackground,
                }}
                className="p-2 rounded-lg border flex items-center justify-between transition-colors hover:brightness-105 active:scale-[0.99] cursor-pointer shadow-sm group"
              >
                <div className="flex-1 min-w-0 pr-2">
                  <div className="flex items-center space-x-1.5 mb-0.5">
                    {snippet.isPinned && (
                      <Pin size={11} className="fill-current text-blue-400 shrink-0" />
                    )}
                    <span
                      className="text-xs font-bold truncate"
                      style={{ color: colors.letterKeyTextColor }}
                    >
                      {snippet.title}
                    </span>
                    {snippet.shortcut && (
                      <span
                        className="px-1.5 py-0.2 rounded text-[10px] font-mono font-bold"
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
                        className="px-1 rounded text-[9px] font-bold"
                        style={{
                          backgroundColor: `${colors.enterKeyBackground}33`,
                          color: colors.enterKeyBackground,
                        }}
                      >
                        IMG
                      </span>
                    )}
                  </div>

                  {isImageClip ? (
                    <div className="w-16 h-10 rounded overflow-hidden bg-black/20 mt-1">
                      <img
                        src={snippet.imageUri || snippet.content}
                        alt={snippet.title}
                        className="w-full h-full object-cover"
                      />
                    </div>
                  ) : (
                    <p
                      className="text-[11px] line-clamp-1 opacity-80"
                      style={{ color: colors.letterKeySecondaryTextColor }}
                    >
                      {snippet.content}
                    </p>
                  )}
                </div>

                {/* Card Actions */}
                <div className="flex items-center space-x-1 shrink-0">
                  <button
                    type="button"
                    onClick={(e) => handleCopy(e, snippet)}
                    title="Copy"
                    className="p-1 rounded hover:bg-white/10 cursor-pointer"
                    style={{ color: copiedId === snippet.id ? colors.enterKeyBackground : colors.toolbarIconTint }}
                  >
                    {copiedId === snippet.id ? <Check size={13} /> : <Copy size={13} />}
                  </button>

                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onTogglePin(snippet);
                    }}
                    title={snippet.isPinned ? 'Unpin' : 'Pin'}
                    className="p-1 rounded hover:bg-white/10 cursor-pointer"
                    style={{ color: snippet.isPinned ? colors.enterKeyBackground : colors.toolbarIconTint }}
                  >
                    <Pin size={13} className={snippet.isPinned ? 'fill-current' : ''} />
                  </button>

                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onDeleteSnippet(snippet.id);
                    }}
                    title="Delete"
                    className="p-1 rounded hover:bg-red-500/20 text-red-400 cursor-pointer"
                  >
                    <Trash2 size={13} />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Bottom Bar */}
      <div className="flex items-center justify-between pt-1 border-t border-black/15">
        <button
          type="button"
          onClick={onBackToLetters}
          className="px-3 py-1 rounded-lg text-xs font-bold cursor-pointer"
          style={{
            backgroundColor: colors.functionKeyBackground,
            color: colors.functionKeyTextColor,
          }}
        >
          ABC
        </button>

        <span
          className="text-[11px] opacity-70"
          style={{ color: colors.letterKeySecondaryTextColor }}
        >
          Tap any clip to insert directly into editor
        </span>

        {selectedFilter === 'HISTORY' && historySnippets.length > 0 && (
          <button
            type="button"
            onClick={onClearHistory}
            className="text-[11px] text-red-400 hover:underline cursor-pointer font-medium"
          >
            Clear History
          </button>
        )}
      </div>

      {/* Add Snippet Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div
            style={{ backgroundColor: colors.toolbarBackground }}
            className="w-full max-w-sm rounded-2xl p-4 shadow-2xl border border-white/10 flex flex-col space-y-3"
          >
            <h3 className="text-sm font-bold" style={{ color: colors.letterKeyTextColor }}>
              New Pinned Snippet / Shortcut
            </h3>

            <form onSubmit={handleSaveModal} className="space-y-2.5">
              <div>
                <label className="text-[11px] font-semibold opacity-80" style={{ color: colors.letterKeyTextColor }}>
                  Title / Label
                </label>
                <input
                  type="text"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="e.g. My Email"
                  className="w-full mt-1 px-2.5 py-1.5 rounded-lg text-xs border outline-none select-text"
                  style={{
                    backgroundColor: colors.letterKeyBackground,
                    color: colors.letterKeyTextColor,
                    borderColor: colors.functionKeyBackground,
                  }}
                />
              </div>

              <div>
                <label className="text-[11px] font-semibold opacity-80" style={{ color: colors.letterKeyTextColor }}>
                  Shortcut (Optional, expands when typed e.g. @email, addr)
                </label>
                <input
                  type="text"
                  value={newShortcut}
                  onChange={(e) => setNewShortcut(e.target.value)}
                  placeholder="e.g. @email"
                  className="w-full mt-1 px-2.5 py-1.5 rounded-lg text-xs font-mono border outline-none select-text"
                  style={{
                    backgroundColor: colors.letterKeyBackground,
                    color: colors.letterKeyTextColor,
                    borderColor: colors.functionKeyBackground,
                  }}
                />
              </div>

              <div>
                <label className="text-[11px] font-semibold opacity-80" style={{ color: colors.letterKeyTextColor }}>
                  Text Content *
                </label>
                <textarea
                  required
                  rows={3}
                  value={newContent}
                  onChange={(e) => setNewContent(e.target.value)}
                  placeholder="Type or paste the snippet content here..."
                  className="w-full mt-1 px-2.5 py-1.5 rounded-lg text-xs border outline-none select-text"
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
                  id="pinCheck"
                  checked={newPinned}
                  onChange={(e) => setNewPinned(e.target.checked)}
                  className="rounded"
                />
                <label htmlFor="pinCheck" className="text-xs font-medium cursor-pointer" style={{ color: colors.letterKeyTextColor }}>
                  Pin to top of clipboard list
                </label>
              </div>

              <div className="flex items-center justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-3 py-1.5 rounded-lg text-xs font-semibold cursor-pointer"
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
