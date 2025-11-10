/**
 * ServerRenderer - Renders events from mpp-server in CLI format
 *
 * Provides similar output to CliRenderer but for server-side events
 */

import { semanticChalk } from '../../design-system/theme-helpers.js';
import type { AgentEvent, AgentStepInfo, AgentEditInfo } from '../ServerAgentClient.js';

export class ServerRenderer {
  private currentIteration: number = 0;
  private maxIterations: number = 20;
  private llmBuffer: string = '';
  private toolCallsInProgress: Map<string, { toolName: string; params: string }> = new Map();
  private isCloning: boolean = false;
  private lastCloneProgress: number = 0;
  private hasStartedLLMOutput: boolean = false;
  private lastOutputLength: number = 0;

  /**
   * Render an event from the server
   */
  renderEvent(event: AgentEvent): void {
    switch (event.type) {
      case 'clone_progress':
        this.renderCloneProgress(event.stage, event.progress);
        break;
      case 'clone_log':
        this.renderCloneLog(event.message, event.isError);
        break;
      case 'iteration':
        this.renderIterationStart(event.current, event.max);
        break;
      case 'llm_chunk':
        this.renderLLMChunk(event.chunk);
        break;
      case 'tool_call':
        this.renderToolCall(event.toolName, event.params);
        break;
      case 'tool_result':
        this.renderToolResult(event.toolName, event.success, event.output);
        break;
      case 'error':
        this.renderError(event.message);
        break;
      case 'complete':
        this.renderComplete(event.success, event.message, event.iterations, event.steps, event.edits);
        break;
    }
  }

  private renderCloneProgress(stage: string, progress?: number): void {
    if (!this.isCloning) {
      // First clone event - show header
      console.log('');
      console.log(semanticChalk.accent('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'));
      console.log(semanticChalk.info('📦 Cloning repository...'));
      console.log('');
      this.isCloning = true;
    }

    // Show progress bar for significant progress updates
    if (progress !== undefined && progress !== this.lastCloneProgress) {
      const barLength = 30;
      const filledLength = Math.floor((progress / 100) * barLength);
      const bar = '█'.repeat(filledLength) + '░'.repeat(barLength - filledLength);

      process.stdout.write(`\r${semanticChalk.accent(`[${bar}]`)} ${progress}% - ${stage}`);

      if (progress === 100) {
        console.log(''); // New line after completion
        console.log(semanticChalk.success('✓ Clone completed'));
        console.log(semanticChalk.accent('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'));
        console.log('');
      }

      this.lastCloneProgress = progress;
    }
  }

  private renderCloneLog(message: string, isError: boolean = false): void {
    // Filter out noisy git messages
    const noisyPatterns = [
      /^Executing:/,
      /^remote:/,
      /^Receiving objects:/,
      /^Resolving deltas:/,
      /^Unpacking objects:/
    ];

    if (noisyPatterns.some(pattern => pattern.test(message))) {
      return; // Skip noisy messages
    }

    // Only show important messages
    if (message.includes('✓') || message.includes('Repository ready') || isError) {
      if (isError) {
        console.log(semanticChalk.error(`  ✗ ${message}`));
      } else {
        console.log(semanticChalk.muted(`  ${message}`));
      }
    }
  }

  private renderIterationStart(current: number, max: number): void {
    this.currentIteration = current;
    this.maxIterations = max;

    // Flush any buffered LLM output
    if (this.llmBuffer.trim()) {
      console.log(''); // Just a newline
      this.llmBuffer = '';
    }

    // Reset LLM output state for new iteration
    this.hasStartedLLMOutput = false;
    this.lastOutputLength = 0;

    // Don't show iteration headers like CliRenderer - they're not in the reference format
    // The reference format shows tools directly without iteration numbers
  }

  private renderLLMChunk(chunk: string): void {
    // Show thinking emoji before first chunk (like CliRenderer)
    if (!this.hasStartedLLMOutput) {
      process.stdout.write(semanticChalk.muted('💭 '));
      this.hasStartedLLMOutput = true;
    }

    // Add chunk to buffer
    this.llmBuffer += chunk;

    // Wait for more content if we detect an incomplete devin block
    if (this.hasIncompleteDevinBlock(this.llmBuffer)) {
      return; // Don't output anything yet, wait for more chunks
    }

    // Process the buffer to filter out devin blocks
    const processedContent = this.filterDevinBlocks(this.llmBuffer);

    // Only output new content that hasn't been printed yet
    if (processedContent.length > 0) {
      const newContent = processedContent.slice(this.lastOutputLength);
      if (newContent.length > 0) {
        // Clean up excessive newlines - replace multiple consecutive newlines with at most 2
        const cleanedContent = newContent.replace(/\n{3,}/g, '\n\n');
        process.stdout.write(cleanedContent);
        this.lastOutputLength = processedContent.length;
      }
    }
  }

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

  private renderToolCall(toolName: string, params: string): void {
    // Flush any buffered LLM output first
    if (this.llmBuffer.trim()) {
      console.log(''); // New line before tool
      this.llmBuffer = '';
      this.hasStartedLLMOutput = false;
      this.lastOutputLength = 0;
    }

    // Get friendly tool display info (matching CliRenderer style)
    const toolInfo = this.formatToolCallDisplay(toolName, params);

    // Display like CliRenderer: ● name - description
    console.log(`● ${toolInfo.name}` + semanticChalk.muted(` - ${toolInfo.description}`));

    if (toolInfo.details) {
      console.log('  ⎿ ' + semanticChalk.muted(toolInfo.details));
    }

    // Store for matching with result
    this.toolCallsInProgress.set(toolName, { toolName, params });
  }

