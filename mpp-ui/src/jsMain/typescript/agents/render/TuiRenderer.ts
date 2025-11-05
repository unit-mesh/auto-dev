/**
 * TUI Renderer - TUI 环境的渲染器
 *
 * 适配 CodingAgent 的渲染接口到 TUI 环境
 * 实现 JsCodingAgentRenderer 接口
 */

import type { ModeContext } from '../../modes/Mode.js';
import type { Message } from '../../ui/App.js';

/**
 * TUI 渲染器
 * 实现 Kotlin CodingAgent 期望的 JsCodingAgentRenderer 接口
 */
export class TuiRenderer {
  // Required by Kotlin JS export interface
  readonly __doNotUseOrImplementIt: any = {};

  private context: ModeContext;
  private currentMessage: Message | null = null;

  constructor(context: ModeContext) {
    this.context = context;
  }

  // JsCodingAgentRenderer interface implementation

  /**
   * 渲染迭代头部
   */
  renderIterationHeader(current: number, max: number): void {
    const message = `🔄 **Iteration ${current}/${max}**`;
    this.renderSystemMessage(message);
  }

  /**
   * 渲染 LLM 响应开始
   */
  renderLLMResponseStart(): void {
    this.currentMessage = {
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      showPrefix: true
    };
    this.context.setPendingMessage(this.currentMessage);
  }

  /**
   * 渲染 LLM 响应块
   */
  renderLLMResponseChunk(chunk: string): void {
    if (this.currentMessage) {
      this.currentMessage.content += chunk;
      this.context.setPendingMessage({ ...this.currentMessage });
    }
  }

  /**
   * 渲染 LLM 响应结束
   */
  renderLLMResponseEnd(): void {
    if (this.currentMessage) {
      this.context.addMessage(this.currentMessage);
      this.context.setPendingMessage(null);
      this.currentMessage = null;
    }
  }

  /**
   * 渲染工具调用
   */
  renderToolCall(toolName: string, paramsStr: string): void {
    const message = `🔧 **Calling tool**: \`${toolName}\`\n\`\`\`json\n${paramsStr}\n\`\`\``;
    this.renderSystemMessage(message);
  }

  /**
   * 渲染工具调用结果
   */
  renderToolResult(toolName: string, success: boolean, output: string | null, fullOutput: string | null): void {
    const icon = success ? '✅' : '❌';
    const resultText = output || fullOutput || 'No output';
    const message = `${icon} **Tool result**: \`${toolName}\`\n\`\`\`\n${resultText}\n\`\`\``;
    this.renderSystemMessage(message);
  }

  /**
   * 渲染任务完成
   */
  renderTaskComplete(): void {
    const message = '✅ **Task completed**';
    this.renderSystemMessage(message);
  }

  /**
   * 渲染最终结果
   */
  renderFinalResult(success: boolean, message: string, iterations: number): void {
    const icon = success ? '✅' : '❌';
    const resultMessage = `${icon} **Final Result** (${iterations} iterations)\n\n${message}`;
    this.renderSystemMessage(resultMessage);
  }

  /**
   * 渲染错误
   */
  renderError(message: string): void {
    const errorMessage = `❌ **Error**: ${message}`;
    this.renderSystemMessage(errorMessage);
  }

  /**
   * 渲染重复警告
   */
  renderRepeatWarning(toolName: string, count: number): void {
    const message = `⚠️  **Warning**: Tool \`${toolName}\` has been called ${count} times. Consider a different approach.`;
    this.renderSystemMessage(message);
  }

  /**
   * 渲染恢复建议
   */
  renderRecoveryAdvice(recoveryAdvice: string): void {
    const message = `💡 **Recovery Advice**: ${recoveryAdvice}`;
    this.renderSystemMessage(message);
  }

  // Helper methods

  /**
   * 渲染系统消息
   */
  private renderSystemMessage(message: string): void {
    const systemMessage: Message = {
      role: 'system',
      content: message,
      timestamp: Date.now(),
      showPrefix: true
    };
    this.context.addMessage(systemMessage);
  }

  /**
   * 强制停止
   */
  forceStop(): void {
    if (this.currentMessage) {
      this.context.addMessage(this.currentMessage);
      this.context.setPendingMessage(null);
      this.currentMessage = null;
    }
  }
}
