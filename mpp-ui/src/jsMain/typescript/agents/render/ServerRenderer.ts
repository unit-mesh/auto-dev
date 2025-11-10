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
      console.log(this.llmBuffer);
      this.llmBuffer = '';
    }

    console.log('');
    console.log(semanticChalk.accent(`\n━━━ Iteration ${current}/${max} ━━━\n`));
  }

  private renderLLMChunk(chunk: string): void {
    // Filter out devin blocks before buffering
    const filtered = this.filterDevinBlock(chunk);
    if (!filtered) return;
    
    // Print immediately for streaming effect (like local mode)
    process.stdout.write(filtered);
    this.llmBuffer += filtered;
  }
  
  private filterDevinBlock(chunk: string): string {
    // Remove any part of <devin> tags
    if (chunk.includes('<devin') || chunk.includes('</devin') || 
        chunk.includes('<de') || chunk.includes('</de')) {
      return '';
    }
    
    // If we're inside a devin block (detected in buffer), skip content
    if (this.llmBuffer.includes('<devin') && !this.llmBuffer.includes('</devin>')) {
      return ''; // Inside devin block, skip
    }
    
    // Remove content that looks like JSON blocks in tool calls
    if (this.llmBuffer.includes('```json') || this.llmBuffer.includes('/glob')) {
      // Skip until we see closing tags
      if (!chunk.includes('</devin>') && !chunk.includes('I expect')) {
        return '';
      }
    }
    
    return chunk;
  }

  private renderToolCall(toolName: string, params: string): void {
    // Flush any buffered LLM output first
    if (this.llmBuffer.trim()) {
      console.log(''); // New line before tool
      this.llmBuffer = '';
    }

    // Parse params to get a friendly description
    let description = toolName;
    try {
      const paramsObj = JSON.parse(params);

      // Create friendly descriptions based on tool type (matching local mode style)
      if (toolName === 'read-file' && paramsObj.path) {
        description = `${paramsObj.path} - read file - file reader`;
      } else if (toolName === 'write-file' && paramsObj.path) {
        description = `${paramsObj.path} - write file - file writer`;
      } else if (toolName === 'edit-file' && paramsObj.path) {
        description = `${paramsObj.path} - edit file - file editor`;
      } else if (toolName === 'glob' && paramsObj.pattern) {
        description = `File search - pattern matcher`;
      } else if (toolName === 'grep' && paramsObj.pattern) {
        description = `Code search - grep`;
      } else if (toolName === 'shell' && paramsObj.command) {
        description = `Shell - ${paramsObj.command}`;
      }
    } catch (e) {
      // If params parsing fails, use tool name
    }

    console.log(`● ${description}`);

    // Store for matching with result
    this.toolCallsInProgress.set(toolName, { toolName, params });
  }

  private renderToolResult(toolName: string, success: boolean, output?: string): void {
    const toolCall = this.toolCallsInProgress.get(toolName);

    if (!toolCall) {
      // Tool result without matching call - just print it
      if (success) {
        console.log(semanticChalk.success(`  ⎿ ${output || 'Success'}`));
      } else {
        console.log(semanticChalk.error(`  ⎿ ${output || 'Failed'}`));
      }
      return;
    }

    // Parse params for context
    try {
      const paramsObj = JSON.parse(toolCall.params);

      // Render based on tool type (simplified to match local mode)
      if (toolName === 'read-file') {
        if (success && output) {
          const lines = output.split('\n');
          console.log(`  ⎿ Reading file: ${paramsObj.path}`);
          console.log(`  ⎿ Read ${lines.length} lines`);

          // Show preview (first 15 lines) like local mode
          if (lines.length > 0) {
            console.log('────────────────────────────────────────────────────────────');
            const preview = lines.slice(0, 15);
            preview.forEach((line, i) => {
              console.log(`${String(i + 1).padStart(3, ' ')} │ ${line}`);
            });
            if (lines.length > 15) {
              console.log(`... (${lines.length - 15} more lines)`);
            }
            console.log('────────────────────────────────────────────────────────────');
          }
        } else {
          console.log(`  ⎿ Failed to read file: ${output || 'Unknown error'}`);
        }
      } else if (toolName === 'write-file' || toolName === 'edit-file') {
        if (success) {
          console.log(`  ⎿ ${toolName === 'write-file' ? 'Written' : 'Edited'}: ${paramsObj.path}`);
        } else {
          console.log(`  ⎿ Failed: ${output || 'Unknown error'}`);
        }
      } else if (toolName === 'glob') {
        if (success && output) {
          const files = output.split('\n').filter(f => f.trim());
          console.log(`  ⎿ Searching for files matching pattern: ${paramsObj.pattern}`);
          console.log(`  ⎿ Found ${files.length} files`);
          
          // Don't show file list - too verbose (matching local mode)
        } else {
          console.log(`  ⎿ Search failed: ${output || 'Unknown error'}`);
        }
      } else if (toolName === 'grep') {
        if (success && output) {
          const matches = output.split('\n').filter(m => m.trim());
          console.log(`  ⎿ Searching for: ${paramsObj.pattern}`);
          console.log(`  ⎿ Found ${matches.length} matches`);
        } else {
          console.log(`  ⎿ Search failed: ${output || 'Unknown error'}`);
        }
      } else if (toolName === 'shell') {
        if (success) {
          console.log(`  ⎿ Command executed`);
          if (output && output.trim()) {
            const shortOutput = output.substring(0, 100);
            console.log(`  ⎿ ${shortOutput}${output.length > 100 ? '...' : ''}`);
          }
        } else {
          console.log(`  ⎿ Command failed: ${output || 'Unknown error'}`);
        }
      } else {
        // Generic tool result
        console.log(success ? `  ⎿ Success` : `  ⎿ Failed: ${output || 'Unknown error'}`);
      }
    } catch (e) {
      // Fallback if params parsing fails - don't show raw output
      console.log(success ? `  ⎿ Success` : `  ⎿ Failed`);
    }

    // Remove from in-progress map
    this.toolCallsInProgress.delete(toolName);
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
      console.log(this.llmBuffer);
      this.llmBuffer = '';
    }

    console.log('');
    console.log(semanticChalk.accent('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'));

    if (success) {
      console.log(semanticChalk.success(`✅ Task completed successfully`));
    } else {
      console.log(semanticChalk.error(`❌ Task failed`));
    }

    console.log(semanticChalk.muted(`\n${message}`));
    console.log(semanticChalk.muted(`\nIterations: ${iterations}`));
    console.log(semanticChalk.muted(`Steps: ${steps.length}`));
    console.log(semanticChalk.muted(`Edits: ${edits.length}`));

    if (edits.length > 0) {
      console.log(semanticChalk.accent('\n📝 File Changes:'));
      edits.forEach(edit => {
        const icon = edit.operation === 'CREATE' ? '➕' : edit.operation === 'DELETE' ? '➖' : '✏️';
        console.log(semanticChalk.info(`  ${icon} ${edit.file} (${edit.operation})`));
      });
    }

    console.log(semanticChalk.accent('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'));
    console.log('');
  }
}


