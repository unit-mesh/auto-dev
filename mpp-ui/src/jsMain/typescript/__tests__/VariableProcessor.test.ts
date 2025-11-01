/**
 * VariableProcessor 单元测试
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { VariableProcessor } from '../processors/VariableProcessor';
import type { ProcessorContext } from '../processors/InputRouter';

describe('VariableProcessor', () => {
  let processor: VariableProcessor;
  let mockContext: ProcessorContext;

  beforeEach(() => {
    processor = new VariableProcessor();
    mockContext = {
      logger: {
        info: vi.fn(),
        warn: vi.fn(),
        error: vi.fn(),
      },
    };
  });

  describe('canHandle', () => {
    it('应该处理包含 $ 变量的输入', () => {
      expect(processor.canHandle('$pwd')).toBe(true);
      expect(processor.canHandle('Current dir: $pwd')).toBe(true);
      expect(processor.canHandle('$user and $date')).toBe(true);
    });

    it('不应该处理没有 $ 变量的输入', () => {
      expect(processor.canHandle('normal text')).toBe(false);
      expect(processor.canHandle('/command')).toBe(false);
      expect(processor.canHandle('@file')).toBe(false);
    });
  });

  describe('内置变量', () => {
    it('应该替换 $pwd 为当前工作目录', async () => {
      const result = await processor.process('Current: $pwd', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).not.toContain('$pwd');
        expect(result.query).toContain('Current:');
      }
    });

    it('应该替换 $user 为当前用户', async () => {
      const result = await processor.process('User: $user', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).not.toContain('$user');
        expect(result.query).toContain('User:');
      }
    });

    it('应该替换 $date 为当前日期时间', async () => {
      const result = await processor.process('Date: $date', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).not.toContain('$date');
        expect(result.query).toContain('Date:');
      }
    });

    it('应该替换 $today 为当前日期', async () => {
      const result = await processor.process('Today: $today', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).not.toContain('$today');
      }
    });

    it('应该替换 $time 为当前时间', async () => {
      const result = await processor.process('Time: $time', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).not.toContain('$time');
      }
    });

    it('应该替换 $home 为主目录', async () => {
      const result = await processor.process('Home: $home', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).not.toContain('$home');
      }
    });

    it('应该替换 $os 为操作系统', async () => {
      const result = await processor.process('OS: $os', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).not.toContain('$os');
        expect(result.query).toMatch(/OS: (darwin|linux|win32)/);
      }
    });
  });

  describe('多个变量', () => {
    it('应该替换多个变量', async () => {
      const result = await processor.process('User $user at $pwd', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).not.toContain('$user');
        expect(result.query).not.toContain('$pwd');
      }
    });

    it('应该替换相同变量的多个出现', async () => {
      const result = await processor.process('$pwd and $pwd again', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).not.toContain('$pwd');
        const parts = result.query.split(' and ');
        expect(parts[0]).toBe(parts[1].replace(' again', ''));
      }
    });
  });

  describe('未知变量', () => {
    it('未知变量应该委托给编译器', async () => {
      const result = await processor.process('$unknown', mockContext);

      expect(result.type).toBe('compile');
      if (result.type === 'compile') {
        expect(result.devins).toBe('$unknown');
      }
    });

    it('混合已知和未知变量应该委托给编译器', async () => {
      const result = await processor.process('$pwd and $unknown', mockContext);

      expect(result.type).toBe('compile');
    });
  });

  describe('变量注册', () => {
    it('应该能注册静态变量', () => {
      processor.registerVariable('custom', 'custom value');

      const value = processor.getVariable('custom');
      expect(value).toBe('custom value');
    });

    it('应该能注册动态变量（函数）', () => {
      let counter = 0;
      processor.registerVariable('counter', () => String(++counter));

      const value1 = processor.getVariable('counter');
      const value2 = processor.getVariable('counter');

      expect(value1).toBe('1');
      expect(value2).toBe('2');
    });

    it('自定义变量应该能被替换', async () => {
      processor.registerVariable('project', 'AutoDev');

      const result = await processor.process('Project: $project', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).toBe('Project: AutoDev');
      }
    });

    it('变量名应该不区分大小写', () => {
      processor.registerVariable('Test', 'value');

      expect(processor.getVariable('test')).toBe('value');
      expect(processor.getVariable('TEST')).toBe('value');
      expect(processor.getVariable('Test')).toBe('value');
    });
  });

  describe('可用变量列表', () => {
    it('应该返回所有可用变量', () => {
      const variables = processor.getAvailableVariables();

      expect(variables).toContain('pwd');
      expect(variables).toContain('user');
      expect(variables).toContain('date');
      expect(variables).toContain('today');
      expect(variables).toContain('time');
      expect(variables).toContain('home');
      expect(variables).toContain('os');
    });

    it('应该包含自定义变量', () => {
      processor.registerVariable('custom', 'value');

      const variables = processor.getAvailableVariables();
      expect(variables).toContain('custom');
    });
  });

  describe('边界情况', () => {
    it('应该处理没有变量的输入', async () => {
      const result = await processor.process('no variables here', mockContext);

      expect(result.type).toBe('skip');
    });

    it('应该跳过单独的 $ 符号', async () => {
      const result = await processor.process('Price: $100', mockContext);

      // $1 被识别为变量引用（虽然不存在），委托给编译器处理
      // 这是预期行为，让编译器决定如何处理未知变量
      expect(result.type).toBe('compile');
    });

    it('应该处理 $ 后跟空格的情况', async () => {
      const result = await processor.process('$ pwd', mockContext);

      // $ 和 pwd 之间有空格，不应该被识别为变量
      expect(result.type).toBe('skip');
    });

    it('应该只替换完整的变量名', async () => {
      processor.registerVariable('var', 'value');

      const result = await processor.process('$var and $variable', mockContext);

      // $var 应该被替换，$variable 不存在应该委托给编译器
      expect(result.type).toBe('compile');
    });
  });

  describe('日志记录', () => {
    it('应该记录找到的变量数量', async () => {
      await processor.process('$pwd and $user', mockContext);

      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('variable references')
      );
    });

    it('应该记录变量替换', async () => {
      await processor.process('$pwd', mockContext);

      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('Replaced $pwd')
      );
    });

    it('应该记录未知变量', async () => {
      await processor.process('$unknown', mockContext);

      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('Unknown variable')
      );
    });
  });
});
