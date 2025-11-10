/**
 * TUI Renderer - TUI 环境的渲染器
 *
 * 适配 CodingAgent 的渲染接口到 TUI 环境
 * 实现 JsCodingAgentRenderer 接口
 *
 * 特性：
 * - 智能输出截断（默认20行）
 * - 工具输出格式化和摘要
 * - 减少冗余日志
 */

import type { ModeContext } from '../../modes';
import type { Message } from '../../ui/App.js';
import {BaseRenderer} from "./BaseRenderer.js";

/**
 * TUI 渲染器
 * 实现 Kotlin CodingAgent 期望的 JsCodingAgentRenderer 接口
 */
export class TuiRenderer extends BaseRenderer {
  protected outputContent(content: string): void {

  }
  protected outputNewline(): void {

  }

  // Required by Kotlin JS export interface
  readonly __doNotUseOrImplementIt: any = {};

  private context: ModeContext;
  private currentMessage: Message | null = null;
  private lastIterationNumber = 0;

  // Configuration - 可通过环境变量调整
  private readonly MAX_OUTPUT_LINES = parseInt(process.env.AUTODEV_MAX_OUTPUT_LINES || '20');
  private readonly MAX_LINE_LENGTH = parseInt(process.env.AUTODEV_MAX_LINE_LENGTH || '120');
  private readonly SHOW_ITERATION_HEADERS = process.env.AUTODEV_SHOW_ITERATIONS === 'true';
  private readonly VERBOSE_MODE = process.env.AUTODEV_VERBOSE === 'true';

  constructor(context: ModeContext) {
    super();
    this.context = context;
  }

  // JsCodingAgentRenderer interface implementation

