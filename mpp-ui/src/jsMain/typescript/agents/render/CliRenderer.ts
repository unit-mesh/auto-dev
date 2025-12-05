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
import { BaseRenderer } from './BaseRenderer.js';
import { cc } from 'autodev-mpp-core/autodev-mpp-core';
import JsPlanSummaryData = cc.unitmesh.agent.JsPlanSummaryData;

/**
 * CliRenderer extends BaseRenderer and implements the unified JsCodingAgentRenderer interface
 */
export class CliRenderer extends BaseRenderer {
  // BaseRenderer already has __doNotUseOrImplementIt
  // BaseRenderer already has reasoningBuffer, isInDevinBlock, lastIterationReasoning, consecutiveRepeats, lastOutputLength

  // ============================================================================
  // Platform-specific output methods (required by BaseRenderer)
  // ============================================================================

  protected outputContent(content: string): void {
    process.stdout.write(chalk.white(content));
  }

  protected outputNewline(): void {
    console.log(); // Single line break
  }

  // ============================================================================
  // JsCodingAgentRenderer Interface Implementation
  // ============================================================================

  renderIterationHeader(current: number, max: number): void {
    // Don't show iteration headers - they're not in the reference format
    // The reference format shows tools directly without iteration numbers
  }

  /**
   * Render a compact plan summary bar
   * Example: 📋 Plan: Create Tag System (3/5 steps, 60%) ████████░░░░░░░░
   */
  renderPlanSummary(summary: JsPlanSummaryData): void {
    const { title, completedSteps, totalSteps, progressPercent, status, currentStepDescription } = summary;

    // Build progress bar (16 chars wide)
    const barWidth = 16;
    const filledWidth = Math.round((progressPercent / 100) * barWidth);
    const emptyWidth = barWidth - filledWidth;
    const progressBar = '█'.repeat(filledWidth) + '░'.repeat(emptyWidth);

    // Status indicator
    let statusIcon = '📋';
    let statusColor = semanticChalk.info;
    if (status === 'COMPLETED') {
      statusIcon = '✅';
      statusColor = semanticChalk.success;
    } else if (status === 'FAILED') {
      statusIcon = '❌';
      statusColor = semanticChalk.error;
    } else if (status === 'IN_PROGRESS') {
      statusIcon = '🔄';
    }

    // Truncate title if too long
    const maxTitleLen = 30;
    const displayTitle = title.length > maxTitleLen ? title.substring(0, maxTitleLen - 3) + '...' : title;

    // Main summary line
    console.log(
      statusIcon + ' ' +
      chalk.bold('Plan: ') +
      chalk.white(displayTitle) + ' ' +
      semanticChalk.muted(`(${completedSteps}/${totalSteps} steps, ${progressPercent}%) `) +
      statusColor(progressBar)
    );

    // Show current step if available
    if (currentStepDescription && status !== 'COMPLETED') {
      const maxStepLen = 50;
      const displayStep = currentStepDescription.length > maxStepLen
        ? currentStepDescription.substring(0, maxStepLen - 3) + '...'
        : currentStepDescription;
      console.log('  ⎿ ' + semanticChalk.muted('Next: ' + displayStep));
    }
  }

  renderLLMResponseStart(): void {
    this.baseLLMResponseStart(); // Use BaseRenderer helper
    process.stdout.write(semanticChalk.muted('💭 '));
  }

  renderLLMResponseChunk(chunk: string): void {
    // Add chunk to buffer
    this.reasoningBuffer += chunk;

    // Wait for more content if we detect an incomplete devin block (from BaseRenderer)
    if (this.hasIncompleteDevinBlock(this.reasoningBuffer)) {
      return; // Don't output anything yet, wait for more chunks
    }

    // Process the buffer to filter out devin blocks (from BaseRenderer)
    let processedContent = this.filterDevinBlocks(this.reasoningBuffer);

    // Only output new content that hasn't been printed yet
    if (processedContent.length > 0) {
      // Find what's new since last output
      const newContent = processedContent.slice(this.lastOutputLength || 0);
      if (newContent.length > 0) {
        // Clean up excessive newlines (from BaseRenderer)
        const cleanedContent = this.cleanNewlines(newContent);
        process.stdout.write(chalk.white(cleanedContent));
        this.lastOutputLength = processedContent.length;
      }
    }
  }

