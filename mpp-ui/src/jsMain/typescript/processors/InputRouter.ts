/**
 * Input Router - 输入路由系统
 * 
 * 负责将用户输入路由到不同的处理器
 * 参考 Gemini CLI 的处理器架构设计
 */

/**
 * 处理器上下文
 * 包含处理器执行所需的所有依赖
 */
export interface ProcessorContext {
  /** 清空消息历史 */
  clearMessages?: () => void;
  
  /** 日志记录器 */
  logger: {
    info: (message: string) => void;
    warn: (message: string) => void;
    error: (message: string, error?: any) => void;
  };
  
  /** 读取文件内容（通过 Kotlin） */
  readFile?: (path: string) => Promise<string>;
  
  /** 添加消息到历史 */
  addMessage?: (role: string, content: string) => void;
  
  /** 设置加载状态 */
  setLoading?: (loading: boolean) => void;
}

/**
 * 处理器结果类型
 */
export type ProcessorResult = 
  | { type: 'handled'; output?: string }      // 已处理，可选输出内容
  | { type: 'compile'; devins: string }       // 需要编译 DevIns
  | { type: 'llm-query'; query: string }      // 发送给 LLM
  | { type: 'error'; message: string }        // 错误
  | { type: 'skip' };                         // 跳过，继续下一个处理器

/**
 * 输入处理器接口
 */
export interface InputProcessor {
  /** 处理器名称 */
  name: string;
  
  /** 判断是否可以处理该输入 */
  canHandle(input: string): boolean;
  
  /** 处理输入 */
  process(input: string, context: ProcessorContext): Promise<ProcessorResult>;
}

/**
 * 处理器注册项（包含优先级）
 */
interface ProcessorEntry {
  processor: InputProcessor;
  priority: number;
}

/**
 * 输入路由器
 * 管理和调度所有输入处理器
 */
export class InputRouter {
  private processors: ProcessorEntry[] = [];
  
  /**
   * 注册处理器
   * @param processor 处理器实例
   * @param priority 优先级（数字越大优先级越高，默认 0）
   */
  register(processor: InputProcessor, priority: number = 0): void {
    this.processors.push({ processor, priority });
    // 按优先级排序（高 → 低）
    this.processors.sort((a, b) => b.priority - a.priority);
  }
  
  /**
   * 路由输入到合适的处理器
   * @param input 用户输入
   * @param context 处理器上下文
   * @returns 处理结果
   */
  async route(input: string, context: ProcessorContext): Promise<ProcessorResult> {
    // 去除首尾空白
    const trimmedInput = input.trim();
    
    if (!trimmedInput) {
      return { type: 'error', message: 'Input cannot be empty' };
    }
    
    // 遍历所有处理器
    for (const entry of this.processors) {
      const { processor } = entry;
      
      try {
        // 检查处理器是否可以处理该输入
        if (processor.canHandle(trimmedInput)) {
          context.logger.info(`[InputRouter] Routing to ${processor.name}`);
          
          // 处理输入
          const result = await processor.process(trimmedInput, context);
          
          // 如果不是 skip，直接返回结果
          if (result.type !== 'skip') {
            context.logger.info(`[InputRouter] ${processor.name} handled: ${result.type}`);
            return result;
          }
          
          // skip 则继续尝试下一个处理器
          context.logger.info(`[InputRouter] ${processor.name} skipped, trying next`);
        }
      } catch (error) {
        context.logger.error(`[InputRouter] Error in ${processor.name}:`, error);
        return {
          type: 'error',
          message: `Processor error: ${error instanceof Error ? error.message : String(error)}`
        };
      }
    }
    
    // 没有处理器处理，默认发送给 LLM
    context.logger.info('[InputRouter] No processor handled, routing to LLM');
    return { type: 'llm-query', query: trimmedInput };
  }
  
  /**
   * 获取所有已注册的处理器
   */
  getProcessors(): InputProcessor[] {
    return this.processors.map(e => e.processor);
  }
  
  /**
   * 清除所有处理器
   */
  clear(): void {
    this.processors = [];
  }
}
