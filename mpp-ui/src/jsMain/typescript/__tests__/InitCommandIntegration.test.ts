/**
 * Init Command Integration Tests
 * 测试 /init 命令在不同模式下的集成
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SlashCommandProcessor } from '../processors/SlashCommandProcessor.js';
import { InputRouter } from '../processors/InputRouter.js';
import type { ProcessorContext } from '../processors/InputRouter.js';

// Mock dependencies
vi.mock('../utils/domainDictUtils.js', () => ({
  DomainDictService: {
    create: vi.fn().mockReturnValue({
      exists: vi.fn().mockResolvedValue(false),
      generateAndSave: vi.fn().mockResolvedValue({
        success: true,
        content: 'test,测试,测试内容\nuser,用户,用户管理',
        errorMessage: null
      })
    })
  },
  getCurrentProjectPath: vi.fn().mockReturnValue('/test/project'),
  isValidProjectPath: vi.fn().mockReturnValue(true)
}));

vi.mock('../config/ConfigManager.js', () => ({
  ConfigManager: {
    load: vi.fn().mockResolvedValue({
      getActiveConfig: vi.fn().mockReturnValue({
        provider: 'deepseek',
        model: 'deepseek-chat',
        apiKey: 'test-key'
      })
    })
  }
}));

describe('Init Command Integration Tests', () => {
  let router: InputRouter;
  let slashProcessor: SlashCommandProcessor;
  let mockContext: ProcessorContext;

  beforeEach(() => {
    router = new InputRouter();
    slashProcessor = new SlashCommandProcessor();
    router.register(slashProcessor, 100);

    mockContext = {
      logger: {
        info: vi.fn(),
        warn: vi.fn(),
        error: vi.fn(),
      },
      clearMessages: vi.fn(),
      addMessage: vi.fn(),
      setLoading: vi.fn(),
    };
  });

  describe('InputRouter Integration', () => {
    it('should route /init command to SlashCommandProcessor', async () => {
      const result = await router.route('/init', mockContext);
      
      expect(result.type).toBe('handled');
      expect(result.output).toContain('Domain dictionary generated successfully');
    });

    it('should handle /init --force command', async () => {
      const result = await router.route('/init --force', mockContext);
      
      expect(result.type).toBe('handled');
      expect(result.output).toContain('Domain dictionary generated successfully');
    });

    it('should handle /help command', async () => {
      const result = await router.route('/help', mockContext);
      
      expect(result.type).toBe('handled');
      expect(result.output).toContain('AutoDev CLI Help');
    });

    it('should handle /clear command', async () => {
      const result = await router.route('/clear', mockContext);
      
      expect(result.type).toBe('handled');
      expect(result.output).toContain('Chat history cleared');
      expect(mockContext.clearMessages).toHaveBeenCalled();
    });
  });

  describe('SlashCommandProcessor Direct Tests', () => {
    it('should recognize /init command', () => {
      expect(slashProcessor.canHandle('/init')).toBe(true);
      expect(slashProcessor.canHandle('/init --force')).toBe(true);
      expect(slashProcessor.canHandle('init')).toBe(false);
      expect(slashProcessor.canHandle('regular text')).toBe(false);
    });

    it('should process /init command directly', async () => {
      const result = await slashProcessor.process('/init', mockContext);
      
      expect(result.type).toBe('handled');
      expect(result.output).toContain('Domain dictionary generated successfully');
    });

    it('should handle errors gracefully', async () => {
      // Mock an error in DomainDictService
      const { DomainDictService } = await import('../utils/domainDictUtils.js');
      vi.mocked(DomainDictService.create).mockReturnValueOnce({
        exists: vi.fn().mockResolvedValue(false),
        generateAndSave: vi.fn().mockResolvedValue({
          success: false,
          content: null,
          errorMessage: 'Generation failed'
        })
      });

      const result = await slashProcessor.process('/init', mockContext);

      expect(result.type).toBe('error');
      expect(result.message).toContain('Generation failed');
    });
  });

  describe('Command Priority', () => {
    it('should prioritize slash commands over other processors', async () => {
      // Add a lower priority processor
      const lowPriorityProcessor = {
        name: 'LowPriorityProcessor',
        canHandle: vi.fn().mockReturnValue(true),
        process: vi.fn().mockResolvedValue({ type: 'handled', output: 'low priority' })
      };
      
      router.register(lowPriorityProcessor, 50);

      const result = await router.route('/init', mockContext);
      
      // Should be handled by SlashCommandProcessor, not the low priority one
      expect(result.type).toBe('handled');
      expect(result.output).toContain('Domain dictionary generated successfully');
      expect(lowPriorityProcessor.process).not.toHaveBeenCalled();
    });
  });

  describe('Error Scenarios', () => {
    it('should handle invalid project path', async () => {
      const { isValidProjectPath } = await import('../utils/domainDictUtils.js');
      vi.mocked(isValidProjectPath).mockReturnValueOnce(false);

      const result = await slashProcessor.process('/init', mockContext);

      expect(result.type).toBe('error');
      expect(result.message).toContain('valid project');
    });

    it('should handle missing configuration', async () => {
      const { ConfigManager } = await import('../config/ConfigManager.js');
      vi.mocked(ConfigManager.load).mockResolvedValueOnce({
        getActiveConfig: vi.fn().mockReturnValue(null)
      });

      const result = await slashProcessor.process('/init', mockContext);

      expect(result.type).toBe('error');
      expect(result.message).toContain('No LLM configuration found');
    });
  });
});
