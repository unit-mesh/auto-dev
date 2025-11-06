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
import { t } from '../i18n/index.js';

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

// Provider 选项 - will be generated dynamically with translations

// 每个 provider 的默认模型和示例
const PROVIDER_DEFAULTS: Record<string, { defaultModel: string; needsApiKey: boolean; baseUrl?: string; requiresBaseUrl?: boolean }> = {
  openai: { defaultModel: 'gpt-4', needsApiKey: true },
  anthropic: { defaultModel: 'claude-3-5-sonnet-20241022', needsApiKey: true },
  google: { defaultModel: 'gemini-pro', needsApiKey: true },
  deepseek: { defaultModel: 'deepseek-chat', needsApiKey: true },
  ollama: { defaultModel: 'llama2', needsApiKey: false, baseUrl: 'http://localhost:11434', requiresBaseUrl: true },
  openrouter: { defaultModel: 'openai/gpt-4', needsApiKey: true },
  glm: { defaultModel: 'glm-4-plus', needsApiKey: true, baseUrl: 'https://open.bigmodel.cn/api/paas/v4', requiresBaseUrl: true },
  qwen: { defaultModel: 'qwen-max', needsApiKey: true, baseUrl: 'https://dashscope.aliyuncs.com/api/v1', requiresBaseUrl: true },
  kimi: { defaultModel: 'moonshot-v1-32k', needsApiKey: true, baseUrl: 'https://api.moonshot.cn/v1', requiresBaseUrl: true },
  'custom-openai-base': { defaultModel: 'model-name', needsApiKey: true, baseUrl: 'https://api.example.com/v1', requiresBaseUrl: true },
};

