/**
 * ChatInterface - Main chat UI component
 * 
 * Displays the chat history and input prompt.
 */

import React, { useState, useEffect, useRef } from 'react';
import { Box, Text, useInput } from 'ink';
import TextInput from 'ink-text-input';
import Spinner from 'ink-spinner';
import type { Message } from './App.js';

interface ChatInterfaceProps {
  messages: Message[];
  onSendMessage: (content: string) => Promise<void>;
}

export const ChatInterface: React.FC<ChatInterfaceProps> = ({ messages, onSendMessage }) => {
  const [input, setInput] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [showInput, setShowInput] = useState(true);

  const handleSubmit = async () => {
    if (!input.trim() || isProcessing) return;

    const message = input.trim();
    setInput('');
    setIsProcessing(true);

    try {
      // Handle slash commands
      if (message.startsWith('/')) {
        await handleSlashCommand(message);
      } else {
        await onSendMessage(message);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  const handleSlashCommand = async (command: string) => {
    const [cmd, ...args] = command.slice(1).split(' ');
    
    switch (cmd.toLowerCase()) {
      case 'help':
        console.log('\nAvailable commands:');
        console.log('  /help     - Show this help message');
        console.log('  /clear    - Clear chat history');
        console.log('  /config   - Show configuration');
        console.log('  /exit     - Exit the application');
        break;
      
      case 'clear':
        // TODO: Clear messages
        console.log('Chat history cleared');
        break;
      
      case 'config':
        // TODO: Show config
        console.log('Configuration:');
        break;
      
      case 'exit':
        process.exit(0);
        break;
      
      default:
        console.log(`Unknown command: /${cmd}`);
        console.log('Type /help for available commands');
    }
  };

  // Handle Ctrl+C
  useInput((input, key) => {
    if (key.ctrl && input === 'c') {
      process.exit(0);
    }
  });

  return (
    <Box flexDirection="column" height="100%">
      {/* Header */}
      <Box borderStyle="single" borderColor="cyan" paddingX={1}>
        <Text bold color="cyan">
          🤖 AutoDev CLI - AI Coding Assistant
        </Text>
      </Box>

      {/* Messages */}
      <Box flexDirection="column" flexGrow={1} paddingX={1} paddingY={1}>
        {messages.length === 0 ? (
          <Box>
            <Text dimColor>No messages yet. Type a message to start chatting!</Text>
          </Box>
        ) : (
          messages.map((msg, idx) => (
            <MessageBubble key={idx} message={msg} />
          ))
        )}
        
        {isProcessing && (
          <Box marginTop={1}>
            <Text color="cyan">
              <Spinner type="dots" /> Processing...
            </Text>
          </Box>
        )}
      </Box>

      {/* Input */}
      {showInput && (
        <Box borderStyle="single" borderColor="gray" paddingX={1}>
          <Text color="green">{'> '}</Text>
          <TextInput
            value={input}
            onChange={setInput}
            onSubmit={handleSubmit}
            placeholder="Type your message... (or /help for commands)"
          />
        </Box>
      )}

      {/* Footer */}
      <Box paddingX={1}>
        <Text dimColor>
          Press Ctrl+C to exit | Type /help for commands
        </Text>
      </Box>
    </Box>
  );
};

interface MessageBubbleProps {
  message: Message;
}

const MessageBubble: React.FC<MessageBubbleProps> = ({ message }) => {
  const isUser = message.role === 'user';
  const color = isUser ? 'green' : 'cyan';
  const prefix = isUser ? '👤 You' : '🤖 AI';

  return (
    <Box flexDirection="column" marginBottom={1}>
      <Box>
        <Text bold color={color}>
          {prefix}:
        </Text>
      </Box>
      <Box paddingLeft={2}>
        <Text>{message.content}</Text>
      </Box>
    </Box>
  );
};

