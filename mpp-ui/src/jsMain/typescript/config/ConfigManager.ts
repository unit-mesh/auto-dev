/**
 * ConfigManager - Manages CLI configuration
 * 
 * Handles loading, saving, and validating configuration for the AutoDev CLI.
 */

import * as fs from 'fs/promises';
import * as path from 'path';
import * as os from 'os';
import YAML from 'yaml';

export type LLMProvider = 'openai' | 'anthropic' | 'google' | 'deepseek' | 'ollama' | 'openrouter';

export interface AutoDevConfig {
  provider: LLMProvider;
  apiKey: string;
  model: string;
  baseUrl?: string;
  temperature?: number;
  maxTokens?: number;
}

export class ConfigManager {
  private static CONFIG_DIR = path.join(os.homedir(), '.autodev');
  private static CONFIG_FILE = path.join(ConfigManager.CONFIG_DIR, 'config.yaml');

  /**
   * Load configuration from file
   */
  static async load(): Promise<AutoDevConfigWrapper> {
    try {
      await fs.mkdir(this.CONFIG_DIR, { recursive: true });
      
      const content = await fs.readFile(this.CONFIG_FILE, 'utf-8');
      const config = YAML.parse(content) as AutoDevConfig;
      
      return new AutoDevConfigWrapper(config);
    } catch (error) {
      // Return empty config if file doesn't exist
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
        return new AutoDevConfigWrapper({
          provider: 'openai',
          apiKey: '',
          model: 'gpt-4',
        });
      }
      throw error;
    }
  }

  /**
   * Save configuration to file
   */
  static async save(config: AutoDevConfig): Promise<void> {
    await fs.mkdir(this.CONFIG_DIR, { recursive: true });
    
    const content = YAML.stringify(config);
    await fs.writeFile(this.CONFIG_FILE, content, 'utf-8');
  }

  /**
   * Get configuration file path
   */
  static getConfigPath(): string {
    return this.CONFIG_FILE;
  }
}

/**
 * Wrapper class for configuration with validation
 */
export class AutoDevConfigWrapper {
  constructor(private config: AutoDevConfig) {}

  isValid(): boolean {
    // Ollama doesn't require API key
    if (this.config.provider === 'ollama') {
      return !!this.config.model;
    }
    
    return !!this.config.provider && !!this.config.apiKey && !!this.config.model;
  }

  getProvider(): LLMProvider {
    return this.config.provider;
  }

  getApiKey(): string {
    return this.config.apiKey;
  }

  getModel(): string {
    return this.config.model;
  }

  getBaseUrl(): string | undefined {
    return this.config.baseUrl;
  }

  getTemperature(): number {
    return this.config.temperature ?? 0.7;
  }

  getMaxTokens(): number {
    return this.config.maxTokens ?? 4096;
  }

  toJSON(): AutoDevConfig {
    return { ...this.config };
  }
}