  renderLLMResponseEnd(): void {
    // Force output any remaining content after filtering devin blocks
    const finalContent = this.filterDevinBlocks(this.reasoningBuffer);
    const remainingContent = finalContent.slice(this.lastOutputLength || 0);

    if (remainingContent.length > 0) {
      this.outputContent(remainingContent);
    }

    // Check if this reasoning is similar to the last one (from BaseRenderer)
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
      this.outputNewline();
    }
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
      case 'DocQL':
      case 'docql': {
        // Use JSON.parse directly - it handles all edge cases properly
        let docqlParams: Record<string, any> = {};
        try {
          // First try direct JSON parse
          docqlParams = JSON.parse(paramsStr);
        } catch {
          // Fallback to the old regex-based parsing
          docqlParams = params;
        }

        const query = docqlParams.query || params.query || paramsStr;
        const docPath = docqlParams.documentPath || params.documentPath;
        const maxResults = docqlParams.maxResults || params.maxResults;
        const reranker = docqlParams.rerankerType || params.rerankerType;

        // Build details string - truncate long queries for display
        const displayQuery = query.length > 80 ? query.substring(0, 77) + '...' : query;
        let details = `Query: "${displayQuery}"`;
        if (docPath) details += ` | Doc: ${docPath}`;
        if (maxResults) details += ` | Max: ${maxResults}`;
        if (reranker) details += ` | Reranker: ${reranker}`;

        return {
          name: 'DocQL',
          description: 'document query',
          details
        };
      }
      case 'plan': {
        const action = params.action || 'unknown';
        const actionLower = action.toLowerCase();
        if (actionLower === 'create') {
          return {
            name: 'Plan',
            description: 'creating plan',
            details: 'Creating new task plan...'
          };
        } else if (actionLower === 'complete_step') {
          const steps = params.steps;
          if (steps) {
            return {
              name: 'Plan',
              description: 'updating progress',
              details: `Completing multiple steps...`
            };
          }
          return {
            name: 'Plan',
            description: 'updating progress',
            details: `Completing step ${params.taskIndex || '?'}.${params.stepIndex || '?'}`
          };
        } else if (actionLower === 'fail_step') {
          return {
            name: 'Plan',
            description: 'marking failed',
            details: `Step ${params.taskIndex || '?'}.${params.stepIndex || '?'} failed`
          };
        } else if (actionLower === 'view') {
          return {
            name: 'Plan',
            description: 'viewing plan',
            details: 'Viewing current plan status'
          };
        }
        return {
          name: 'Plan',
          description: action,
          details: paramsStr
        };
      }
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

      // Normalize input: remove surrounding braces and trim
      const cleaned = paramsStr.trim().replace(/^\{|\}$/g, '').trim();
      if (!cleaned) return params;

      // Match key=value where value may be double-quoted, single-quoted, or unquoted (stops at comma/space/})
      const regex = /(\w+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^,\s}]+))/g;
      let match: RegExpExecArray | null;

      while ((match = regex.exec(cleaned)) !== null) {
        const key = match[1];
        const value = match[2] ?? match[3] ?? match[4] ?? '';
        params[key] = value;
      }

      return params;
    }

  renderToolResult(toolName: string, success: boolean, output: string | null, fullOutput: string | null, metadata?: Record<string, string>): void {
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
    // Display metadata if provided
    if (metadata && Object.keys(metadata).length > 0) {
      this.displayMetadata(metadata);
    }
  }

  private displayMetadata(metadata: Record<string, string>): void {
    const entries = Object.entries(metadata);
    if (entries.length === 0) return;

    // Format metadata with icons based on common keys
    const formattedEntries = entries.map(([key, value]) => {
      switch (key) {
        case 'duration':
        case 'elapsed':
        case 'time':
          return `⏱  ${key}: ${semanticChalk.accent(value)}`;
        case 'size':
        case 'fileSize':
        case 'bytes':
          return `📦 ${key}: ${semanticChalk.accent(value)}`;
        case 'lines':
        case 'lineCount':
          return `📄 ${key}: ${semanticChalk.accent(value)}`;
        case 'status':
        case 'exitCode':
          return `📊 ${key}: ${semanticChalk.accent(value)}`;
        default:
          return `   ${key}: ${semanticChalk.muted(value)}`;
      }
    });

    console.log(semanticChalk.muted('  ├─ metadata:'));
    formattedEntries.forEach(entry => {
      console.log(semanticChalk.muted(`  │  ${entry}`));
    });
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

      case 'DocQL':
        // Simplify DocQL output - just show success
        return 'Query executed';

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

  renderTaskComplete(executionTimeMs: number = 0, toolsUsedCount: number = 0): void {
    const parts: string[] = [];

    if (executionTimeMs > 0) {
      const seconds = (executionTimeMs / 1000).toFixed(2);
      parts.push(`${seconds}s`);
    }

    if (toolsUsedCount > 0) {
      parts.push(`${toolsUsedCount} tools`);
    }

    const info = parts.length > 0 ? ` (${parts.join(', ')})` : '';
    console.log(semanticChalk.successBold(`\n✓ Task marked as complete${info}\n`));
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

