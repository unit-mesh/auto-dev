/**
 * ConfigManager Tests
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ConfigManager, AutoDevConfigWrapper, ConfigFile, LLMConfig } from '../config/ConfigManager.js';
import * as fs from 'fs/promises';
import * as path from 'path';
import * as os from 'os';

describe('ConfigManager', () => {
  const testConfigDir = path.join(os.tmpdir(), '.autodev-test');
  const testConfigFile = path.join(testConfigDir, 'config.yaml');

  beforeEach(async () => {
    // Set test directory
    ConfigManager.setConfigDir(testConfigDir);

    // Clean up test directory
    try {
      await fs.rm(testConfigDir, { recursive: true, force: true });
    } catch (error) {
      // Ignore if doesn't exist
    }
  });

  afterEach(async () => {
    // Clean up after tests
    try {
      await fs.rm(testConfigDir, { recursive: true, force: true });
    } catch (error) {
      // Ignore errors
    }

    // Reset to default
    ConfigManager.resetConfigDir();
  });

  it('should create empty config when file does not exist', async () => {
    const config = await ConfigManager.load();
    expect(config.isValid()).toBe(false);
    expect(config.getAllConfigs().length).toBe(0);
  });

  it('should save and load configuration', async () => {
    const testConfig: LLMConfig = {
      name: 'test',
      provider: 'openai',
      apiKey: 'test-key',
      model: 'gpt-4',
    };

    await ConfigManager.saveConfig(testConfig, true);
    const loaded = await ConfigManager.load();

    expect(loaded.getProvider()).toBe('openai');
    expect(loaded.getApiKey()).toBe('test-key');
    expect(loaded.getModel()).toBe('gpt-4');
    expect(loaded.getActiveName()).toBe('test');
  });

  it('should validate config correctly', () => {
    const validConfig = new AutoDevConfigWrapper({
      active: 'default',
      configs: [{
        name: 'default',
        provider: 'openai',
        apiKey: 'sk-test',
        model: 'gpt-4',
      }]
    });

    expect(validConfig.isValid()).toBe(true);

    const invalidConfig = new AutoDevConfigWrapper({
      active: 'default',
      configs: [{
        name: 'default',
        provider: 'openai',
        apiKey: '',
        model: 'gpt-4',
      }]
    });

    expect(invalidConfig.isValid()).toBe(false);
  });

  it('should allow empty API key for Ollama', () => {
    const ollamaConfig = new AutoDevConfigWrapper({
      active: 'ollama',
      configs: [{
        name: 'ollama',
        provider: 'ollama',
        apiKey: '',
        model: 'llama3.2',
      }]
    });

    expect(ollamaConfig.isValid()).toBe(true);
  });

  it('should use default temperature and maxTokens', () => {
    const config = new AutoDevConfigWrapper({
      active: 'default',
      configs: [{
        name: 'default',
        provider: 'openai',
        apiKey: 'test',
        model: 'gpt-4',
      }]
    });

    expect(config.getTemperature()).toBe(0.7);
    expect(config.getMaxTokens()).toBe(8192);
  });

  it('should handle multiple configs and switch active', async () => {
    const config1: LLMConfig = {
      name: 'gpt4',
      provider: 'openai',
      apiKey: 'key1',
      model: 'gpt-4',
    };

    const config2: LLMConfig = {
      name: 'claude',
      provider: 'anthropic',
      apiKey: 'key2',
      model: 'claude-3-5-sonnet-20241022',
    };

    await ConfigManager.saveConfig(config1, true);
    await ConfigManager.saveConfig(config2, false);

    let loaded = await ConfigManager.load();
    expect(loaded.getAllConfigs().length).toBe(2);
    expect(loaded.getActiveName()).toBe('gpt4');

    await ConfigManager.setActive('claude');
    loaded = await ConfigManager.load();
    expect(loaded.getActiveName()).toBe('claude');
    expect(loaded.getProvider()).toBe('anthropic');
  });

  it('should delete config correctly', async () => {
    const config1: LLMConfig = {
      name: 'config1',
      provider: 'openai',
      apiKey: 'key1',
      model: 'gpt-4',
    };

    const config2: LLMConfig = {
      name: 'config2',
      provider: 'deepseek',
      apiKey: 'key2',
      model: 'deepseek-chat',
    };

    await ConfigManager.saveConfig(config1, true);
    await ConfigManager.saveConfig(config2, false);

    await ConfigManager.deleteConfig('config1');

    const loaded = await ConfigManager.load();
    expect(loaded.getAllConfigs().length).toBe(1);
    expect(loaded.getActiveName()).toBe('config2');
  });
});

