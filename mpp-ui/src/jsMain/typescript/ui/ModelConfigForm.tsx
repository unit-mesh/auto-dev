/**
 * ModelConfigForm - 模型配置表单
 * 
 * 提供一个简洁的表单让用户配置 LLM provider、model 和 API key
 */

import React, { useState } from 'react';
import { Box, Text } from 'ink';
import TextInput from 'ink-text-input';
import SelectInput from 'ink-select-input';
import type { LLMProvider } from '../config/ConfigManager.js';

interface ModelConfigFormProps {
  onSubmit: (config: {
    provider: LLMProvider;
    model: string;
    apiKey: string;
    baseUrl?: string;
  }) => void;
  onCancel?: () => void;
}

type FormStep = 'provider' | 'model' | 'apiKey' | 'baseUrl' | 'confirm';

// Provider 选项
const PROVIDER_OPTIONS = [
  { label: '🔹 OpenAI (GPT-4, GPT-3.5)', value: 'openai' },
  { label: '🔹 Anthropic (Claude)', value: 'anthropic' },
  { label: '🔹 Google (Gemini)', value: 'google' },
  { label: '🔹 DeepSeek', value: 'deepseek' },
  { label: '🔹 Ollama (Local)', value: 'ollama' },
  { label: '🔹 OpenRouter', value: 'openrouter' },
];

// 每个 provider 的默认模型和示例
const PROVIDER_DEFAULTS: Record<string, { defaultModel: string; needsApiKey: boolean; baseUrl?: string }> = {
  openai: { defaultModel: 'gpt-4', needsApiKey: true },
  anthropic: { defaultModel: 'claude-3-5-sonnet-20241022', needsApiKey: true },
  google: { defaultModel: 'gemini-pro', needsApiKey: true },
  deepseek: { defaultModel: 'deepseek-chat', needsApiKey: true },
  ollama: { defaultModel: 'llama2', needsApiKey: false, baseUrl: 'http://localhost:11434' },
  openrouter: { defaultModel: 'openai/gpt-4', needsApiKey: true },
};

