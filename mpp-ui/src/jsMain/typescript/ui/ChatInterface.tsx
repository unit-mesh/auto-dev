import React, { useState, useEffect, useMemo } from 'react';
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
  compileDevIns
} from '../utils/commandUtils.js';
import { GOODBYE_MESSAGE } from '../constants/asciiArt.js';
import { InputRouter } from '../processors/InputRouter.js';
import { SlashCommandProcessor } from '../processors/SlashCommandProcessor.js';
import { AtCommandProcessor } from '../processors/AtCommandProcessor.js';
import { VariableProcessor } from '../processors/VariableProcessor.js';
import { t } from '../i18n/index.js';

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
  onClearMessages?: () => void;
}

export const ChatInterface: React.FC<ChatInterfaceProps> = ({ 
  messages, 
  pendingMessage,
  isCompiling,
  onSendMessage,
  onClearMessages
}) => {
  const [input, setInput] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [completionItems, setCompletionItems] = useState<CompletionItem[]>([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [showBanner, setShowBanner] = useState(true);
  const [shouldPreventSubmit, setShouldPreventSubmit] = useState(false);

  // Initialize input router with processors
  const router = useMemo(() => {
    const r = new InputRouter();
    
    // Register processors with priorities
    // Higher priority = executed first
    r.register(new SlashCommandProcessor(), 100);
    r.register(new AtCommandProcessor(), 50);
    r.register(new VariableProcessor(), 30);
    
    return r;
  }, []);

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
    setIsProcessing(true);

    try {
      // Route input through processor chain
      const result = await router.route(message, {
        clearMessages: onClearMessages || (() => {}),
        logger: {
          info: (msg) => console.log(`[INFO] ${msg}`),
          warn: (msg) => console.warn(`[WARN] ${msg}`),
          error: (msg, err) => console.error(`[ERROR] ${msg}`, err || '')
        },
        readFile: async (path) => {
          // Read file through DevIns compiler
          const compileResult = await compileDevIns(`/file:${path}`);
          if (compileResult?.success) {
            return compileResult.output;
          }
          throw new Error(compileResult?.errorMessage || 'Failed to read file');
        }
      });

      // Handle result
      switch (result.type) {
        case 'handled':
          // Command was handled, output already shown
          break;
        
        case 'compile':
          // Need to compile DevIns
          await handleDevInsCompile(result.devins);
          break;
        
        case 'llm-query':
          // Send to LLM
          await onSendMessage(result.query);
          break;
        
        case 'error':
          // Show error
          console.error(result.message);
          break;
      }
    } catch (error) {
      console.error('Error processing input:', error);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDevInsCompile = async (devins: string) => {
    // Compile DevIns and send result to LLM
    const compileResult = await compileDevIns(devins);
    
    if (compileResult?.success) {
      // Send compiled output to LLM
      await onSendMessage(compileResult.output);
    } else {
      // Show compilation error
      console.error(`DevIns compilation error: ${compileResult?.errorMessage}`);
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
          {t('chat.title')}
        </Text>
      </Box>

      {/* Messages */}
      <Box flexDirection="column" flexGrow={1} paddingX={1} paddingY={1}>
        {messages.length === 0 && !pendingMessage ? (
          <Box>
            <Text dimColor>
              {t('chat.emptyHint')}
            </Text>
            <Text dimColor>
              {t('chat.startHint')}
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
          placeholder={t('chat.inputPlaceholder')}
        />
      </Box>

      {/* Footer */}
      <Box paddingX={1}>
        <Text dimColor>
          {t('chat.exitHint')} | {t('chat.helpHint')}
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
            {t('chat.prefixes.system')}:
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
  const prefix = isUser ? t('chat.prefixes.you') : t('chat.prefixes.ai');

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
        {/* Simple text rendering for now */}
        <Text>{message.content || (isPending ? '...' : '')}</Text>
      </Box>
    </Box>
  );
};

