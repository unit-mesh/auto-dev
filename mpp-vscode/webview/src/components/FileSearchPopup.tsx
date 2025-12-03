/**
 * FileSearchPopup Component
 * 
 * A popup for searching and selecting files to add to context.
 * Similar to IdeaFileSearchPopup from mpp-idea.
 */

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useVSCode, ExtensionMessage } from '../hooks/useVSCode';
import { SelectedFile } from './FileChip';
import './FileSearchPopup.css';

interface FileSearchPopupProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectFile: (file: SelectedFile) => void;
  selectedFiles: SelectedFile[];
}

interface FileItem {
  name: string;
  path: string;
  relativePath: string;
  isDirectory: boolean;
}

interface FileSearchItemProps {
  item: FileItem;
  isSelected: boolean;
  onClick: () => void;
  onMouseEnter: () => void;
}

const FileSearchItem: React.FC<FileSearchItemProps> = ({ item, isSelected, onClick, onMouseEnter }) => (
  <div
    className={`file-item ${isSelected ? 'selected' : ''} ${item.isDirectory ? 'directory' : ''}`}
    onClick={onClick}
    onMouseEnter={onMouseEnter}
  >
    <span className="file-item-icon">
      {item.isDirectory ? (
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
          <path d="M1.5 2A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h13a1.5 1.5 0 0 0 1.5-1.5V5a1.5 1.5 0 0 0-1.5-1.5H7.707l-.853-.854A.5.5 0 0 0 6.5 2.5H1.5z"/>
        </svg>
      ) : (
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
          <path d="M4 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4.5L9.5 0H4zm5.5 0v3A1.5 1.5 0 0 0 11 4.5h3V14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.5z"/>
        </svg>
      )}
    </span>
    <div className="file-item-info">
      <span className="file-item-name">{item.name}</span>
      <span className="file-item-path">{item.relativePath}</span>
    </div>
  </div>
);

export const FileSearchPopup: React.FC<FileSearchPopupProps> = ({
  isOpen, onClose, onSelectFile, selectedFiles
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [files, setFiles] = useState<FileItem[]>([]);
  const [folders, setFolders] = useState<FileItem[]>([]);
  const [recentFiles, setRecentFiles] = useState<FileItem[]>([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const { postMessage, onMessage } = useVSCode();

  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
      postMessage({ type: 'getRecentFiles' });
    }
  }, [isOpen, postMessage]);

  useEffect(() => {
    return onMessage((message: ExtensionMessage) => {
      if (message.type === 'searchFilesResult') {
        const data = message.data as { files: FileItem[]; folders: FileItem[] };
        setFiles(data.files || []);
        setFolders(data.folders || []);
        setIsLoading(false);
        setSelectedIndex(0);
      } else if (message.type === 'recentFilesResult') {
        setRecentFiles((message.data as { files: FileItem[] }).files || []);
      }
    });
  }, [onMessage]);

  useEffect(() => {
    if (!isOpen) return;
    if (searchQuery.length >= 2) {
      setIsLoading(true);
      const timer = setTimeout(() => {
        postMessage({ type: 'searchFiles', data: { query: searchQuery } });
      }, 150);
      return () => clearTimeout(timer);
    } else {
      setFiles([]);
      setFolders([]);
    }
  }, [searchQuery, isOpen, postMessage]);

  const allItems = React.useMemo(() => {
    return searchQuery.length >= 2 ? [...folders, ...files] : recentFiles;
  }, [searchQuery, folders, files, recentFiles]);

  const filteredItems = allItems.filter(item => !selectedFiles.some(f => f.path === item.path));

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'ArrowDown': e.preventDefault(); setSelectedIndex(i => Math.min(i + 1, filteredItems.length - 1)); break;
      case 'ArrowUp': e.preventDefault(); setSelectedIndex(i => Math.max(i - 1, 0)); break;
      case 'Enter':
        e.preventDefault();
        if (filteredItems[selectedIndex]) { onSelectFile(filteredItems[selectedIndex]); setSearchQuery(''); onClose(); }
        break;
      case 'Escape': e.preventDefault(); onClose(); break;
    }
  }, [filteredItems, selectedIndex, onSelectFile, onClose]);

  useEffect(() => {
    if (listRef.current) listRef.current.querySelector('.file-item.selected')?.scrollIntoView({ block: 'nearest' });
  }, [selectedIndex]);

  if (!isOpen) return null;

  return (
    <div className="file-search-popup-overlay" onClick={onClose}>
      <div className="file-search-popup" onClick={e => e.stopPropagation()}>
        <div className="file-search-header">
          <input ref={inputRef} type="text" className="file-search-input" placeholder="Search files..."
            value={searchQuery} onChange={e => setSearchQuery(e.target.value)} onKeyDown={handleKeyDown} />
        </div>
        <div className="file-search-list" ref={listRef}>
          {isLoading && <div className="file-search-loading">Searching...</div>}
          {!isLoading && filteredItems.length === 0 && (
            <div className="file-search-empty">{searchQuery.length >= 2 ? 'No files found' : 'Type to search files'}</div>
          )}
          {!isLoading && searchQuery.length < 2 && recentFiles.length > 0 && (
            <div className="file-search-section-title">Recent Files</div>
          )}
          {filteredItems.map((item, index) => (
            <FileSearchItem key={item.path} item={item} isSelected={index === selectedIndex}
              onClick={() => { onSelectFile(item); setSearchQuery(''); onClose(); }}
              onMouseEnter={() => setSelectedIndex(index)} />
          ))}
        </div>
      </div>
    </div>
  );
};

