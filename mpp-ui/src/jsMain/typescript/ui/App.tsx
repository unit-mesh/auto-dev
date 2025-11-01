/**
 * AutoDev CLI - Main App Component
 * 
 * This is the root component of the AutoDev CLI application.
 * It manages the overall application state and renders the chat interface.
 */

import React, { useState, useEffect, useCallback } from 'react';
import { Box, Text, useApp, useInput } from 'ink';
import { ChatInterface } from './ChatInterface.js';
import { WelcomeScreen } from './WelcomeScreen.js';
import { ConfigManager } from '../config/ConfigManager.js';
import { LLMService } from '../services/LLMService.js';
import { compileDevIns, hasDevInsCommands } from '../utils/commandUtils.js';
import { findLastSafeSplitPoint } from '../utils/markdownSplitter.js';

export interface Message {
  role: 'user' | 'assistant' | 'system' | 'compiling';
  content: string;
  timestamp: number;
  showPrefix?: boolean;  // For multi-block messages, only first shows prefix
}

export const App: React.FC = () => {
  const { exit } = useApp();
  // Completed messages (history)
  const [messages, setMessages] = useState<Message[]>([]);
  // Pending message being streamed
  const [pendingMessage, setPendingMessage] = useState<Message | null>(null);
  const [isConfigured, setIsConfigured] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [llmService, setLLMService] = useState<LLMService | null>(null);
  // Track if we're currently compiling DevIns
  const [isCompiling, setIsCompiling] = useState(false);

  // Initialize configuration
  useEffect(() => {
    const initConfig = async () => {
      try {
        const config = await ConfigManager.load();
        const isValid = config.isValid();
        setIsConfigured(isValid);

        if (isValid) {
          // Initialize LLM service with config
          const service = new LLMService(config.toJSON());
          setLLMService(service);
        }

        setIsLoading(false);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load configuration');
        setIsLoading(false);
      }
    };

    initConfig();
  }, []);

  // Handle Ctrl+C to exit
  useInput((input, key) => {
    if (key.ctrl && input === 'c') {
      exit();
    }
  });

  const handleSendMessage = useCallback(async (content: string) => {
    if (!llmService) {
      setError('LLM service not initialized');
      return;
    }

    // Add user message to history
    const userMessage: Message = {
      role: 'user',
      content,
      timestamp: Date.now(),
      showPrefix: true,
    };

    setMessages(prev => [...prev, userMessage]);

    try {
      // Check if the message contains DevIns commands that need compilation
      let processedContent = content;
      
      if (hasDevInsCommands(content)) {
        // Show compiling state as pending message
        setIsCompiling(true);
        setPendingMessage({
          role: 'compiling',
          content: '🔧 Compiling DevIns commands...',
          timestamp: Date.now(),
          showPrefix: true,
        });

        const compileResult = await compileDevIns(content);
        
        if (compileResult) {
          if (compileResult.success) {
            // Use the compiled output instead of raw input
            processedContent = compileResult.output;
            
            // Add a system message showing the compilation result if it processed commands
            if (compileResult.hasCommand && compileResult.output !== content) {
              const compileMessage: Message = {
                role: 'system',
                content: `📝 Compiled output:\n${compileResult.output}`,
                timestamp: Date.now(),
                showPrefix: true,
              };
              setMessages(prev => [...prev, compileMessage]);
            }
          } else {
            // Compilation failed - show error but still send original to LLM
            const errorMessage: Message = {
              role: 'system',
              content: `⚠️  DevIns compilation error: ${compileResult.errorMessage}`,
              timestamp: Date.now(),
              showPrefix: true,
            };
            setMessages(prev => [...prev, errorMessage]);
          }
        }
        
        setIsCompiling(false);
        setPendingMessage(null);
      }

      // Create pending assistant message for streaming
      setPendingMessage({
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
      });

      // Stream response from LLM with block-level splitting
      // Complete blocks are moved to Static to prevent scroll flicker
      let assistantContent = '';
      const startTimestamp = Date.now();
      let isFirstBlock = true;  // Track if this is the first block (should show prefix)

      await llmService.streamMessage(processedContent, (chunk) => {
        assistantContent += chunk;
        
        // Find safe split point (end of complete block)
        const splitPoint = findLastSafeSplitPoint(assistantContent);
        
        if (splitPoint === assistantContent.length) {
          // No complete block yet, just update pending
          setPendingMessage({
            role: 'assistant',
            content: assistantContent,
            timestamp: startTimestamp,
            showPrefix: isFirstBlock,
          });
        } else {
          // Found complete block(s) - split it
          const completedContent = assistantContent.substring(0, splitPoint);
          const pendingContent = assistantContent.substring(splitPoint);
          
          // Move completed block to history (Static area)
          setMessages(prev => [...prev, {
            role: 'assistant',
            content: completedContent,
            timestamp: startTimestamp,
            showPrefix: isFirstBlock,  // Only first block shows prefix
          }]);
          
          // Keep only pending content
          setPendingMessage({
            role: 'assistant',
            content: pendingContent,
            timestamp: startTimestamp,
            showPrefix: false,  // Continuation blocks don't show prefix
          });
          
          // Update accumulator to only pending content
          assistantContent = pendingContent;
          isFirstBlock = false;  // Subsequent blocks are continuations
        }
      });

      // Clear pending FIRST to avoid duplication
      setPendingMessage(null);
      
      // Move any remaining content to history
      if (assistantContent.trim()) {
        setMessages(prev => [...prev, {
          role: 'assistant',
          content: assistantContent,
          timestamp: startTimestamp,
          showPrefix: isFirstBlock,  // Show prefix if this is the only block
        }]);
      }

    } catch (err) {
      // Clear pending state first
      setPendingMessage(null);
      setIsCompiling(false);
      
      // Display error message in chat
      const errorMessage: Message = {
        role: 'system',
        content: err instanceof Error ? err.message : 'Failed to send message',
        timestamp: Date.now(),
        showPrefix: true,
      };
      setMessages(prev => [...prev, errorMessage]);
    }
  }, [llmService]);

  const handleConfigured = useCallback(async (config: any) => {
    try {
      const service = new LLMService(config);
      setLLMService(service);
      setIsConfigured(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to initialize LLM service');
    }
  }, []);

  const handleClearMessages = useCallback(() => {
    setMessages([]);
    setPendingMessage(null);
    console.clear();
  }, []);

  if (isLoading) {
    return (
      <Box flexDirection="column" padding={1}>
        <Text>Loading AutoDev CLI...</Text>
      </Box>
    );
  }

  if (error) {
    return (
      <Box flexDirection="column" padding={1}>
        <Text color="red">Error: {error}</Text>
        <Text dimColor>Press Ctrl+C to exit</Text>
      </Box>
    );
  }

  if (!isConfigured) {
    return <WelcomeScreen onConfigured={handleConfigured} />;
  }

  return (
    <ChatInterface 
      messages={messages} 
      pendingMessage={pendingMessage}
      isCompiling={isCompiling}
      onSendMessage={handleSendMessage}
      onClearMessages={handleClearMessages}
    />
  );
};

