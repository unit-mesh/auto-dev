/**
 * TerminalRenderer - Renders terminal/shell commands and output
 */

import React, { useState } from 'react';
import './TerminalRenderer.css';

interface TerminalRendererProps {
  command: string;
  output?: string;
  exitCode?: number;
  executionTimeMs?: number;
  isComplete?: boolean;
  onAction?: (action: string, data: any) => void;
}

export const TerminalRenderer: React.FC<TerminalRendererProps> = ({
  command,
  output,
  exitCode,
  executionTimeMs,
  isComplete = true,
  onAction
}) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(command);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  const handleRun = () => {
    onAction?.('run-command', { command });
  };

  const isSuccess = exitCode === undefined || exitCode === 0;
  const statusIcon = isSuccess ? '✓' : '✕';
  const statusClass = isSuccess ? 'success' : 'error';

  return (
    <div className="terminal-renderer">
      <div className="terminal-header">
        <span className="terminal-icon">⌘</span>
        <span className="terminal-title">Terminal</span>
        {exitCode !== undefined && (
          <span className={`terminal-status ${statusClass}`}>
            {statusIcon} Exit: {exitCode}
          </span>
        )}
        {executionTimeMs !== undefined && (
          <span className="terminal-time">
            {formatTime(executionTimeMs)}
          </span>
        )}
        <div className="terminal-actions">
          <button
            className="terminal-action-btn"
            onClick={handleCopy}
            title="Copy command"
          >
            {copied ? '✓' : '📋'}
          </button>
          <button
            className="terminal-action-btn primary"
            onClick={handleRun}
            title="Run in terminal"
          >
            ▶ Run
          </button>
        </div>
      </div>
      
      <div className="terminal-content">
        <div className="terminal-command">
          <span className="terminal-prompt">$</span>
          <span className="terminal-command-text">{command}</span>
        </div>
        
        {output && (
          <div className="terminal-output">
            {output}
          </div>
        )}
        
        {!isComplete && (
          <div className="terminal-running">
            <span className="dot"></span>
            <span className="dot"></span>
            <span className="dot"></span>
            <span className="running-text">Running...</span>
          </div>
        )}
      </div>
    </div>
  );
};

function formatTime(ms: number): string {
  if (ms < 1000) {
    return `${ms}ms`;
  } else if (ms < 60000) {
    return `${(ms / 1000).toFixed(1)}s`;
  } else {
    const minutes = Math.floor(ms / 60000);
    const seconds = ((ms % 60000) / 1000).toFixed(0);
    return `${minutes}m ${seconds}s`;
  }
}

