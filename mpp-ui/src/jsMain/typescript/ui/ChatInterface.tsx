/**
 * ChatInterface - Main chat UI component
 * 
 * Displays the chat history and input prompt with command auto-completion from Kotlin.
 */

import React, { useState, useEffect } from 'react';
import { Box, Text, useInput } from 'ink';
import TextInput from 'ink-text-input';
import Spinner from 'ink-spinner';
import type { Message } from './App.js';
import { Banner } from './Banner.js';
import { CommandSuggestions } from './CommandSuggestions.js';
import { 
  getCompletionSuggestions,
  shouldTriggerCompletion,
  extractCommand
} from '../utils/commandUtils.js';
import { HELP_TEXT, GOODBYE_MESSAGE } from '../constants/asciiArt.js';

type CompletionItem = {
  text: string;
  displayText: string;
  description: string | null;
  icon: string | null;
  triggerType: string;
};

interface ChatInterfaceProps {
  messages: Message[];
  onSendMessage: (content: string) => Promise<void>;
}

export const ChatInterface: React.FC<ChatInterfaceProps> = ({ messages, onSendMessage }) => {
  const [input, setInput] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [completionItems, setCompletionItems] = useState<CompletionItem[]>([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [showBanner, setShowBanner] = useState(true);

  // Update completions when input changes
  useEffect(() => {
    const updateCompletions = async () => {
      if (input.length === 0) {
        setCompletionItems([]);
        return;
      }

      // Check if last character triggers completion
      const lastChar = input[input.length - 1];
      const shouldTrigger = await shouldTriggerCompletion(lastChar);
      
      if (shouldTrigger || completionItems.length > 0) {
        const items = await getCompletionSuggestions(input, input.length);
        setCompletionItems(items);
        setSelectedIndex(0);
      }
    };

    updateCompletions();
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
    setCompletionItems([]);

    try {
      // Handle slash commands
      if (message.startsWith('/')) {
        await handleSlashCommand(message);
      } 
      // Regular message (including @agent commands)
      else {
        await onSendMessage(message);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  const applyCompletion = (item: CompletionItem) => {
    // Find the trigger character position
    const lastTrigger = Math.max(
      input.lastIndexOf('@'),
      input.lastIndexOf('/'),
      input.lastIndexOf('$'),
      input.lastIndexOf(':')
    );
    
    if (lastTrigger >= 0) {
      const before = input.substring(0, lastTrigger + 1);
      const newInput = before + item.text;
      setInput(newInput);
      setCompletionItems([]);
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

  // Handle keyboard navigation for completions
  useInput((input, key) => {
    if (completionItems.length > 0) {
      if (key.upArrow) {
        setSelectedIndex(prev => 
          prev > 0 ? prev - 1 : completionItems.length - 1
        );
        return;
      }
      
      if (key.downArrow) {
        setSelectedIndex(prev => 
          prev < completionItems.length - 1 ? prev + 1 : 0
        );
        return;
      }
      
      if (key.tab || key.return) {
        // Apply selected completion
        const selected = completionItems[selectedIndex];
        if (selected) {
          applyCompletion(selected);
        }
        return;
      }
      
      if (key.escape) {
        setCompletionItems([]);
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
        items={completionItems} 
        selectedIndex={selectedIndex}
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

