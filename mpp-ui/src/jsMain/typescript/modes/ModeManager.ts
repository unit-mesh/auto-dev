/**
 * Mode Manager - 模式管理器
 * 
 * 负责管理不同的交互模式，包括注册、切换、状态管理等
 */

import type { Mode, ModeFactory, ModeContext } from './Mode.js';

/**
 * 模式切换事件
 */
export interface ModeChangeEvent {
  /** 之前的模式 */
  previousMode: string | null;
  /** 当前模式 */
  currentMode: string;
  /** 切换时间戳 */
  timestamp: number;
}

/**
 * 模式管理器
 */
export class ModeManager {
  private factories = new Map<string, ModeFactory>();
  private currentMode: Mode | null = null;
  private currentModeType: string | null = null;
  private context: ModeContext | null = null;
  private changeListeners: Array<(event: ModeChangeEvent) => void> = [];

  /**
   * 注册模式工厂
   */
  registerMode(factory: ModeFactory): void {
    this.factories.set(factory.type, factory);
  }

  /**
   * 获取所有已注册的模式类型
   */
  getAvailableModes(): string[] {
    return Array.from(this.factories.keys());
  }

  /**
   * 获取模式信息
   */
  getModeInfo(type: string): { name: string; displayName: string; description: string; icon: string } | null {
    const factory = this.factories.get(type);
    if (!factory) return null;
    
    // 创建临时实例获取信息
    const tempMode = factory.createMode();
    return {
      name: tempMode.name,
      displayName: tempMode.displayName,
      description: tempMode.description,
      icon: tempMode.icon
    };
  }

  /**
   * 切换到指定模式
   */
  async switchToMode(type: string, context: ModeContext): Promise<boolean> {
    const factory = this.factories.get(type);
    if (!factory) {
      context.logger.error(`[ModeManager] Unknown mode type: ${type}`);
      return false;
    }

    const previousModeType = this.currentModeType;

    try {
      // 清理当前模式
      if (this.currentMode) {
        context.logger.info(`[ModeManager] Cleaning up mode: ${this.currentModeType}`);
        await this.currentMode.cleanup();
      }

      // 创建新模式
      context.logger.info(`[ModeManager] Switching to mode: ${type}`);
      const newMode = factory.createMode();
      
      // 初始化新模式
      await newMode.initialize(context);
      
      // 更新状态
      this.currentMode = newMode;
      this.currentModeType = type;
      this.context = context;

      // 触发切换事件
      const event: ModeChangeEvent = {
        previousMode: previousModeType,
        currentMode: type,
        timestamp: Date.now()
      };
      
      this.changeListeners.forEach(listener => {
        try {
          listener(event);
        } catch (error) {
          context.logger.error('[ModeManager] Error in mode change listener:', error);
        }
      });

      context.logger.info(`[ModeManager] Successfully switched to mode: ${type}`);
      return true;

    } catch (error) {
      context.logger.error(`[ModeManager] Failed to switch to mode ${type}:`, error);
      return false;
    }
  }

  /**
   * 处理用户输入
   */
  async handleInput(input: string): Promise<boolean> {
    if (!this.currentMode || !this.context) {
      this.context?.logger.error('[ModeManager] No active mode to handle input');
      return false;
    }

    try {
      const result = await this.currentMode.handleInput(input, this.context);
      return result.success;
    } catch (error) {
      this.context.logger.error('[ModeManager] Error handling input:', error);
      return false;
    }
  }

  /**
   * 获取当前模式
   */
  getCurrentMode(): { type: string; mode: Mode } | null {
    if (!this.currentMode || !this.currentModeType) {
      return null;
    }
    return {
      type: this.currentModeType,
      mode: this.currentMode
    };
  }

  /**
   * 获取当前模式状态
   */
  getCurrentModeStatus(): string {
    if (!this.currentMode) {
      return 'No active mode';
    }
    return this.currentMode.getStatus();
  }

  /**
   * 添加模式切换监听器
   */
  onModeChange(listener: (event: ModeChangeEvent) => void): void {
    this.changeListeners.push(listener);
  }

  /**
   * 移除模式切换监听器
   */
  removeModeChangeListener(listener: (event: ModeChangeEvent) => void): void {
    const index = this.changeListeners.indexOf(listener);
    if (index >= 0) {
      this.changeListeners.splice(index, 1);
    }
  }

  /**
   * 清理所有资源
   */
  async cleanup(): Promise<void> {
    if (this.currentMode) {
      await this.currentMode.cleanup();
      this.currentMode = null;
      this.currentModeType = null;
    }
    this.context = null;
    this.changeListeners = [];
  }
}
