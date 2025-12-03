/**
 * CompletionPopup Component
 *
 * Shows auto-completion suggestions for DevIn commands, agents, and variables.
 * Uses mpp-core's CompletionManager for completion items.
 */

import React, { useEffect, useCallback } from 'react';
import './CompletionPopup.css';

// CompletionItem from mpp-core
export interface CompletionItem {
  text: string;
  displayText: string;
  description: string | null;
  icon: string | null;
  triggerType?: string;
  index?: number;
  // Legacy fields for backward compatibility
  label?: string;
  detail?: string;
  insertText?: string;
  kind?: 'command' | 'agent' | 'variable';
}

interface CompletionPopupProps {
  isOpen: boolean;
  items: CompletionItem[];
  selectedIndex?: number;
  onSelect: (item: CompletionItem, index: number) => void;
  onClose: () => void;
  onNavigate?: (index: number) => void;
  position?: { top: number; left: number };
}

// Helper to get icon for completion item
const getItemIcon = (item: CompletionItem): string => {
  if (item.icon) return item.icon;
  if (item.triggerType === 'COMMAND') return '/';
  if (item.triggerType === 'AGENT') return '@';
  if (item.triggerType === 'VARIABLE') return '$';
  if (item.kind === 'command') return '/';
  if (item.kind === 'agent') return '@';
  if (item.kind === 'variable') return '$';
  return '';
};

// Helper to get display text
const getDisplayText = (item: CompletionItem): string => {
  return item.displayText || item.label || item.text;
};

// Helper to get description
const getDescription = (item: CompletionItem): string | null => {
  return item.description || item.detail || null;
};

export const CompletionPopup: React.FC<CompletionPopupProps> = ({
  isOpen, items, selectedIndex = 0, onSelect, onClose, onNavigate, position
}) => {
  // Handle keyboard navigation
  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (!isOpen) return;

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        if (onNavigate) {
          onNavigate(Math.min(selectedIndex + 1, items.length - 1));
        }
        break;
      case 'ArrowUp':
        e.preventDefault();
        if (onNavigate) {
          onNavigate(Math.max(selectedIndex - 1, 0));
        }
        break;
      case 'Enter':
      case 'Tab':
        e.preventDefault();
        if (items[selectedIndex]) {
          onSelect(items[selectedIndex], selectedIndex);
        }
        break;
      case 'Escape':
        e.preventDefault();
        onClose();
        break;
    }
  }, [isOpen, items, selectedIndex, onSelect, onClose, onNavigate]);

  useEffect(() => {
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
      return () => window.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, handleKeyDown]);

  if (!isOpen || items.length === 0) return null;

  return (
    <div className="completion-popup" style={position ? { top: position.top, left: position.left } : undefined}>
      {items.map((item, index) => (
        <div
          key={`${item.text || item.label}-${index}`}
          className={`completion-item ${index === selectedIndex ? 'selected' : ''}`}
          onClick={() => onSelect(item, index)}
          onMouseEnter={() => onNavigate?.(index)}
        >
          <span className="completion-icon">{getItemIcon(item)}</span>
          <span className="completion-label">{getDisplayText(item)}</span>
          {getDescription(item) && <span className="completion-detail">{getDescription(item)}</span>}
        </div>
      ))}
    </div>
  );
};

