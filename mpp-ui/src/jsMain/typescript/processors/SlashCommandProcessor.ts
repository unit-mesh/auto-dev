/**
 * Slash Command Processor
 * 
 * 处理以 / 开头的命令，如 /help, /clear, /exit 等
 * 参考 Gemini CLI 的 slashCommandProcessor.ts
 */

import type { InputProcessor, ProcessorContext, ProcessorResult } from './InputRouter.js';
import { HELP_TEXT, GOODBYE_MESSAGE } from '../constants/asciiArt.js';
import { t } from '../i18n/index.js';

/**
 * 命令定义
 */
export interface SlashCommand {
  /** 命令描述 */
  description: string;
  
  /** 命令别名 */
  aliases?: string[];
  
  /** 命令执行函数 */
  action: (context: ProcessorContext, args: string) => Promise<ProcessorResult>;
}

/**
 * Slash 命令处理器
 */
export class SlashCommandProcessor implements InputProcessor {
  name = 'SlashCommandProcessor';
  
  private commands = new Map<string, SlashCommand>();
  
  constructor() {
    this.initializeBuiltinCommands();
  }
  
  /**
   * 初始化内置命令
   */
  private initializeBuiltinCommands(): void {
    // /help - 显示帮助
    this.registerCommand('help', {
      description: t('commands.help.description'),
      aliases: ['h', '?'],
      action: async () => ({
        type: 'handled',
        output: HELP_TEXT
      })
    });
    
    // /clear - 清空历史
    this.registerCommand('clear', {
      description: t('commands.clear.description'),
      aliases: ['cls'],
      action: async (context) => {
        if (context.clearMessages) {
          context.clearMessages();
        }
        return { type: 'handled', output: t('commands.clear.success') };
      }
    });
    
    // /exit - 退出程序
    this.registerCommand('exit', {
      description: t('commands.exit.description'),
      aliases: ['quit', 'q'],
      action: async () => {
        console.log(GOODBYE_MESSAGE);
        process.exit(0);
        return { type: 'handled' };
      }
    });
    
    // /config - 显示配置
    this.registerCommand('config', {
      description: t('commands.config.description'),
      action: async () => ({
        type: 'handled',
        output: t('commands.config.output', { model: 'DeepSeek' })
      })
    });
    
    // /model - 切换模型（占位）
    this.registerCommand('model', {
      description: t('commands.model.description'),
      action: async (context, args) => ({
        type: 'handled',
        output: `${t('commands.model.available', { models: 'deepseek, claude, gpt' })}\n${t('commands.model.current', { model: 'deepseek' })}\n\n${t('commands.model.usage')}`
      })
    });
  }
  
  /**
   * 注册命令
   */
  registerCommand(name: string, command: SlashCommand): void {
    this.commands.set(name.toLowerCase(), command);
    
    // 注册别名
    if (command.aliases) {
      for (const alias of command.aliases) {
        this.commands.set(alias.toLowerCase(), command);
      }
    }
  }
  
  /**
   * 获取所有命令
   */
  getCommands(): Map<string, SlashCommand> {
    return this.commands;
  }
  
  /**
   * 判断是否可以处理
   */
  canHandle(input: string): boolean {
    return input.trim().startsWith('/');
  }
  
  /**
   * 处理命令
   */
  async process(input: string, context: ProcessorContext): Promise<ProcessorResult> {
    const trimmed = input.trim();
    
    // 提取命令名和参数
    const parts = trimmed.substring(1).trim().split(/\s+/);
    const commandName = parts[0]?.toLowerCase() || '';
    const args = parts.slice(1).join(' ');
    
    if (!commandName) {
      return {
        type: 'error',
        message: t('commands.usage')
      };
    }
    
    // 查找命令
    const command = this.commands.get(commandName);
    
    if (!command) {
      // 未知命令，可能是 DevIns 命令（如 /file:, /symbol:）
      // 委托给 Kotlin 编译器处理
      context.logger.info(`[SlashCommandProcessor] Unknown command: ${commandName}, delegating to compiler`);
      return { type: 'compile', devins: trimmed };
    }
    
    // 执行命令
    try {
      const result = await command.action(context, args);
      
      // 如果有输出，打印到控制台
      if (result.type === 'handled' && result.output) {
        console.log(result.output);
      }
      
      return result;
    } catch (error) {
      context.logger.error(`[SlashCommandProcessor] Error executing ${commandName}:`, error);
      return {
        type: 'error',
        message: t('commands.executionError', { error: error instanceof Error ? error.message : String(error) })
      };
    }
  }
}
