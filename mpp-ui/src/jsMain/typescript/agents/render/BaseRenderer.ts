/**
 * Base TypeScript renderer implementing JsCodingAgentRenderer interface
 * Provides common functionality for all TypeScript renderer implementations
 */

export abstract class BaseRenderer {
  // Required by Kotlin JS export interface
  readonly __doNotUseOrImplementIt: any = {};

  protected reasoningBuffer: string = '';
  protected isInDevinBlock: boolean = false;
  protected lastIterationReasoning: string = '';
  protected consecutiveRepeats: number = 0;
  protected lastOutputLength: number = 0;

  /**
   * Filter out devin blocks from content
   */
  protected filterDevinBlocks(content: string): string {
    // Remove all complete devin blocks
    let filtered = content.replace(/<devin[^>]*>[\s\S]*?<\/devin>/g, '');
    
    // Handle incomplete devin blocks at the end - remove them completely
    const openDevinIndex = filtered.lastIndexOf('<devin');
    if (openDevinIndex !== -1) {
      const closeDevinIndex = filtered.indexOf('</devin>', openDevinIndex);
      if (closeDevinIndex === -1) {
        // Incomplete devin block, remove it
        filtered = filtered.substring(0, openDevinIndex);
      }
    }
    
    // Also remove partial devin tags at the end and any standalone '<' that might be part of a devin tag
    const partialDevinPattern = /<de(?:v(?:i(?:n)?)?)?$|<$/;
    filtered = filtered.replace(partialDevinPattern, '');
    
    return filtered;
  }

  /**
   * Check if content has incomplete devin blocks
   */
  protected hasIncompleteDevinBlock(content: string): boolean {
    // Check if there's an incomplete devin block
    const lastOpenDevin = content.lastIndexOf('<devin');
    const lastCloseDevin = content.lastIndexOf('</devin>');
    
    // Also check for partial opening tags like '<de' or '<dev' or just '<'
    const partialDevinPattern = /<de(?:v(?:i(?:n)?)?)?$|<$/;
    const hasPartialTag = partialDevinPattern.test(content);
    
    return lastOpenDevin > lastCloseDevin || hasPartialTag;
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

  // Abstract methods that must be implemented by subclasses
  abstract renderIterationHeader(current: number, max: number): void;
  abstract renderLLMResponseStart(): void;
  abstract renderLLMResponseChunk(chunk: string): void;
  abstract renderLLMResponseEnd(): void;
  abstract renderToolCall(toolName: string, paramsStr: string): void;
  abstract renderToolResult(toolName: string, success: boolean, output: string | null, fullOutput: string | null): void;
  abstract renderTaskComplete(): void;
  abstract renderFinalResult(success: boolean, message: string, iterations: number): void;
  abstract renderError(message: string): void;
  abstract renderRepeatWarning(toolName: string, count: number): void;

  /**
   * Common implementation for LLM response start
   */
  protected baseLLMResponseStart(): void {
    this.reasoningBuffer = '';
    this.isInDevinBlock = false;
    this.lastOutputLength = 0;
  }

  /**
   * Common implementation for LLM response end with similarity checking
   */
  protected baseLLMResponseEnd(): void {
    // Force output any remaining content after filtering devin blocks
    const finalContent = this.filterDevinBlocks(this.reasoningBuffer);
    const remainingContent = finalContent.slice(this.lastOutputLength || 0);

    if (remainingContent.length > 0) {
      this.outputContent(remainingContent);
    }

    // Check if this reasoning is similar to the last one
    const currentReasoning = finalContent.trim();
    const similarity = this.calculateSimilarity(currentReasoning, this.lastIterationReasoning);

    if (similarity > 0.8 && this.lastIterationReasoning.length > 0) {
      this.consecutiveRepeats++;
      if (this.consecutiveRepeats >= 2) {
        this.renderRepeatAnalysisWarning();
      }
    } else {
      this.consecutiveRepeats = 0;
    }

    this.lastIterationReasoning = currentReasoning;
    
    // Only add a line break if the content doesn't already end with one
    const trimmedContent = finalContent.trimEnd();
    if (trimmedContent.length > 0 && !trimmedContent.endsWith('\n')) {
      this.outputNewline();
    }
  }

  /**
   * Output content - to be implemented by subclasses
   */
  protected abstract outputContent(content: string): void;

  /**
   * Output newline - to be implemented by subclasses
   */
  protected abstract outputNewline(): void;

  /**
   * Render warning for repetitive analysis - can be overridden by subclasses
   */
  protected renderRepeatAnalysisWarning(): void {
    this.renderError('Agent appears to be repeating similar analysis...');
  }
}
