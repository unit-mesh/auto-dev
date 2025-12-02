/**
 * ThinkingRenderer - Renders thinking/reasoning blocks
 * Collapsible by default, shows AI's reasoning process
 */

import React, { useState } from 'react';
import './ThinkingRenderer.css';

interface ThinkingRendererProps {
  content: string;
  isComplete?: boolean;
}

export const ThinkingRenderer: React.FC<ThinkingRendererProps> = ({
  content,
  isComplete = true
}) => {
  const [isExpanded, setIsExpanded] = useState(false);

  const toggleExpand = () => {
    setIsExpanded(!isExpanded);
  };

  // Get preview (first 100 chars)
  const preview = content.length > 100 
    ? content.substring(0, 100) + '...' 
    : content;

  return (
    <div className={`thinking-renderer ${isExpanded ? 'expanded' : 'collapsed'}`}>
      <div className="thinking-header" onClick={toggleExpand}>
        <span className="thinking-icon">💭</span>
        <span className="thinking-title">Thinking</span>
        <span className="thinking-toggle">
          {isExpanded ? '▼' : '▶'}
        </span>
        {!isComplete && (
          <span className="thinking-streaming">
            <span className="dot"></span>
            <span className="dot"></span>
            <span className="dot"></span>
          </span>
        )}
      </div>
      
      {isExpanded ? (
        <div className="thinking-content">
          {content.split('\n').map((line, index) => (
            <p key={index}>{line}</p>
          ))}
        </div>
      ) : (
        <div className="thinking-preview">
          {preview}
        </div>
      )}
    </div>
  );
};

