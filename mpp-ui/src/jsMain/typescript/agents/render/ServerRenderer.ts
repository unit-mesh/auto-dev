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

  /**
   * Render an event from the server
   */
  renderEvent(event: AgentEvent): void {
    switch (event.type) {
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
    // Buffer LLM output and print when we have a complete thought
    this.llmBuffer += chunk;

    // Print if we have a newline or enough content
    if (chunk.includes('\n') || this.llmBuffer.length > 100) {
      process.stdout.write(semanticChalk.muted(chunk));
    }
  }

  private renderToolCall(toolName: string, params: string): void {
    // Flush any buffered LLM output
    if (this.llmBuffer.trim()) {
      console.log(this.llmBuffer);
      this.llmBuffer = '';
    }

    // Parse params to get a friendly description
    let description = toolName;
    try {
      const paramsObj = JSON.parse(params);

      // Create friendly descriptions based on tool type
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

    console.log(semanticChalk.info(`● ${description}`));

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

      // Render based on tool type
      if (toolName === 'read-file') {
        if (success && output) {
          const lines = output.split('\n');
          console.log(semanticChalk.muted(`  ⎿ Reading file: ${paramsObj.path}`));
          console.log(semanticChalk.muted(`  ⎿ Read ${lines.length} lines`));

          // Show preview (first 15 lines)
          if (lines.length > 0) {
            console.log(semanticChalk.muted('────────────────────────────────────────────────────────────'));
            const preview = lines.slice(0, 15);
            preview.forEach((line, i) => {
              console.log(semanticChalk.muted(`${String(i + 1).padStart(3, ' ')} │ ${line}`));
            });
            if (lines.length > 15) {
              console.log(semanticChalk.muted(`... (${lines.length - 15} more lines)`));
            }
            console.log(semanticChalk.muted('────────────────────────────────────────────────────────────'));
          }
        } else {
          console.log(semanticChalk.error(`  ⎿ Failed to read file: ${output || 'Unknown error'}`));
        }
      } else if (toolName === 'write-file') {
        if (success) {
          console.log(semanticChalk.success(`  ⎿ File written: ${paramsObj.path}`));
        } else {
          console.log(semanticChalk.error(`  ⎿ Failed to write file: ${output || 'Unknown error'}`));
        }
      } else if (toolName === 'edit-file') {
        if (success) {
          console.log(semanticChalk.success(`  ⎿ File edited: ${paramsObj.path}`));
        } else {
          console.log(semanticChalk.error(`  ⎿ Failed to edit file: ${output || 'Unknown error'}`));
        }
      } else if (toolName === 'glob') {
        if (success && output) {
          const files = output.split('\n').filter(f => f.trim());
          console.log(semanticChalk.muted(`  ⎿ Searching for files matching pattern: ${paramsObj.pattern}`));
          console.log(semanticChalk.success(`  ⎿ Found ${files.length} files`));
        } else {
          console.log(semanticChalk.error(`  ⎿ Search failed: ${output || 'Unknown error'}`));
        }
      } else if (toolName === 'grep') {
        if (success && output) {
          const matches = output.split('\n').filter(m => m.trim());
          console.log(semanticChalk.muted(`  ⎿ Searching for: ${paramsObj.pattern}`));
          console.log(semanticChalk.success(`  ⎿ Found ${matches.length} matches`));
        } else {
          console.log(semanticChalk.error(`  ⎿ Search failed: ${output || 'Unknown error'}`));
        }
      } else if (toolName === 'shell') {
        if (success) {
          console.log(semanticChalk.success(`  ⎿ Command executed`));
          if (output) {
            console.log(semanticChalk.muted(`  ⎿ Output: ${output.substring(0, 200)}${output.length > 200 ? '...' : ''}`));
          }
        } else {
          console.log(semanticChalk.error(`  ⎿ Command failed: ${output || 'Unknown error'}`));
        }
      } else {
        // Generic tool result
        if (success) {
          console.log(semanticChalk.success(`  ⎿ ${output || 'Success'}`));
        } else {
          console.log(semanticChalk.error(`  ⎿ ${output || 'Failed'}`));
        }
      }
    } catch (e) {
      // Fallback if params parsing fails
      if (success) {
        console.log(semanticChalk.success(`  ⎿ ${output || 'Success'}`));
      } else {
        console.log(semanticChalk.error(`  ⎿ ${output || 'Failed'}`));
      }
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

