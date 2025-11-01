/**
 * Language Switcher Component for CLI
 */

import React, { useState } from 'react';
import { Box, Text } from 'ink';
import SelectInput from 'ink-select-input';
import { getLanguage, setLanguage, saveLanguagePreference, t, type SupportedLanguage } from '../i18n/index.js';

interface LanguageSwitcherProps {
  onLanguageChange?: (language: SupportedLanguage) => void;
}

export const LanguageSwitcher: React.FC<LanguageSwitcherProps> = ({ onLanguageChange }) => {
  const [currentLang, setCurrentLang] = useState<SupportedLanguage>(getLanguage());

  const languageOptions = [
    { label: 'English', value: 'en' as SupportedLanguage },
    { label: '中文 (Chinese)', value: 'zh' as SupportedLanguage },
  ];

  const handleSelect = async (item: { value: SupportedLanguage }) => {
    const newLang = item.value;
    setLanguage(newLang);
    setCurrentLang(newLang);
    
    try {
      await saveLanguagePreference(newLang);
      if (onLanguageChange) {
        onLanguageChange(newLang);
      }
    } catch (error) {
      console.error('Failed to save language preference:', error);
    }
  };

  return (
    <Box flexDirection="column">
      <Box marginBottom={1}>
        <Text bold color="cyan">
          🌐 Language / 语言
        </Text>
      </Box>
      <Box marginBottom={1}>
        <Text dimColor>Current: {currentLang === 'en' ? 'English' : '中文'}</Text>
      </Box>
      <SelectInput items={languageOptions} onSelect={handleSelect} />
    </Box>
  );
};