export const ModelConfigForm: React.FC<ModelConfigFormProps> = ({ onSubmit, onCancel }) => {
  const [step, setStep] = useState<FormStep>('provider');
  const [provider, setProvider] = useState<LLMProvider>('openai');
  const [model, setModel] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [baseUrl, setBaseUrl] = useState('');

  const providerInfo = PROVIDER_DEFAULTS[provider];
  
  // Generate provider options dynamically with translations
  const PROVIDER_OPTIONS = [
    { label: t('modelConfig.providers.openai'), value: 'openai' },
    { label: t('modelConfig.providers.anthropic'), value: 'anthropic' },
    { label: t('modelConfig.providers.google'), value: 'google' },
    { label: t('modelConfig.providers.deepseek'), value: 'deepseek' },
    { label: t('modelConfig.providers.ollama'), value: 'ollama' },
    { label: t('modelConfig.providers.openrouter'), value: 'openrouter' },
    { label: t('modelConfig.providers.glm'), value: 'glm' },
    { label: t('modelConfig.providers.qwen'), value: 'qwen' },
    { label: t('modelConfig.providers.kimi'), value: 'kimi' },
    { label: t('modelConfig.providers.customOpenAIBase'), value: 'custom-openai-base' },
  ];

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
    
    // Ask for baseUrl if provider requires one
    if (providerInfo.requiresBaseUrl) {
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
          {t('modelConfig.title')} ({t('modelConfig.stepInfo')})
        </Text>
      </Box>
      <Box marginBottom={1}>
        <Text dimColor>
          {t('modelConfig.nextStepInfo')}
        </Text>
      </Box>

      {/* Step 1: Provider Selection */}
      {step === 'provider' && (
        <Box flexDirection="column">
          <Box marginBottom={1}>
            <Text dimColor>{t('modelConfig.selectProvider')}</Text>
          </Box>
          <SelectInput items={PROVIDER_OPTIONS} onSelect={handleProviderSelect} />
          {onCancel && (
            <Box marginTop={1}>
              <Text dimColor>{t('welcome.exitHint')}</Text>
            </Box>
          )}
        </Box>
      )}

      {/* Step 2: Model Input */}
      {step === 'model' && (
        <Box flexDirection="column">
          <Box marginBottom={1}>
            <Text>
              <Text color="green">✓</Text> {t('modelConfig.fields.provider')}: <Text bold>{provider}</Text>
            </Text>
          </Box>
          <Box marginBottom={1}>
            <Text dimColor>
              {t('modelConfig.enterModel')} ({t('modelConfig.defaultHint', { default: providerInfo.defaultModel })}):
            </Text>
          </Box>
          <Box>
            <Text color="cyan">{t('modelConfig.fields.model')}: </Text>
            <TextInput
              value={model}
              onChange={setModel}
              onSubmit={handleModelSubmit}
              placeholder={providerInfo.defaultModel}
            />
          </Box>
          <Box marginTop={1}>
            <Text dimColor>{t('modelConfig.backHint')}</Text>
          </Box>
        </Box>
      )}

      {/* Step 3: API Key Input */}
      {step === 'apiKey' && (
        <Box flexDirection="column">
          <Box marginBottom={1}>
            <Text>
              <Text color="green">✓</Text> {t('modelConfig.fields.provider')}: <Text bold>{provider}</Text>
            </Text>
            <Text>
              <Text color="green">✓</Text> {t('modelConfig.fields.model')}: <Text bold>{model}</Text>
            </Text>
          </Box>
          <Box marginBottom={1}>
            <Text dimColor>{t('modelConfig.enterApiKey')}</Text>
          </Box>
          <Box>
            <Text color="cyan">{t('modelConfig.fields.apiKey')}: </Text>
            <TextInput
              value={apiKey}
              onChange={setApiKey}
              onSubmit={handleApiKeySubmit}
              placeholder="sk-..."
              mask="*"
            />
          </Box>
          <Box marginTop={1}>
            <Text dimColor>{t('modelConfig.backHint')}</Text>
          </Box>
        </Box>
      )}

      {/* Step 4: Base URL (Optional) */}
      {step === 'baseUrl' && (
        <Box flexDirection="column">
          <Box marginBottom={1}>
            <Text>
              <Text color="green">✓</Text> {t('modelConfig.fields.provider')}: <Text bold>{provider}</Text>
            </Text>
            <Text>
              <Text color="green">✓</Text> {t('modelConfig.fields.model')}: <Text bold>{model}</Text>
            </Text>
            {providerInfo.needsApiKey && (
              <Text>
                <Text color="green">✓</Text> {t('modelConfig.fields.apiKey')}: <Text bold>{'*'.repeat(8)}...</Text>
              </Text>
            )}
          </Box>
          <Box marginBottom={1}>
            <Text dimColor>
              {provider === 'ollama' 
                ? t('modelConfig.ollamaUrl') 
                : t('modelConfig.customBaseUrl')}
            </Text>
          </Box>
          <Box>
            <Text color="cyan">{t('modelConfig.fields.baseUrl')}: </Text>
            <TextInput
              value={baseUrl}
              onChange={setBaseUrl}
              onSubmit={handleBaseUrlSubmit}
              placeholder={providerInfo.baseUrl || 'https://api.example.com'}
            />
          </Box>
          <Box marginTop={1}>
            <Text dimColor>{t('modelConfig.leaveEmpty')} • {t('modelConfig.backHint')}</Text>
          </Box>
        </Box>
      )}

      {/* Step 5: Confirmation */}
      {step === 'confirm' && (
        <Box flexDirection="column">
          <Box marginBottom={1}>
            <Text bold color="green">
              {t('modelConfig.summary')}
            </Text>
          </Box>
          <Box flexDirection="column" marginBottom={1} paddingLeft={2}>
            <Text>
              • {t('modelConfig.fields.provider')}: <Text bold color="cyan">{provider}</Text>
            </Text>
            <Text>
              • {t('modelConfig.fields.model')}: <Text bold color="cyan">{model}</Text>
            </Text>
            {providerInfo.needsApiKey && (
              <Text>
                • {t('modelConfig.fields.apiKey')}: <Text bold color="cyan">{apiKey.substring(0, 8)}...{apiKey.substring(apiKey.length - 4)}</Text>
              </Text>
            )}
            {baseUrl && (
              <Text>
                • {t('modelConfig.fields.baseUrl')}: <Text bold color="cyan">{baseUrl}</Text>
              </Text>
            )}
          </Box>
          <Box marginBottom={1}>
            <Text dimColor>{t('modelConfig.nameHint')}, {t('modelConfig.backHint')}</Text>
          </Box>
          <SelectInput 
            items={[
              { label: `✓ ${t('common.save')}`, value: 'save' },
              { label: `← ${t('common.back')}`, value: 'back' }
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
