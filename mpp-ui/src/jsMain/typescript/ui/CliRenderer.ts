/**
 * CLI Renderer for CodingAgent
 * Provides developer-friendly output for autonomous coding tasks
 */

import chalk from 'chalk';
import { parseCodeBlocksSync } from '../utils/renderUtils.js';

export interface ToolCall {
  name: string;
  params: Record<string, any>;
}

export interface ToolResult {
  success: boolean;
  output?: string;
  error?: string;
}

export class CliRenderer {
  private quiet: boolean;

  constructor(quiet: boolean = false) {
    this.quiet = quiet;
  }

  /**
   * Render iteration header
   */
  renderIterationHeader(current: number, max: number): void {
    console.log(`\n${chalk.bold.blue(`[${current}/${max}]`)} Analyzing and executing...`);
  }

  /**
   * Render LLM response with tool calls and reasoning
   */
  renderLLMResponse(response: string): void {
    console.log('\n' + chalk.gray('━'.repeat(60)));
    
    // Parse code blocks to find devin blocks and reasoning
    const blocks = parseCodeBlocksSync(response);
    
    const toolCalls: string[] = [];
    const reasoningParts: string[] = [];
    
    for (const block of blocks) {
      if (block.languageId === 'devin') {
        // Extract tool calls
        const lines = block.text.split('\n');
        for (const line of lines) {
          const trimmed = line.trim();
          if (trimmed.startsWith('/')) {
            toolCalls.push(trimmed);
          }
        }
      } else if (!block.languageId) {
        // Plain text - reasoning
        reasoningParts.push(block.text);
      }
    }
    
    // Display tool calls
    if (toolCalls.length > 0) {
      for (const call of toolCalls) {
        console.log(chalk.cyan('🔧 ') + call);
      }
    }
    
    // Display reasoning (first sentence only)
    if (reasoningParts.length > 0) {
      const reasoning = reasoningParts.join(' ').trim();
      if (reasoning.length > 0) {
        const firstSentence = reasoning.split(/[.!?]/)[0]?.trim() || '';
        if (firstSentence.length > 10) {
          const display = firstSentence.length > 100 
            ? firstSentence.substring(0, 100) + '...'
            : firstSentence;
          console.log(chalk.gray('💭 ') + display);
        }
      }
    }
    
    console.log(chalk.gray('━'.repeat(60)) + '\n');
  }

  /**
   * Render tool execution result
   */
  renderToolResult(toolName: string, result: ToolResult): void {
    const icon = result.success ? chalk.green('✓') : chalk.red('✗');
    let output = `   ${icon} ${toolName}`;
    
    if (result.success && result.output) {
      // Show preview of output
      const preview = result.output.substring(0, 60).replace(/\n/g, ' ');
      if (preview.length > 0 && !preview.startsWith('Successfully')) {
        output += chalk.gray(` → ${preview}`);
        if (result.output.length > 60) {
          output += chalk.gray('...');
        }
      }
    } else if (!result.success && result.error) {
      output += chalk.red(` → ${result.error.substring(0, 60)}`);
    }
    
    console.log(output);
  }

  /**
   * Render file change notification
   */
  renderFileChange(operation: 'create' | 'update' | 'delete', filePath: string): void {
    const icons = {
      create: chalk.green('✨ CREATE'),
      update: chalk.yellow('📝 UPDATE'),
      delete: chalk.red('🗑️  DELETE')
    };
    
    console.log(`   ${icons[operation]} ${chalk.bold(filePath)}`);
  }

  /**
   * Render code block with syntax highlighting
   */
  renderCodeBlock(language: string, code: string, maxLines: number = 10): void {
    const lines = code.split('\n');
    const displayLines = lines.slice(0, maxLines);
    
    console.log(chalk.gray(`\n   ┌─ [${language}] ─`));
    for (const line of displayLines) {
      console.log(chalk.gray('   │ ') + line);
    }
    
    if (lines.length > maxLines) {
      console.log(chalk.gray(`   │ ... (${lines.length - maxLines} more lines)`));
    }
    
    console.log(chalk.gray('   └─'));
  }

  /**
   * Render task completion message
   */
  renderTaskComplete(): void {
    console.log(chalk.green('\n✓ Task marked as complete\n'));
  }

  /**
   * Render final result
   */
  renderFinalResult(success: boolean, message: string, iterations: number): void {
    console.log();
    if (success) {
      console.log(chalk.green('✅ Task completed successfully'));
    } else {
      console.log(chalk.yellow('⚠️  Task incomplete'));
    }
    console.log(chalk.gray(`Task completed after ${iterations} iterations`));
  }

  /**
   * Render error message
   */
  renderError(message: string): void {
    console.log(chalk.red('❌ ') + message);
  }

  /**
   * Render warning message
   */
  renderWarning(message: string): void {
    console.log(chalk.yellow('⚠️  ') + message);
  }

  /**
   * Render info message
   */
  renderInfo(message: string): void {
    if (!this.quiet) {
      console.log(chalk.blue('ℹ ') + message);
    }
  }

  /**
   * Render debug message
   */
  renderDebug(message: string): void {
    if (!this.quiet) {
      console.log(chalk.gray(`[DEBUG] ${message}`));
    }
  }

  /**
   * Render section separator
   */
  renderSeparator(): void {
    console.log(chalk.gray('─'.repeat(60)));
  }

  /**
   * Render summary statistics
   */
  renderSummary(stats: {
    iterations: number;
    steps: number;
    edits: number;
    creates?: number;
    updates?: number;
    deletes?: number;
  }): void {
    console.log('\n' + chalk.bold.cyan('Summary'));
    console.log(chalk.gray('─'.repeat(60)));
    console.log(`${chalk.bold('Iterations:')} ${chalk.cyan(stats.iterations)}`);
    console.log(`${chalk.bold('Steps:')}      ${chalk.cyan(stats.steps)}`);
    console.log(`${chalk.bold('Edits:')}      ${chalk.cyan(stats.edits)}`);
    
    if (stats.creates !== undefined) {
      console.log(`  ${chalk.green('✨ Creates:')}  ${stats.creates}`);
    }
    if (stats.updates !== undefined) {
      console.log(`  ${chalk.yellow('📝 Updates:')}  ${stats.updates}`);
    }
    if (stats.deletes !== undefined) {
      console.log(`  ${chalk.red('🗑️  Deletes:')}  ${stats.deletes}`);
    }
  }
}

