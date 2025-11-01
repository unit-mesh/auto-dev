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

export interface Message {
  role: 'user' | 'assistant' | 'system' | 'compiling';
  content: string;
  timestamp: number;
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
              };
              setMessages(prev => [...prev, compileMessage]);
            }
          } else {
            // Compilation failed - show error but still send original to LLM
            const errorMessage: Message = {
              role: 'system',
              content: `⚠️  DevIns compilation error: ${compileResult.errorMessage}`,
              timestamp: Date.now(),
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

      // Stream response from LLM using the processed content
      let assistantContent = '';
      let lastLineCount = 0;

      await llmService.streamMessage(processedContent, (chunk) => {
        assistantContent += chunk;
        
        // Only update UI when content contains new lines (to reduce flickering)
        // Count lines in the content
        const currentLineCount = (assistantContent.match(/\n/g) || []).length;
        
        // Update if:
        // 1. New line was added
        // 2. Content is still short (< 100 chars) - for initial feedback
        // 3. Every 50 characters for long single-line content
        const shouldUpdate = 
          currentLineCount > lastLineCount || 
          assistantContent.length < 100 ||
          assistantContent.length % 50 === 0;
        
        if (shouldUpdate) {
          setPendingMessage({
            role: 'assistant',
            content: assistantContent,
            timestamp: Date.now(),
          });
          lastLineCount = currentLineCount;
        }
      });

      // Final update to ensure all content is shown
      setPendingMessage({
        role: 'assistant',
        content: assistantContent,
        timestamp: Date.now(),
      });

      // Move pending message to history once complete
      if (assistantContent) {
        setMessages(prev => [...prev, {
          role: 'assistant',
          content: assistantContent,
          timestamp: Date.now(),
        }]);
      }
      setPendingMessage(null);

    } catch (err) {
      // Display error message in chat
      const errorMessage: Message = {
        role: 'system',
        content: err instanceof Error ? err.message : 'Failed to send message',
        timestamp: Date.now(),
      };
      setMessages(prev => [...prev, errorMessage]);
      
      setPendingMessage(null);
      setIsCompiling(false);
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

