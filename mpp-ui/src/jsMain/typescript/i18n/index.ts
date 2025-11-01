/**
 * i18n - Internationalization Support
 * 
 * Provides simple internationalization without external dependencies
 * Supports English and Chinese (Simplified)
 */

import { en } from './locales/en.js';
import { zh } from './locales/zh.js';
import type { SupportedLanguage, TranslationKeys } from './types.js';
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';

// Re-export types for external use
export type { SupportedLanguage, TranslationKeys } from './types.js';

const translations: Record<SupportedLanguage, TranslationKeys> = {
  en,
  zh,
};

let currentLanguage: SupportedLanguage = 'en';

/**
 * Initialize i18n with user's language preference
 */
export function initI18n(): SupportedLanguage {
  // Try to load language preference from config
  const configPath = path.join(os.homedir(), '.autodev', 'config.yaml');
  
  try {
    if (fs.existsSync(configPath)) {
      const content = fs.readFileSync(configPath, 'utf-8');
      const match = content.match(/language:\s*['"]?(en|zh)['"]?/);
      if (match) {
        currentLanguage = match[1] as SupportedLanguage;
        return currentLanguage;
      }
    }
  } catch (error) {
    // Ignore errors, use default
  }
  
  // Fall back to system locale
  const locale = process.env.LANG || process.env.LC_ALL || 'en';
  if (locale.startsWith('zh')) {
    currentLanguage = 'zh';
  } else {
    currentLanguage = 'en';
  }
  
  return currentLanguage;
}

/**
 * Set current language
 */
export function setLanguage(lang: SupportedLanguage): void {
  currentLanguage = lang;
}

/**
 * Get current language
 */
export function getLanguage(): SupportedLanguage {
  return currentLanguage;
}

/**
 * Get translation for a key path
 * 
 * @param keyPath - Dot-separated key path (e.g., 'common.save', 'welcome.title')
 * @param params - Optional parameters for string interpolation
 * @returns Translated string
 * 
 * @example
 * t('common.save') // => 'Save' or '保存'
 * t('modelConfig.defaultHint', { default: 'gpt-4' }) // => 'default: gpt-4'
 */
export function t(keyPath: string, params?: Record<string, any>): string {
  const keys = keyPath.split('.');
  const langData = translations[currentLanguage];
  
  // Navigate through the nested object
  let value: any = langData;
  for (const key of keys) {
    if (value && typeof value === 'object' && key in value) {
      value = value[key];
    } else {
      // Fallback to English if key not found
      value = translations.en;
      for (const k of keys) {
        if (value && typeof value === 'object' && k in value) {
          value = value[k];
        } else {
          return keyPath; // Return key path if not found
        }
      }
      break;
    }
  }
  
  if (typeof value !== 'string') {
    return keyPath;
  }
  
  // Simple string interpolation
  if (params) {
    return value.replace(/\{\{(\w+)\}\}/g, (match, key) => {
      return params[key]?.toString() || match;
    });
  }
  
  return value;
}

/**
 * Save language preference to config file
 */
export async function saveLanguagePreference(lang: SupportedLanguage): Promise<void> {
  const configDir = path.join(os.homedir(), '.autodev');
  const configPath = path.join(configDir, 'config.yaml');
  
  try {
    // Ensure directory exists
    if (!fs.existsSync(configDir)) {
      fs.mkdirSync(configDir, { recursive: true });
    }
    
    let content = '';
    
    // Read existing config if it exists
    if (fs.existsSync(configPath)) {
      content = fs.readFileSync(configPath, 'utf-8');
      
      // Update or add language setting
      if (content.includes('language:')) {
        content = content.replace(/language:\s*['"]?\w+['"]?/, `language: ${lang}`);
      } else {
        content = `language: ${lang}\n${content}`;
      }
    } else {
      content = `language: ${lang}\n`;
    }
    
    fs.writeFileSync(configPath, content, 'utf-8');
    setLanguage(lang);
  } catch (error) {
    console.error('Failed to save language preference:', error);
    throw error;
  }
}

// Initialize on import
initI18n();

