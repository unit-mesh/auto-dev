/**
 * LogSummaryAgent - AI SubAgent for summarizing long command outputs
 * 
 * When shell commands produce lengthy logs (e.g., build outputs, test results),
 * this agent analyzes and summarizes them into key insights.
 * 
 * Similar to Cursor's "Running Command" tool design.
 */

import { LLMService } from './LLMService.js';
import type { LLMConfig } from '../config/ConfigManager.js';

export interface LogSummaryContext {
  command: string;
  output: string;
  exitCode: number;
  executionTime: number;
}

export interface LogSummaryResult {
  success: boolean;
  summary: string;
  keyPoints: string[];
  errors: string[];
  warnings: string[];
  statistics?: {
    totalLines: number;
    errorCount: number;
    warningCount: number;
  };
  nextSteps?: string[];
}

export class LogSummaryAgent {
  private llmService: LLMService;
  private threshold: number; // Minimum output length to trigger summarization

  constructor(config: LLMConfig, threshold: number = 2000) {
    this.llmService = new LLMService(config);
    this.threshold = threshold;
  }

  /**
   * Check if output needs summarization
   */
  needsSummarization(output: string): boolean {
    return output.length > this.threshold;
  }

  /**
   * Analyze and summarize command output
   */
  async summarize(
    context: LogSummaryContext,
    progressCallback?: (status: string) => void
  ): Promise<LogSummaryResult> {
    progressCallback?.('Starting log analysis...');

    // Quick heuristic analysis first
    const heuristics = this.quickAnalysis(context);
    progressCallback?.('Performing AI analysis...');

    // Build prompt for AI analysis
    const prompt = this.buildAnalysisPrompt(context, heuristics);
    
    try {
      let aiResponse = '';
      await this.llmService.streamMessageWithSystem(
        this.getSystemPrompt(),
        prompt,
        (chunk) => {
          aiResponse += chunk;
        }
      );

      progressCallback?.('Parsing results...');
      const result = this.parseResponse(aiResponse, heuristics, context);
      
      return result;
    } catch (error) {
      // Fallback to heuristic analysis if AI fails
      progressCallback?.('AI analysis failed, using heuristics');
      return this.heuristicFallback(context, heuristics);
    }
  }

  /**
   * Quick heuristic analysis of the output
   */
  private quickAnalysis(context: LogSummaryContext): {
    totalLines: number;
    errorCount: number;
    warningCount: number;
    hasTestResults: boolean;
    hasBuildInfo: boolean;
    successIndicators: number;
    failureIndicators: number;
  } {
    const lines = context.output.split('\n');
    const lowerOutput = context.output.toLowerCase();
    
    let errorCount = 0;
    let warningCount = 0;
    let successIndicators = 0;
    let failureIndicators = 0;

    for (const line of lines) {
      const lower = line.toLowerCase();
      
      // Count errors
      if (lower.includes('error') || lower.includes('failed') || lower.includes('exception')) {
        errorCount++;
      }
      
      // Count warnings
      if (lower.includes('warn') || lower.includes('deprecated')) {
        warningCount++;
      }
      
      // Success indicators
      if (lower.includes('success') || lower.includes('passed') || lower.includes('✓')) {
        successIndicators++;
      }
      
      // Failure indicators
      if (lower.includes('fail') || lower.includes('✗')) {
        failureIndicators++;
      }
    }

    return {
      totalLines: lines.length,
      errorCount,
      warningCount,
      hasTestResults: lowerOutput.includes('test') && (lowerOutput.includes('passed') || lowerOutput.includes('failed')),
      hasBuildInfo: lowerOutput.includes('build') && (lowerOutput.includes('success') || lowerOutput.includes('failed')),
      successIndicators,
      failureIndicators
    };
  }

  /**
   * System prompt for the AI
   */
  private getSystemPrompt(): string {
    return `You are a Log Summary Agent specialized in analyzing command-line output.

Your task is to analyze build logs, test results, and command outputs, then provide:
1. A concise summary (1-2 sentences)
2. Key points (3-5 bullet points)
3. Any errors found
4. Any warnings found
5. Suggested next steps (if applicable)

Response format (JSON):
{
  "summary": "Brief overview",
  "keyPoints": ["point1", "point2", ...],
  "errors": ["error1", "error2", ...],
  "warnings": ["warning1", "warning2", ...],
  "nextSteps": ["step1", "step2", ...]
}

Keep it concise and actionable. Focus on what matters most.`;
  }

