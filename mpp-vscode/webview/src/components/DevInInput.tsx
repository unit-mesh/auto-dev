/**
 * DevInInput Component
 * 
 * A textarea with DevIn syntax highlighting overlay.
 * Highlights commands (/file:, /dir:, etc.), agents (@), and variables ($).
 */

import React, { useRef, useEffect, KeyboardEvent, useCallback } from 'react';
import './DevInInput.css';

interface DevInInputProps {
  value: string;
  onChange: (value: string) => void;
  onKeyDown?: (e: KeyboardEvent<HTMLTextAreaElement>) => void;
  onTriggerCompletion?: (trigger: '/' | '@' | '$', position: number) => void;
  placeholder?: string;
  disabled?: boolean;
}

// DevIn syntax patterns
const PATTERNS = {
  command: /\/[a-zA-Z_][a-zA-Z0-9_]*(?::[^\s\n]*)?/g,
  agent: /@[a-zA-Z_][a-zA-Z0-9_]*/g,
  variable: /\$[a-zA-Z_][a-zA-Z0-9_]*/g,
};

export const DevInInput: React.FC<DevInInputProps> = ({
  value, onChange, onKeyDown, onTriggerCompletion, placeholder, disabled
}) => {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const highlightRef = useRef<HTMLDivElement>(null);

  // Sync scroll between textarea and highlight overlay
  const syncScroll = useCallback(() => {
    if (textareaRef.current && highlightRef.current) {
      highlightRef.current.scrollTop = textareaRef.current.scrollTop;
      highlightRef.current.scrollLeft = textareaRef.current.scrollLeft;
    }
  }, []);

  // Auto-resize textarea
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 150)}px`;
    }
  }, [value]);

  // Handle input change and detect completion triggers
  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newValue = e.target.value;
    const cursorPos = e.target.selectionStart;
    onChange(newValue);

    // Check for completion triggers
    if (onTriggerCompletion && cursorPos > 0) {
      const charBefore = newValue[cursorPos - 1];
      const charBeforeThat = cursorPos > 1 ? newValue[cursorPos - 2] : ' ';
      
      // Trigger completion if typing /, @, or $ after whitespace or at start
      if ((charBefore === '/' || charBefore === '@' || charBefore === '$') &&
          (charBeforeThat === ' ' || charBeforeThat === '\n' || cursorPos === 1)) {
        onTriggerCompletion(charBefore as '/' | '@' | '$', cursorPos);
      }
    }
  };

  // Generate highlighted HTML
  const getHighlightedHtml = () => {
    let html = escapeHtml(value);
    
    // Apply highlighting in order (commands, agents, variables)
    html = html.replace(PATTERNS.command, '<span class="devin-command">$&</span>');
    html = html.replace(PATTERNS.agent, '<span class="devin-agent">$&</span>');
    html = html.replace(PATTERNS.variable, '<span class="devin-variable">$&</span>');
    
    // Preserve line breaks
    html = html.replace(/\n/g, '<br/>');
    
    // Add trailing space to match textarea behavior
    if (html.endsWith('<br/>') || html === '') {
      html += '&nbsp;';
    }
    
    return html;
  };

  return (
    <div className="devin-input-container">
      <div
        ref={highlightRef}
        className="devin-highlight-overlay"
        dangerouslySetInnerHTML={{ __html: getHighlightedHtml() }}
        aria-hidden="true"
      />
      <textarea
        ref={textareaRef}
        value={value}
        onChange={handleChange}
        onKeyDown={onKeyDown}
        onScroll={syncScroll}
        placeholder={placeholder}
        disabled={disabled}
        rows={1}
        className="devin-textarea"
        spellCheck={false}
      />
    </div>
  );
};

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

