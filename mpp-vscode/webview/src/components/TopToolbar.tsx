/**
 * TopToolbar Component
 *
 * Displays file context management toolbar with add file button and selected files.
 * Similar to IdeaTopToolbar from mpp-idea.
 */

import React, { useState, useRef } from 'react';
import { FileChip, FileChipExpanded, SelectedFile } from './FileChip';
import { FileSearchPopup } from './FileSearchPopup';
import './TopToolbar.css';

interface TopToolbarProps {
  selectedFiles: SelectedFile[];
  onAddFile: (file: SelectedFile) => void;
  onRemoveFile: (file: SelectedFile) => void;
  onClearFiles: () => void;
}

export const TopToolbar: React.FC<TopToolbarProps> = ({
  selectedFiles, onAddFile, onRemoveFile, onClearFiles
}) => {
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const addButtonRef = useRef<HTMLButtonElement>(null);

  if (selectedFiles.length === 0 && !isSearchOpen) {
    return (
      <div className="top-toolbar empty">
        <button className="add-file-button" onClick={() => setIsSearchOpen(true)} ref={addButtonRef}>
          <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
          </svg>
          <span>Add context</span>
        </button>
        <FileSearchPopup isOpen={isSearchOpen} onClose={() => setIsSearchOpen(false)}
          onSelectFile={onAddFile} selectedFiles={selectedFiles} />
      </div>
    );
  }

  return (
    <div className={`top-toolbar ${isExpanded ? 'expanded' : 'collapsed'}`}>
      <div className="toolbar-header">
        <button className="add-file-button icon-only" onClick={() => setIsSearchOpen(true)} ref={addButtonRef}
          title="Add file to context">
          <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
          </svg>
        </button>
        
        {!isExpanded && (
          <div className="files-row">
            {selectedFiles.slice(0, 5).map(file => (
              <FileChip key={file.path} file={file} onRemove={() => onRemoveFile(file)} />
            ))}
            {selectedFiles.length > 5 && (
              <span className="more-files" onClick={() => setIsExpanded(true)}>
                +{selectedFiles.length - 5} more
              </span>
            )}
          </div>
        )}
        
        <div className="toolbar-actions">
          {selectedFiles.length > 1 && (
            <button className="expand-button" onClick={() => setIsExpanded(!isExpanded)}
              title={isExpanded ? 'Collapse' : 'Expand'}>
              <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
                {isExpanded ? (
                  <path d="M7.646 4.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1-.708.708L8 5.707l-5.646 5.647a.5.5 0 0 1-.708-.708l6-6z"/>
                ) : (
                  <path d="M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708z"/>
                )}
              </svg>
            </button>
          )}
          <button className="clear-button" onClick={onClearFiles} title="Clear all files">
            <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
              <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
            </svg>
          </button>
        </div>
      </div>
      
      {isExpanded && (
        <div className="files-list">
          {selectedFiles.map(file => (
            <FileChipExpanded key={file.path} file={file} onRemove={() => onRemoveFile(file)} />
          ))}
        </div>
      )}
      
      <FileSearchPopup isOpen={isSearchOpen} onClose={() => setIsSearchOpen(false)}
        onSelectFile={onAddFile} selectedFiles={selectedFiles} />
    </div>
  );
};

