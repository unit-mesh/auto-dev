/**
 * LLMService - Wrapper around mpp-core's KoogLLMService for browser
 * Based on mpp-ui/src/jsMain/typescript/agents/LLMService.ts
 */

import type { ModelConfig } from './ConfigService';
// @ts-ignore - Kotlin/JS generated module
import * as mppCore from '@autodev/mpp-core';

const { cc: KotlinCC } = mppCore;

// Provider type mapping
const ProviderTypes: Record<string, string> = {
  'openai': 'OPENAI',
  'anthropic': 'ANTHROPIC',
  'google': 'GOOGLE',
  'deepseek': 'DEEPSEEK',
  'ollama': 'OLLAMA',
  'openrouter': 'OPENROUTER'
};

export interface Message {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

/**
 * LLM Service wrapper for browser
 */
export class LLMService {
  private koogService: any;
  private chatHistory: Message[] = [];

  constructor(private config: ModelConfig) {
    try {
      // Create JsModelConfig
      const modelConfig = new KotlinCC.unitmesh.llm.JsModelConfig(
        ProviderTypes[this.config.provider.toLowerCase()],
        this.config.model,
        this.config.apiKey,
        this.config.temperature || 0.7,
        this.config.maxTokens || 8192,
        this.config.baseUrl || ''
      );

      // Create JsKoogLLMService
      this.koogService = new KotlinCC.unitmesh.llm.JsKoogLLMService(modelConfig);

      console.log(`✓ LLM Service initialized: ${this.config.provider}/${this.config.model}`);
    } catch (error) {
      console.error('Failed to initialize LLM Service:', error);
      throw error;
    }
  }

  /**
   * Send a message and receive streaming response
   */
  async streamMessage(
    message: string,
    onChunk: (chunk: string) => void,
    onError?: (error: any) => void,
    onComplete?: () => void
  ): Promise<void> {
    // Add user message to history
    this.chatHistory.push({ role: 'user', content: message });

    try {
      // Convert history to JsMessage format (exclude current message)
      const historyMessages = this.chatHistory.slice(0, -1).map(msg =>
        new KotlinCC.unitmesh.llm.JsMessage(msg.role, msg.content)
      );

      let fullResponse = '';
      let streamError: any = null;

      // Call streamPrompt with callbacks
      await this.koogService.streamPrompt(
        message,
        historyMessages,
        (chunk: string) => {
          fullResponse += chunk;
          onChunk(chunk);
        },
        (error: any) => {
          streamError = error;
          if (onError) {
            onError(error);
          }
        },
        () => {
          if (onComplete) {
            onComplete();
          }
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
   * Format LLM error into user-friendly message
   */
  private formatLLMError(error: any): Error {
    const errorMessage = error?.message || String(error);

    // Check for common error patterns
    if (errorMessage.includes('401') || errorMessage.includes('Unauthorized')) {
      return new Error(`❌ API Key invalid or expired\nPlease check your API key in configuration`);
    }

    if (errorMessage.includes('403') || errorMessage.includes('Forbidden')) {
      return new Error(`❌ API access denied\nPlease check API key permissions or account balance`);
    }

    if (errorMessage.includes('429') || errorMessage.includes('rate limit')) {
      return new Error(`❌ API rate limit exceeded\nPlease try again later or upgrade your API quota`);
    }

    if (errorMessage.includes('timeout') || errorMessage.includes('ETIMEDOUT')) {
      return new Error(`❌ Request timeout\nPlease check your network connection or try again later`);
    }

    if (errorMessage.includes('ECONNREFUSED') || errorMessage.includes('connection refused')) {
      return new Error(`❌ Cannot connect to API server\nPlease check your network connection${this.config.baseUrl ? ` and baseUrl: ${this.config.baseUrl}` : ''}`);
    }

    if (errorMessage.includes('ENOTFOUND') || errorMessage.includes('getaddrinfo')) {
      return new Error(`❌ Cannot resolve API server address\nPlease check your network connection and DNS settings`);
    }

    // Default error message
    return new Error(`❌ LLM call failed: ${errorMessage}\n\nTips: Check your configuration:\n- provider: ${this.config.provider}\n- model: ${this.config.model}\n- apiKey: ${this.config.apiKey.substring(0, 8)}...`);
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
  getHistory(): Message[] {
    return [...this.chatHistory];
  }

  /**
   * Validate configuration by making a test call
   */
  async validateConfig(): Promise<{ success: boolean; message: string }> {
    try {
      const testMessage = "Say 'OK' if you can hear me.";
      let response = '';

      await this.streamMessage(
        testMessage,
        (chunk) => { response += chunk; },
        undefined,
        undefined
      );

      // Remove test messages from history
      this.chatHistory.pop(); // assistant
      this.chatHistory.pop(); // user

      return {
        success: true,
        message: `✓ Configuration validated successfully!\nResponse: ${response}`
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.message || 'Configuration validation failed'
      };
    }
  }
}

