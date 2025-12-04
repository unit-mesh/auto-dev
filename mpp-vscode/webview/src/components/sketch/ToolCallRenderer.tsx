/**
 * ToolCallRenderer - Renders tool call information
 * Mirrors mpp-ui's TuiRenderer.renderToolCall and renderToolResult
 */

import React, { useState } from 'react';
import type { ToolCallInfo } from '../../types/timeline';
import './ToolCallRenderer.css';

interface ToolCallRendererProps {
  toolCall: ToolCallInfo;
  onAction?: (action: string, data: any) => void;
}

export const ToolCallRenderer: React.FC<ToolCallRendererProps> = ({
  toolCall
}) => {
  const [isExpanded, setIsExpanded] = useState(false);
  
  const {
    toolName,
    description,
    params,
    success,
    summary,
    output,
    executionTimeMs,
    filePath
  } = toolCall;

  const getStatusIcon = () => {
    if (success === null || success === undefined) {
      return '🔧'; // Running
    }
    return success ? '✅' : '❌';
  };

  const getStatusClass = () => {
    if (success === null || success === undefined) {
      return 'running';
    }
    return success ? 'success' : 'error';
  };

  const formatParams = () => {
    try {
      const parsed = JSON.parse(params);
      // Format based on tool type
      switch (toolName) {
        case 'read-file':
        case 'write-file':
          return parsed.path || parsed.file || '';
        case 'list-files':
          return `${parsed.path || '.'} ${parsed.recursive ? '(recursive)' : ''}`;
        case 'grep':
          return `"${parsed.pattern || parsed.query || ''}" in ${parsed.path || '.'}`;
        case 'shell':
          return parsed.command || '';
        default:
          return Object.keys(parsed).length > 0 
            ? `(${Object.keys(parsed).join(', ')})` 
            : '';
      }
    } catch {
      return params;
    }
  };

  return (
    <div className={`tool-call-renderer ${getStatusClass()}`}>
      <div className="tool-call-header" onClick={() => setIsExpanded(!isExpanded)}>
        <span className="tool-call-icon">{getStatusIcon()}</span>
        <span className="tool-call-name">{toolName}</span>
        <span className="tool-call-params">{formatParams()}</span>
        {filePath && <span className="tool-call-file">{filePath}</span>}
        {executionTimeMs !== undefined && (
          <span className="tool-call-time">{formatTime(executionTimeMs)}</span>
        )}
        <span className="tool-call-toggle">{isExpanded ? '▼' : '▶'}</span>
      </div>
      
      {isExpanded && (
        <div className="tool-call-details">
          {description && (
            <div className="tool-call-description">{description}</div>
          )}
          
          {summary && (
            <div className="tool-call-summary">{summary}</div>
          )}
          
          {output && (
            <div className="tool-call-output">
              <pre>{truncateOutput(output)}</pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

function formatTime(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
  const minutes = Math.floor(ms / 60000);
  const seconds = ((ms % 60000) / 1000).toFixed(0);
  return `${minutes}m ${seconds}s`;
}

function truncateOutput(output: string, maxLines = 20): string {
  const lines = output.split('\n');
  if (lines.length <= maxLines) return output;
  return lines.slice(0, maxLines).join('\n') + `\n... (${lines.length - maxLines} more lines)`;
}

