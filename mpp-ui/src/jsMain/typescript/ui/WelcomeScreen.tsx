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
import { t } from '../i18n/index.js';
import { semanticInk } from '../design-system/theme-helpers.js';

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
        <Text color={semanticInk.success}>{t('messages.configSaved')}</Text>
        <Text dimColor>{t('messages.starting')}</Text>
      </Box>
    );
  }

  if (saving) {
    return (
      <Box flexDirection="column" padding={2}>
        <Text color={semanticInk.warning}>{t('messages.configSaving')}</Text>
      </Box>
    );
  }

  if (showNameInput && pendingConfig) {
    return (
      <Box flexDirection="column" padding={2}>
        <Box marginBottom={1}>
          <Text bold color={semanticInk.accent}>
            {t('modelConfig.nameConfig')}
          </Text>
        </Box>

        <Box marginBottom={1}>
          <Text dimColor>
            {t('modelConfig.namePrompt')}
          </Text>
        </Box>

        <Box marginBottom={1}>
          <Text>
            <Text color={semanticInk.success}>✓</Text> {t('modelConfig.fields.provider')}: <Text bold>{pendingConfig.provider}</Text>
          </Text>
          <Text>
            <Text color={semanticInk.success}>✓</Text> {t('modelConfig.fields.model')}: <Text bold>{pendingConfig.model}</Text>
          </Text>
        </Box>

        <Box>
          <Text color={semanticInk.accent}>{t('modelConfig.fields.provider')}: </Text>
          <TextInput
            value={configName}
            onChange={setConfigName}
            onSubmit={handleNameSubmit}
            placeholder="default"
          />
        </Box>

        <Box marginTop={1}>
          <Text dimColor>{t('modelConfig.nameHint')}</Text>
        </Box>
      </Box>
    );
  }

  return (
    <Box flexDirection="column" padding={1}>
      {/* Header */}
      <Box marginBottom={1}>
        <Text bold color={semanticInk.accent}>
          {t('welcome.title')}
        </Text>
      </Box>

      <Box marginBottom={1}>
        <Text dimColor>
          {t('welcome.subtitle')}
        </Text>
      </Box>

      {/* Configuration Form */}
      <ModelConfigForm onSubmit={handleConfigSubmit} />

      {/* Footer */}
      <Box marginTop={1}>
        <Text dimColor>
          {t('welcome.exitHint')}
        </Text>
      </Box>
    </Box>
  );
};

