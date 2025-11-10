/**
 * CLI Renderer for CodingAgent
 * 
 * Implements JsCodingAgentRenderer interface from Kotlin Multiplatform.
 * Provides enhanced CLI output with colors, syntax highlighting, and formatting.
 * 
 * @see mpp-core/src/jsMain/kotlin/cc/unitmesh/agent/RendererExports.kt - Interface definition
 */

import chalk from 'chalk';
import hljs from 'highlight.js';
import { semanticChalk, dividers } from '../../design-system/theme-helpers.js';

/**
 * CliRenderer implements the unified JsCodingAgentRenderer interface
 */
export class CliRenderer {
  readonly __doNotUseOrImplementIt: any = {};

  private reasoningBuffer: string = '';
  private isInDevinBlock: boolean = false;
  private lastIterationReasoning: string = '';
  private consecutiveRepeats: number = 0;

  renderIterationHeader(current: number, max: number): void {
    // Don't show iteration headers - they're not in the reference format
    // The reference format shows tools directly without iteration numbers
  }

  renderLLMResponseStart(): void {
    this.reasoningBuffer = '';
    this.isInDevinBlock = false;
    this.lastOutputLength = 0;
    process.stdout.write(semanticChalk.muted('💭 '));
  }

  renderLLMResponseChunk(chunk: string): void {
    // Add chunk to buffer
    this.reasoningBuffer += chunk;

    // Wait for more content if we detect an incomplete devin block
    if (this.hasIncompleteDevinBlock(this.reasoningBuffer)) {
      return; // Don't output anything yet, wait for more chunks
    }

    // Process the buffer to filter out devin blocks
    let processedContent = this.filterDevinBlocks(this.reasoningBuffer);

    // Only output new content that hasn't been printed yet
    if (processedContent.length > 0) {
      // Find what's new since last output
      const newContent = processedContent.slice(this.lastOutputLength || 0);
      if (newContent.length > 0) {
        // Clean up excessive newlines - replace multiple consecutive newlines with at most 2
        const cleanedContent = newContent.replace(/\n{3,}/g, '\n\n');
        process.stdout.write(chalk.white(cleanedContent));
        this.lastOutputLength = processedContent.length;
      }
    }
  }

  private lastOutputLength: number = 0;

  private hasIncompleteDevinBlock(content: string): boolean {
    // Check if there's an incomplete devin block
    const lastOpenDevin = content.lastIndexOf('<devin');
    const lastCloseDevin = content.lastIndexOf('</devin>');

    // If we have an opening tag without a closing tag after it, it's incomplete
    // Also check for partial opening tags like '<de' or '<dev' or just '<'
    const partialDevinPattern = /<de(?:v(?:i(?:n)?)?)?$|<$/;
    const hasPartialTag = partialDevinPattern.test(content);

    return lastOpenDevin > lastCloseDevin || hasPartialTag;
  }

