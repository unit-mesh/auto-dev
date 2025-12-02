import React, { useState, useEffect, useCallback } from 'react';
import { MessageList, Message } from './components/MessageList';
import { ChatInput } from './components/ChatInput';
import { useVSCode, ExtensionMessage } from './hooks/useVSCode';
import './App.css';

const App: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const { postMessage, onMessage, isVSCode } = useVSCode();

  // Handle messages from extension
  const handleExtensionMessage = useCallback((msg: ExtensionMessage) => {
    switch (msg.type) {
      case 'userMessage':
        setMessages(prev => [...prev, { role: 'user', content: msg.content || '' }]);
        break;

      case 'startResponse':
        setIsStreaming(true);
        setMessages(prev => [...prev, { role: 'assistant', content: '', isStreaming: true }]);
        break;

      case 'responseChunk':
        setMessages(prev => {
          const updated = [...prev];
          const lastIdx = updated.length - 1;
          if (lastIdx >= 0 && updated[lastIdx].role === 'assistant') {
            updated[lastIdx] = {
              ...updated[lastIdx],
              content: updated[lastIdx].content + (msg.content || '')
            };
          }
          return updated;
        });
        break;

      case 'endResponse':
        setIsStreaming(false);
        setMessages(prev => {
          const updated = [...prev];
          const lastIdx = updated.length - 1;
          if (lastIdx >= 0 && updated[lastIdx].role === 'assistant') {
            updated[lastIdx] = { ...updated[lastIdx], isStreaming: false };
          }
          return updated;
        });
        break;

      case 'error':
        setIsStreaming(false);
        setMessages(prev => [...prev, { role: 'error', content: msg.content || 'An error occurred' }]);
        break;

      case 'historyCleared':
        setMessages([]);
        break;
    }
  }, []);

  // Subscribe to extension messages
  useEffect(() => {
    return onMessage(handleExtensionMessage);
  }, [onMessage, handleExtensionMessage]);

  // Send message to extension
  const handleSend = useCallback((content: string) => {
    postMessage({ type: 'sendMessage', content });
  }, [postMessage]);

  // Clear history
  const handleClear = useCallback(() => {
    postMessage({ type: 'clearHistory' });
  }, [postMessage]);

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-title">
          <span className="header-icon">✨</span>
          <span>AutoDev Chat</span>
        </div>
        {!isVSCode && (
          <span className="dev-badge">Dev Mode</span>
        )}
      </header>
      
      <MessageList messages={messages} />
      
      <ChatInput
        onSend={handleSend}
        onClear={handleClear}
        disabled={isStreaming}
        placeholder="Ask AutoDev anything..."
      />
    </div>
  );
};

export default App;

