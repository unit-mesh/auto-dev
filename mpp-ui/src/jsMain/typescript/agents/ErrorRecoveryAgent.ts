/**
 * Error Recovery Agent - A SubAgent that analyzes and fixes errors
 * 
 * When a command fails (especially shell commands), this agent:
 * 1. Checks if any files were modified (using git diff)
 * 2. Collects error context (error message + diff if available)
 * 3. Calls LLM to analyze and propose a fix
 * 4. Returns the fix to the main agent
 */

import * as path from 'path';
import { exec } from 'child_process';
import { promisify } from 'util';
import { LLMService } from '../services/LLMService.js';
import type { LLMConfig } from '../config/ConfigManager.js';

const execAsync = promisify(exec);

export interface ErrorContext {
  command: string;
  errorMessage: string;
  exitCode?: number;
  stdout?: string;
  stderr?: string;
  affectedFiles?: string[];
  fileDiffs?: Map<string, string>;
}

export interface RecoveryResult {
  success: boolean;
  analysis: string;
  suggestedActions: string[];
  recoveryCommands?: string[];
  shouldRetry: boolean;
  shouldAbort: boolean;
}

export class ErrorRecoveryAgent {
  private projectPath: string;
  private llmService: LLMService;

  constructor(projectPath: string, config: LLMConfig) {
    this.projectPath = projectPath;
    this.llmService = new LLMService(config);
  }

  /**
   * Main entry point - analyze and recover from error
   * Runs as an independent SubAgent with its own progress display
   */
  async analyzeAndRecover(
    errorContext: ErrorContext,
    progressCallback?: (status: string) => void
  ): Promise<RecoveryResult> {
    
    const updateProgress = (status: string) => {
      if (progressCallback) {
        progressCallback(status);
      } else {
        console.log(`   ${status}`);
      }
    };

    console.log('\n┌─────────────────────────────────────────┐');
    console.log('│  🔧 Error Recovery SubAgent            │');
    console.log('└─────────────────────────────────────────┘');
    console.log(`Command: ${errorContext.command}`);
    console.log(`Error:   ${errorContext.errorMessage.substring(0, 80)}...`);

    // Step 1: Check for file modifications
    updateProgress('Checking for file modifications...');
    const modifiedFiles = await this.getModifiedFiles();
    
    // Step 2: Get diffs for modified files
    if (modifiedFiles.length > 0) {
      updateProgress(`Getting diffs for ${modifiedFiles.length} file(s)...`);
    }
    const fileDiffs = await this.getFileDiffs(modifiedFiles);
    
    // Step 3: Build context for LLM
    updateProgress('Building error context...');
    const context = this.buildErrorContext(errorContext, modifiedFiles, fileDiffs);
    
    // Step 4: Ask LLM to analyze and suggest fixes
    updateProgress('🤖 Analyzing error with AI...');
    const analysis = await this.askLLMForFix(context);
    
    updateProgress('✓ Analysis complete');
    console.log('└─────────────────────────────────────────┘\n');
    
    return analysis;
  }

  /**
   * Get list of modified files using git diff
   */
  private async getModifiedFiles(): Promise<string[]> {
    try {
      const { stdout } = await execAsync('git diff --name-only', {
        cwd: this.projectPath
      });
      
      const files = stdout.trim().split('\n').filter(f => f.length > 0);
      
      if (files.length > 0) {
        console.log(`   📝 Modified: ${files.map(f => f.split('/').pop()).join(', ')}`);
      } else {
        console.log(`   ✓ No modifications detected`);
      }
      
      return files;
    } catch (error) {
      console.warn(`   ⚠️  Git check failed: ${error}`);
      return [];
    }
  }

  /**
   * Get diff for each modified file
   */
  private async getFileDiffs(files: string[]): Promise<Map<string, string>> {
    const diffs = new Map<string, string>();
    
    for (const file of files) {
      try {
        const { stdout } = await execAsync(`git diff -- "${file}"`, {
          cwd: this.projectPath
        });
        
        if (stdout.trim().length > 0) {
          diffs.set(file, stdout);
        }
      } catch (error) {
        // Silently skip - will be reported in context
      }
    }
    
    if (diffs.size > 0) {
      console.log(`   📄 Collected ${diffs.size} diff(s)`);
    }
    
    return diffs;
  }

  /**
   * Build comprehensive error context for LLM
   */
  private buildErrorContext(
    errorContext: ErrorContext,
    modifiedFiles: string[],
    fileDiffs: Map<string, string>
  ): string {
    const parts: string[] = [];
    
    parts.push('# Error Recovery Context\n');
    
    // 1. Command that failed
    parts.push('## Failed Command');
    parts.push('```bash');
    parts.push(errorContext.command);
    parts.push('```\n');
    
    // 2. Exit code
    if (errorContext.exitCode !== undefined) {
      parts.push(`**Exit Code:** ${errorContext.exitCode}\n`);
    }
    
    // 3. Error message
    parts.push('## Error Message');
    parts.push('```');
    parts.push(errorContext.errorMessage);
    parts.push('```\n');
    
    // 4. Modified files with diffs (MOST IMPORTANT)
    if (modifiedFiles.length > 0 && fileDiffs.size > 0) {
      parts.push('## ⚠️ Files Modified Before Error');
      parts.push('The following files were changed, which may have caused the error:\n');
      
      for (const [file, diff] of fileDiffs.entries()) {
        parts.push(`### ${file}`);
        parts.push('```diff');
        parts.push(diff);
        parts.push('```\n');
      }
    } else if (modifiedFiles.length > 0) {
      parts.push('## Modified Files');
      parts.push(modifiedFiles.map(f => `- ${f}`).join('\n'));
      parts.push('\n(No diff available)');
    } else {
      parts.push('## No Files Modified');
      parts.push('No changes detected in the repository.\n');
    }
    
    // 5. Additional context
    if (errorContext.stdout && errorContext.stdout.trim().length > 0) {
      parts.push('## Command Output (stdout)');
      parts.push('```');
      parts.push(errorContext.stdout.substring(0, 1000)); // Limit length
      parts.push('```\n');
    }
    
    return parts.join('\n');
  }