  private filterDevinBlocks(content: string): string {
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

  renderLLMResponseEnd(): void {
    // Force output any remaining content after filtering devin blocks
    const finalContent = this.filterDevinBlocks(this.reasoningBuffer);
    const remainingContent = finalContent.slice(this.lastOutputLength || 0);

    if (remainingContent.length > 0) {
      process.stdout.write(chalk.white(remainingContent));
    }

    // Check if this reasoning is similar to the last one
    const currentReasoning = finalContent.trim();
    const similarity = this.calculateSimilarity(currentReasoning, this.lastIterationReasoning);

    if (similarity > 0.8 && this.lastIterationReasoning.length > 0) {
      this.consecutiveRepeats++;
      if (this.consecutiveRepeats >= 2) {
        console.log(semanticChalk.warning('\n  ⚠️  Agent appears to be repeating similar analysis...'));
      }
    } else {
      this.consecutiveRepeats = 0;
    }

    this.lastIterationReasoning = currentReasoning;

    // Only add a line break if the content doesn't already end with one
    const trimmedContent = finalContent.trimEnd();
    if (trimmedContent.length > 0 && !trimmedContent.endsWith('\n')) {
      console.log(); // Single line break after reasoning only if needed
    }
  }

  private calculateSimilarity(str1: string, str2: string): number {
    // Simple similarity calculation based on common words
    if (!str1 || !str2) return 0;

    const words1 = str1.toLowerCase().split(/\s+/);
    const words2 = str2.toLowerCase().split(/\s+/);

    const commonWords = words1.filter(word => words2.includes(word));
    const totalWords = Math.max(words1.length, words2.length);

    return totalWords > 0 ? commonWords.length / totalWords : 0;
  }

  renderToolCall(toolName: string, paramsStr: string): void {
    const toolInfo = this.formatToolCallDisplay(toolName, paramsStr);
    console.log(chalk.bold('● ') + chalk.bold(toolInfo.name) + semanticChalk.muted(' - ' + toolInfo.description));
    if (toolInfo.details) {
      console.log('  ⎿ ' + semanticChalk.muted(toolInfo.details));
    }
  }

  private formatToolCallDisplay(toolName: string, paramsStr: string): {name: string, description: string, details?: string} {
    // Parse parameters from the string format
    const params = this.parseParamsString(paramsStr);

    switch (toolName) {
      case 'read-file':
        return {
          name: `${params.path || 'unknown'} - read file`,
          description: 'file reader',
          details: `Reading file: ${params.path || 'unknown'}`
        };
      case 'write-file':
        const mode = params.mode || 'update';
        return {
          name: `${params.path || 'unknown'} - edit file`,
          description: 'file editor',
          details: `${mode === 'create' ? 'Creating' : 'Updating'} file: ${params.path || 'unknown'}`
        };
      case 'shell':
        const command = params.command || params.cmd || 'unknown';
        return {
          name: 'Shell command',
          description: 'command executor',
          details: `Running: ${command}`
        };
      case 'glob':
        return {
          name: 'File search',
          description: 'pattern matcher',
          details: `Searching for files matching pattern: ${params.pattern || 'unknown'}`
        };
      case 'grep':
        return {
          name: 'Text search',
          description: 'content finder',
          details: `Searching for pattern: ${params.pattern || 'unknown'}${params.path ? ` in ${params.path}` : ''}`
        };
      default:
        return {
          name: toolName,
          description: 'tool',
          details: paramsStr
        };
    }
  }

  private parseParamsString(paramsStr: string): Record<string, string> {
    const params: Record<string, string> = {};

    // Match key="value" patterns, handling quoted values with spaces
    const regex = /(\w+)="([^"]*)"/g;
    let match;

    while ((match = regex.exec(paramsStr)) !== null) {
      params[match[1]] = match[2];
    }

