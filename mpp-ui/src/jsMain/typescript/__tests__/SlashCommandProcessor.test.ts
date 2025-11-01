/**
 * SlashCommandProcessor 单元测试
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { SlashCommandProcessor } from '../processors/SlashCommandProcessor';
import type { ProcessorContext } from '../processors/InputRouter';

describe('SlashCommandProcessor', () => {
  let processor: SlashCommandProcessor;
  let mockContext: ProcessorContext;
  let consoleSpy: any;

  beforeEach(() => {
    processor = new SlashCommandProcessor();
    mockContext = {
      clearMessages: vi.fn(),
      logger: {
        info: vi.fn(),
        warn: vi.fn(),
        error: vi.fn(),
      },
    };
    consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleSpy.mockRestore();
  });

  describe('canHandle', () => {
    it('应该处理以 / 开头的输入', () => {
      expect(processor.canHandle('/help')).toBe(true);
      expect(processor.canHandle('/clear')).toBe(true);
      expect(processor.canHandle('  /exit  ')).toBe(true);
    });

    it('不应该处理非 / 开头的输入', () => {
      expect(processor.canHandle('help')).toBe(false);
      expect(processor.canHandle('@agent')).toBe(false);
      expect(processor.canHandle('$variable')).toBe(false);
    });
  });

  describe('内置命令', () => {
    describe('/help', () => {
      it('应该显示帮助信息', async () => {
        const result = await processor.process('/help', mockContext);

        expect(result.type).toBe('handled');
        if (result.type === 'handled') {
          expect(result.output).toBeDefined();
          expect(result.output).toContain('AutoDev');
        }
        expect(consoleSpy).toHaveBeenCalled();
      });

      it('应该支持别名 /h', async () => {
        const result = await processor.process('/h', mockContext);
        expect(result.type).toBe('handled');
      });

      it('应该支持别名 /?', async () => {
        const result = await processor.process('/?', mockContext);
        expect(result.type).toBe('handled');
      });
    });

    describe('/clear', () => {
      it('应该清空消息历史', async () => {
        const result = await processor.process('/clear', mockContext);

        expect(result.type).toBe('handled');
        expect(mockContext.clearMessages).toHaveBeenCalled();
        if (result.type === 'handled') {
          expect(result.output).toContain('cleared');
        }
      });

      it('应该支持别名 /cls', async () => {
        const result = await processor.process('/cls', mockContext);
        expect(result.type).toBe('handled');
        expect(mockContext.clearMessages).toHaveBeenCalled();
      });
    });

    describe('/exit', () => {
      it('应该准备退出程序', async () => {
        // 需要 mock process.exit
        const exitSpy = vi.spyOn(process, 'exit').mockImplementation(() => {
          throw new Error('process.exit called');
        });

        // process.exit 会被 catch 捕获并返回 error
        const result = await processor.process('/exit', mockContext);
        expect(result.type).toBe('error');
        if (result.type === 'error') {
          expect(result.message).toContain('process.exit called');
        }
        exitSpy.mockRestore();
      });

      it('应该支持别名 /quit', async () => {
        const exitSpy = vi.spyOn(process, 'exit').mockImplementation(() => {
          throw new Error('process.exit called');
        });

        const result = await processor.process('/quit', mockContext);
        expect(result.type).toBe('error');
        if (result.type === 'error') {
          expect(result.message).toContain('process.exit called');
        }
        exitSpy.mockRestore();
      });

      it('应该支持别名 /q', async () => {
        const exitSpy = vi.spyOn(process, 'exit').mockImplementation(() => {
          throw new Error('process.exit called');
        });

        const result = await processor.process('/q', mockContext);
        expect(result.type).toBe('error');
        if (result.type === 'error') {
          expect(result.message).toContain('process.exit called');
        }
        exitSpy.mockRestore();
      });
    });

    describe('/config', () => {
      it('应该显示配置信息', async () => {
        const result = await processor.process('/config', mockContext);

        expect(result.type).toBe('handled');
        if (result.type === 'handled') {
          expect(result.output).toContain('Configuration');
        }
      });
    });

    describe('/model', () => {
      it('应该显示模型信息', async () => {
        const result = await processor.process('/model', mockContext);

        expect(result.type).toBe('handled');
        if (result.type === 'handled') {
          expect(result.output).toContain('model');
        }
      });
    });
  });

  describe('未知命令', () => {
    it('未知的 slash 命令应该委托给编译器', async () => {
      const result = await processor.process('/file:test.kt', mockContext);

      expect(result.type).toBe('compile');
      if (result.type === 'compile') {
        expect(result.devins).toBe('/file:test.kt');
      }
    });

    it('应该保留命令的原始格式', async () => {
      const result = await processor.process('/symbol:MyClass', mockContext);

      expect(result.type).toBe('compile');
      if (result.type === 'compile') {
        expect(result.devins).toBe('/symbol:MyClass');
      }
    });
  });

  describe('命令参数', () => {
    it('应该正确解析带参数的命令', async () => {
      const result = await processor.process('/model deepseek', mockContext);

      expect(result.type).toBe('handled');
    });

    it('应该处理多个空格分隔的参数', async () => {
      const result = await processor.process('/model set deepseek', mockContext);

      expect(result.type).toBe('handled');
    });
  });

  describe('边界情况', () => {
    it('应该处理只有 / 的输入', async () => {
      const result = await processor.process('/', mockContext);

      expect(result.type).toBe('error');
      if (result.type === 'error') {
        expect(result.message).toContain('required');
      }
    });

    it('应该处理带空格的命令名', async () => {
      const result = await processor.process('/  help  ', mockContext);

      expect(result.type).toBe('handled');
    });

    it('应该忽略大小写', async () => {
      const result = await processor.process('/HELP', mockContext);

      expect(result.type).toBe('handled');
    });
  });

  describe('命令注册', () => {
    it('应该能注册新命令', () => {
      processor.registerCommand('test', {
        description: 'Test command',
        action: async () => ({ type: 'handled', output: 'test executed' }),
      });

      const commands = processor.getCommands();
      expect(commands.has('test')).toBe(true);
    });

    it('应该支持命令别名', () => {
      processor.registerCommand('longname', {
        description: 'Long command',
        aliases: ['ln', 'short'],
        action: async () => ({ type: 'handled' }),
      });

      const commands = processor.getCommands();
      expect(commands.has('longname')).toBe(true);
      expect(commands.has('ln')).toBe(true);
      expect(commands.has('short')).toBe(true);
    });

    it('自定义命令应该能被执行', async () => {
      processor.registerCommand('custom', {
        description: 'Custom command',
        action: async () => ({ type: 'handled', output: 'custom output' }),
      });

      const result = await processor.process('/custom', mockContext);

      expect(result.type).toBe('handled');
      if (result.type === 'handled') {
        expect(result.output).toBe('custom output');
      }
    });
  });

  describe('错误处理', () => {
    it('命令执行错误应该被捕获', async () => {
      processor.registerCommand('error', {
        description: 'Error command',
        action: async () => {
          throw new Error('Command failed');
        },
      });

      const result = await processor.process('/error', mockContext);

      expect(result.type).toBe('error');
      if (result.type === 'error') {
        expect(result.message).toContain('Command failed');
      }
    });
  });
});