  /**
   * Ask LLM to analyze the error and suggest fixes
   */
  private async askLLMForFix(context: string): Promise<RecoveryResult> {
    const systemPrompt = `You are an Error Recovery Agent. Your job is to:
1. Analyze why a command failed
2. Identify the root cause (especially if files were corrupted)
3. Suggest specific fixes

Focus on:
- Build file corruption (build.gradle.kts, pom.xml, package.json, etc.)
- Syntax errors introduced by recent changes
- File permission or path issues

Respond in this JSON format:
{
  "analysis": "Brief explanation of what went wrong",
  "rootCause": "The specific cause (e.g., 'build.gradle.kts was corrupted')",
  "suggestedActions": [
    "Specific action 1",
    "Specific action 2"
  ],
  "recoveryCommands": [
    "git checkout build.gradle.kts",
    "./gradlew build"
  ],
  "shouldRetry": true,
  "shouldAbort": false
}`;

    const userPrompt = `${context}

**Task:** Analyze this error and provide a recovery plan. If files were modified and caused the error, suggest restoring them from git.`;

    try {
      let response = '';
      await this.llmService.streamMessageWithSystem(
        systemPrompt,
        userPrompt,
        (chunk) => {
          response += chunk;
        }
      );

      // Parse JSON response
      const result = this.parseRecoveryResponse(response);
      
      console.log('\n   📋 Analysis:');
      console.log(`      ${result.analysis}`);
      
      if (result.suggestedActions.length > 0) {
        console.log('\n   💡 Suggested Actions:');
        result.suggestedActions.forEach((action, i) => {
          console.log(`      ${i + 1}. ${action}`);
        });
      }
      
      if (result.recoveryCommands && result.recoveryCommands.length > 0) {
        console.log('\n   🔧 Recovery Commands:');
        result.recoveryCommands.forEach(cmd => {
          console.log(`      $ ${cmd}`);
        });
      }
      
      return result;
      
    } catch (error) {
      console.error('   ❌ LLM analysis failed:', error);
      
      // Fallback response
      return {
        success: false,
        analysis: `Failed to analyze error: ${error}`,
        suggestedActions: ['Manual intervention required'],
        shouldRetry: false,
        shouldAbort: true
      };
    }
  }

  /**
   * Parse LLM response into RecoveryResult
   */
  private parseRecoveryResponse(response: string): RecoveryResult {
    try {
      // Try to extract JSON from response (might be wrapped in markdown)
      const jsonMatch = response.match(/```json\s*([\s\S]*?)\s*```/) || 
                       response.match(/\{[\s\S]*\}/);
      
      if (jsonMatch) {
        const jsonStr = jsonMatch[1] || jsonMatch[0];
        const parsed = JSON.parse(jsonStr);
        
        return {
          success: true,
          analysis: parsed.analysis || parsed.rootCause || 'Unknown error',
          suggestedActions: parsed.suggestedActions || [],
          recoveryCommands: parsed.recoveryCommands,
          shouldRetry: parsed.shouldRetry !== false,
          shouldAbort: parsed.shouldAbort === true
        };
      }
    } catch (error) {
      console.warn('   ⚠️  Could not parse JSON response, using raw text');
    }
    
    // Fallback: use raw response as analysis
    return {
      success: true,
      analysis: response,
      suggestedActions: this.extractActionsFromText(response),
      shouldRetry: response.toLowerCase().includes('retry'),
      shouldAbort: response.toLowerCase().includes('abort')
    };
  }

  /**
   * Extract action items from free-form text
   */
  private extractActionsFromText(text: string): string[] {
    const actions: string[] = [];
    const lines = text.split('\n');
    
    for (const line of lines) {
      // Look for lines that start with numbers, bullets, or action words
      if (/^(\d+\.|[-*]|\s*-)\s*/.test(line)) {
        const action = line.replace(/^(\d+\.|[-*]|\s*-)\s*/, '').trim();
        if (action.length > 0 && action.length < 200) {
          actions.push(action);
        }
      }
    }
    
    return actions;
  }

  /**
   * Execute recovery commands (with confirmation)
   */
  async executeRecovery(recoveryCommands: string[]): Promise<boolean> {
    console.log('\n🔄 Executing recovery commands...');
    
    for (const cmd of recoveryCommands) {
      console.log(`   $ ${cmd}`);
      
      try {
        const { stdout, stderr } = await execAsync(cmd, {
          cwd: this.projectPath
        });
        
        if (stdout) console.log(`   ${stdout.trim()}`);
        if (stderr) console.warn(`   ${stderr.trim()}`);
        
        console.log('   ✓ Success');
      } catch (error: any) {
        console.error(`   ✗ Failed: ${error.message}`);
        return false;
      }
    }
    
    return true;
  }
}

