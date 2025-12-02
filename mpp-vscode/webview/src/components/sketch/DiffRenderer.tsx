/**
 * DiffRenderer - Renders diff/patch content with Accept/Reject actions
 */

import React, { useState } from 'react';
import './DiffRenderer.css';

interface DiffRendererProps {
  diffContent: string;
  onAction?: (action: string, data: any) => void;
}

interface DiffLine {
  type: 'add' | 'remove' | 'context' | 'header';
  content: string;
  lineNumber?: number;
}

export const DiffRenderer: React.FC<DiffRendererProps> = ({
  diffContent,
  onAction
}) => {
  const [applied, setApplied] = useState(false);
  const lines = parseDiff(diffContent);

  const handleAccept = () => {
    onAction?.('accept-diff', { diff: diffContent });
    setApplied(true);
  };

  const handleReject = () => {
    onAction?.('reject-diff', { diff: diffContent });
  };

  const handleViewDiff = () => {
    onAction?.('view-diff', { diff: diffContent });
  };

  return (
    <div className={`diff-renderer ${applied ? 'applied' : ''}`}>
      <div className="diff-header">
        <span className="diff-title">📝 Diff</span>
        <div className="diff-actions">
          <button
            className="diff-action-btn"
            onClick={handleViewDiff}
            title="View in diff editor"
          >
            👁️ View
          </button>
          <button
            className="diff-action-btn reject"
            onClick={handleReject}
            title="Reject changes"
            disabled={applied}
          >
            ✕ Reject
          </button>
          <button
            className="diff-action-btn accept"
            onClick={handleAccept}
            title="Accept changes"
            disabled={applied}
          >
            ✓ Accept
          </button>
        </div>
      </div>
      <div className="diff-content">
        {lines.map((line, index) => (
          <div key={index} className={`diff-line ${line.type}`}>
            <span className="diff-line-prefix">
              {line.type === 'add' ? '+' : line.type === 'remove' ? '-' : ' '}
            </span>
            <span className="diff-line-content">{line.content}</span>
          </div>
        ))}
      </div>
      {applied && (
        <div className="diff-applied-overlay">
          <span>✓ Applied</span>
        </div>
      )}
    </div>
  );
};

function parseDiff(content: string): DiffLine[] {
  const lines = content.split('\n');
  return lines.map(line => {
    if (line.startsWith('+++') || line.startsWith('---') || line.startsWith('@@')) {
      return { type: 'header', content: line };
    } else if (line.startsWith('+')) {
      return { type: 'add', content: line.substring(1) };
    } else if (line.startsWith('-')) {
      return { type: 'remove', content: line.substring(1) };
    } else {
      return { type: 'context', content: line.startsWith(' ') ? line.substring(1) : line };
    }
  });
}

