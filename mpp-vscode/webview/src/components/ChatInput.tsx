import React, { useState, useRef, useEffect, KeyboardEvent, useCallback } from 'react';
import { ModelSelector, ModelConfig } from './ModelSelector';
import { TopToolbar } from './TopToolbar';
import { SelectedFile } from './FileChip';
import { DevInInput } from './DevInInput';
import { CompletionPopup, CompletionItem } from './CompletionPopup';
import { PlanSummaryBar, PlanData } from './plan';
import './ChatInput.css';

interface ChatInputProps {
  onSend: (message: string, files?: SelectedFile[]) => void;
  onClear?: () => void;
  onStop?: () => void;
  onConfigSelect?: (config: ModelConfig) => void;
  onConfigureClick?: () => void;
  onMcpConfigClick?: () => void;
  onPromptOptimize?: (prompt: string) => Promise<string>;
  onGetCompletions?: (text: string, cursorPosition: number) => void;
  onApplyCompletion?: (text: string, cursorPosition: number, completionIndex: number) => void;
  completionItems?: CompletionItem[];
  completionResult?: { newText: string; newCursorPosition: number; shouldTriggerNextCompletion: boolean } | null;
  disabled?: boolean;
  isExecuting?: boolean;
  placeholder?: string;
  availableConfigs?: ModelConfig[];
  currentConfigName?: string | null;
  totalTokens?: number | null;
  activeFile?: SelectedFile | null;
  currentPlan?: PlanData | null;
}

