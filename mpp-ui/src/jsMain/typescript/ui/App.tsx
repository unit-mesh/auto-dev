/**
 * AutoDev CLI - Main App Component
 *
 * This is the root component of the AutoDev CLI application.
 * It manages the overall application state and renders the appropriate interface
 * based on the current mode (Agent or Chat).
 */

import React, { useState, useEffect, useCallback } from 'react';
import { Box, Text, useApp, useInput } from 'ink';
import { ChatInterface } from './ChatInterface.js';
import { WelcomeScreen } from './WelcomeScreen.js';
import { ConfigManager } from '../config/ConfigManager.js';
import { ModeManager, AgentModeFactory, ChatModeFactory } from '../modes/index.js';
import type { ModeContext } from '../modes/index.js';
import { ModeCommandProcessor } from '../processors/ModeCommandProcessor.js';

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
  // Track if we're currently compiling DevIns
  const [isCompiling, setIsCompiling] = useState(false);
  // Mode management
  const [modeManager] = useState(() => new ModeManager());
  const [currentModeType, setCurrentModeType] = useState<string>('agent'); // Default to agent mode

  // Initialize configuration and mode system
  useEffect(() => {
    const initApp = async () => {
      try {
        const config = await ConfigManager.load();
        const isValid = config.isValid();
        setIsConfigured(isValid);

        if (isValid) {
          // Register mode factories
          modeManager.registerMode(new AgentModeFactory());
          modeManager.registerMode(new ChatModeFactory());

          // Set up mode change listener
          modeManager.onModeChange((event) => {
            setCurrentModeType(event.currentMode);
          });

          // Initialize default mode (agent)
          await initializeMode('agent', config.toJSON());
        }

        setIsLoading(false);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load configuration');
        setIsLoading(false);
      }
    };

    initApp();
  }, [modeManager]);

  // Initialize mode with context
  const initializeMode = useCallback(async (modeType: string, llmConfig: any) => {
    const modeContext: ModeContext = {
      addMessage: (message: Message) => setMessages(prev => [...prev, message]),
      setPendingMessage,
      setIsCompiling,
      clearMessages: () => {
        setMessages([]);
        setPendingMessage(null);
      },
      logger: {
        info: (msg) => console.log(`[INFO] ${msg}`),
        warn: (msg) => console.warn(`[WARN] ${msg}`),
        error: (msg, err) => console.error(`[ERROR] ${msg}`, err || '')
      },
      projectPath: process.cwd(),
      llmConfig
    };

    const success = await modeManager.switchToMode(modeType, modeContext);
    if (!success) {
      throw new Error(`Failed to initialize ${modeType} mode`);
    }
  }, [modeManager]);

  // Handle Ctrl+C to exit
  useInput((input, key) => {
    if (key.ctrl && input === 'c') {
      exit();
    }
  });

  const handleSendMessage = useCallback(async (content: string) => {
    const trimmedContent = content.trim();
    if (!trimmedContent) {
      return;
    }

    try {
      // Check if it's a mode switch command first
      const modeCommandProcessor = new ModeCommandProcessor(modeManager, {
        addMessage: (message: Message) => setMessages(prev => [...prev, message]),
        setPendingMessage,
        setIsCompiling,
        clearMessages: () => {
          setMessages([]);
          setPendingMessage(null);
        },
        logger: {
          info: (msg) => console.log(`[INFO] ${msg}`),
          warn: (msg) => console.warn(`[WARN] ${msg}`),
          error: (msg, err) => console.error(`[ERROR] ${msg}`, err || '')
        },
        projectPath: process.cwd(),
        llmConfig: null // Will be set when needed
      });

      if (modeCommandProcessor.canHandle(trimmedContent)) {
        const result = await modeCommandProcessor.process(trimmedContent, {
          clearMessages: () => {
            setMessages([]);
            setPendingMessage(null);
          },
          logger: {
            info: (msg) => console.log(`[INFO] ${msg}`),
            warn: (msg) => console.warn(`[WARN] ${msg}`),
            error: (msg, err) => console.error(`[ERROR] ${msg}`, err || '')
          }
        });

        if (result.type === 'handled' && result.output) {
          const outputMessage: Message = {
            role: 'system',
            content: result.output,
            timestamp: Date.now(),
            showPrefix: true
          };
          setMessages(prev => [...prev, outputMessage]);
        } else if (result.type === 'error') {
          const errorMessage: Message = {
            role: 'system',
            content: `❌ ${result.message}`,
            timestamp: Date.now(),
            showPrefix: true
          };
          setMessages(prev => [...prev, errorMessage]);
        }
        return;
      }

      // Handle input through current mode
      const success = await modeManager.handleInput(trimmedContent);
      if (!success) {
        const errorMessage: Message = {
          role: 'system',
          content: '❌ Failed to process input',
          timestamp: Date.now(),
          showPrefix: true
        };
        setMessages(prev => [...prev, errorMessage]);
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
  }, [modeManager]);

  const handleConfigured = useCallback(async (config: any) => {
    try {
      // Register mode factories
      modeManager.registerMode(new AgentModeFactory());
      modeManager.registerMode(new ChatModeFactory());

      // Set up mode change listener
      modeManager.onModeChange((event) => {
        setCurrentModeType(event.currentMode);
      });

      // Initialize default mode (agent)
      await initializeMode('agent', config);
      setIsConfigured(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to initialize application');
    }
  }, [modeManager, initializeMode]);

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

  // Get current mode info for display
  const currentMode = modeManager.getCurrentMode();
  const modeInfo = currentMode ? modeManager.getModeInfo(currentMode.type) : null;

  return (
    <ChatInterface
      messages={messages}
      pendingMessage={pendingMessage}
      isCompiling={isCompiling}
      onSendMessage={handleSendMessage}
      onClearMessages={handleClearMessages}
      currentMode={modeInfo}
      modeStatus={currentMode?.mode.getStatus()}
    />
  );
};

