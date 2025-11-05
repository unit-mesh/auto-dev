import React, { useState, useEffect, useMemo } from 'react';
import { Box, Text, useInput, Static } from 'ink';
import TextInput from 'ink-text-input';
import type { Message } from './App.js';
import { Banner } from './Banner.js';
import { CommandSuggestions } from './CommandSuggestions.js';
import { MessageBubble } from './MessageRenderer.js';
import {
  getCompletionSuggestions,
  shouldTriggerCompletion,
  applyCompletionItem
} from '../utils/commandUtils.js';
import { GOODBYE_MESSAGE } from '../constants/asciiArt.js';
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
  currentMode?: { name: string; displayName: string; description: string; icon: string } | null;
  modeStatus?: string;
}

export const ChatInterface: React.FC<ChatInterfaceProps> = ({
  messages,
  pendingMessage,
  isCompiling,
  onSendMessage,
  onClearMessages,
  currentMode,
  modeStatus
}) => {
  const [input, setInput] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [completionItems, setCompletionItems] = useState<CompletionItem[]>([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [showBanner, setShowBanner] = useState(true);
  const [shouldPreventSubmit, setShouldPreventSubmit] = useState(false);

  // Router logic is now handled by the mode system

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
      // Send message directly to the mode system
      await onSendMessage(message);
    } catch (error) {
      console.error('Error processing input:', error);
    } finally {
      setIsProcessing(false);
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
    <Box flexDirection="column">
      {/* Banner (shown initially, before any messages) */}
      {showBanner && messages.length === 0 && !pendingMessage && <Banner />}

      {/* Static area - includes Header and all completed messages */}
      <Static items={[
        // Header as first item
        <Box key="header" borderStyle="single" borderColor="cyan" paddingX={1}>
          <Box flexDirection="row" justifyContent="space-between">
            <Text bold color="cyan">
              {t('chat.title')}
            </Text>
            {currentMode && (
              <Text color="yellow">
                {currentMode.icon} {currentMode.displayName}
                {modeStatus && ` - ${modeStatus}`}
              </Text>
            )}
          </Box>
        </Box>,
        // All completed messages
        ...messages.map((msg, idx) => (
          <MessageBubble key={`msg-${idx}`} message={msg} />
        ))
      ]}>
        {(item) => item}
      </Static>

      {/* Dynamic area - only pending messages */}
      {pendingMessage && (
        <Box flexDirection="column" paddingX={1} paddingY={1}>
          <MessageBubble 
            key="pending" 
            message={pendingMessage} 
            isPending={true}
          />
        </Box>
      )}

      {/* Empty state hint - only show when truly empty */}
      {messages.length === 0 && !pendingMessage && !showBanner && (
        <Box flexDirection="column" paddingX={1} paddingY={1}>
          <Text dimColor>
            {t('chat.emptyHint')}
          </Text>
          <Text dimColor>
            {t('chat.startHint')}
          </Text>
        </Box>
      )}

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
};