  /**
   * 渲染迭代头部 - 只在重要时显示
   */
  renderIterationHeader(current: number, max: number): void {
    // 只在第一次或每5次迭代时显示，减少冗余
    if (!this.SHOW_ITERATION_HEADERS) {
      return;
    }

    if (current === 1 || current % 5 === 0 || current === max) {
      const message = `🔄 **Iteration ${current}/${max}**`;
      this.renderSystemMessage(message);
    }
    this.lastIterationNumber = current;
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
   * 渲染工具调用 - 简化显示
   */
  renderToolCall(toolName: string, paramsStr: string): void {
    // 解析参数以提供更友好的显示
    let params = '';
    try {
      const parsed = JSON.parse(paramsStr);
      params = this.formatToolParams(toolName, parsed);
    } catch {
      params = paramsStr;
    }

    const message = `🔧 **${toolName}** ${params}`;
    this.renderSystemMessage(message);
  }

  /**
   * 渲染工具调用结果 - 智能截断和格式化
   */
  renderToolResult(toolName: string, success: boolean, output: string | null, fullOutput: string | null): void {
    const icon = success ? '✅' : '❌';
    const resultText = output || fullOutput || 'No output';

    if (!success) {
      // 错误信息保持简短
      const errorText = this.truncateText(resultText, 3);
      const message = `${icon} **${toolName}** failed\n\`\`\`\n${errorText}\n\`\`\``;
      this.renderSystemMessage(message);
      return;
    }

    // 成功结果根据工具类型智能处理
    const formattedResult = this.formatToolResult(toolName, resultText);
    const message = `${icon} **${toolName}** ${formattedResult}`;
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
   * 渲染最终结果 - 简化显示
   */
  renderFinalResult(success: boolean, message: string, iterations: number): void {
    const icon = success ? '✅' : '❌';
    // 只在多次迭代时显示迭代数
    const iterationInfo = iterations > 1 ? ` (${iterations} iterations)` : '';
    const resultMessage = `${icon} **Task ${success ? 'completed' : 'failed'}**${iterationInfo}\n\n${this.truncateText(message, 10)}`;
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
   * 渲染重复警告 - 简化显示
   */
  renderRepeatWarning(toolName: string, count: number): void {
    const message = `⚠️  **${toolName}** called ${count} times - consider different approach`;
    this.renderSystemMessage(message);
  }

  /**
   * 渲染恢复建议 - 简化显示
   */
  renderRecoveryAdvice(recoveryAdvice: string): void {
    const message = `💡 **Suggestion**: ${this.truncateText(recoveryAdvice, 3)}`;
    this.renderSystemMessage(message);
  }

  /**
   * 渲染用户确认请求
   */
  renderUserConfirmationRequest(toolName: string, params: Record<string, any>): void {
    const paramStr = Object.entries(params)
      .map(([k, v]) => `${k}=${JSON.stringify(v)}`)
      .join(', ');

    const message = `🔐 Tool '${toolName}' needs approval: ${paramStr} (Auto-approved)`;
    this.renderSystemMessage(message);
  }

  // Helper methods

  /**
   * 格式化工具参数显示
   */
  private formatToolParams(toolName: string, params: any): string {
    switch (toolName) {
      case 'read-file':
        return `\`${params.path || params.file || ''}\``;
      case 'write-file':
        return `\`${params.path || params.file || ''}\``;
      case 'list-files':
        return `\`${params.path || '.'}\`${params.recursive ? ' (recursive)' : ''}`;
      case 'grep':
        return `"${params.pattern || params.query || ''}" in \`${params.path || '.'}\``;
      case 'shell':
        return `\`${params.command || ''}\``;
      default:
        return Object.keys(params).length > 0 ? `(${Object.keys(params).join(', ')})` : '';
    }
  }

  /**
   * 格式化工具结果显示
   */
  private formatToolResult(toolName: string, output: string): string {
    const lines = output.split('\n');
    const totalLines = lines.length;

    switch (toolName) {
      case 'list-files':
        if (totalLines > this.MAX_OUTPUT_LINES) {
          const preview = lines.slice(0, this.MAX_OUTPUT_LINES).join('\n');
          return `found ${totalLines} files\n\`\`\`\n${preview}\n... (${totalLines - this.MAX_OUTPUT_LINES} more files)\n\`\`\``;
        }
        return `found ${totalLines} files\n\`\`\`\n${output}\n\`\`\``;

      case 'read-file':
        if (totalLines > this.MAX_OUTPUT_LINES) {
          const preview = lines.slice(0, this.MAX_OUTPUT_LINES).join('\n');
          return `(${totalLines} lines)\n\`\`\`\n${preview}\n... (${totalLines - this.MAX_OUTPUT_LINES} more lines)\n\`\`\``;
        }
        return `(${totalLines} lines)\n\`\`\`\n${output}\n\`\`\``;

      case 'grep':
        const matches = lines.filter(line => line.trim());
        if (matches.length > this.MAX_OUTPUT_LINES) {
          const preview = matches.slice(0, this.MAX_OUTPUT_LINES).join('\n');
          return `found ${matches.length} matches\n\`\`\`\n${preview}\n... (${matches.length - this.MAX_OUTPUT_LINES} more matches)\n\`\`\``;
        }
        return `found ${matches.length} matches\n\`\`\`\n${output}\n\`\`\``;

      case 'shell':
        if (totalLines > this.MAX_OUTPUT_LINES) {
          const preview = this.truncateText(output, this.MAX_OUTPUT_LINES);
          return `completed\n\`\`\`\n${preview}\n\`\`\``;
        }
        return `completed\n\`\`\`\n${output}\n\`\`\``;

      case 'write-file':
        return 'completed';

      default:
        // 在 verbose 模式下显示更多信息
        const maxLines = this.VERBOSE_MODE ? this.MAX_OUTPUT_LINES * 2 : this.MAX_OUTPUT_LINES;
        return this.truncateText(output, maxLines);
    }
  }

  /**
   * 截断文本到指定行数
   */
  private truncateText(text: string, maxLines: number): string {
    const lines = text.split('\n');
    if (lines.length <= maxLines) {
      return text;
    }

    const truncated = lines.slice(0, maxLines).join('\n');
    const remaining = lines.length - maxLines;
    return `${truncated}\n... (${remaining} more lines)`;
  }

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