  /**
   * Build analysis prompt
   */
  private buildAnalysisPrompt(context: LogSummaryContext, heuristics: any): string {
    // Truncate very long outputs for AI analysis
    const maxCharsForAI = 8000;
    let outputForAI = context.output;
    
    if (context.output.length > maxCharsForAI) {
      // Include beginning and end of output
      const headSize = Math.floor(maxCharsForAI * 0.6);
      const tailSize = Math.floor(maxCharsForAI * 0.4);
      outputForAI = 
        context.output.substring(0, headSize) +
        '\n\n... [truncated ' + (context.output.length - maxCharsForAI) + ' chars] ...\n\n' +
        context.output.substring(context.output.length - tailSize);
    }

    return `Analyze this command output:

**Command**: \`${context.command}\`
**Exit Code**: ${context.exitCode}
**Execution Time**: ${context.executionTime}ms
**Output Length**: ${context.output.length} chars, ${heuristics.totalLines} lines

**Heuristic Analysis**:
- Errors detected: ${heuristics.errorCount}
- Warnings detected: ${heuristics.warningCount}
- Success indicators: ${heuristics.successIndicators}
- Failure indicators: ${heuristics.failureIndicators}
- Contains test results: ${heuristics.hasTestResults}
- Contains build info: ${heuristics.hasBuildInfo}

**Output**:
\`\`\`
${outputForAI}
\`\`\`

Provide a JSON summary as specified in your system prompt.`;
  }

  /**
   * Parse AI response into structured result
   */
  private parseResponse(
    aiResponse: string,
    heuristics: any,
    context: LogSummaryContext
  ): LogSummaryResult {
    try {
      // Try to extract JSON from response
      const jsonMatch = aiResponse.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        const parsed = JSON.parse(jsonMatch[0]);
        return {
          success: context.exitCode === 0,
          summary: parsed.summary || 'Command executed',
          keyPoints: parsed.keyPoints || [],
          errors: parsed.errors || [],
          warnings: parsed.warnings || [],
          statistics: {
            totalLines: heuristics.totalLines,
            errorCount: heuristics.errorCount,
            warningCount: heuristics.warningCount
          },
          nextSteps: parsed.nextSteps || []
        };
      }
    } catch (e) {
      // Parsing failed, use fallback
    }

    // Fallback: extract from plain text
    return this.heuristicFallback(context, heuristics);
  }

  /**
   * Fallback analysis using heuristics only
   */
  private heuristicFallback(
    context: LogSummaryContext,
    heuristics: any
  ): LogSummaryResult {
    const success = context.exitCode === 0;
    const lines = context.output.split('\n');
    
    // Extract error lines
    const errors = lines
      .filter(line => 
        line.toLowerCase().includes('error') || 
        line.toLowerCase().includes('exception') ||
        line.toLowerCase().includes('failed')
      )
      .slice(0, 5); // Top 5 errors
    
    // Extract warning lines
    const warnings = lines
      .filter(line => 
        line.toLowerCase().includes('warn') || 
        line.toLowerCase().includes('deprecated')
      )
      .slice(0, 3); // Top 3 warnings

    // Generate summary
    let summary = '';
    if (success) {
      if (heuristics.hasBuildInfo) {
        summary = `Build completed successfully in ${context.executionTime}ms`;
      } else if (heuristics.hasTestResults) {
        summary = `Tests completed in ${context.executionTime}ms`;
      } else {
        summary = `Command completed successfully in ${context.executionTime}ms`;
      }
    } else {
      summary = `Command failed with exit code ${context.exitCode}`;
    }

    // Generate key points
    const keyPoints: string[] = [];
    if (heuristics.totalLines > 100) {
      keyPoints.push(`Output contains ${heuristics.totalLines} lines`);
    }
    if (heuristics.errorCount > 0) {
      keyPoints.push(`Found ${heuristics.errorCount} error messages`);
    }
    if (heuristics.warningCount > 0) {
      keyPoints.push(`Found ${heuristics.warningCount} warnings`);
    }
    if (heuristics.hasTestResults) {
      keyPoints.push('Contains test execution results');
    }

    return {
      success,
      summary,
      keyPoints,
      errors,
      warnings,
      statistics: {
        totalLines: heuristics.totalLines,
        errorCount: heuristics.errorCount,
        warningCount: heuristics.warningCount
      },
      nextSteps: success ? [] : ['Check error messages above', 'Fix the issues and retry']
    };
  }

  /**
   * Format summary for display
   */
  static formatSummary(result: LogSummaryResult): string {
    const lines: string[] = [];
    
    lines.push(`📊 Summary: ${result.summary}`);
    
    if (result.keyPoints.length > 0) {
      lines.push('\n🔍 Key Points:');
      result.keyPoints.forEach(point => lines.push(`  • ${point}`));
    }
    
    if (result.errors.length > 0) {
      lines.push('\n❌ Errors:');
      result.errors.forEach(error => lines.push(`  • ${error.trim()}`));
    }
    
    if (result.warnings.length > 0) {
      lines.push('\n⚠️  Warnings:');
      result.warnings.forEach(warning => lines.push(`  • ${warning.trim()}`));
    }
    
    if (result.statistics) {
      lines.push(`\n📈 Statistics: ${result.statistics.totalLines} lines, ${result.statistics.errorCount} errors, ${result.statistics.warningCount} warnings`);
    }
    
    if (result.nextSteps && result.nextSteps.length > 0) {
      lines.push('\n💡 Next Steps:');
      result.nextSteps.forEach(step => lines.push(`  • ${step}`));
    }
    
    return lines.join('\n');
  }
}

