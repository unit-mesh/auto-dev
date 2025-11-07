/**
 * Prompt Enhancement Processor - 提示词增强处理器
 * 
 * 在 CLI 模式下自动增强用户输入的提示词
 */

import type { InputProcessor, ProcessorContext, ProcessorResult } from './InputRouter.js';
import { getCurrentProjectPath } from '../utils/domainDictUtils.js';
import { ConfigManager } from '../config/ConfigManager.js';
import * as mppCore from '@autodev/mpp-core';

/**
 * 提示词增强处理器
 * 在 CLI 模式下自动对用户输入进行增强
 */
export class PromptEnhancementProcessor implements InputProcessor {
  name = 'PromptEnhancementProcessor';
  
  private enhancer: any = null;
  private isInitialized = false;
  
  /**
   * 初始化增强器
   */
  private async initializeEnhancer(context: ProcessorContext): Promise<boolean> {
    if (this.isInitialized) return true;
    
    try {
      const projectPath = getCurrentProjectPath();
      if (!projectPath) {
        context.logger.warn('[PromptEnhancementProcessor] No project path available');
        return false;
      }
      
      const config = await ConfigManager.load();
      const activeConfig = config.getActiveConfig();
      if (!activeConfig) {
        context.logger.warn('[PromptEnhancementProcessor] No LLM configuration available');
        return false;
      }

      // Create KoogLLMService
      const modelConfig = new mppCore.cc.unitmesh.llm.JsModelConfig(
        activeConfig.provider,
        activeConfig.model,
        activeConfig.apiKey,
        activeConfig.temperature || 0.7,
        activeConfig.maxTokens || 4096,
        activeConfig.baseUrl || ''
      );

      const llmService = mppCore.cc.unitmesh.llm.JsKoogLLMService.Companion.create(modelConfig);

      // Create file system
      const fileSystem = new mppCore.cc.unitmesh.devins.filesystem.FileSystem(projectPath);

      // Create domain dict service
      const domainDictService = new mppCore.cc.unitmesh.llm.JsDomainDictService(fileSystem);

      // Create prompt enhancer
      this.enhancer = new mppCore.cc.unitmesh.llm.JsPromptEnhancer(
        llmService,
        fileSystem,
        domainDictService
      );
      
      this.isInitialized = true;
      context.logger.info('[PromptEnhancementProcessor] Initialized successfully');
      return true;
      
    } catch (error) {
      context.logger.error('[PromptEnhancementProcessor] Failed to initialize:', error);
      return false;
    }
  }
  
  /**
   * 判断是否可以处理该输入
   * CLI 模式下，对所有非命令输入进行增强
   */
  canHandle(input: string): boolean {
    const trimmed = input.trim();
    
    // 跳过空输入
    if (!trimmed) return false;
    
    // 跳过命令（以 / 或 @ 开头）
    if (trimmed.startsWith('/') || trimmed.startsWith('@')) return false;
    
    // 跳过太短的输入（可能是简单回复）
    if (trimmed.length < 10) return false;
    
    // 跳过已经很详细的输入（超过 200 字符可能已经足够详细）
    if (trimmed.length > 200) return false;
    
    return true;
  }
  
  /**
   * 处理输入 - 增强提示词
   */
  async process(input: string, context: ProcessorContext): Promise<ProcessorResult> {
    try {
      // 初始化增强器
      const initialized = await this.initializeEnhancer(context);
      if (!initialized) {
        context.logger.warn('[PromptEnhancementProcessor] Enhancer not available, skipping');
        return { type: 'skip' };
      }
      
      context.logger.info('[PromptEnhancementProcessor] Enhancing prompt...');
      context.setLoading?.(true);
      
      // 显示增强过程
      context.addMessage?.('system', '🔍 正在增强您的提示词...');
      
      // 调用增强器
      const enhanced = await this.enhancer.enhance(input.trim(), 'zh');
      
      context.setLoading?.(false);
      
      // 检查是否真的增强了
      if (enhanced && enhanced !== input.trim() && enhanced.length > input.trim().length) {
        context.logger.info(`[PromptEnhancementProcessor] Enhanced: "${input.trim()}" -> "${enhanced}"`);
        
        // 显示增强结果
        context.addMessage?.('system', `✨ 增强后的提示词：\n${enhanced}`);
        
        // 返回增强后的查询
        return {
          type: 'llm-query',
          query: enhanced
        };
      } else {
        context.logger.info('[PromptEnhancementProcessor] No enhancement needed or failed');
        
        // 没有增强，继续使用原始输入
        return { type: 'skip' };
      }
      
    } catch (error) {
      context.setLoading?.(false);
      context.logger.error('[PromptEnhancementProcessor] Enhancement failed:', error);
      
      // 增强失败，继续使用原始输入
      context.addMessage?.('system', '⚠️ 提示词增强失败，使用原始输入');
      return { type: 'skip' };
    }
  }
}
