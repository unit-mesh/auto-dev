/**
 * CLI Renderer for CodingAgent
 * Implements JsCodingAgentRenderer interface from Kotlin
 */

import chalk from 'chalk';

/**
 * Parse code blocks from LLM response
 * Simplified version of Kotlin's CodeFence.parseAll
 */
interface CodeBlock {
  languageId: string;
  text: string;
}

function parseCodeBlocks(response: string): CodeBlock[] {
  const blocks: CodeBlock[] = [];
  const lines = response.split('\n');

  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    const trimmed = line.trim();

    // Check for devin block
    if (trimmed === '<devin>') {
      const devinLines: string[] = [];
      i++;
      while (i < lines.length && lines[i].trim() !== '</devin>') {
        devinLines.push(lines[i]);
        i++;
      }
      blocks.push({
        languageId: 'devin',
        text: devinLines.join('\n').trim()
      });
      i++; // Skip </devin>
      continue;
    }

    // Check for markdown code block
    if (trimmed.startsWith('```')) {
      const lang = trimmed.substring(3).trim();
      const codeLines: string[] = [];
      i++;
      while (i < lines.length && !lines[i].trim().startsWith('```')) {
        codeLines.push(lines[i]);
        i++;
      }
      blocks.push({
        languageId: lang,
        text: codeLines.join('\n')
      });
      i++; // Skip closing ```
      continue;
    }

    // Regular text - collect until next code block
    const textLines: string[] = [];
    while (i < lines.length) {
      const currentLine = lines[i];
      const currentTrimmed = currentLine.trim();
      if (currentTrimmed.startsWith('```') || currentTrimmed === '<devin>') {
        break;
      }
      textLines.push(currentLine);
      i++;
    }

    if (textLines.length > 0) {
      blocks.push({
        languageId: 'markdown',
        text: textLines.join('\n').trim()
      });
    }
  }

  return blocks;
}

/**
 * CLI Renderer implementation
 */
export class CliRenderer {
  // Required by Kotlin JS export interface
  readonly __doNotUseOrImplementIt: any = {};

  private reasoningBuffer: string = '';
  private isInDevinBlock: boolean = false;

  renderIterationHeader(current: number, max: number): void {
    console.log(`\n${chalk.bold.blue(`[${current}/${max}]`)} Analyzing and executing...`);
  }

  renderLLMResponseStart(): void {
    this.reasoningBuffer = '';
    this.isInDevinBlock = false;
    process.stdout.write(chalk.gray('💭 '));
  }

  renderLLMResponseChunk(chunk: string): void {
    // Parse chunk to detect devin blocks
    this.reasoningBuffer += chunk;

    // Check if we're entering or leaving a devin block
    if (this.reasoningBuffer.includes('<devin>')) {
      this.isInDevinBlock = true;
    }
    if (this.reasoningBuffer.includes('</devin>')) {
      this.isInDevinBlock = false;
    }

    // Only print if not in devin block
    if (!this.isInDevinBlock && !chunk.includes('<devin>') && !chunk.includes('</devin>')) {
      process.stdout.write(chalk.white(chunk));
    }
  }

  renderLLMResponseEnd(): void {
    console.log('\n');
  }

  renderToolCall(toolName: string, paramsStr: string): void {
    console.log(chalk.cyan('🔧 ') + `/${toolName} ${paramsStr}`);
  }

  renderToolResult(toolName: string, success: boolean, output: string | null, fullOutput: string | null): void {
    const icon = success ? chalk.green('✓') : chalk.red('✗');
    let line = `   ${icon} ${toolName}`;

    if (success && output) {
      // For read-file, show full content (no truncation) so LLM can see complete file
      // For other tools, show preview (300 chars)
      const shouldTruncate = toolName !== 'read-file';
      const maxLength = shouldTruncate ? 300 : Number.MAX_SAFE_INTEGER;

      const preview = output.substring(0, Math.min(output.length, maxLength)).replace(/\n/g, ' ');
      if (preview.length > 0 && !preview.startsWith('Successfully')) {
        line += chalk.gray(` → ${preview}`);
        if (shouldTruncate && output.length > maxLength) {
          line += chalk.gray('...');
        }
      }
    } else if (!success && output) {
      line += chalk.red(` → ${output.substring(0, 300)}`);
    }

    console.log(line);
  }

  renderTaskComplete(): void {
    console.log(chalk.green('\n✓ Task marked as complete\n'));
  }

  renderFinalResult(success: boolean, message: string, iterations: number): void {
    console.log();
    if (success) {
      console.log(chalk.green('✅ Task completed successfully'));
    } else {
      console.log(chalk.yellow('⚠️  Task incomplete'));
    }
    console.log(chalk.gray(`Task completed after ${iterations} iterations`));
  }

  renderError(message: string): void {
    console.log(chalk.red('❌ ') + message);
  }

  renderRepeatWarning(toolName: string, count: number): void {
    console.log(chalk.yellow(`⚠️  Warning: Tool '${toolName}' has been called ${count} times in a row`));
  }
}

