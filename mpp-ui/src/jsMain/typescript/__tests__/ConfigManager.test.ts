/**
 * ConfigManager Tests
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ConfigManager, AutoDevConfigWrapper } from '../config/ConfigManager.js';
import * as fs from 'fs/promises';
import * as path from 'path';
import * as os from 'os';

describe('ConfigManager', () => {
  const testConfigDir = path.join(os.tmpdir(), '.autodev-test');
  const testConfigFile = path.join(testConfigDir, 'config.yaml');

  beforeEach(async () => {
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
  });

  it('should create empty config when file does not exist', async () => {
    const config = await ConfigManager.load();
    // 默认使用 Ollama provider，不需要 API key，所以是有效的
    expect(config.isValid()).toBe(true);
  });

  it('should save and load configuration', async () => {
    const testConfig = {
      provider: 'openai' as const,
      apiKey: 'test-key',
      model: 'gpt-4',
    };

    await ConfigManager.save(testConfig);
    const loaded = await ConfigManager.load();

    expect(loaded.getProvider()).toBe('openai');
    expect(loaded.getApiKey()).toBe('test-key');
    expect(loaded.getModel()).toBe('gpt-4');
  });

  it('should validate config correctly', () => {
    const validConfig = new AutoDevConfigWrapper({
      provider: 'openai',
      apiKey: 'sk-test',
      model: 'gpt-4',
    });

    expect(validConfig.isValid()).toBe(true);

    const invalidConfig = new AutoDevConfigWrapper({
      provider: 'openai',
      apiKey: '',
      model: 'gpt-4',
    });

    expect(invalidConfig.isValid()).toBe(false);
  });

  it('should allow empty API key for Ollama', () => {
    const ollamaConfig = new AutoDevConfigWrapper({
      provider: 'ollama',
      apiKey: '',
      model: 'llama3.2',
    });

    expect(ollamaConfig.isValid()).toBe(true);
  });

  it('should use default temperature and maxTokens', () => {
    const config = new AutoDevConfigWrapper({
      provider: 'openai',
      apiKey: 'test',
      model: 'gpt-4',
    });

    expect(config.getTemperature()).toBe(0.7);
    expect(config.getMaxTokens()).toBe(4096);
  });
});

