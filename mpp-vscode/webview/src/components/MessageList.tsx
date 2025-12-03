import React, { useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import './MessageList.css';

export interface Message {
  role: 'user' | 'assistant' | 'error';
  content: string;
  isStreaming?: boolean;
}

interface MessageListProps {
  messages: Message[];
}

const MessageBubble: React.FC<{ message: Message }> = ({ message }) => {
  const isUser = message.role === 'user';
  const isError = message.role === 'error';

  return (
    <div className={`message ${message.role}`}>
      <div className="message-header">
        <span className="message-role">
          {isUser ? 'You' : isError ? 'Error' : 'AutoDev'}
        </span>
        {message.isStreaming && (
          <span className="streaming-indicator">
            <span className="dot"></span>
            <span className="dot"></span>
            <span className="dot"></span>
          </span>
        )}
      </div>
      <div className="message-content">
        {isUser || isError ? (
          <p>{message.content}</p>
        ) : (
          <ReactMarkdown remarkPlugins={[remarkGfm]}>
            {message.content || '...'}
          </ReactMarkdown>
        )}
      </div>
    </div>
  );
};

export const MessageList: React.FC<MessageListProps> = ({ messages }) => {
  const containerRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when messages change
  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [messages]);

  if (messages.length === 0) {
    return (
      <div className="message-list empty">
        <div className="empty-state">
          <div className="empty-icon">💬</div>
          <h3>Welcome to AutoDev</h3>
          <p>Ask me anything about your code!</p>
          <div className="suggestions">
            <span className="suggestion">Explain this code</span>
            <span className="suggestion">Write tests</span>
            <span className="suggestion">Refactor</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="message-list" ref={containerRef}>
      {messages.map((message, index) => (
        <MessageBubble key={index} message={message} />
      ))}
    </div>
  );
};

