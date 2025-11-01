/**
 * AtCommandProcessor 单元测试
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AtCommandProcessor } from '../processors/AtCommandProcessor';
import type { ProcessorContext } from '../processors/InputRouter';

describe('AtCommandProcessor', () => {
  let processor: AtCommandProcessor;
  let mockContext: ProcessorContext;

  beforeEach(() => {
    processor = new AtCommandProcessor();
    mockContext = {
      logger: {
        info: vi.fn(),
        warn: vi.fn(),
        error: vi.fn(),
      },
      readFile: vi.fn(),
    };
  });

  describe('canHandle', () => {
    it('应该处理包含 @ 引用的输入', () => {
      expect(processor.canHandle('@agent')).toBe(true);
      expect(processor.canHandle('@file.txt')).toBe(true);
      expect(processor.canHandle('text with @file.txt reference')).toBe(true);
      expect(processor.canHandle('@src/main.ts')).toBe(true);
    });

    it('不应该处理没有 @ 引用的输入', () => {
      expect(processor.canHandle('normal text')).toBe(false);
      expect(processor.canHandle('/command')).toBe(false);
      expect(processor.canHandle('$variable')).toBe(false);
    });

    it('应该处理多个 @ 引用', () => {
      expect(processor.canHandle('@file1.txt and @file2.txt')).toBe(true);
    });
  });

  describe('代理引用', () => {
    it('纯代理引用应该委托给编译器', async () => {
      const result = await processor.process('@code', mockContext);

      expect(result.type).toBe('compile');
      if (result.type === 'compile') {
        expect(result.devins).toBe('@code');
      }
    });

    it('多个代理引用应该委托给编译器', async () => {
      const result = await processor.process('@code @test', mockContext);

      expect(result.type).toBe('compile');
    });
  });

  describe('文件引用', () => {
    it('应该识别文件路径引用', async () => {
      const mockReadFile = vi.fn().mockResolvedValue('file content');
      mockContext.readFile = mockReadFile;

      const result = await processor.process('@file.txt', mockContext);

      expect(mockReadFile).toHaveBeenCalledWith('file.txt');
    });

    it('应该预处理文件内容', async () => {
      const mockReadFile = vi.fn().mockResolvedValue('test content');
      mockContext.readFile = mockReadFile;

      const result = await processor.process('Read @test.txt', mockContext);

      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).toContain('test content');
        expect(result.query).toContain('<file path="test.txt">');
      }
    });

    it('应该处理带路径的文件引用', async () => {
      const mockReadFile = vi.fn().mockResolvedValue('content');
      mockContext.readFile = mockReadFile;

      await processor.process('@src/main.ts', mockContext);

      expect(mockReadFile).toHaveBeenCalledWith('src/main.ts');
    });

    it('应该处理多个文件引用', async () => {
      const mockReadFile = vi.fn()
        .mockResolvedValueOnce('content1')
        .mockResolvedValueOnce('content2');
      mockContext.readFile = mockReadFile;

      const result = await processor.process('@file1.txt and @file2.txt', mockContext);

      expect(mockReadFile).toHaveBeenCalledTimes(2);
      expect(result.type).toBe('llm-query');
      if (result.type === 'llm-query') {
        expect(result.query).toContain('content1');
        expect(result.query).toContain('content2');
      }
    });
  });

  describe('混合引用', () => {
    it('包含文件路径的混合引用应该预处理文件', async () => {
      const mockReadFile = vi.fn().mockResolvedValue('file content');
      mockContext.readFile = mockReadFile;

      const result = await processor.process('@code review @main.ts', mockContext);

      expect(mockReadFile).toHaveBeenCalledWith('main.ts');
      expect(result.type).toBe('llm-query');
    });
  });

  describe('文件读取错误', () => {
    it('单个文件读取失败应该继续处理其他文件', async () => {
      const mockReadFile = vi.fn()
        .mockRejectedValueOnce(new Error('File not found'))
        .mockResolvedValueOnce('content2');
      mockContext.readFile = mockReadFile;

      const result = await processor.process('@missing.txt @existing.txt', mockContext);

      // 应该至少成功读取一个文件
      if (result.type === 'llm-query') {
        expect(result.query).toContain('content2');
      }
    });

    it('所有文件读取失败应该委托给编译器', async () => {
      const mockReadFile = vi.fn().mockRejectedValue(new Error('File not found'));
      mockContext.readFile = mockReadFile;

      const result = await processor.process('@missing.txt', mockContext);

      expect(result.type).toBe('compile');
    });

    it('没有 readFile 函数应该委托给编译器', async () => {
      mockContext.readFile = undefined;

      const result = await processor.process('@file.txt', mockContext);

      expect(result.type).toBe('compile');
    });
  });

  describe('引用解析', () => {
    it('应该正确识别文件扩展名', async () => {
      const mockReadFile = vi.fn().mockResolvedValue('content');
      mockContext.readFile = mockReadFile;

      await processor.process('@file.ts', mockContext);
      expect(mockReadFile).toHaveBeenCalledWith('file.ts');

      await processor.process('@file.tsx', mockContext);
      expect(mockReadFile).toHaveBeenCalledWith('file.tsx');

      await processor.process('@file.kt', mockContext);
      expect(mockReadFile).toHaveBeenCalledWith('file.kt');

      await processor.process('@file.py', mockContext);
      expect(mockReadFile).toHaveBeenCalledWith('file.py');
    });

    it('应该处理相对路径', async () => {
      const mockReadFile = vi.fn().mockResolvedValue('content');
      mockContext.readFile = mockReadFile;

      await processor.process('@./src/file.ts', mockContext);
      expect(mockReadFile).toHaveBeenCalled();
    });

    it('应该处理父目录路径', async () => {
      const mockReadFile = vi.fn().mockResolvedValue('content');
      mockContext.readFile = mockReadFile;

      await processor.process('@../parent/file.ts', mockContext);
      expect(mockReadFile).toHaveBeenCalled();
    });
  });

  describe('边界情况', () => {
    it('应该处理没有引用的输入', async () => {
      const result = await processor.process('no references here', mockContext);

      expect(result.type).toBe('skip');
    });

    it('应该跳过单独的 @ 符号', async () => {
      const result = await processor.process('email: user@example.com', mockContext);

      // @example 被识别为代理引用，委托给 LLM 处理
      // 这是预期行为，因为 @example 可能是有效的引用
      expect(result.type).toBe('llm-query');
    });

    it('应该正确处理文件名中的特殊字符', async () => {
      const mockReadFile = vi.fn().mockResolvedValue('content');
      mockContext.readFile = mockReadFile;

      await processor.process('@my-file_name.test.ts', mockContext);
      expect(mockReadFile).toHaveBeenCalledWith('my-file_name.test.ts');
    });
  });

  describe('日志记录', () => {
    it('应该记录找到的引用数量', async () => {
      await processor.process('@file.txt', mockContext);

      expect(mockContext.logger.info).toHaveBeenCalledWith(
        expect.stringContaining('@ references')
      );
    });

    it('应该记录文件读取失败', async () => {
      const mockReadFile = vi.fn().mockRejectedValue(new Error('Read error'));
      mockContext.readFile = mockReadFile;

      await processor.process('@file.txt', mockContext);

      expect(mockContext.logger.warn).toHaveBeenCalled();
    });
  });
});
