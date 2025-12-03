/**
 * ConfigManager - Loads configuration from ~/.autodev/config.yaml
 * 
 * Mirrors mpp-ui's ConfigManager.ts for consistency across platforms.
 * Uses Node.js fs for file operations in VSCode extension context.
 */

import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';
import * as yaml from 'yaml';

export type LLMProvider = 'openai' | 'anthropic' | 'google' | 'deepseek' | 'ollama' | 'openrouter' | 'glm' | 'qwen' | 'kimi' | 'custom-openai-base';

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
 * MCP Server configuration
 */
export interface McpServerConfig {
  command?: string;
  url?: string;
  args?: string[];
  disabled?: boolean;
  autoApprove?: string[];
  env?: Record<string, string>;
  timeout?: number;
  trust?: boolean;
  headers?: Record<string, string>;
  cwd?: string;
}

/**
 * Root configuration file structure
 */
export interface ConfigFile {
  active: string;
  configs: LLMConfig[];
  mcpServers?: Record<string, McpServerConfig>;
}

/**
 * Configuration wrapper with validation
 */
export class AutoDevConfigWrapper {
  constructor(private configFile: ConfigFile) {}

  getConfigFile(): ConfigFile {
    return this.configFile;
  }

  getActiveConfig(): LLMConfig | null {
    if (!this.configFile.active || this.configFile.configs.length === 0) {
      return null;
    }
    const config = this.configFile.configs.find(c => c.name === this.configFile.active);
    return config || this.configFile.configs[0];
  }

  getAllConfigs(): LLMConfig[] {
    return this.configFile.configs;
  }

  getActiveName(): string {
    return this.configFile.active;
  }

  getMcpServers(): Record<string, McpServerConfig> {
    return this.configFile.mcpServers || {};
  }

  getEnabledMcpServers(): Record<string, McpServerConfig> {
    const mcpServers = this.getMcpServers();
    return Object.fromEntries(
      Object.entries(mcpServers).filter(([_, server]) => !server.disabled)
    );
  }

  isValid(): boolean {
    const active = this.getActiveConfig();
    if (!active) return false;

    // Ollama doesn't require API key
    if (active.provider === 'ollama') {
      return !!active.model;
    }

    // Custom OpenAI-compatible providers require baseUrl, apiKey, and model
    if (active.provider === 'custom-openai-base') {
      return !!active.baseUrl && !!active.apiKey && !!active.model;
    }

    return !!active.provider && !!active.apiKey && !!active.model;
  }
}

/**
 * ConfigManager - Manages loading configuration from ~/.autodev/config.yaml
 */
export class ConfigManager {
  private static CONFIG_DIR = path.join(os.homedir(), '.autodev');
  private static CONFIG_FILE = path.join(ConfigManager.CONFIG_DIR, 'config.yaml');

  /**
   * Load configuration from file
   */
  static async load(): Promise<AutoDevConfigWrapper> {
    try {
      // Ensure directory exists
      if (!fs.existsSync(this.CONFIG_DIR)) {
        fs.mkdirSync(this.CONFIG_DIR, { recursive: true });
      }

      if (!fs.existsSync(this.CONFIG_FILE)) {
        return this.createEmpty();
      }

      const content = fs.readFileSync(this.CONFIG_FILE, 'utf-8');
      const parsed = yaml.parse(content);

      // Check if it's the new format (has 'configs' array)
      if (parsed && Array.isArray(parsed.configs)) {
        return new AutoDevConfigWrapper(parsed as ConfigFile);
      }

      // Legacy format - convert to new format
      if (parsed && parsed.provider) {
        const migrated: ConfigFile = {
          active: 'default',
          configs: [{
            name: 'default',
            provider: parsed.provider,
            apiKey: parsed.apiKey,
            model: parsed.model,
            baseUrl: parsed.baseUrl,
            temperature: parsed.temperature,
            maxTokens: parsed.maxTokens,
          }]
        };
        return new AutoDevConfigWrapper(migrated);
      }

      return this.createEmpty();
    } catch (error) {
      return this.createEmpty();
    }
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

