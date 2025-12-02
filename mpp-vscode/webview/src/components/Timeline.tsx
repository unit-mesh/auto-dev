/**
 * Timeline Component
 * 
 * Renders a timeline of events (messages, tool calls, terminal output, etc.)
 * Mirrors mpp-ui's ComposeRenderer timeline architecture
 */

import React from 'react';
import type { TimelineItem, MessageTimelineItem, ToolCallTimelineItem, TerminalTimelineItem } from '../types/timeline';
import { SketchRenderer, ToolCallRenderer, TerminalRenderer } from './sketch';
import './Timeline.css';

interface TimelineProps {
  items: TimelineItem[];
  currentStreamingContent?: string;
  onAction?: (action: string, data: any) => void;
}

export const Timeline: React.FC<TimelineProps> = ({
  items,
  currentStreamingContent,
  onAction
}) => {
  return (
    <div className="timeline">
      {items.map((item, index) => (
        <TimelineItemRenderer
          key={`${item.type}-${item.timestamp}-${index}`}
          item={item}
          onAction={onAction}
        />
      ))}
      
      {/* Streaming content indicator */}
      {currentStreamingContent && (
        <div className="timeline-item message assistant streaming">
          <div className="timeline-item-header">
            <span className="timeline-role">AutoDev</span>
            <span className="streaming-indicator">
              <span className="dot"></span>
              <span className="dot"></span>
              <span className="dot"></span>
            </span>
          </div>
          <div className="timeline-item-content">
            <SketchRenderer
              content={currentStreamingContent}
              isComplete={false}
              onAction={onAction}
            />
          </div>
        </div>
      )}
    </div>
  );
};

interface TimelineItemRendererProps {
  item: TimelineItem;
  onAction?: (action: string, data: any) => void;
}

const TimelineItemRenderer: React.FC<TimelineItemRendererProps> = ({ item, onAction }) => {
  switch (item.type) {
    case 'message':
      return <MessageItemRenderer item={item} onAction={onAction} />;
    case 'tool_call':
      return <ToolCallItemRenderer item={item} onAction={onAction} />;
    case 'terminal_output':
      return <TerminalItemRenderer item={item} onAction={onAction} />;
    case 'task_complete':
      return <TaskCompleteItemRenderer item={item} />;
    case 'error':
      return <ErrorItemRenderer item={item} />;
    default:
      return null;
  }
};

const MessageItemRenderer: React.FC<{ item: MessageTimelineItem; onAction?: (action: string, data: any) => void }> = ({ item, onAction }) => {
  const { message, isStreaming } = item;
  const isUser = message.role === 'user';
  const isSystem = message.role === 'system';

  return (
    <div className={`timeline-item message ${message.role} ${isStreaming ? 'streaming' : ''}`}>
      <div className="timeline-item-header">
        <span className="timeline-role">
          {isUser ? 'You' : isSystem ? 'System' : 'AutoDev'}
        </span>
        {isStreaming && (
          <span className="streaming-indicator">
            <span className="dot"></span>
            <span className="dot"></span>
            <span className="dot"></span>
          </span>
        )}
      </div>
      <div className="timeline-item-content">
        {isUser ? (
          <p className="user-message">{message.content}</p>
        ) : (
          <SketchRenderer
            content={message.content}
            isComplete={!isStreaming}
            onAction={onAction}
          />
        )}
      </div>
    </div>
  );
};

const ToolCallItemRenderer: React.FC<{ item: ToolCallTimelineItem; onAction?: (action: string, data: any) => void }> = ({ item, onAction }) => (
  <div className="timeline-item tool-call">
    <ToolCallRenderer toolCall={item.toolCall} onAction={onAction} />
  </div>
);

const TerminalItemRenderer: React.FC<{ item: TerminalTimelineItem; onAction?: (action: string, data: any) => void }> = ({ item, onAction }) => (
  <div className="timeline-item terminal">
    <TerminalRenderer
      command={item.terminal.command}
      output={item.terminal.output}
      exitCode={item.terminal.exitCode}
      executionTimeMs={item.terminal.executionTimeMs}
      isComplete={true}
      onAction={onAction}
    />
  </div>
);

const TaskCompleteItemRenderer: React.FC<{ item: { success: boolean; message: string } }> = ({ item }) => (
  <div className={`timeline-item task-complete ${item.success ? 'success' : 'error'}`}>
    <span className="task-icon">{item.success ? '✅' : '❌'}</span>
    <span className="task-message">{item.message}</span>
  </div>
);

const ErrorItemRenderer: React.FC<{ item: { message: string } }> = ({ item }) => (
  <div className="timeline-item error">
    <span className="error-icon">❌</span>
    <span className="error-message">{item.message}</span>
  </div>
);

