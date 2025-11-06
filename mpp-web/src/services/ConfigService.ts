/**
 * Configuration Service for mpp-web
 * Stores LLM configuration in localStorage
 */

export interface ModelConfig {
  provider: string;
  model: string;
  apiKey: string;
  baseUrl?: string;
  temperature?: number;
  maxTokens?: number;
}

const CONFIG_KEY = 'autodev_web_config';

export class ConfigService {
  static save(config: ModelConfig): void {
    localStorage.setItem(CONFIG_KEY, JSON.stringify(config));
  }

  static load(): ModelConfig | null {
    const stored = localStorage.getItem(CONFIG_KEY);
    if (!stored) return null;
    
    try {
      return JSON.parse(stored);
    } catch (error) {
      console.error('Failed to parse config:', error);
      return null;
    }
  }

  static clear(): void {
    localStorage.removeItem(CONFIG_KEY);
  }

  static getDefaultConfig(): ModelConfig {
    return {
      provider: 'openai',
      model: 'gpt-4',
      apiKey: '',
      temperature: 0.7,
      maxTokens: 8192,
    };
  }
}

