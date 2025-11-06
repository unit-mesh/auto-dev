/**
 * Test for SlashCommandProcessor /init command
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { SlashCommandProcessor } from '../../../jsMain/typescript/processors/SlashCommandProcessor.js';
import type { ProcessorContext } from '../../../jsMain/typescript/processors/InputRouter.js';

// Mock the domain dictionary utilities
vi.mock('../../../jsMain/typescript/utils/domainDictUtils.js', () => ({
  DomainDictService: vi.fn().mockImplementation(() => ({
    exists: vi.fn().mockResolvedValue(false),
    generateAndSave: vi.fn().mockResolvedValue({
      success: true,
      content: '中文,代码翻译,描述\n用户,User,用户实体\n博客,Blog,博客实体',
      errorMessage: null
    })
  })),
  getCurrentProjectPath: vi.fn().mockReturnValue('/mock/project'),
  isValidProjectPath: vi.fn().mockReturnValue(true)
}));

// Mock the config manager
vi.mock('../../../jsMain/typescript/config/ConfigManager.js', () => ({
  ConfigManager: vi.fn().mockImplementation(() => ({
    loadConfig: vi.fn().mockResolvedValue({
      provider: 'deepseek',
      model: 'deepseek-chat',
      apiKey: 'test-key',
      temperature: 0.7,
      maxTokens: 8192
    })
  }))
}));

describe('SlashCommandProcessor /init command', () => {
  let processor: SlashCommandProcessor;
  let mockContext: ProcessorContext;

  beforeEach(() => {
    processor = new SlashCommandProcessor();
    mockContext = {
      logger: {
        info: vi.fn(),
        error: vi.fn(),
        warn: vi.fn(),
        debug: vi.fn()
      }
    } as any;
  });

  it('should handle /init command successfully', async () => {
    const result = await processor.process('/init', mockContext);

    expect(result.type).toBe('handled');
    expect(result.output).toContain('域字典生成成功');
  });

  it('should handle /init --force command', async () => {
    const result = await processor.process('/init --force', mockContext);

    expect(result.type).toBe('handled');
    expect(result.output).toContain('域字典生成成功');
  });

  it('should show warning when domain dictionary already exists', async () => {
    // Mock exists to return true
    const { DomainDictService } = await import('../../../jsMain/typescript/utils/domainDictUtils.js');
    const mockService = new DomainDictService('', {} as any);
    vi.mocked(mockService.exists).mockResolvedValue(true);

    const result = await processor.process('/init', mockContext);

    expect(result.type).toBe('handled');
    expect(result.output).toContain('Domain dictionary already exists');
  });

  it('should handle invalid project path', async () => {
    const { isValidProjectPath } = await import('../../../jsMain/typescript/utils/domainDictUtils.js');
    vi.mocked(isValidProjectPath).mockReturnValue(false);

    const result = await processor.process('/init', mockContext);

    expect(result.type).toBe('error');
    expect(result.message).toContain("doesn't appear to be a valid project");
  });

  it('should handle missing configuration', async () => {
    const { ConfigManager } = await import('../../../jsMain/typescript/config/ConfigManager.js');
    const mockConfigManager = new ConfigManager();
    vi.mocked(mockConfigManager.loadConfig).mockResolvedValue(null);

    const result = await processor.process('/init', mockContext);

    expect(result.type).toBe('error');
    expect(result.message).toContain('No LLM configuration found');
  });

  it('should handle generation failure', async () => {
    const { DomainDictService } = await import('../../../jsMain/typescript/utils/domainDictUtils.js');
    const mockService = new DomainDictService('', {} as any);
    vi.mocked(mockService.generateAndSave).mockResolvedValue({
      success: false,
      content: '',
      errorMessage: 'Generation failed'
    });

    const result = await processor.process('/init', mockContext);

    expect(result.type).toBe('error');
    expect(result.message).toContain('Generation failed');
  });

  it('should handle unexpected errors', async () => {
    const { DomainDictService } = await import('../../../jsMain/typescript/utils/domainDictUtils.js');
    const mockService = new DomainDictService('', {} as any);
    vi.mocked(mockService.generateAndSave).mockRejectedValue(new Error('Unexpected error'));

    const result = await processor.process('/init', mockContext);

    expect(result.type).toBe('error');
    expect(result.message).toContain('Unexpected error');
  });

  it('should show help for /init command', async () => {
    const result = await processor.process('/help', mockContext);

    expect(result.type).toBe('handled');
    expect(result.output).toContain('init');
    expect(result.output).toContain('初始化项目域字典');
  });
});
