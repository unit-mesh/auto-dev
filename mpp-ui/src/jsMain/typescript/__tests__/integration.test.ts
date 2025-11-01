/**
 * 处理器架构集成测试
 * 测试完整的输入处理流程
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { InputRouter, type ProcessorContext } from '../processors/InputRouter';
import { SlashCommandProcessor } from '../processors/SlashCommandProcessor';
import { AtCommandProcessor } from '../processors/AtCommandProcessor';
import { VariableProcessor } from '../processors/VariableProcessor';

describe('处理器架构集成测试', () => {
  let router: InputRouter;
  let mockContext: ProcessorContext;

  beforeEach(() => {
    router = new InputRouter();
    mockContext = {
      clearMessages: vi.fn(),
      logger: {
        info: vi.fn(),
        warn: vi.fn(),
        error: vi.fn(),
      },
      readFile: vi.fn().mockResolvedValue('file content'),
    };

    // 注册所有处理器（按优先级）
    router.register(new SlashCommandProcessor(), 100);
    router.register(new AtCommandProcessor(), 50);
    router.register(new VariableProcessor(), 30);
  });

  describe('单一处理器场景', () => {
    it('slash 命令应该被 SlashCommandProcessor 处理', async () => {
      const result = await router.route('/help', mockContext);

      expect(result.type).toBe('handled');
      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('SlashCommandProcessor')
      );
    });

    it('@ 引用应该被 AtCommandProcessor 处理', async () => {
      const result = await router.route('@file.txt', mockContext);

      expect(result.type).toBe('llm-query');
      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('AtCommandProcessor')
      );
    });

    it('$ 变量应该被 VariableProcessor 处理', async () => {
      const result = await router.route('$pwd', mockContext);

      expect(result.type).toBe('llm-query');
      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('VariableProcessor')
      );
    });

    it('普通文本应该路由到 LLM', async () => {
      const result = await router.route('Hello, how are you?', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).toBe('Hello, how are you?');
      }
    });
  });

  describe('处理器优先级', () => {
    it('slash 命令应该优先于其他处理器', async () => {
      // 即使输入包含 @ 和 $，/ 应该首先被处理
      const result = await router.route('/help @file $pwd', mockContext);

      expect(result.type).toBe('handled');
      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('SlashCommandProcessor')
      );
    });

    it('@ 引用应该优先于 $ 变量', async () => {
      const result = await router.route('@file.txt with $pwd', mockContext);

      // AtCommandProcessor 优先级更高
      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('AtCommandProcessor')
      );
    });
  });

  describe('组合场景', () => {
    it('变量应该在文件内容中被替换', async () => {
      // 先处理 @ 引用，然后变量可能在后续被处理
      const result = await router.route('Read @config.txt for $user', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).toContain('file content');
      }
    });

    it('未知命令应该委托给编译器', async () => {
      const result = await router.route('/file:test.kt', mockContext);

      expect(result.type).toBe('compile');
      if (result.type === 'compile') {
        expect(result.devins).toBe('/file:test.kt');
      }
    });

    it('未知变量应该委托给编译器', async () => {
      const result = await router.route('$customVar', mockContext);

      expect(result.type).toBe('compile');
    });
  });

  describe('边界情况', () => {
    it('空输入应该返回错误', async () => {
      const result = await router.route('', mockContext);

      expect(result.type).toBe('error');
    });

    it('只有空白的输入应该返回错误', async () => {
      const result = await router.route('   \n\t  ', mockContext);

      expect(result.type).toBe('error');
    });

    it('特殊字符混合应该正确路由', async () => {
      const result = await router.route('/@test', mockContext);

      // / 优先，应该被 SlashCommandProcessor 处理
      // 但 /@test 不是有效的命令，所以会委托给编译器
      expect(result.type).toBe('compile');
    });
  });

  describe('错误处理', () => {
    it('文件读取失败应该优雅处理', async () => {
      mockContext.readFile = vi.fn().mockRejectedValue(new Error('File not found'));

      const result = await router.route('@missing.txt', mockContext);

      // 应该回退到编译器
      expect(result.type).toBe('compile');
    });

    it('处理器异常应该被捕获', async () => {
      // 创建一个会抛出错误的处理器
      const errorRouter = new InputRouter();
      const errorProcessor = {
        name: 'ErrorProcessor',
        canHandle: () => true,
        process: async () => {
          throw new Error('Processor error');
        },
      };

      errorRouter.register(errorProcessor);

      const result = await errorRouter.route('test', mockContext);

      expect(result.type).toBe('error');
      if (result.type === 'error') {
        expect(result.message).toContain('Processor error');
      }
    });
  });

  describe('实际使用场景', () => {
    it('场景1: 查看帮助', async () => {
      const result = await router.route('/help', mockContext);

      expect(result.type).toBe('handled');
    });

    it('场景2: 清空历史', async () => {
      const result = await router.route('/clear', mockContext);

      expect(result.type).toBe('handled');
      expect(mockContext.clearMessages).toHaveBeenCalled();
    });

    it('场景3: 读取文件并询问', async () => {
      const result = await router.route('Explain @src/main.ts', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).toContain('Explain');
        expect(result.query).toContain('file content');
      }
    });

    it('场景4: 使用变量', async () => {
      const result = await router.route('Current directory is $pwd', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).not.toContain('$pwd');
        expect(result.query).toContain('Current directory is');
      }
    });

    it('场景5: DevIns 命令', async () => {
      const result = await router.route('/file:src/**/*.kt', mockContext);

      expect(result.type).toBe('compile');
      if (result.type === 'compile') {
        expect(result.devins).toBe('/file:src/**/*.kt');
      }
    });

    it('场景6: 复杂查询', async () => {
      const result = await router.route(
        'Analyze @src/main.ts and tell me about the user $user',
        mockContext
      );

      // @ 引用优先，应该预处理文件
      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).toContain('Analyze');
        expect(result.query).toContain('file content');
      }
    });

    it('场景7: 纯文本对话', async () => {
      const result = await router.route(
        'How do I implement a singleton pattern?',
        mockContext
      );

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).toBe('How do I implement a singleton pattern?');
      }
    });
  });

  describe('性能考虑', () => {
    it('简单命令应该快速返回', async () => {
      const start = Date.now();
      await router.route('/help', mockContext);
      const duration = Date.now() - start;

      // 应该在 100ms 内完成
      expect(duration).toBeLessThan(100);
    });

    it('多处理器检查应该高效', async () => {
      const start = Date.now();
      
      // 测试多次路由
      for (let i = 0; i < 10; i++) {
        await router.route('test input', mockContext);
      }
      
      const duration = Date.now() - start;
      const avgDuration = duration / 10;

      // 平均每次应该在 10ms 内完成
      expect(avgDuration).toBeLessThan(10);
    });
  });

  describe('日志验证', () => {
    it('应该记录处理器选择', async () => {
      await router.route('/help', mockContext);

      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('Routing to SlashCommandProcessor')
      );
    });

    it('应该记录处理结果', async () => {
      await router.route('/help', mockContext);

      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('handled')
      );
    });
  });
});
