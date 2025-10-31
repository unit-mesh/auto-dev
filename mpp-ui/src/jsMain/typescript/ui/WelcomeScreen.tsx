/**
 * WelcomeScreen - Initial configuration screen
 * 
 * Displayed when the CLI is run for the first time or when configuration is missing.
 */

import React, { useState } from 'react';
import { Box, Text } from 'ink';
import TextInput from 'ink-text-input';
import SelectInput from 'ink-select-input';
import { ConfigManager, LLMProvider } from '../config/ConfigManager.js';

interface WelcomeScreenProps {
  onConfigured: (config: any) => void;
}

type ConfigStep = 'provider' | 'apiKey' | 'model' | 'complete';

export const WelcomeScreen: React.FC<WelcomeScreenProps> = ({ onConfigured }) => {
  const [step, setStep] = useState<ConfigStep>('provider');
  const [provider, setProvider] = useState<LLMProvider>('openai');
  const [apiKey, setApiKey] = useState('');
  const [model, setModel] = useState('');

  const providerOptions = [
    { label: 'OpenAI (GPT-4, GPT-3.5)', value: 'openai' },
    { label: 'Anthropic (Claude)', value: 'anthropic' },
    { label: 'Google (Gemini)', value: 'google' },
    { label: 'DeepSeek', value: 'deepseek' },
    { label: 'Ollama (Local)', value: 'ollama' },
    { label: 'OpenRouter', value: 'openrouter' },
  ];

  const handleProviderSelect = (item: { value: string }) => {
    setProvider(item.value as LLMProvider);
    setStep('apiKey');
  };

  const handleApiKeySubmit = (value: string) => {
    setApiKey(value);
    setStep('model');
  };

  const handleModelSubmit = async (value: string) => {
    setModel(value || getDefaultModel(provider));

    // Save configuration
    try {
      const config = {
        provider,
        apiKey,
        model: value || getDefaultModel(provider),
        baseUrl: provider === 'ollama' ? 'http://localhost:11434' : '',
      };

      await ConfigManager.save(config);

      setStep('complete');
      setTimeout(() => {
        onConfigured(config);
      }, 1000);
    } catch (error) {
      console.error('Failed to save configuration:', error);
    }
  };

  const getDefaultModel = (provider: LLMProvider): string => {
    switch (provider) {
      case 'openai':
        return 'gpt-4';
      case 'anthropic':
        return 'claude-3-5-sonnet-20241022';
      case 'google':
        return 'gemini-2.0-flash-exp';
      case 'deepseek':
        return 'deepseek-chat';
      case 'ollama':
        return 'llama3.2';
      case 'openrouter':
        return 'anthropic/claude-3.5-sonnet';
      default:
        return '';
    }
  };

  return (
    <Box flexDirection="column" padding={2}>
      {/* Header */}
      <Box marginBottom={2}>
        <Text bold color="cyan">
          🚀 Welcome to AutoDev CLI!
        </Text>
      </Box>

      <Box marginBottom={1}>
        <Text>
          Let's set up your AI configuration. You can change these settings later.
        </Text>
      </Box>

      {/* Provider Selection */}
      {step === 'provider' && (
        <Box flexDirection="column" marginTop={1}>
          <Box marginBottom={1}>
            <Text bold>Select your LLM provider:</Text>
          </Box>
          <SelectInput items={providerOptions} onSelect={handleProviderSelect} />
        </Box>
      )}

      {/* API Key Input */}
      {step === 'apiKey' && (
        <Box flexDirection="column" marginTop={1}>
          <Box marginBottom={1}>
            <Text bold>
              Enter your {provider.toUpperCase()} API key:
            </Text>
          </Box>
          <Box>
            <Text color="green">{'> '}</Text>
            <TextInput
              value={apiKey}
              onChange={setApiKey}
              onSubmit={handleApiKeySubmit}
              placeholder={provider === 'ollama' ? 'Press Enter to skip (local)' : 'sk-...'}
              mask={provider !== 'ollama' ? '*' : undefined}
            />
          </Box>
          {provider === 'ollama' && (
            <Box marginTop={1}>
              <Text dimColor>
                Note: Ollama runs locally, no API key needed.
              </Text>
            </Box>
          )}
        </Box>
      )}

      {/* Model Input */}
      {step === 'model' && (
        <Box flexDirection="column" marginTop={1}>
          <Box marginBottom={1}>
            <Text bold>
              Enter model name (or press Enter for default: {getDefaultModel(provider)}):
            </Text>
          </Box>
          <Box>
            <Text color="green">{'> '}</Text>
            <TextInput
              value={model}
              onChange={setModel}
              onSubmit={handleModelSubmit}
              placeholder={getDefaultModel(provider)}
            />
          </Box>
        </Box>
      )}

      {/* Complete */}
      {step === 'complete' && (
        <Box flexDirection="column" marginTop={1}>
          <Text color="green">✓ Configuration saved!</Text>
          <Text dimColor>Starting AutoDev CLI...</Text>
        </Box>
      )}

      {/* Footer */}
      <Box marginTop={2}>
        <Text dimColor>
          Press Ctrl+C to exit
        </Text>
      </Box>
    </Box>
  );
};

