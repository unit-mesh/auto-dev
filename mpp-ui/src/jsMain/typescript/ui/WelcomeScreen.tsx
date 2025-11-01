/**
 * WelcomeScreen - Initial configuration screen
 * 
 * Displayed when the CLI is run for the first time or when configuration is missing.
 */

import React, { useState } from 'react';
import { Box, Text } from 'ink';
import TextInput from 'ink-text-input';
import { ConfigManager, LLMProvider, LLMConfig } from '../config/ConfigManager.js';
import { ModelConfigForm } from './ModelConfigForm.js';

interface WelcomeScreenProps {
  onConfigured: (config: any) => void;
}

export const WelcomeScreen: React.FC<WelcomeScreenProps> = ({ onConfigured }) => {
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [showNameInput, setShowNameInput] = useState(false);
  const [configName, setConfigName] = useState('');
  const [pendingConfig, setPendingConfig] = useState<{
    provider: LLMProvider;
    model: string;
    apiKey: string;
    baseUrl?: string;
  } | null>(null);

  const handleConfigSubmit = (config: {
    provider: LLMProvider;
    model: string;
    apiKey: string;
    baseUrl?: string;
  }) => {
    // First, ask for config name
    setPendingConfig(config);
    setShowNameInput(true);
  };

  const handleNameSubmit = async (name: string) => {
    if (!name.trim() || !pendingConfig) {
      return;
    }

    setSaving(true);

    try {
      const llmConfig: LLMConfig = {
        name: name.trim(),
        provider: pendingConfig.provider,
        apiKey: pendingConfig.apiKey,
        model: pendingConfig.model,
        baseUrl: pendingConfig.baseUrl,
      };

      await ConfigManager.saveConfig(llmConfig, true);
      setSaved(true);
      
      setTimeout(() => {
        onConfigured(pendingConfig);
      }, 1000);
    } catch (error) {
      console.error('Failed to save configuration:', error);
      setSaving(false);
    }
  };

  if (saved) {
    return (
      <Box flexDirection="column" padding={2}>
        <Text color="green">✓ Configuration saved!</Text>
        <Text dimColor>Starting AutoDev CLI...</Text>
      </Box>
    );
  }

  if (saving) {
    return (
      <Box flexDirection="column" padding={2}>
        <Text color="yellow">⏳ Saving configuration...</Text>
      </Box>
    );
  }

  if (showNameInput && pendingConfig) {
    return (
      <Box flexDirection="column" padding={2}>
        <Box marginBottom={1}>
          <Text bold color="cyan">
            💾 Name Your Configuration
          </Text>
        </Box>

        <Box marginBottom={1}>
          <Text dimColor>
            Give this configuration a name (e.g., "work", "personal", "gpt4"):
          </Text>
        </Box>

        <Box marginBottom={1}>
          <Text>
            <Text color="green">✓</Text> Provider: <Text bold>{pendingConfig.provider}</Text>
          </Text>
          <Text>
            <Text color="green">✓</Text> Model: <Text bold>{pendingConfig.model}</Text>
          </Text>
        </Box>

        <Box>
          <Text color="cyan">Name: </Text>
          <TextInput
            value={configName}
            onChange={setConfigName}
            onSubmit={handleNameSubmit}
            placeholder="default"
          />
        </Box>

        <Box marginTop={1}>
          <Text dimColor>Press Enter to save</Text>
        </Box>
      </Box>
    );
  }

  return (
    <Box flexDirection="column" padding={1}>
      {/* Header */}
      <Box marginBottom={1}>
        <Text bold color="cyan">
          🚀 Welcome to AutoDev CLI!
        </Text>
      </Box>

      <Box marginBottom={1}>
        <Text dimColor>
          Let's set up your AI configuration. You can add more later in ~/.autodev/config.yaml
        </Text>
      </Box>

      {/* Configuration Form */}
      <ModelConfigForm onSubmit={handleConfigSubmit} />

      {/* Footer */}
      <Box marginTop={1}>
        <Text dimColor>
          Press Ctrl+C to exit
        </Text>
      </Box>
    </Box>
  );
};

