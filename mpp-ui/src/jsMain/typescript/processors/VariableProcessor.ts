/**
 * Variable Processor
 * 
 * 处理 $ 变量引用，如 $pwd, $user, $date 等
 */

import type { InputProcessor, ProcessorContext, ProcessorResult } from './InputRouter.js';

/**
 * 变量值类型（可以是字符串或函数）
 */
type VariableValue = string | (() => string);

/**
 * 变量处理器
 */
export class VariableProcessor implements InputProcessor {
  name = 'VariableProcessor';
  
  private variables = new Map<string, VariableValue>();
  
  constructor() {
    this.initializeBuiltinVariables();
  }
  
  /**
   * 初始化内置变量
   */
  private initializeBuiltinVariables(): void {
    // 当前工作目录
    this.registerVariable('pwd', () => process.cwd());
    
    // 当前用户
    this.registerVariable('user', () => process.env.USER || process.env.USERNAME || 'unknown');
    
    // 当前日期时间
    this.registerVariable('date', () => new Date().toISOString());
    
    // 当前日期（短格式）
    this.registerVariable('today', () => new Date().toLocaleDateString());
    
    // 当前时间
    this.registerVariable('time', () => new Date().toLocaleTimeString());
    
    // 主目录
    this.registerVariable('home', () => process.env.HOME || process.env.USERPROFILE || '~');
    
    // 操作系统
    this.registerVariable('os', () => process.platform);
  }
  
  /**
   * 注册变量
   */
  registerVariable(name: string, value: VariableValue): void {
    this.variables.set(name.toLowerCase(), value);
  }
  
  /**
   * 获取变量值
   */
  getVariable(name: string): string | undefined {
    const value = this.variables.get(name.toLowerCase());
    if (!value) return undefined;
    
    return typeof value === 'function' ? value() : value;
  }
  
  /**
   * 判断是否可以处理
   */
  canHandle(input: string): boolean {
    // 必须包含有效的变量引用（$后面跟字母开头的标识符）
    return /\$[a-zA-Z]\w*/.test(input);
  }
  
  /**
   * 处理变量替换
   */
  async process(input: string, context: ProcessorContext): Promise<ProcessorResult> {
    // 查找所有变量引用
    const regex = /\$(\w+)/g;
    const matches = [...input.matchAll(regex)];
    
    if (matches.length === 0) {
      return { type: 'skip' };
    }
    
    context.logger.info(`[VariableProcessor] Found ${matches.length} variable references`);
    
    let processed = input;
    let hasUnknownVariables = false;
    const replacements: Array<{ variable: string; value: string }> = [];
    
    // 替换所有已知变量
    for (const match of matches) {
      const fullMatch = match[0]; // $variable
      const varName = match[1];   // variable
      const value = this.getVariable(varName);
      
      if (value !== undefined) {
        processed = processed.replace(fullMatch, value);
        replacements.push({ variable: varName, value });
        context.logger.info(`[VariableProcessor] Replaced $${varName} with ${value}`);
      } else {
        hasUnknownVariables = true;
        context.logger.info(`[VariableProcessor] Unknown variable: $${varName}`);
      }
    }
    
    // 如果有未知变量，可能是 DevIns 变量，委托给编译器
    if (hasUnknownVariables) {
      context.logger.info('[VariableProcessor] Has unknown variables, delegating to compiler');
      return { type: 'compile', devins: input };
    }
    
    // 如果有替换发生，继续路由处理后的输入
    if (replacements.length > 0) {
      context.logger.info('[VariableProcessor] Variables replaced, continuing to LLM');
      return { type: 'llm-query', query: processed };
    }
    
    // 没有任何变化，跳过
    return { type: 'skip' };
  }
  
  /**
   * 获取所有可用变量
   */
  getAvailableVariables(): string[] {
    return Array.from(this.variables.keys());
  }
}
