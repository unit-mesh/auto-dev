/**
 * DevInRenderer - Renders DevIn blocks as tool call items
 * 
 * Parses devin blocks (language id = "devin") and renders them as tool call items
 * when the block is complete. Mirrors IdeaDevInBlockRenderer.kt
 */

import React, { useState, useMemo } from 'react';
import './DevInRenderer.css';

interface DevInRendererProps {
  content: string;
  isComplete?: boolean;
  onAction?: (action: string, data: any) => void;
}

interface ParsedToolCall {
  toolName: string;
  params: Record<string, string>;
}

/**
 * Parse DevIn content to extract tool calls
 * Handles format like: /command-name param1="value1" param2="value2"
 *
 * Note: This parser does not handle escaped quotes within quoted values.
 * For example, key="value with \"quote\"" will not parse correctly.
 */
function parseDevInContent(content: string): ParsedToolCall[] {
  const toolCalls: ParsedToolCall[] = [];
  const lines = content.trim().split('\n');

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;

    // Match /command-name or command-name at start
    const commandMatch = trimmed.match(/^\/?([a-zA-Z][a-zA-Z0-9_-]*)/);
    if (!commandMatch) continue;

    const toolName = commandMatch[1];
    const paramsStr = trimmed.slice(commandMatch[0].length).trim();
    const params: Record<string, string> = {};

    // Parse key="value" or key=value patterns
    // Note: Does not handle escaped quotes within quoted values
    const paramRegex = /([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*(?:"([^"]*)"|'([^']*)'|(\S+))/g;
    let match;
    while ((match = paramRegex.exec(paramsStr)) !== null) {
      const key = match[1];
      const value = match[2] ?? match[3] ?? match[4] ?? '';
      params[key] = value;
    }

    // If no key=value params, treat rest as single param
    if (Object.keys(params).length === 0 && paramsStr) {
      params['args'] = paramsStr;
    }

    toolCalls.push({ toolName, params });
  }

  return toolCalls;
}

/**
 * Format tool call details for display
 */
function formatToolCallDetails(params: Record<string, string>): string {
  return Object.entries(params)
    .map(([key, value]) => {
      const truncated = value.length > 50 ? value.slice(0, 50) + '...' : value;
      return `${key}="${truncated}"`;
    })
    .join(' ');
}

export const DevInRenderer: React.FC<DevInRendererProps> = ({
  content,
  isComplete = false,
  onAction
}) => {
  const toolCalls = useMemo(() => parseDevInContent(content), [content]);
  
  // Don't render incomplete or empty devin blocks
  if (!isComplete || toolCalls.length === 0) {
    return null;
  }
  
  return (
    <div className="devin-renderer">
      {toolCalls.map((tc, index) => (
        <DevInToolItem
          key={`${tc.toolName}-${index}`}
          toolName={tc.toolName}
          params={tc.params}
          onAction={onAction}
        />
      ))}
    </div>
  );
};

interface DevInToolItemProps {
  toolName: string;
  params: Record<string, string>;
  onAction?: (action: string, data: any) => void;
}

// Tools that support opening files in VSCode
const FILE_TOOLS = ['read-file', 'read_file', 'readFile', 'file', 'open'];

const DevInToolItem: React.FC<DevInToolItemProps> = ({
  toolName,
  params,
  onAction
}) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const hasParams = Object.keys(params).length > 0;
  const details = formatToolCallDetails(params);

  // Check if this is a file-related tool
  const isFileTool = FILE_TOOLS.includes(toolName);
  const filePath = params['path'] || params['file'] || params['args'] || '';

  const handleOpenFile = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (filePath && onAction) {
      onAction('openFile', { path: filePath });
    }
  };

  const handleToggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (hasParams) {
      setIsExpanded(!isExpanded);
    }
  };

  return (
    <div className="devin-tool-item">
      <div className={`devin-tool-header ${hasParams ? 'clickable' : ''}`} onClick={handleToggle}>
        <span className="devin-tool-icon" role="img" aria-label="Completed">✅</span>
        <span className="devin-tool-name">{toolName}</span>
        {!isExpanded && details && (
          <span className="devin-tool-details">
            {details.length > 60 ? details.slice(0, 60) + '...' : details}
          </span>
        )}
        <div className="devin-tool-actions">
          {isFileTool && filePath && (
            <button
              className="devin-action-btn"
              onClick={handleOpenFile}
              aria-label="Open file in editor"
              title="Open file in editor"
            >
              <span role="img" aria-hidden="true">📂</span>
            </button>
          )}
          {hasParams && (
            <button
              className="devin-action-btn toggle"
              onClick={handleToggle}
              aria-label={isExpanded ? 'Collapse parameters' : 'Expand parameters'}
              title={isExpanded ? 'Collapse' : 'Expand'}
              aria-expanded={isExpanded}
            >
              <span aria-hidden="true">{isExpanded ? '▼' : '▶'}</span>
            </button>
          )}
        </div>
      </div>

      {isExpanded && hasParams && (
        <div className="devin-tool-params">
          <pre>{JSON.stringify(params, null, 2)}</pre>
        </div>
      )}
    </div>
  );
};

