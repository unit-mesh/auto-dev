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

export interface Message {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: number;
}

export const App: React.FC = () => {
  const { exit } = useApp();
  const [messages, setMessages] = useState<Message[]>([]);
  const [isConfigured, setIsConfigured] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [llmService, setLLMService] = useState<LLMService | null>(null);

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

    // Add user message
    const userMessage: Message = {
      role: 'user',
      content,
      timestamp: Date.now(),
    };

    setMessages(prev => [...prev, userMessage]);

    try {
      // Stream response from LLM
      let assistantContent = '';

      await llmService.streamMessage(content, (chunk) => {
        assistantContent += chunk;
        // Update the last message with streaming content
        setMessages(prev => {
          const newMessages = [...prev];
          const lastMsg = newMessages[newMessages.length - 1];

          if (lastMsg && lastMsg.role === 'assistant') {
            lastMsg.content = assistantContent;
          } else {
            newMessages.push({
              role: 'assistant',
              content: assistantContent,
              timestamp: Date.now(),
            });
          }

          return newMessages;
        });
      });

    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to send message');
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

  return <ChatInterface messages={messages} onSendMessage={handleSendMessage} />;
};

