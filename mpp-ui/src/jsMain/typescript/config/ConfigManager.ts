/**
 * ConfigManager - Manages CLI configuration
 * 
 * Handles loading, saving, and validating configuration for the AutoDev CLI.
 * Supports multiple named configurations with active selection.
 */

import * as fs from 'fs/promises';
import * as path from 'path';
import * as os from 'os';
import YAML from 'yaml';

export type LLMProvider = 'openai' | 'anthropic' | 'google' | 'deepseek' | 'ollama' | 'openrouter';

/**
 * Single LLM configuration
 */
export interface LLMConfig {
  name: string;
  provider: LLMProvider;
  apiKey: string;
  model: string;
  baseUrl?: string;
  temperature?: number;
  maxTokens?: number;
}

/**
 * Root configuration file structure
 */
export interface ConfigFile {
  active: string;  // Name of the active configuration
  configs: LLMConfig[];
}

/**
 * Legacy config format (for backward compatibility)
 */
export interface LegacyConfig {
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
   * Override config directory (for testing)
   */
  static setConfigDir(dir: string): void {
    this.CONFIG_DIR = dir;
    this.CONFIG_FILE = path.join(dir, 'config.yaml');
  }

  /**
   * Reset to default config directory
   */
  static resetConfigDir(): void {
    this.CONFIG_DIR = path.join(os.homedir(), '.autodev');
    this.CONFIG_FILE = path.join(this.CONFIG_DIR, 'config.yaml');
  }

  /**
   * Load configuration from file
   */
  static async load(): Promise<AutoDevConfigWrapper> {
    try {
      await fs.mkdir(this.CONFIG_DIR, { recursive: true });
      
      const content = await fs.readFile(this.CONFIG_FILE, 'utf-8');
      const parsed = YAML.parse(content);
      
      // Check if it's the new format (has 'configs' array)
      if (parsed && Array.isArray(parsed.configs)) {
        const configFile = parsed as ConfigFile;
        return new AutoDevConfigWrapper(configFile);
      }
      
      // Legacy format - convert to new format
      if (parsed && parsed.provider) {
        const legacy = parsed as LegacyConfig;
        const migrated: ConfigFile = {
          active: 'default',
          configs: [{
            name: 'default',
            provider: legacy.provider,
            apiKey: legacy.apiKey,
            model: legacy.model,
            baseUrl: legacy.baseUrl,
            temperature: legacy.temperature,
            maxTokens: legacy.maxTokens,
          }]
        };
        return new AutoDevConfigWrapper(migrated);
      }
      
      // Empty config
      return this.createEmpty();
    } catch (error) {
      // Return empty config if file doesn't exist
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
        return this.createEmpty();
      }
      throw error;
    }
  }

  /**
   * Save configuration to file
   */
  static async save(configFile: ConfigFile): Promise<void> {
    await fs.mkdir(this.CONFIG_DIR, { recursive: true });
    
    const content = YAML.stringify(configFile);
    await fs.writeFile(this.CONFIG_FILE, content, 'utf-8');
  }

  /**
   * Add or update a single configuration
   */
  static async saveConfig(config: LLMConfig, setActive: boolean = true): Promise<void> {
    const wrapper = await this.load();
    const configFile = wrapper.getConfigFile();
    
    // Check if config with this name exists
    const existingIndex = configFile.configs.findIndex(c => c.name === config.name);
    
    if (existingIndex >= 0) {
      // Update existing
      configFile.configs[existingIndex] = config;
    } else {
      // Add new
      configFile.configs.push(config);
    }
    
    // Set as active if requested
    if (setActive) {
      configFile.active = config.name;
    }
    
    await this.save(configFile);
  }

  /**
   * Delete a configuration by name
   */
  static async deleteConfig(name: string): Promise<void> {
    const wrapper = await this.load();
    const configFile = wrapper.getConfigFile();
    
    configFile.configs = configFile.configs.filter(c => c.name !== name);
    
    // If we deleted the active config, switch to first available
    if (configFile.active === name && configFile.configs.length > 0) {
      configFile.active = configFile.configs[0].name;
    }
    
    await this.save(configFile);
  }

  /**
   * Switch active configuration
   */
  static async setActive(name: string): Promise<void> {
    const wrapper = await this.load();
    const configFile = wrapper.getConfigFile();
    
    // Verify the config exists
    if (!configFile.configs.find(c => c.name === name)) {
      throw new Error(`Configuration '${name}' not found`);
    }
    
    configFile.active = name;
    await this.save(configFile);
  }

  /**
   * Get configuration file path
   */
  static getConfigPath(): string {
    return this.CONFIG_FILE;
  }

  /**
   * Create empty configuration
   */
  private static createEmpty(): AutoDevConfigWrapper {
    return new AutoDevConfigWrapper({
      active: '',
      configs: []
    });
  }
}

/**
 * Wrapper class for configuration with validation
 */
export class AutoDevConfigWrapper {
  constructor(private configFile: ConfigFile) {}

  /**
   * Get the entire config file structure
   */
  getConfigFile(): ConfigFile {
    return this.configFile;
  }

  /**
   * Get the active configuration
   */
  getActiveConfig(): LLMConfig | null {
    if (!this.configFile.active || this.configFile.configs.length === 0) {
      return null;
    }
    
    const config = this.configFile.configs.find(c => c.name === this.configFile.active);
    return config || this.configFile.configs[0];
  }

  /**
   * Get all configurations
   */
  getAllConfigs(): LLMConfig[] {
    return this.configFile.configs;
  }

  /**
   * Get active config name
   */
  getActiveName(): string {
    return this.configFile.active;
  }

  /**
   * Check if any valid configuration exists
   */
  isValid(): boolean {
    const active = this.getActiveConfig();
    if (!active) return false;
    
    // Ollama doesn't require API key
    if (active.provider === 'ollama') {
      return !!active.model;
    }
    
    return !!active.provider && !!active.apiKey && !!active.model;
  }

  // Convenience methods for active config
  getProvider(): LLMProvider {
    return this.getActiveConfig()?.provider || 'openai';
  }

  getApiKey(): string {
    return this.getActiveConfig()?.apiKey || '';
  }

  getModel(): string {
    return this.getActiveConfig()?.model || '';
  }

  getBaseUrl(): string | undefined {
    return this.getActiveConfig()?.baseUrl;
  }

  getTemperature(): number {
    return this.getActiveConfig()?.temperature ?? 0.7;
  }

  getMaxTokens(): number {
    return this.getActiveConfig()?.maxTokens ?? 4096;
  }

  /**
   * Convert active config to legacy format (for backward compatibility)
   */
  toJSON(): LegacyConfig {
    const active = this.getActiveConfig();
    if (!active) {
      return {
        provider: 'openai',
        apiKey: '',
        model: 'gpt-4',
      };
    }
    
    return {
      provider: active.provider,
      apiKey: active.apiKey,
      model: active.model,
      baseUrl: active.baseUrl,
      temperature: active.temperature,
      maxTokens: active.maxTokens,
    };
  }
}

