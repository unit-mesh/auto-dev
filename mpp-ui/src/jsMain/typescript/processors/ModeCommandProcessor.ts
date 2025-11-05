/**
 * Mode Command Processor
 * 
 * 处理模式切换命令，如 /chat, /agent 等
 */

import type { InputProcessor, ProcessorContext, ProcessorResult } from './InputRouter.js';
import type { ModeManager } from '../modes/ModeManager.js';

/**
 * 模式切换命令处理器
 */
export class ModeCommandProcessor implements InputProcessor {
  name = 'ModeCommandProcessor';
  
  private modeManager: ModeManager;
  private modeContext: any; // ModeContext 类型

  constructor(modeManager: ModeManager, modeContext: any) {
    this.modeManager = modeManager;
    this.modeContext = modeContext;
  }

  /**
   * 判断是否可以处理
   */
  canHandle(input: string): boolean {
    const trimmed = input.trim().toLowerCase();
    return trimmed === '/chat' || 
           trimmed === '/agent' || 
           trimmed.startsWith('/mode ') ||
           trimmed === '/mode';
  }

  /**
   * 处理模式切换命令
   */
  async process(input: string, context: ProcessorContext): Promise<ProcessorResult> {
    const trimmed = input.trim().toLowerCase();
    
    try {
      if (trimmed === '/chat') {
        // 切换到聊天模式
        const success = await this.modeManager.switchToMode('chat', this.modeContext);
        if (success) {
          return {
            type: 'handled',
            output: '💬 Switched to **Chat Mode**\n\nYou can now have conversations with the AI assistant.'
          };
        } else {
          return {
            type: 'error',
            message: 'Failed to switch to chat mode'
          };
        }
      }
      
      if (trimmed === '/agent') {
        // 切换到代理模式
        const success = await this.modeManager.switchToMode('agent', this.modeContext);
        if (success) {
          return {
            type: 'handled',
            output: '🤖 Switched to **Agent Mode**\n\nDescribe development tasks and I\'ll complete them autonomously.'
          };
        } else {
          return {
            type: 'error',
            message: 'Failed to switch to agent mode'
          };
        }
      }
      
      if (trimmed === '/mode') {
        // 显示当前模式和可用模式
        const currentMode = this.modeManager.getCurrentMode();
        const availableModes = this.modeManager.getAvailableModes();
        
        let output = '🔄 **Mode Information**\n\n';
        
        if (currentMode) {
          const modeInfo = this.modeManager.getModeInfo(currentMode.type);
          output += `**Current Mode**: ${modeInfo?.icon} ${modeInfo?.displayName} (\`${currentMode.type}\`)\n`;
          output += `**Status**: ${currentMode.mode.getStatus()}\n\n`;
        } else {
          output += '**Current Mode**: None\n\n';
        }
        
        output += '**Available Modes**:\n';
        for (const modeType of availableModes) {
          const modeInfo = this.modeManager.getModeInfo(modeType);
          if (modeInfo) {
            output += `- ${modeInfo.icon} **${modeInfo.displayName}** (\`/${modeType}\`) - ${modeInfo.description}\n`;
          }
        }
        
        output += '\n**Usage**: Type `/chat` or `/agent` to switch modes.';
        
        return {
          type: 'handled',
          output
        };
      }
      
      if (trimmed.startsWith('/mode ')) {
        // 切换到指定模式
        const modeType = trimmed.substring(6).trim();
        const availableModes = this.modeManager.getAvailableModes();
        
        if (!availableModes.includes(modeType)) {
          return {
            type: 'error',
            message: `Unknown mode: ${modeType}. Available modes: ${availableModes.join(', ')}`
          };
        }
        
        const success = await this.modeManager.switchToMode(modeType, this.modeContext);
        if (success) {
          const modeInfo = this.modeManager.getModeInfo(modeType);
          return {
            type: 'handled',
            output: `${modeInfo?.icon} Switched to **${modeInfo?.displayName}**\n\n${modeInfo?.description}`
          };
        } else {
          return {
            type: 'error',
            message: `Failed to switch to ${modeType} mode`
          };
        }
      }
      
      return { type: 'skip' };
      
    } catch (error) {
      context.logger.error('[ModeCommandProcessor] Error processing mode command:', error);
      return {
        type: 'error',
        message: `Mode switch error: ${error instanceof Error ? error.message : String(error)}`
      };
    }
  }
}
