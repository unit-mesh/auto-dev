/**
 * Base TypeScript renderer implementing JsCodingAgentRenderer interface
 * Provides common functionality for all TypeScript renderer implementations
 *
 * This mirrors the Kotlin BaseRenderer from mpp-core.
 * All TypeScript renderers (CliRenderer, ServerRenderer, TuiRenderer) should extend this class.
 *
 * @see mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/render/BaseRenderer.kt
 * @see mpp-core/src/jsMain/kotlin/cc/unitmesh/agent/RendererExports.kt - JsCodingAgentRenderer interface
 */

import {cc} from "autodev-mpp-core/autodev-mpp-core";
import JsCodingAgentRenderer = cc.unitmesh.agent.JsCodingAgentRenderer;
import JsPlanSummaryData = cc.unitmesh.agent.JsPlanSummaryData;

export abstract class BaseRenderer implements JsCodingAgentRenderer {
  // Required by Kotlin JS export interface
  readonly __doNotUseOrImplementIt: any = {};

  protected reasoningBuffer: string = '';
  protected isInDevinBlock: boolean = false;
  protected lastIterationReasoning: string = '';
  protected consecutiveRepeats: number = 0;
  protected lastOutputLength: number = 0;

  /**
   * Filter out devin blocks and thinking blocks from content
   */
  protected filterDevinBlocks(content: string): string {
    // Remove all complete devin blocks
    let filtered = content.replace(/<devin[^>]*>[\s\S]*?<\/devin>/g, '');

    // Remove all complete thinking blocks
    filtered = filtered.replace(/<thinking>[\s\S]*?<\/thinking>/g, '');

    // Handle incomplete devin blocks at the end - remove them completely
    const openDevinIndex = filtered.lastIndexOf('<devin');
    if (openDevinIndex !== -1) {
      const closeDevinIndex = filtered.indexOf('</devin>', openDevinIndex);
      if (closeDevinIndex === -1) {
        // Incomplete devin block, remove it
        filtered = filtered.substring(0, openDevinIndex);
      }
    }

    // Handle incomplete thinking blocks at the end - remove them completely
    const openThinkingIndex = filtered.lastIndexOf('<thinking');
    if (openThinkingIndex !== -1) {
      const closeThinkingIndex = filtered.indexOf('</thinking>', openThinkingIndex);
      if (closeThinkingIndex === -1) {
        // Incomplete thinking block, remove it
        filtered = filtered.substring(0, openThinkingIndex);
      }
    }

    // Also remove partial devin/thinking tags at the end and any standalone '<' that might be part of a tag
    const partialTagPattern = /<(?:de(?:v(?:i(?:n)?)?)?|th(?:i(?:n(?:k(?:i(?:n(?:g)?)?)?)?)?)?)?$|<$/;
    filtered = filtered.replace(partialTagPattern, '');

    return filtered;
  }

  /**
   * Check if content has incomplete devin or thinking blocks
   */
  protected hasIncompleteDevinBlock(content: string): boolean {
    // Check if there's an incomplete devin block
    const lastOpenDevin = content.lastIndexOf('<devin');
    const lastCloseDevin = content.lastIndexOf('</devin>');

    // Check if there's an incomplete thinking block
    const lastOpenThinking = content.lastIndexOf('<thinking');
    const lastCloseThinking = content.lastIndexOf('</thinking>');

    // Also check for partial opening tags like '<de', '<dev', '<th', '<thi' or just '<'
    const partialTagPattern = /<(?:de(?:v(?:i(?:n)?)?)?|th(?:i(?:n(?:k(?:i(?:n(?:g)?)?)?)?)?)?)?$|<$/;
    const hasPartialTag = partialTagPattern.test(content);

    const hasIncompleteDevin = lastOpenDevin > lastCloseDevin;
    const hasIncompleteThinking = lastOpenThinking > lastCloseThinking;

    return hasIncompleteDevin || hasIncompleteThinking || hasPartialTag;
  }

  /**
   * Calculate similarity between two strings for repeat detection
   */
  protected calculateSimilarity(str1: string, str2: string): number {
    if (!str1 || !str2) return 0;

    const words1 = str1.toLowerCase().split(/\s+/);
    const words2 = str2.toLowerCase().split(/\s+/);

    const commonWords = words1.filter(word => words2.includes(word));
    const totalWords = Math.max(words1.length, words2.length);

    return totalWords > 0 ? commonWords.length / totalWords : 0;
  }

  /**
   * Clean up excessive newlines in content
   */
  protected cleanNewlines(content: string): string {
    return content.replace(/\n{3,}/g, '\n\n');
  }

  // ============================================================================
  // JsCodingAgentRenderer Interface - Abstract methods
  // These must be implemented by subclasses (CliRenderer, ServerRenderer, TuiRenderer)
  // ============================================================================

  abstract renderIterationHeader(current: number, max: number): void;
  abstract renderLLMResponseStart(): void;
  abstract renderLLMResponseChunk(chunk: string): void;
  abstract renderLLMResponseEnd(): void;
  abstract renderToolCall(toolName: string, paramsStr: string): void;
  abstract renderToolResult(toolName: string, success: boolean, output: string | null, fullOutput?: string | null, metadata?: Record<string, string>): void;
  abstract renderTaskComplete(executionTimeMs?: number, toolsUsedCount?: number): void;
  abstract renderFinalResult(success: boolean, message: string, iterations: number): void;
  abstract renderError(message: string): void;
  abstract renderRepeatWarning(toolName: string, count: number): void;
  abstract renderRecoveryAdvice(recoveryAdvice: string): void;
  /**
   * Optional policy/permission prompt. Default: no-op; subclasses can override.
   */
  renderUserConfirmationRequest(toolName: string, params: Record<string, any>): void {
    // Default to an info/error line if desired; keeping no-op to avoid extra noise in CLI/Server outputs.
  }

  /**
   * Live terminal sessions are optional; default no-op implementation.
   * Subclasses that support PTY streaming can override.
   */
  addLiveTerminal(sessionId: string, command: string, workingDirectory?: string | null, ptyHandle?: any): void {
    // no-op by default
  }

  /**
   * Render a compact plan summary bar.
   * Default implementation - subclasses can override for custom rendering.
   *
   * @param summary The plan summary data
   */
  renderPlanSummary(summary: JsPlanSummaryData): void {
    // Default: no-op, subclasses can override
  }

  /**
   * Common implementation for LLM response start
   */
  protected baseLLMResponseStart(): void {
    this.reasoningBuffer = '';
    this.isInDevinBlock = false;
    this.lastOutputLength = 0;
  }
    /**
     * Output content - to be implemented by subclasses
   */
  protected abstract outputContent(content: string): void;

  /**
   * Output newline - to be implemented by subclasses
   */
  protected abstract outputNewline(): void;
}
