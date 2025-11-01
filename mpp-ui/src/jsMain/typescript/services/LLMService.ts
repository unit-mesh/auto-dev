/**
 * LLMService - Wrapper around mpp-core's KoogLLMService
 *
 * This service provides a TypeScript-friendly interface to the Kotlin/JS
 * compiled mpp-core module.
 */

import type { LegacyConfig } from '../config/ConfigManager.js';

// Import the compiled Kotlin/JS module
// @ts-ignore - Kotlin/JS generated module
import MppCore from '@autodev/mpp-core';

// Access the exported Kotlin/JS classes
const { JsKoogLLMService, JsModelConfig, JsMessage } = MppCore.cc.unitmesh.llm;

// Provider type mapping
const ProviderTypes: Record<string, any> = {
  'openai': 'OPENAI',
  'anthropic': 'ANTHROPIC',
  'google': 'GOOGLE',
  'deepseek': 'DEEPSEEK',
  'ollama': 'OLLAMA',
  'openrouter': 'OPENROUTER'
};

/**
 * LLM Service wrapper
 */
export class LLMService {
  private koogService: any;
  private chatHistory: Array<{ role: string; content: string }> = [];

  constructor(private config: LegacyConfig) {
    // Create JsModelConfig
    const modelConfig = new JsModelConfig(
      ProviderTypes[this.config.provider.toLowerCase()],
      this.config.model,
      this.config.apiKey,
      0.7, // temperature
      4096, // maxTokens
      this.config.baseUrl || ''
    );

    // Create JsKoogLLMService
    this.koogService = new JsKoogLLMService(modelConfig);

    console.log(`✓ LLM Service initialized: ${this.config.provider}/${this.config.model}`);
  }

  /**
   * Send a message and receive streaming response
   */
  async streamMessage(
    message: string,
    onChunk: (chunk: string) => void
  ): Promise<void> {
    // Add user message to history
    this.chatHistory.push({ role: 'user', content: message });

    try {
      // Convert history to JsMessage format (exclude current message)
      const historyMessages = this.chatHistory.slice(0, -1).map(msg =>
        new JsMessage(msg.role, msg.content)
      );

      let fullResponse = '';
      let streamError: any = null;

      // Call streamPrompt with callbacks - it now returns a Promise
      await this.koogService.streamPrompt(
        message,
        historyMessages,
        (chunk: string) => {
          fullResponse += chunk;
          onChunk(chunk);
        },
        (error: any) => {
          // Capture error from error callback
          streamError = error;
        },
        () => {
          // Streaming completed
        }
      );

      // If error callback was called, throw the error
      if (streamError) {
        throw this.formatLLMError(streamError);
      }

      // Add assistant response to history
      this.chatHistory.push({ role: 'assistant', content: fullResponse });

    } catch (error) {
      // Remove the user message from history if request failed
      this.chatHistory.pop();

      console.error('Error in streamMessage:', error);
      throw this.formatLLMError(error);
    }
  }

  /**
   * Send a message with system prompt and receive streaming response
   */
  async streamMessageWithSystem(
    systemPrompt: string,
    userMessage: string,
    onChunk: (chunk: string) => void
  ): Promise<void> {
    // Add system message if this is the first message
    if (this.chatHistory.length === 0) {
      this.chatHistory.push({ role: 'system', content: systemPrompt });
    }

    // Add user message to history
    this.chatHistory.push({ role: 'user', content: userMessage });

    try {
      // Convert history to JsMessage format (exclude current message)
      const historyMessages = this.chatHistory.slice(0, -1).map(msg =>
        new JsMessage(msg.role, msg.content)
      );

      let fullResponse = '';
      let streamError: any = null;

      // Call streamPrompt with callbacks - it now returns a Promise
      await this.koogService.streamPrompt(
        userMessage,
        historyMessages,
        (chunk: string) => {
          fullResponse += chunk;
          onChunk(chunk);
        },
        (error: any) => {
          // Capture error from error callback
          streamError = error;
        },
        () => {
          // Streaming completed
        }
      );

      // If error callback was called, throw the error
      if (streamError) {
        throw this.formatLLMError(streamError);
      }

      // Add assistant response to history
      this.chatHistory.push({ role: 'assistant', content: fullResponse });

    } catch (error) {
      // Remove the user message from history if request failed
      this.chatHistory.pop();

      console.error('Error in streamMessageWithSystem:', error);
      throw this.formatLLMError(error);
    }
  }

  /**
   * Format LLM error into user-friendly message
   */
  private formatLLMError(error: any): Error {
    const errorMessage = error?.message || String(error);
    
    // Check for common error patterns
    if (errorMessage.includes('401') || errorMessage.includes('Unauthorized')) {
      return new Error(`❌ API Key 无效或已过期\n请检查配置文件 ~/.autodev/config.yaml 中的 apiKey`);
    }
    
    if (errorMessage.includes('403') || errorMessage.includes('Forbidden')) {
      return new Error(`❌ API 访问被拒绝\n请检查 API Key 权限或账户余额`);
    }
    
    if (errorMessage.includes('429') || errorMessage.includes('rate limit')) {
      return new Error(`❌ API 请求频率超限\n请稍后再试或升级您的 API 配额`);
    }
    
    if (errorMessage.includes('timeout') || errorMessage.includes('ETIMEDOUT')) {
      return new Error(`❌ 请求超时\n请检查网络连接或稍后再试`);
    }
    
    if (errorMessage.includes('ECONNREFUSED') || errorMessage.includes('connection refused')) {
      return new Error(`❌ 无法连接到 API 服务器\n请检查网络连接${this.config.baseUrl ? ` 和 baseUrl: ${this.config.baseUrl}` : ''}`);
    }
    
    if (errorMessage.includes('ENOTFOUND') || errorMessage.includes('getaddrinfo')) {
      return new Error(`❌ 无法解析 API 服务器地址\n请检查网络连接和 DNS 设置`);
    }
    
    // Default error message
    return new Error(`❌ LLM 调用失败: ${errorMessage}\n\n提示：检查 ~/.autodev/config.yaml 中的配置\n- provider: ${this.config.provider}\n- model: ${this.config.model}\n- apiKey: ${this.config.apiKey.substring(0, 8)}...`);
  }

  /**
   * Clear chat history
   */
  clearHistory(): void {
    this.chatHistory = [];
    console.log('✓ Chat history cleared');
  }

  /**
   * Get chat history
   */
  getHistory(): Array<{ role: string; content: string }> {
    return [...this.chatHistory];
  }

}

