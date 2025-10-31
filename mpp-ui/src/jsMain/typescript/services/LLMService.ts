/**
 * LLMService - Wrapper around mpp-core's KoogLLMService
 *
 * This service provides a TypeScript-friendly interface to the Kotlin/JS
 * compiled mpp-core module.
 */

import type { AutoDevConfig } from '../config/ConfigManager.js';

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

  constructor(private config: AutoDevConfig) {
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

      // Call streamPrompt with callbacks - it now returns a Promise
      await this.koogService.streamPrompt(
        message,
        historyMessages,
        (chunk: string) => {
          fullResponse += chunk;
          onChunk(chunk);
        },
        (error: any) => {
          throw error;
        },
        () => {
          // Streaming completed
        }
      );

      // Add assistant response to history
      this.chatHistory.push({ role: 'assistant', content: fullResponse });

    } catch (error) {
      console.error('Error in streamMessage:', error);
      throw error;
    }
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