export const ChatInput: React.FC<ChatInputProps> = ({
  onSend,
  onClear,
  onStop,
  onConfigSelect,
  onConfigureClick,
  onMcpConfigClick,
  onPromptOptimize,
  onGetCompletions,
  onApplyCompletion,
  completionItems: externalCompletionItems,
  completionResult,
  disabled = false,
  isExecuting = false,
  placeholder = 'Ask AutoDev...',
  availableConfigs = [],
  currentConfigName = null,
  totalTokens = null,
  activeFile = null,
  currentPlan = null
}) => {
  const [input, setInput] = useState('');
  const [selectedFiles, setSelectedFiles] = useState<SelectedFile[]>([]);
  const [isEnhancing, setIsEnhancing] = useState(false);
  const [completionOpen, setCompletionOpen] = useState(false);
  const [selectedCompletionIndex, setSelectedCompletionIndex] = useState(0);
  const [autoAddCurrentFile, setAutoAddCurrentFile] = useState(true);
  const inputRef = useRef<HTMLDivElement>(null);
  const cursorPositionRef = useRef(0);

  // Use external completion items if provided
  const completionItems = externalCompletionItems || [];

  // Auto-add active file when it changes
  useEffect(() => {
    if (autoAddCurrentFile && activeFile) {
      setSelectedFiles(prev => {
        if (prev.some(f => f.path === activeFile.path)) return prev;
        return [...prev, activeFile];
      });
    }
  }, [activeFile, autoAddCurrentFile]);

  // Handle completion result from mpp-core
  useEffect(() => {
    if (completionResult) {
      setInput(completionResult.newText);
      cursorPositionRef.current = completionResult.newCursorPosition;
      if (completionResult.shouldTriggerNextCompletion && onGetCompletions) {
        // Trigger next completion
        onGetCompletions(completionResult.newText, completionResult.newCursorPosition);
      } else {
        setCompletionOpen(false);
      }
    }
  }, [completionResult, onGetCompletions]);

  // Update completion items when external items change
  useEffect(() => {
    if (externalCompletionItems && externalCompletionItems.length > 0) {
      setCompletionOpen(true);
      setSelectedCompletionIndex(0);
    } else if (externalCompletionItems && externalCompletionItems.length === 0) {
      setCompletionOpen(false);
    }
  }, [externalCompletionItems]);

  const handleAddFile = useCallback((file: SelectedFile) => {
    setSelectedFiles(prev => {
      if (prev.some(f => f.path === file.path)) return prev;
      return [...prev, file];
    });
  }, []);

  const handleRemoveFile = useCallback((file: SelectedFile) => {
    setSelectedFiles(prev => prev.filter(f => f.path !== file.path));
  }, []);

  const handleClearFiles = useCallback(() => {
    setSelectedFiles([]);
  }, []);

  // Handle completion trigger - request completions from mpp-core
  const handleTriggerCompletion = useCallback((trigger: '/' | '@' | '$', position: number) => {
    cursorPositionRef.current = position;
    if (onGetCompletions) {
      // Use mpp-core for completions
      onGetCompletions(input.substring(0, position) + trigger, position + 1);
    }
  }, [input, onGetCompletions]);

  // Handle completion selection
  const handleSelectCompletion = useCallback((item: CompletionItem, _index: number) => {
    if (onApplyCompletion && item.index !== undefined) {
      // Use mpp-core to apply completion
      onApplyCompletion(input, cursorPositionRef.current, item.index);
    } else {
      // Fallback: simple text replacement
      setInput(prev => prev + (item.insertText || item.text));
    }
    setCompletionOpen(false);
  }, [input, onApplyCompletion]);

  const handleSubmit = () => {
    const trimmed = input.trim();
    if (trimmed && !disabled) {
      onSend(trimmed, selectedFiles.length > 0 ? selectedFiles : undefined);
      setInput('');
      // Keep files in context for follow-up questions
    }
  };

  const handlePromptOptimize = useCallback(async () => {
    if (!onPromptOptimize || !input.trim() || isEnhancing || isExecuting) return;

    setIsEnhancing(true);
    try {
      const enhanced = await onPromptOptimize(input);
      if (enhanced) {
        setInput(enhanced);
      }
    } catch (error) {
      console.error('Failed to optimize prompt:', error);
    } finally {
      setIsEnhancing(false);
    }
  }, [input, onPromptOptimize, isEnhancing, isExecuting]);

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    // Don't submit if completion popup is open
    if (completionOpen) return;

    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <div className="chat-input-container" ref={inputRef}>
      {/* Plan Summary Bar - shown above file toolbar when a plan is active */}
      <PlanSummaryBar plan={currentPlan} />

      {/* File Context Toolbar */}
      <TopToolbar
        selectedFiles={selectedFiles}
        onAddFile={handleAddFile}
        onRemoveFile={handleRemoveFile}
        onClearFiles={handleClearFiles}
        autoAddCurrentFile={autoAddCurrentFile}
        onToggleAutoAdd={() => setAutoAddCurrentFile(prev => !prev)}
      />

      {/* Input Area with DevIn highlighting */}
      <div className="input-wrapper">
        <div className="input-with-completion">
          <DevInInput
            value={input}
            onChange={setInput}
            onKeyDown={handleKeyDown}
            onTriggerCompletion={handleTriggerCompletion}
            placeholder={placeholder}
            disabled={disabled || isExecuting}
          />

          {/* Completion Popup - positioned relative to input */}
          <CompletionPopup
            isOpen={completionOpen}
            items={completionItems}
            selectedIndex={selectedCompletionIndex}
            onSelect={handleSelectCompletion}
            onClose={() => setCompletionOpen(false)}
            onNavigate={setSelectedCompletionIndex}
          />
        </div>
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

      {/* Bottom Toolbar - Model Selector, Token Info, and Actions */}
      <div className="input-toolbar">
        <div className="toolbar-left">
          <ModelSelector
            availableConfigs={availableConfigs}
            currentConfigName={currentConfigName}
            onConfigSelect={onConfigSelect || (() => {})}
            onConfigureClick={onConfigureClick || (() => {})}
          />
          {/* Token usage indicator */}
          {totalTokens != null && totalTokens > 0 && (
            <span className="token-indicator" title="Total tokens used">
              {totalTokens}t
            </span>
          )}
        </div>
        <div className="toolbar-right">
          {/* MCP Config button */}
          {onMcpConfigClick && (
            <button
              className="toolbar-button mcp-button"
              onClick={onMcpConfigClick}
              title="MCP Configuration"
            >
              <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
                <path d="M9.405 1.05c-.413-1.4-2.397-1.4-2.81 0l-.1.34a1.464 1.464 0 0 1-2.105.872l-.31-.17c-1.283-.698-2.686.705-1.987 1.987l.169.311c.446.82.023 1.841-.872 2.105l-.34.1c-1.4.413-1.4 2.397 0 2.81l.34.1a1.464 1.464 0 0 1 .872 2.105l-.17.31c-.698 1.283.705 2.686 1.987 1.987l.311-.169a1.464 1.464 0 0 1 2.105.872l.1.34c.413 1.4 2.397 1.4 2.81 0l.1-.34a1.464 1.464 0 0 1 2.105-.872l.31.17c1.283.698 2.686-.705 1.987-1.987l-.169-.311a1.464 1.464 0 0 1 .872-2.105l.34-.1c1.4-.413 1.4-2.397 0-2.81l-.34-.1a1.464 1.464 0 0 1-.872-2.105l.17-.31c.698-1.283-.705-2.686-1.987-1.987l-.311.169a1.464 1.464 0 0 1-2.105-.872l-.1-.34zM8 10.93a2.929 2.929 0 1 1 0-5.86 2.929 2.929 0 0 1 0 5.858z"/>
              </svg>
            </button>
          )}
          {/* Prompt optimization button */}
          {onPromptOptimize && (
            <button
              className={`toolbar-button enhance-button ${isEnhancing ? 'enhancing' : ''}`}
              onClick={handlePromptOptimize}
              disabled={!input.trim() || isEnhancing || isExecuting}
              title={isEnhancing ? 'Enhancing prompt...' : 'Enhance prompt with AI'}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z"/>
              </svg>
              {isEnhancing && <span className="enhancing-text">...</span>}
            </button>
          )}
          <span className="input-hint">
            <kbd>Enter</kbd> send · <kbd>Shift+Enter</kbd> newline
          </span>
          {onClear && (
            <button className="toolbar-button" onClick={onClear} title="Clear history" disabled={isExecuting}>
              <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
                <path d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1zm0 13A6 6 0 1 1 8 2a6 6 0 0 1 0 12z"/>
                <path d="M10.5 5.5l-5 5m0-5l5 5" stroke="currentColor" strokeWidth="1.5" fill="none"/>
              </svg>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

