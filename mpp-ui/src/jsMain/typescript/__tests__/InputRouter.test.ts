/**
 * InputRouter 单元测试
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { InputRouter, type InputProcessor, type ProcessorContext, type ProcessorResult } from '../processors/InputRouter';

describe('InputRouter', () => {
  let router: InputRouter;
  let mockContext: ProcessorContext;

  beforeEach(() => {
    router = new InputRouter();
    mockContext = {
      logger: {
        info: vi.fn(),
        warn: vi.fn(),
        error: vi.fn(),
      },
    };
  });

  describe('处理器注册', () => {
    it('应该成功注册处理器', () => {
      const mockProcessor: InputProcessor = {
        name: 'TestProcessor',
        canHandle: () => true,
        process: async () => ({ type: 'handled' }),
      };

      router.register(mockProcessor);
      const processors = router.getProcessors();

      expect(processors).toHaveLength(1);
      expect(processors[0].name).toBe('TestProcessor');
    });

    it('应该按优先级排序处理器', () => {
      const lowPriorityProcessor: InputProcessor = {
        name: 'LowPriority',
        canHandle: () => true,
        process: async () => ({ type: 'handled' }),
      };

      const highPriorityProcessor: InputProcessor = {
        name: 'HighPriority',
        canHandle: () => true,
        process: async () => ({ type: 'handled' }),
      };

      // 先注册低优先级，再注册高优先级
      router.register(lowPriorityProcessor, 10);
      router.register(highPriorityProcessor, 100);

      const processors = router.getProcessors();
      expect(processors[0].name).toBe('HighPriority');
      expect(processors[1].name).toBe('LowPriority');
    });

    it('应该能清除所有处理器', () => {
      const mockProcessor: InputProcessor = {
        name: 'TestProcessor',
        canHandle: () => true,
        process: async () => ({ type: 'handled' }),
      };

      router.register(mockProcessor);
      expect(router.getProcessors()).toHaveLength(1);

      router.clear();
      expect(router.getProcessors()).toHaveLength(0);
    });
  });

  describe('输入路由', () => {
    it('应该拒绝空输入', async () => {
      const result = await router.route('', mockContext);
      
      expect(result.type).toBe('error');
      if (result.type === 'error') {
        expect(result.message).toContain('empty');
      }
    });

    it('应该拒绝只有空白的输入', async () => {
      const result = await router.route('   ', mockContext);
      
      expect(result.type).toBe('error');
      if (result.type === 'error') {
        expect(result.message).toContain('empty');
      }
    });

    it('当没有处理器时应该路由到 LLM', async () => {
      const result = await router.route('test input', mockContext);
      
      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).toBe('test input');
      }
    });

    it('应该使用第一个可以处理的处理器', async () => {
      const processor1: InputProcessor = {
        name: 'Processor1',
        canHandle: () => false,
        process: async () => ({ type: 'handled' }),
      };

      const processor2: InputProcessor = {
        name: 'Processor2',
        canHandle: () => true,
        process: async () => ({ type: 'handled', output: 'handled by processor2' }),
      };

      router.register(processor1, 100);
      router.register(processor2, 50);

      const result = await router.route('test', mockContext);
      
      expect(result.type).toBe('handled');
      if (result.type === 'handled') {
        expect(result.output).toBe('handled by processor2');
      }
    });

    it('处理器返回 skip 时应该继续下一个处理器', async () => {
      const skipProcessor: InputProcessor = {
        name: 'SkipProcessor',
        canHandle: () => true,
        process: async () => ({ type: 'skip' }),
      };

      const handleProcessor: InputProcessor = {
        name: 'HandleProcessor',
        canHandle: () => true,
        process: async () => ({ type: 'handled', output: 'final handler' }),
      };

      router.register(skipProcessor, 100);
      router.register(handleProcessor, 50);

      const result = await router.route('test', mockContext);
      
      expect(result.type).toBe('handled');
      if (result.type === 'handled') {
        expect(result.output).toBe('final handler');
      }
    });

    it('处理器抛出错误时应该返回错误结果', async () => {
      const errorProcessor: InputProcessor = {
        name: 'ErrorProcessor',
        canHandle: () => true,
        process: async () => {
          throw new Error('Test error');
        },
      };

      router.register(errorProcessor);

      const result = await router.route('test', mockContext);
      
      expect(result.type).toBe('error');
      if (result.type === 'error') {
        expect(result.message).toContain('Test error');
      }
    });
  });

  describe('处理器结果类型', () => {
    it('应该正确处理 handled 结果', async () => {
      const processor: InputProcessor = {
        name: 'Test',
        canHandle: () => true,
        process: async () => ({ type: 'handled', output: 'done' }),
      };

      router.register(processor);
      const result = await router.route('test', mockContext);
      
      expect(result.type).toBe('handled');
    });

    it('应该正确处理 compile 结果', async () => {
      const processor: InputProcessor = {
        name: 'Test',
        canHandle: () => true,
        process: async () => ({ type: 'compile', devins: '/file:test.kt' }),
      };

      router.register(processor);
      const result = await router.route('test', mockContext);
      
      expect(result.type).toBe('compile');
      if (result.type === 'compile') {
        expect(result.devins).toBe('/file:test.kt');
      }
    });

    it('应该正确处理 llm-query 结果', async () => {
      const processor: InputProcessor = {
        name: 'Test',
        canHandle: () => true,
        process: async () => ({ type: 'llm-query', query: 'processed query' }),
      };

      router.register(processor);
      const result = await router.route('test', mockContext);
      
      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).toBe('processed query');
      }
    });

    it('应该正确处理 error 结果', async () => {
      const processor: InputProcessor = {
        name: 'Test',
        canHandle: () => true,
        process: async () => ({ type: 'error', message: 'Something went wrong' }),
      };

      router.register(processor);
      const result = await router.route('test', mockContext);
      
      expect(result.type).toBe('error');
      if (result.type === 'error') {
        expect(result.message).toBe('Something went wrong');
      }
    });
  });

  describe('日志记录', () => {
    it('应该记录路由信息', async () => {
      const processor: InputProcessor = {
        name: 'TestProcessor',
        canHandle: () => true,
        process: async () => ({ type: 'handled' }),
      };

      router.register(processor);
      await router.route('test', mockContext);

      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('TestProcessor')
      );
    });

    it('应该记录处理器错误', async () => {
      const errorProcessor: InputProcessor = {
        name: 'ErrorProcessor',
        canHandle: () => true,
        process: async () => {
          throw new Error('Test error');
        },
      };

      router.register(errorProcessor);
      await router.route('test', mockContext);

      expect(mockContext.logger.error).toHaveBeenCalled();
    });
  });
});
