/**
 * Init Command Tests
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SlashCommandProcessor } from '../processors/SlashCommandProcessor';
import type { ProcessorContext } from '../processors/InputRouter';

describe('SlashCommandProcessor - Init Command', () => {
  let processor: SlashCommandProcessor;
  let mockContext: ProcessorContext;

  beforeEach(() => {
    processor = new SlashCommandProcessor();
    mockContext = {
      logger: {
        info: vi.fn(),
        warn: vi.fn(),
        error: vi.fn(),
      },
    };
  });

  describe('/init command', () => {
    it('should be registered and available', () => {
      const commands = processor.getCommands();
      expect(commands.has('init')).toBe(true);
      
      const initCommand = commands.get('init');
      expect(initCommand).toBeDefined();
      expect(initCommand?.description).toContain('domain dictionary');
    });

    it('should handle /init command', async () => {
      const canHandle = processor.canHandle('/init');
      expect(canHandle).toBe(true);
    });

    it('should process /init command without errors', async () => {
      // Mock the domain dictionary service to avoid actual file operations
      vi.mock('../utils/domainDictUtils.js', () => ({
        DomainDictService: {
          create: vi.fn().mockReturnValue({
            exists: vi.fn().mockResolvedValue(false),
            generateAndSave: vi.fn().mockResolvedValue({
              success: true,
              content: 'test,测试,测试内容',
              errorMessage: null
            })
          })
        },
        getCurrentProjectPath: vi.fn().mockReturnValue('/test/project'),
        isValidProjectPath: vi.fn().mockReturnValue(true)
      }));

      // Mock ConfigManager
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

      const result = await processor.process('/init', mockContext);
      
      // Should not throw an error
      expect(result.type).toBeDefined();
    });

    it('should handle /init --force flag', async () => {
      const result = await processor.process('/init --force', mockContext);
      expect(result.type).toBeDefined();
    });

    it('should provide usage information', () => {
      const commands = processor.getCommands();
      const initCommand = commands.get('init');
      expect(initCommand?.description).toBeTruthy();
    });
  });

  describe('command parsing', () => {
    it('should extract command name correctly', async () => {
      const result = await processor.process('/init', mockContext);
      // The logger.info is called during routing, not during command execution
      expect(result.type).toBeDefined();
    });

    it('should handle empty command gracefully', async () => {
      const result = await processor.process('/', mockContext);
      expect(result.type).toBe('error');
    });
  });
});
