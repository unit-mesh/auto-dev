/**
 * Output formatting utilities for CLI
 * Provides pretty-printing, code highlighting, and diff display
 */

import chalk from 'chalk';
import { semanticChalk, dividers, coloredStatus } from '../design-system/theme-helpers.js';
import * as diff from 'diff';

export interface FileChange {
  file: string;
  operation: 'create' | 'update' | 'delete';
  oldContent?: string;
  newContent?: string;
}

export class OutputFormatter {
  private quiet: boolean;

  constructor(quiet: boolean = false) {
    this.quiet = quiet;
  }

  /**
   * Print section header
   */
  header(title: string): void {
    console.log('\n' + chalk.bold.cyan('═'.repeat(60)));
    console.log(chalk.bold.cyan(`  ${title}`));
    console.log(chalk.bold.cyan('═'.repeat(60)));
  }

  /**
   * Print subsection
   */
  section(title: string): void {
    console.log('\n' + chalk.bold(`▶ ${title}`));
  }

  /**
   * Print success message
   */
  success(message: string): void {
    console.log(chalk.green('✓') + ' ' + message);
  }

  /**
   * Print error message
   */
  error(message: string): void {
    console.log(chalk.red('✗') + ' ' + chalk.red(message));
  }

  /**
   * Print warning message
   */
  warn(message: string): void {
    console.log(chalk.yellow('⚠') + ' ' + chalk.yellow(message));
  }

  /**
   * Print info message
   */
  info(message: string): void {
    console.log(chalk.blue('ℹ') + ' ' + message);
  }

  /**
   * Print debug message (only if not quiet)
   */
  debug(message: string): void {
    if (!this.quiet) {
      console.log(chalk.gray(`[DEBUG] ${message}`));
    }
  }

  /**
   * Print step progress
   */
  step(current: number, total: number, action: string): void {
    const progress = `[${current}/${total}]`;
    console.log(chalk.bold.blue(progress) + ' ' + action);
  }

  /**
   * Display file change with diff
   */
  displayFileChange(change: FileChange): void {
    console.log('\n' + chalk.bold('─'.repeat(60)));
    
    // File header with operation type
    const opIcon = change.operation === 'create' ? '✨' : 
                   change.operation === 'update' ? '📝' : '🗑️';
    const opColor = change.operation === 'create' ? semanticChalk.success : 
                    change.operation === 'update' ? semanticChalk.warning : semanticChalk.error;
    
    console.log(opColor(opIcon + ' ' + change.operation.toUpperCase()) + ' ' + 
                chalk.bold(change.file));
    
    // Show diff for updates
    if (change.operation === 'update' && change.oldContent && change.newContent) {
      this.displayDiff(change.oldContent, change.newContent);
    } 
    // Show content for creates
    else if (change.operation === 'create' && change.newContent) {
      console.log(chalk.gray('─'.repeat(60)));
      console.log(chalk.green(change.newContent));
    }
    // Show deletion message
    else if (change.operation === 'delete') {
      console.log(chalk.gray('  (file will be deleted)'));
    }
  }

  /**
   * Display unified diff
   */
  private displayDiff(oldContent: string, newContent: string): void {
    const patches = diff.createPatch('file', oldContent, newContent, '', '');
    const lines = patches.split('\n').slice(4); // Skip patch header
    
    console.log(chalk.gray('─'.repeat(60)));
    
    for (const line of lines) {
      if (line.startsWith('+')) {
        console.log(chalk.green(line));
      } else if (line.startsWith('-')) {
        console.log(chalk.red(line));
      } else if (line.startsWith('@@')) {
        console.log(chalk.cyan(line));
      } else {
        console.log(chalk.gray(line));
      }
    }
  }

  /**
   * Display summary statistics
   */
  displaySummary(stats: {
    iterations: number;
    edits: number;
    creates: number;
    updates: number;
    deletes: number;
    duration: number;
  }): void {
    this.header('Summary');
    
    console.log(chalk.bold('Iterations:  ') + semanticChalk.accent(stats.iterations.toString()));
    console.log(chalk.bold('Total Edits: ') + semanticChalk.accent(stats.edits.toString()));
    console.log('  ' + semanticChalk.success('✨ Creates:  ') + stats.creates);
    console.log('  ' + semanticChalk.warning('📝 Updates:  ') + stats.updates);
    console.log('  ' + semanticChalk.error('🗑️  Deletes:  ') + stats.deletes);
    console.log(chalk.bold('Duration:    ') + semanticChalk.accent(`${(stats.duration / 1000).toFixed(2)}s`));
  }

  /**
   * Display progress bar
   */
  progressBar(current: number, total: number, label: string = ''): void {
    const percentage = Math.floor((current / total) * 100);
    const barLength = 40;
    const filledLength = Math.floor((barLength * current) / total);
    const bar = '█'.repeat(filledLength) + '░'.repeat(barLength - filledLength);
    
    process.stdout.write('\r' + semanticChalk.accent(`[${bar}]`) + ` ${percentage}% ${label}`);
    
    if (current === total) {
      console.log(); // New line when complete
    }
  }
}

