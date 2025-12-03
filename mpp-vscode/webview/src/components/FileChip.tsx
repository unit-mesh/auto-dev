/**
 * FileChip Component
 * 
 * Displays a selected file as a removable chip.
 * Similar to IdeaTopToolbar's FileChip from mpp-idea.
 */

import React from 'react';
import './FileChip.css';

export interface SelectedFile {
  name: string;
  path: string;
  relativePath: string;
  isDirectory: boolean;
}

interface FileChipProps {
  file: SelectedFile;
  onRemove: () => void;
  showPath?: boolean;
}

export const FileChip: React.FC<FileChipProps> = ({ file, onRemove, showPath = false }) => {
  return (
    <div className={`file-chip ${file.isDirectory ? 'directory' : 'file'}`} title={file.relativePath}>
      <span className="file-chip-icon">
        {file.isDirectory ? (
          <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
            <path d="M1.5 2A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h13a1.5 1.5 0 0 0 1.5-1.5V5a1.5 1.5 0 0 0-1.5-1.5H7.707l-.853-.854A.5.5 0 0 0 6.5 2.5H1.5z"/>
          </svg>
        ) : (
          <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
            <path d="M4 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4.5L9.5 0H4zm5.5 0v3A1.5 1.5 0 0 0 11 4.5h3V14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.5z"/>
          </svg>
        )}
      </span>
      <span className="file-chip-name">{file.name}</span>
      {showPath && file.relativePath !== file.name && (
        <span className="file-chip-path">{getTruncatedPath(file.relativePath)}</span>
      )}
      <button className="file-chip-remove" onClick={onRemove} title="Remove from context">
        <svg width="12" height="12" viewBox="0 0 16 16" fill="currentColor">
          <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
        </svg>
      </button>
    </div>
  );
};

/**
 * Truncate path for display, showing last 3-4 parts
 */
function getTruncatedPath(path: string): string {
  const parentPath = path.substring(0, path.lastIndexOf('/'));
  if (!parentPath) return '';
  
  if (parentPath.length <= 30) return parentPath;
  
  const parts = parentPath.split('/');
  if (parts.length <= 2) return `...${parentPath}`;
  
  const keepParts = parts.slice(-3);
  return `.../${keepParts.join('/')}`;
}

/**
 * Expanded FileChip for vertical list view
 */
export const FileChipExpanded: React.FC<FileChipProps> = ({ file, onRemove }) => {
  return (
    <div className="file-chip-expanded" title={file.path}>
      <span className="file-chip-icon">
        {file.isDirectory ? (
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M1.5 2A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h13a1.5 1.5 0 0 0 1.5-1.5V5a1.5 1.5 0 0 0-1.5-1.5H7.707l-.853-.854A.5.5 0 0 0 6.5 2.5H1.5z"/>
          </svg>
        ) : (
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M4 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4.5L9.5 0H4zm5.5 0v3A1.5 1.5 0 0 0 11 4.5h3V14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.5z"/>
          </svg>
        )}
      </span>
      <div className="file-chip-info">
        <span className="file-chip-name">{file.name}</span>
        <span className="file-chip-full-path">{file.relativePath}</span>
      </div>
      <button className="file-chip-remove" onClick={onRemove} title="Remove from context">
        <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
          <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
        </svg>
      </button>
    </div>
  );
};

