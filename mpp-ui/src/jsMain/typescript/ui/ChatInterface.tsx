/**
 * ChatInterface - Main chat UI component
 * 
 * Displays the chat history and input prompt with command auto-completion.
 */

import React, { useState, useEffect } from 'react';
import { Box, Text, useInput } from 'ink';
import TextInput from 'ink-text-input';
import Spinner from 'ink-spinner';
import type { Message } from './App.js';
import { Banner } from './Banner.js';
import { CommandSuggestions } from './CommandSuggestions.js';
import { 
  isAtCommand, 
  isSlashCommand, 
  getCommandSuggestions,
  extractCommand,
  SLASH_COMMANDS,
  AT_COMMANDS
} from '../utils/commandUtils.js';
import { HELP_TEXT, GOODBYE_MESSAGE } from '../constants/asciiArt.js';

interface ChatInterfaceProps {
  messages: Message[];
  onSendMessage: (content: string) => Promise<void>;
}

export const ChatInterface: React.FC<ChatInterfaceProps> = ({ messages, onSendMessage }) => {
  const [input, setInput] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [suggestions, setSuggestions] = useState<Array<{name: string, description: string}>>([]);
  const [selectedSuggestionIndex, setSelectedSuggestionIndex] = useState(0);
  const [showBanner, setShowBanner] = useState(true);

  // Update suggestions when input changes
  useEffect(() => {
    if (isSlashCommand(input) || isAtCommand(input)) {
      const newSuggestions = getCommandSuggestions(input);
      setSuggestions(newSuggestions);
      setSelectedSuggestionIndex(0);
    } else {
      setSuggestions([]);
    }
  }, [input]);

  // Hide banner after first message
  useEffect(() => {
    if (messages.length > 0) {
      setShowBanner(false);
    }
  }, [messages]);

  const handleSubmit = async () => {
    if (!input.trim() || isProcessing) return;

    const message = input.trim();
    setInput('');
    setIsProcessing(true);
    setSuggestions([]);

    try {
      // Handle slash commands
      if (isSlashCommand(message)) {
        await handleSlashCommand(message);
      } 
      // Handle at commands (agents)
      else if (isAtCommand(message)) {
        await handleAtCommand(message);
      }
      // Regular message
      else {
        await onSendMessage(message);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  const handleSlashCommand = async (command: string) => {
    const cmdName = extractCommand(command);
    
    switch (cmdName?.toLowerCase()) {
      case 'help':
        console.log(HELP_TEXT);
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
        console.log(GOODBYE_MESSAGE);
        process.exit(0);
        break;
      
      case 'model':
        // TODO: Change model
        console.log('Available models: ...');
        break;
      
      default:
        console.log(`Unknown command: /${cmdName}`);
        console.log('Type /help for available commands');
    }
  };

  const handleAtCommand = async (command: string) => {
    const agentName = extractCommand(command);
    const messageContent = command.replace(`@${agentName}`, '').trim();
    
    // Prepend agent context to the message
    const agentMessage = `[Agent: ${agentName}] ${messageContent}`;
    await onSendMessage(agentMessage);
  };

  // Handle keyboard navigation for suggestions
  useInput((input, key) => {
    if (suggestions.length > 0) {
      if (key.upArrow) {
        setSelectedSuggestionIndex(prev => 
          prev > 0 ? prev - 1 : suggestions.length - 1
        );
        return;
      }
      
      if (key.downArrow) {
        setSelectedSuggestionIndex(prev => 
          prev < suggestions.length - 1 ? prev + 1 : 0
        );
        return;
      }
      
      if (key.tab) {
        // Auto-complete with selected suggestion
        const selected = suggestions[selectedSuggestionIndex];
        if (selected) {
          setInput(selected.name + ' ');
          setSuggestions([]);
        }
        return;
      }
    }
    
    if (key.ctrl && input === 'c') {
      console.log(GOODBYE_MESSAGE);
      process.exit(0);
    }
    
    if (key.ctrl && input === 'l') {
      // Clear screen
      console.clear();
    }
  });

  return (
    <Box flexDirection="column" height="100%">
      {/* Banner (shown initially) */}
      {showBanner && messages.length === 0 && <Banner />}

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
            <Text dimColor>
              💬 Type your message to start coding
            </Text>
            <Text dimColor>
              💡 Try /help or @code to get started
            </Text>
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

      {/* Command Suggestions */}
      <CommandSuggestions 
        suggestions={suggestions} 
        selectedIndex={selectedSuggestionIndex}
      />

      {/* Input */}
      <Box borderStyle="single" borderColor="gray" paddingX={1}>
        <Text color="green">{'> '}</Text>
        <TextInput
          value={input}
          onChange={setInput}
          onSubmit={handleSubmit}
          placeholder="Type your message... (or /help for commands)"
        />
      </Box>

      {/* Footer */}
      <Box paddingX={1}>
        <Text dimColor>
          Press Ctrl+C to exit | Type /help for commands
        </Text>
      </Box>
    </Box>
  );
};;

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

