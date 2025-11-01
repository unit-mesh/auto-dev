/**
 * At Command Processor
 * 
 * 处理 @ 符号引用，包括：
 * - 代理引用：@code, @test
 * - 文件引用：@file.txt, @src/main.ts
 * 
 * 参考 Gemini CLI 的 atCommandProcessor.ts
 */

import type { InputProcessor, ProcessorContext, ProcessorResult } from './InputRouter.js';

/**
 * At 引用类型
 */
interface AtReference {
  /** 原始文本 */
  raw: string;
  
  /** 引用内容（去掉 @ 符号） */
  content: string;
  
  /** 是否是文件路径（包含 / 或 .） */
  isFilePath: boolean;
  
  /** 起始位置 */
  startIndex: number;
  
  /** 结束位置 */
  endIndex: number;
}

/**
 * At 命令处理器
 */
export class AtCommandProcessor implements InputProcessor {
  name = 'AtCommandProcessor';
  
  /**
   * 判断是否可以处理
   */
  canHandle(input: string): boolean {
    // 必须包含有效的 @ 引用（@后面跟字母、数字、下划线、-、.、/）
    const matches = input.match(/@([\w\-./]+)/);
    return matches !== null && matches[1].length > 0;
  }
  
  /**
   * 处理 @ 引用
   */
  async process(input: string, context: ProcessorContext): Promise<ProcessorResult> {
    // 解析所有 @ 引用
    const references = this.parseAtReferences(input);
    
    if (references.length === 0) {
      return { type: 'skip' };
    }
    
    context.logger.info(`[AtCommandProcessor] Found ${references.length} @ references`);
    
    // 分类引用
    const fileReferences = references.filter(ref => ref.isFilePath);
    const agentReferences = references.filter(ref => !ref.isFilePath);
    
    // 如果只有代理引用（无文件路径），委托给编译器
    if (fileReferences.length === 0) {
      context.logger.info('[AtCommandProcessor] Only agent references, delegating to compiler');
      return { type: 'compile', devins: input };
    }
    
    // 如果包含文件路径，需要预处理
    try {
      const processedQuery = await this.preprocessFileReferences(
        input,
        fileReferences,
        context
      );
      
      return { type: 'llm-query', query: processedQuery };
    } catch (error) {
      context.logger.error('[AtCommandProcessor] Failed to preprocess file references:', error);
      
      // 预处理失败，仍然委托给编译器尝试
      return { type: 'compile', devins: input };
    }
  }
  
  /**
   * 解析所有 @ 引用
   */
  private parseAtReferences(input: string): AtReference[] {
    const references: AtReference[] = [];
    const regex = /@([\w\-./]+)/g;
    let match;
    
    while ((match = regex.exec(input)) !== null) {
      const raw = match[0];
      const content = match[1];
      const startIndex = match.index;
      const endIndex = startIndex + raw.length;
      
      // 判断是否是文件路径（包含 / 或 . 或常见扩展名）
      const isFilePath = content.includes('/') || 
                        content.includes('.') ||
                        /\.(ts|tsx|js|jsx|kt|java|py|md|txt)$/i.test(content);
      
      references.push({
        raw,
        content,
        isFilePath,
        startIndex,
        endIndex
      });
    }
    
    return references;
  }
  
  /**
   * 预处理文件引用
   * 读取文件内容并替换到输入中
   */
  private async preprocessFileReferences(
    input: string,
    references: AtReference[],
    context: ProcessorContext
  ): Promise<string> {
    if (!context.readFile) {
      context.logger.warn('[AtCommandProcessor] readFile not available in context');
      throw new Error('File reading not supported');
    }
    
    // 读取所有文件内容
    const fileContents = new Map<string, string>();
    
    for (const ref of references) {
      try {
        context.logger.info(`[AtCommandProcessor] Reading file: ${ref.content}`);
        const content = await context.readFile(ref.content);
        fileContents.set(ref.content, content);
      } catch (error) {
        context.logger.warn(`[AtCommandProcessor] Failed to read ${ref.content}: ${error}`);
        // 继续处理其他文件
      }
    }
    
    // 如果没有成功读取任何文件，抛出错误
    if (fileContents.size === 0) {
      throw new Error('Failed to read any referenced files');
    }
    
    // 替换 @file 为实际内容
    let processedQuery = input;
    
    for (const [path, content] of fileContents.entries()) {
      // 转义特殊字符用于正则表达式
      const escapedPath = path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const pattern = new RegExp(`@${escapedPath}`, 'g');
      
      // 替换为格式化的文件内容
      const replacement = `\n<file path="${path}">\n${content}\n</file>\n`;
      processedQuery = processedQuery.replace(pattern, replacement);
    }
    
    context.logger.info('[AtCommandProcessor] File references preprocessed successfully');
    return processedQuery;
  }
}