  private formatToolCallDisplay(toolName: string, params: string): {name: string, description: string, details?: string} {
    try {
      const paramsObj = JSON.parse(params);

      switch (toolName) {
        case 'read-file':
          return {
            name: `${paramsObj.path || 'unknown'} - read file`,
            description: 'file reader',
            details: `Reading file: ${paramsObj.path || 'unknown'}`
          };
        case 'write-file':
        case 'edit-file':
          const mode = paramsObj.mode || 'update';
          return {
            name: `${paramsObj.path || 'unknown'} - edit file`,
            description: 'file editor',
            details: `${mode === 'create' ? 'Creating' : 'Updating'} file: ${paramsObj.path || 'unknown'}`
          };
        case 'shell':
          const command = paramsObj.command || paramsObj.cmd || 'unknown';
          return {
            name: 'Shell command',
            description: 'command executor',
            details: `Running: ${command}`
          };
        case 'glob':
          return {
            name: 'File search',
            description: 'pattern matcher',
            details: `Searching for files matching pattern: ${paramsObj.pattern || 'unknown'}`
          };
        case 'grep':
          return {
            name: 'Text search',
            description: 'content finder',
            details: `Searching for pattern: ${paramsObj.pattern || 'unknown'}${paramsObj.path ? ` in ${paramsObj.path}` : ''}`
          };
        default:
          return {
            name: toolName,
            description: 'tool',
            details: params
          };
      }
    } catch (e) {
      // If params parsing fails, use tool name
      return {
        name: toolName,
        description: 'tool'
      };
    }
  }

  private renderToolResult(toolName: string, success: boolean, output?: string): void {
    if (success && output) {
      const summary = this.generateToolSummary(toolName, output);
      console.log('  ⎿ ' + semanticChalk.success(summary));

      // For read-file, show formatted code content (like CliRenderer)
      if (toolName === 'read-file') {
        this.displayCodeContent(output);
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

    // Remove from in-progress map
    this.toolCallsInProgress.delete(toolName);
  }

  private generateToolSummary(toolName: string, output: string): string {
    switch (toolName) {
      case 'glob':
        const fileMatches = output.match(/Found (\d+) files/);
        if (fileMatches) {
          return `Found ${fileMatches[1]} files`;
        }
        // Try to count files from output
        const files = output.split('\n').filter(f => f.trim());
        return `Found ${files.length} files`;

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
        const matches = output.split('\n').filter(m => m.trim()).length;
        return `Found ${matches} matches`;

      default:
        return 'Operation completed';
    }
  }

  private displayCodeContent(content: string): void {
    const lines = content.split('\n');
    const maxLines = 15; // Show first 15 lines like CliRenderer
    const displayLines = lines.slice(0, maxLines);

    console.log('────────────────────────────────────────────────────────────');

    displayLines.forEach((line, index) => {
      const lineNumber = (index + 1).toString().padStart(3, ' ');
      console.log(semanticChalk.muted(`${lineNumber} │ `) + line);
    });

    if (lines.length > maxLines) {
      console.log(semanticChalk.muted(`... (${lines.length - maxLines} more lines)`));
    }

    console.log('────────────────────────────────────────────────────────────');
  }

  private displayDiffSummary(additions: number, deletions: number): void {
    // Display diff summary similar to git diff output
    console.log('────────────────────────────────────────────────────────────');
    console.log(semanticChalk.success(`  +${additions} lines added`) + ' │ ' + semanticChalk.error(`-${deletions} lines removed`));
    console.log('────────────────────────────────────────────────────────────');
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

  private renderError(message: string): void {
    console.log('');
    console.log(semanticChalk.error(`❌ Error: ${message}`));
    console.log('');
  }

  private renderComplete(
    success: boolean,
    message: string,
    iterations: number,
    steps: AgentStepInfo[],
    edits: AgentEditInfo[]
  ): void {
    // Flush any buffered LLM output
    if (this.llmBuffer.trim()) {
      const finalContent = this.filterDevinBlocks(this.llmBuffer);
      const remainingContent = finalContent.slice(this.lastOutputLength || 0);
      if (remainingContent.trim()) {
        process.stdout.write(remainingContent);
      }
      console.log(''); // Ensure newline
      this.llmBuffer = '';
    }

    console.log('');

    if (success) {
      console.log(semanticChalk.success('✅ Task completed successfully'));
    } else {
      console.log(semanticChalk.error('❌ Task failed'));
    }

    if (message && message.trim()) {
      console.log(semanticChalk.muted(`${message}`));
    }

    console.log(semanticChalk.muted(`Task completed after ${iterations} iterations`));

    if (edits.length > 0) {
      console.log('');
      console.log(semanticChalk.accent('📝 File Changes:'));
      edits.forEach(edit => {
        const icon = edit.operation === 'CREATE' ? '➕' : edit.operation === 'DELETE' ? '➖' : '✏️';
        console.log(semanticChalk.info(`  ${icon} ${edit.file}`));
      });
    }

    console.log('');
  }
}