    return params;
  }

  renderToolResult(toolName: string, success: boolean, output: string | null, fullOutput: string | null): void {
    if (success && output) {
      const summary = this.generateToolSummary(toolName, output);
      console.log('  ⎿ ' + semanticChalk.success(summary));

      if (toolName === 'read-file') {
        // For read-file, show formatted code content with syntax highlighting
        this.displayCodeContent(output, this.getFileExtension(output));
      } else if (toolName === 'write-file' || toolName === 'edit-file') {
        // For write-file/edit-file, show diff view if available
        const diffInfo = this.extractDiffInfo(output);
        if (diffInfo && (diffInfo.additions > 0 || diffInfo.deletions > 0)) {
          this.displayDiffSummary(diffInfo.additions, diffInfo.deletions);
        }
      }
    } else if (!success && output) {
      console.log('  ⎿ ' + semanticChalk.error(`Error: ${output.substring(0, 200)}`));
    }
  }

  private generateToolSummary(toolName: string, output: string): string {
    switch (toolName) {
      case 'glob':
        const fileMatches = output.match(/Found (\d+) files/);
        if (fileMatches) {
          return `Found ${fileMatches[1]} files`;
        }
        return 'Files found';

      case 'read-file':
        const lines = output.split('\n').length;
        return `Read ${lines} lines`;

      case 'write-file':
      case 'edit-file':
        if (output.includes('created')) {
          const lines = output.split('\n').length;
          return `File created with ${lines} lines`;
        } else if (output.includes('updated') || output.includes('overwrote') || output.includes('Successfully')) {
          // Try to extract diff information if available
          const diffInfo = this.extractDiffInfo(output);
          if (diffInfo && (diffInfo.additions > 0 || diffInfo.deletions > 0)) {
            return `Edited with ${semanticChalk.success('+' + diffInfo.additions)} and ${semanticChalk.error('-' + diffInfo.deletions)}`;
          }
          return 'File updated successfully';
        }
        return 'File operation completed';

      case 'shell':
        return 'Command executed successfully';

      case 'grep':
        const matches = output.split('\n').filter(line => line.trim()).length;
        return `Found ${matches} matches`;

      default:
        return 'Operation completed';
    }
  }

  private displayCodeContent(content: string, fileExtension: string): void {
    const lines = content.split('\n');
    const maxLines = 15; // Show first 15 lines to avoid overwhelming output
    const displayLines = lines.slice(0, maxLines);

    console.log(dividers.solid(60));

    displayLines.forEach((line, index) => {
      const lineNumber = (index + 1).toString().padStart(3, ' ');
      const highlightedLine = this.highlightCode(line, fileExtension);
      console.log(semanticChalk.muted(`${lineNumber} │ `) + highlightedLine);
    });

    if (lines.length > maxLines) {
      console.log(semanticChalk.muted(`... (${lines.length - maxLines} more lines)`));
    }

    console.log(dividers.solid(60));
  }

  private displayDiffSummary(additions: number, deletions: number): void {
    // Display diff summary similar to git diff output
    console.log(dividers.solid(60));
    console.log(semanticChalk.success(`  +${additions} lines added`) + ' │ ' + semanticChalk.error(`-${deletions} lines removed`));
    console.log(dividers.solid(60));
  }

  private highlightCode(code: string, fileExtension: string): string {
    try {
      const language = this.getLanguageFromExtension(fileExtension);
      if (language && hljs.getLanguage(language)) {
        const highlighted = hljs.highlight(code, { language }).value;
        // Convert ANSI codes to chalk colors (simplified)
        return this.convertAnsiToChalk(highlighted);
      }
    } catch (e) {
      // Fallback to no highlighting
    }
    return code;
  }

  private getFileExtension(content: string): string {
    // Try to extract file extension from content or context
    // This is a simplified approach - in practice, you'd get this from the file path
    if (content.includes('public class') || content.includes('import java')) return 'java';
    if (content.includes('function') && content.includes('{')) return 'js';
    if (content.includes('def ') || content.includes('import ')) return 'py';
    if (content.includes('#include') || content.includes('int main')) return 'cpp';
    return 'txt';
  }

  private getLanguageFromExtension(ext: string): string | null {
    const mapping: Record<string, string> = {
      'java': 'java',
      'js': 'javascript',
      'ts': 'typescript',
      'py': 'python',
      'cpp': 'cpp',
      'c': 'c',
      'kt': 'kotlin',
      'rs': 'rust',
      'go': 'go',
      'rb': 'ruby',
      'php': 'php',
      'html': 'html',
      'css': 'css',
      'json': 'json',
      'xml': 'xml',
      'yaml': 'yaml',
      'yml': 'yaml',
      'md': 'markdown'
    };
    return mapping[ext] || null;
  }

  private convertAnsiToChalk(highlighted: string): string {
    // Simple conversion of highlight.js HTML to colored text
    // This is a basic implementation - you might want to use a proper HTML-to-ANSI converter
    return highlighted
      .replace(/<span class="hljs-keyword">/g, semanticChalk.primary(''))
      .replace(/<span class="hljs-string">/g, semanticChalk.success(''))
      .replace(/<span class="hljs-comment">/g, semanticChalk.muted(''))
      .replace(/<span class="hljs-number">/g, semanticChalk.warning(''))
      .replace(/<\/span>/g, chalk.reset(''))
      .replace(/<[^>]*>/g, ''); // Remove any remaining HTML tags
  }

  private extractDiffInfo(output: string): {additions: number, deletions: number} | null {
    // Try to extract diff information from output
    const lines = output.split('\n');
    let additions = 0;
    let deletions = 0;

    // Check for unified diff format (from tool output)
    for (const line of lines) {
      if (line.startsWith('+') && !line.startsWith('+++')) {
        additions++;
      } else if (line.startsWith('-') && !line.startsWith('---')) {
        deletions++;
      }
    }

    if (additions > 0 || deletions > 0) {
      return { additions, deletions };
    }

    // Try to parse from success message like "Successfully created file: path (123 chars, 45 lines)"
    // This is from WriteFileTool output
    const newFileMatch = output.match(/created file.*\((\d+) chars, (\d+) lines\)/);
    if (newFileMatch) {
      const lineCount = parseInt(newFileMatch[2], 10);
      return { additions: lineCount, deletions: 0 };
    }

    // Try to parse from success message like "Successfully overwrote file: path (123 chars, 45 lines)"
    const overwriteMatch = output.match(/overwrote file.*\((\d+) chars, (\d+) lines\)/);
    if (overwriteMatch) {
      const lineCount = parseInt(overwriteMatch[2], 10);
      // For overwrites, we can't determine exact diff without original content
      // So we'll just show it as additions for now
      return { additions: lineCount, deletions: 0 };
    }

    return null;
  }

  renderTaskComplete(): void {
    console.log(semanticChalk.successBold('\n✓ Task marked as complete\n'));
  }

  renderFinalResult(success: boolean, message: string, iterations: number): void {
    console.log();
    if (success) {
      console.log(semanticChalk.successBold('✅ Task completed successfully'));
    } else {
      console.log(semanticChalk.warningBold('⚠️  Task incomplete'));
    }
    console.log(semanticChalk.muted(`Task completed after ${iterations} iterations`));
  }

  renderError(message: string): void {
    console.log(semanticChalk.errorBold('❌ ') + message);
  }

  renderRepeatWarning(toolName: string, count: number): void {
    console.log(semanticChalk.warning(`⚠️  Warning: Tool '${toolName}' has been called ${count} times in a row`));
    console.log(semanticChalk.warning(`   This may indicate the agent is stuck in a loop. Stopping execution.`));
  }

  renderRecoveryAdvice(recoveryAdvice: string): void {
    console.log();
    console.log(semanticChalk.accentBold('🔧 ERROR RECOVERY ADVICE:'));
    console.log(semanticChalk.accent(dividers.solid(50)));

    // Split by lines and add proper indentation with colors
    const lines = recoveryAdvice.split('\n');
    for (const line of lines) {
      if (line.trim().length > 0) {
        // Color different sections differently
        if (line.includes('Analysis:')) {
          console.log(semanticChalk.primary(`   ${line}`));
        } else if (line.includes('Recommended Actions:')) {
          console.log(semanticChalk.success(`   ${line}`));
        } else if (line.includes('Recovery Commands:')) {
          console.log(semanticChalk.warning(`   ${line}`));
        } else if (line.includes('Next Steps:')) {
          console.log(chalk.magenta(`   ${line}`));
        } else if (line.trim().startsWith('$')) {
          // Command lines in cyan
          console.log(semanticChalk.accent(`   ${line}`));
        } else if (line.trim().match(/^\d+\./)) {
          // Numbered items in white
          console.log(chalk.white(`   ${line}`));
        } else {
          // Regular text in gray
          console.log(semanticChalk.muted(`   ${line}`));
        }
      } else {
        console.log();
      }
    }

    console.log(semanticChalk.accent(dividers.solid(50)));
    console.log();
  }
}

