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
  applyCompletionItem,
  extractCommand
} from '../utils/commandUtils.js';
import { HELP_TEXT, GOODBYE_MESSAGE } from '../constants/asciiArt.js';

type CompletionItem = {
  text: string;
  displayText: string;
  description: string | null;
  icon: string | null;
  triggerType: string;
  index: number;
};

interface ChatInterfaceProps {
  messages: Message[];
  pendingMessage: Message | null;
  isCompiling: boolean;
  onSendMessage: (content: string) => Promise<void>;
}

export const ChatInterface: React.FC<ChatInterfaceProps> = ({ 
  messages, 
  pendingMessage,
  isCompiling,
  onSendMessage 
}) => {
  const [input, setInput] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [completionItems, setCompletionItems] = useState<CompletionItem[]>([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [showBanner, setShowBanner] = useState(true);
  const [shouldPreventSubmit, setShouldPreventSubmit] = useState(false);

  // Sync isProcessing with external state
  useEffect(() => {
    setIsProcessing(isCompiling || pendingMessage !== null);
  }, [isCompiling, pendingMessage]);

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
    // Don't submit if we should prevent it (e.g., when applying completion)
    if (shouldPreventSubmit) {
      setShouldPreventSubmit(false);
      return;
    }
    
    if (!input.trim() || isProcessing) return;

    const message = input.trim();
    setInput('');
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
    } catch (error) {
      console.error('Error sending message:', error);
    }
  };

  const applyCompletion = async (item: CompletionItem) => {
    // Use the Kotlin insert handler which properly handles special cases
    const result = await applyCompletionItem(input, input.length, item.index);
    
    if (result) {
      // Update the input text
      setInput(result.newText);
      
      // If the completion indicates we should trigger next completion (e.g., ":" was added)
      // fetch new completions at the new cursor position immediately
      if (result.shouldTriggerNextCompletion) {
        // Trigger completion immediately - the cursor is at the end of the new text
        // which is exactly where newCursorPosition points
        const items = await getCompletionSuggestions(result.newText, result.newCursorPosition);
        if (items.length > 0) {
          setCompletionItems(items);
          setSelectedIndex(0);
        } else {
          setCompletionItems([]);
        }
      } else {
        setCompletionItems([]);
      }
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
        // Prevent form submission when applying completion
        setShouldPreventSubmit(true);
        
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
        {messages.length === 0 && !pendingMessage ? (
          <Box>
            <Text dimColor>
              💬 Type your message to start coding
            </Text>
            <Text dimColor>
              💡 Try /help or @code to get started
            </Text>
          </Box>
        ) : (
          <>
            {/* Render completed messages */}
            {messages.map((msg, idx) => (
              <MessageBubble key={idx} message={msg} />
            ))}
            
            {/* Render pending message (streaming or compiling) */}
            {pendingMessage && (
              <MessageBubble 
                key="pending" 
                message={pendingMessage} 
                isPending={true}
              />
            )}
          </>
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
  isPending?: boolean;
}

const MessageBubble: React.FC<MessageBubbleProps> = ({ message, isPending = false }) => {
  // Handle different message roles
  if (message.role === 'compiling') {
    return (
      <Box marginBottom={1}>
        <Text color="yellow">
          <Spinner type="dots" /> {message.content}
        </Text>
      </Box>
    );
  }

  if (message.role === 'system') {
    return (
      <Box flexDirection="column" marginBottom={1}>
        <Box>
          <Text bold color="blue">
            ℹ️  System:
          </Text>
        </Box>
        <Box paddingLeft={2}>
          <Text dimColor>{message.content}</Text>
        </Box>
      </Box>
    );
  }

  const isUser = message.role === 'user';
  const color = isUser ? 'green' : 'cyan';
  const prefix = isUser ? '👤 You' : '🤖 AI';

  return (
    <Box flexDirection="column" marginBottom={1}>
      <Box>
        <Text bold color={color}>
          {prefix}:
          {isPending && !isUser && (
            <Text color="cyan"> <Spinner type="dots" /></Text>
          )}
        </Text>
      </Box>
      <Box paddingLeft={2}>
        <Text>{message.content || (isPending ? '...' : '')}</Text>
      </Box>
    </Box>
  );
};