export const ModelConfigForm: React.FC<ModelConfigFormProps> = ({ onSubmit, onCancel }) => {
  const [step, setStep] = useState<FormStep>('provider');
  const [provider, setProvider] = useState<LLMProvider>('openai');
  const [model, setModel] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [baseUrl, setBaseUrl] = useState('');

  const providerInfo = PROVIDER_DEFAULTS[provider];

  const handleProviderSelect = (item: { value: string }) => {
    const selectedProvider = item.value as LLMProvider;
    setProvider(selectedProvider);
    setModel(PROVIDER_DEFAULTS[selectedProvider].defaultModel);
    setBaseUrl(PROVIDER_DEFAULTS[selectedProvider].baseUrl || '');
    setStep('model');
  };

  const handleModelSubmit = (value: string) => {
    setModel(value);
    
    // If provider doesn't need API key (like Ollama), skip to baseUrl
    if (!providerInfo.needsApiKey) {
      if (providerInfo.baseUrl) {
        setStep('baseUrl');
      } else {
        setStep('confirm');
      }
    } else {
      setStep('apiKey');
    }
  };

  const handleApiKeySubmit = (value: string) => {
    setApiKey(value);
    
    // Ask for baseUrl if provider typically has one
    if (provider === 'ollama' || provider === 'openrouter') {
      setStep('baseUrl');
    } else {
      setStep('confirm');
    }
  };

  const handleBaseUrlSubmit = (value: string) => {
    setBaseUrl(value);
    setStep('confirm');
  };

  const handleConfirm = () => {
    onSubmit({
      provider,
      model: model || providerInfo.defaultModel,
      apiKey,
      baseUrl: baseUrl || undefined,
    });
  };

  const handleBack = () => {
    switch (step) {
      case 'model':
        setStep('provider');
        break;
      case 'apiKey':
        setStep('model');
        break;
      case 'baseUrl':
        if (providerInfo.needsApiKey) {
          setStep('apiKey');
        } else {
          setStep('model');
        }
        break;
      case 'confirm':
        if (baseUrl || provider === 'ollama') {
          setStep('baseUrl');
        } else if (providerInfo.needsApiKey) {
          setStep('apiKey');
        } else {
          setStep('model');
        }
        break;
    }
  };

  return (
    <Box flexDirection="column" paddingX={2} paddingY={1}>
      {/* Header */}
      <Box marginBottom={1}>
        <Text bold color="cyan">
          🤖 Configure LLM Model (Step 1/2)
        </Text>
      </Box>
      <Box marginBottom={1}>
        <Text dimColor>
          You'll name this configuration in the next step
        </Text>
      </Box>

      {/* Step 1: Provider Selection */}
      {step === 'provider' && (
        <Box flexDirection="column">
          <Box marginBottom={1}>
            <Text dimColor>Select your LLM provider:</Text>
          </Box>
          <SelectInput items={PROVIDER_OPTIONS} onSelect={handleProviderSelect} />
          {onCancel && (
            <Box marginTop={1}>
              <Text dimColor>Press ESC to cancel</Text>
            </Box>
          )}
        </Box>
      )}

      {/* Step 2: Model Input */}
      {step === 'model' && (
        <Box flexDirection="column">
          <Box marginBottom={1}>
            <Text>
              <Text color="green">✓</Text> Provider: <Text bold>{provider}</Text>
            </Text>
          </Box>
          <Box marginBottom={1}>
            <Text dimColor>
              Enter model name (default: {providerInfo.defaultModel}):
            </Text>
          </Box>
          <Box>
            <Text color="cyan">Model: </Text>
            <TextInput
              value={model}
              onChange={setModel}
              onSubmit={handleModelSubmit}
              placeholder={providerInfo.defaultModel}
            />
          </Box>
          <Box marginTop={1}>
            <Text dimColor>Press Ctrl+B to go back</Text>
          </Box>
        </Box>
      )}

      {/* Step 3: API Key Input */}
      {step === 'apiKey' && (
        <Box flexDirection="column">
          <Box marginBottom={1}>
            <Text>
              <Text color="green">✓</Text> Provider: <Text bold>{provider}</Text>
            </Text>
            <Text>
              <Text color="green">✓</Text> Model: <Text bold>{model}</Text>
            </Text>
          </Box>
          <Box marginBottom={1}>
            <Text dimColor>Enter your API key:</Text>
          </Box>
          <Box>
            <Text color="cyan">API Key: </Text>
            <TextInput
              value={apiKey}
              onChange={setApiKey}
              onSubmit={handleApiKeySubmit}
              placeholder="sk-..."
              mask="*"
            />
          </Box>
          <Box marginTop={1}>
            <Text dimColor>Press Ctrl+B to go back</Text>
          </Box>
        </Box>
      )}

      {/* Step 4: Base URL (Optional) */}
      {step === 'baseUrl' && (
        <Box flexDirection="column">
          <Box marginBottom={1}>
            <Text>
              <Text color="green">✓</Text> Provider: <Text bold>{provider}</Text>
            </Text>
            <Text>
              <Text color="green">✓</Text> Model: <Text bold>{model}</Text>
            </Text>
            {providerInfo.needsApiKey && (
              <Text>
                <Text color="green">✓</Text> API Key: <Text bold>{'*'.repeat(8)}...</Text>
              </Text>
            )}
          </Box>
          <Box marginBottom={1}>
            <Text dimColor>
              {provider === 'ollama' 
                ? 'Enter Ollama server URL:' 
                : 'Enter custom base URL (optional):'}
            </Text>
          </Box>
          <Box>
            <Text color="cyan">Base URL: </Text>
            <TextInput
              value={baseUrl}
              onChange={setBaseUrl}
              onSubmit={handleBaseUrlSubmit}
              placeholder={providerInfo.baseUrl || 'https://api.example.com'}
            />
          </Box>
          <Box marginTop={1}>
            <Text dimColor>Leave empty to use default • Press Ctrl+B to go back</Text>
          </Box>
        </Box>
      )}

      {/* Step 5: Confirmation */}
      {step === 'confirm' && (
        <Box flexDirection="column">
          <Box marginBottom={1}>
            <Text bold color="green">
              Configuration Summary:
            </Text>
          </Box>
          <Box flexDirection="column" marginBottom={1} paddingLeft={2}>
            <Text>
              • Provider: <Text bold color="cyan">{provider}</Text>
            </Text>
            <Text>
              • Model: <Text bold color="cyan">{model}</Text>
            </Text>
            {providerInfo.needsApiKey && (
              <Text>
                • API Key: <Text bold color="cyan">{apiKey.substring(0, 8)}...{apiKey.substring(apiKey.length - 4)}</Text>
              </Text>
            )}
            {baseUrl && (
              <Text>
                • Base URL: <Text bold color="cyan">{baseUrl}</Text>
              </Text>
            )}
          </Box>
          <Box marginBottom={1}>
            <Text dimColor>Press Enter to save, or Ctrl+B to go back</Text>
          </Box>
          <SelectInput 
            items={[
              { label: '✓ Save Configuration', value: 'save' },
              { label: '← Go Back', value: 'back' }
            ]}
            onSelect={(item) => {
              if (item.value === 'save') {
                handleConfirm();
              } else {
                handleBack();
              }
            }}
          />
        </Box>
      )}
    </Box>
  );
};
