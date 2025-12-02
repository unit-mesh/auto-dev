/**
 * CodeBlockRenderer - Renders code blocks with syntax highlighting
 */

import React, { useState } from 'react';
import { getDisplayName } from '../../utils/codeFence';
import './CodeBlockRenderer.css';

interface CodeBlockRendererProps {
  code: string;
  language: string;
  isComplete?: boolean;
  onAction?: (action: string, data: any) => void;
}

export const CodeBlockRenderer: React.FC<CodeBlockRendererProps> = ({
  code,
  language,
  isComplete = true,
  onAction
}) => {
  const [copied, setCopied] = useState(false);
  const displayName = getDisplayName(language);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  const handleInsert = () => {
    onAction?.('insert', { code, language });
  };

  const handleApply = () => {
    onAction?.('apply', { code, language });
  };

  return (
    <div className="code-block">
      <div className="code-block-header">
        <span className="code-block-language">{displayName}</span>
        <div className="code-block-actions">
          <button
            className="code-action-btn"
            onClick={handleCopy}
            title="Copy to clipboard"
          >
            {copied ? '✓ Copied' : '📋 Copy'}
          </button>
          <button
            className="code-action-btn"
            onClick={handleInsert}
            title="Insert at cursor"
          >
            📝 Insert
          </button>
          <button
            className="code-action-btn primary"
            onClick={handleApply}
            title="Apply to file"
          >
            ✨ Apply
          </button>
        </div>
      </div>
      <div className="code-block-content">
        <pre>
          <code className={`language-${language}`}>
            {code}
          </code>
        </pre>
        {!isComplete && (
          <div className="code-streaming-indicator">
            <span className="dot"></span>
            <span className="dot"></span>
            <span className="dot"></span>
          </div>
        )}
      </div>
    </div>
  );
};

