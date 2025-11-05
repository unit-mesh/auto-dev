/**
 * Mode Interface - 交互模式接口
 * 
 * 定义了不同交互模式（Agent、Chat等）的统一接口
 */

import type { Message } from '../ui/App.js';

/**
 * 模式处理结果
 */
export interface ModeResult {
  /** 是否成功处理 */
  success: boolean;
  /** 错误信息（如果有） */
  error?: string;
  /** 输出消息（如果有） */
  message?: string;
}

/**
 * 模式上下文
 * 包含模式执行所需的依赖和回调
 */
export interface ModeContext {
  /** 添加消息到历史 */
  addMessage: (message: Message) => void;
  
  /** 设置待处理消息（用于流式输出） */
  setPendingMessage: (message: Message | null) => void;
  
  /** 设置编译状态 */
  setIsCompiling: (compiling: boolean) => void;
  
  /** 清空消息历史 */
  clearMessages: () => void;
  
  /** 日志记录器 */
  logger: {
    info: (message: string) => void;
    warn: (message: string) => void;
    error: (message: string, error?: any) => void;
  };
  
  /** 项目路径 */
  projectPath?: string;
  
  /** LLM 服务配置 */
  llmConfig?: any;
}

/**
 * 交互模式接口
 */
export interface Mode {
  /** 模式名称 */
  readonly name: string;
  
  /** 模式显示名称 */
  readonly displayName: string;
  
  /** 模式描述 */
  readonly description: string;
  
  /** 模式图标/emoji */
  readonly icon: string;
  
  /** 初始化模式 */
  initialize(context: ModeContext): Promise<void>;
  
  /** 处理用户输入 */
  handleInput(input: string, context: ModeContext): Promise<ModeResult>;
  
  /** 清理模式资源 */
  cleanup(): Promise<void>;
  
  /** 获取模式状态信息 */
  getStatus(): string;
}

/**
 * 模式工厂接口
 */
export interface ModeFactory {
  /** 创建模式实例 */
  createMode(): Mode;
  
  /** 模式类型标识 */
  readonly type: string;
}
