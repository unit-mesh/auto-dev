import React, { useState, useRef, useEffect, KeyboardEvent } from 'react';
import { ModelSelector, ModelConfig } from './ModelSelector';
import './ChatInput.css';

interface ChatInputProps {
  onSend: (message: string) => void;
  onClear?: () => void;
  onStop?: () => void;
  onConfigSelect?: (config: ModelConfig) => void;
  onConfigureClick?: () => void;
  disabled?: boolean;
  isExecuting?: boolean;
  placeholder?: string;
  availableConfigs?: ModelConfig[];
  currentConfigName?: string | null;
}

export const ChatInput: React.FC<ChatInputProps> = ({
  onSend,
  onClear,
  onStop,
  onConfigSelect,
  onConfigureClick,
  disabled = false,
  isExecuting = false,
  placeholder = 'Ask AutoDev...',
  availableConfigs = [],
  currentConfigName = null
}) => {
  const [input, setInput] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-resize textarea
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 150)}px`;
    }
  }, [input]);

  // Focus on mount
  useEffect(() => {
    textareaRef.current?.focus();
  }, []);

  const handleSubmit = () => {
    const trimmed = input.trim();
    if (trimmed && !disabled) {
      onSend(trimmed);
      setInput('');
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <div className="chat-input-container">
      {/* Toolbar - Model Selector and Config */}
      <div className="input-toolbar">
        <div className="toolbar-left">
          <ModelSelector
            availableConfigs={availableConfigs}
            currentConfigName={currentConfigName}
            onConfigSelect={onConfigSelect || (() => {})}
            onConfigureClick={onConfigureClick || (() => {})}
          />
        </div>
        <div className="toolbar-right">
          {onClear && (
            <button
              className="toolbar-button"
              onClick={onClear}
              title="Clear history"
              disabled={isExecuting}
            >
              <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
                <path d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1zm0 13A6 6 0 1 1 8 2a6 6 0 0 1 0 12z"/>
                <path d="M10.5 5.5l-5 5m0-5l5 5" stroke="currentColor" strokeWidth="1.5" fill="none"/>
              </svg>
            </button>
          )}
        </div>
      </div>

      {/* Input Area */}
      <div className="input-wrapper">
        <textarea
          ref={textareaRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled || isExecuting}
          rows={1}
          className="chat-textarea"
        />
        <div className="input-actions">
          {isExecuting ? (
            <button
              className="action-button stop-button"
              onClick={onStop}
              title="Stop execution"
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                <rect x="3" y="3" width="10" height="10" rx="1"/>
              </svg>
              <span>Stop</span>
            </button>
          ) : (
            <button
              className="action-button send-button"
              onClick={handleSubmit}
              disabled={disabled || !input.trim()}
              title="Send message (Enter)"
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                <path d="M1 8l7-7v4h7v6H8v4L1 8z"/>
              </svg>
              <span>Send</span>
            </button>
          )}
        </div>
      </div>

      {/* Footer Hint */}
      <div className="input-hint">
        <span>Press <kbd>Enter</kbd> to send, <kbd>Shift+Enter</kbd> for new line</span>
      </div>
    </div>
  );
};

