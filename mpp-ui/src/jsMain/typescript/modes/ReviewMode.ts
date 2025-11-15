/**
 * Review Mode - Automated code review with linting and AI analysis
 * 
 * Optimized Version: Uses Data-Driven approach instead of Tool-Driven
 * - Pre-collects all data (code content, lint results, diff context)
 * - Single LLM call with structured prompt
 * - ~87% token savings compared to tool-driven approach
 */

import mppCore from '@autodev/mpp-core';
import { semanticChalk } from '../design-system/theme-helpers.js';

const { cc: KotlinCC } = mppCore;

export interface ReviewOptions {
  projectPath: string;
  diff?: string;           // Git diff content
  commitHash?: string;     // Specific commit to review
  baseBranch?: string;     // Base branch for comparison
  compareWith?: string;    // Branch/commit to compare with
  reviewType?: 'COMPREHENSIVE' | 'SECURITY' | 'PERFORMANCE' | 'STYLE';
  skipLint?: boolean;      // Skip linting phase
  autoFix?: boolean;       // Attempt to auto-fix issues
}

export interface ReviewFinding {
  severity: string;
  category: string;
  description: string;
  filePath?: string;
  lineNumber?: number;
  suggestion?: string;
}

export interface ReviewResult {
  success: boolean;
  message: string;
  findings: ReviewFinding[];
  lintResults?: Array<{
    filePath: string;
    issues: Array<{
      line: number;
      column: number;
      severity: string;
      message: string;
      rule?: string;
    }>;
  }>;
  analysisOutput?: string;  // Raw AI analysis for debugging
}

/**
 * Run code review on a project or specific changes
 * 
 * Uses Kotlin's CodeReviewAgent with optimizations:
 * 1. Fetch git diff and extract file paths
 * 2. Detect linters (for display)
 * 3. Use CodeReviewAgent with proper prompt rendering
 */
export async function runReview(
  options: ReviewOptions,
  llmService: any,
  renderer?: any
): Promise<ReviewResult> {
  const { projectPath, diff, reviewType = 'COMPREHENSIVE', skipLint = false } = options;
  const startTime = Date.now();

  console.log(semanticChalk.info(`\n🚀 AutoDev Code Review`));
  console.log(semanticChalk.muted(`Project: ${projectPath}`));
  console.log(semanticChalk.muted(`Review Type: ${reviewType}`));
  console.log();

  try {
    // ===== STEP 1: Fetch Git Diff =====
    const { diffContent, filePaths } = await fetchGitDiff(options);
    
    if (filePaths.length === 0) {
      console.log(semanticChalk.warning('⚠️  No changes found to review'));
      return {
        success: true,
        message: 'No changes found to review',
        findings: []
      };
    }

    // ===== STEP 2: Detect Linters (for display) =====
    if (!skipLint && filePaths.length > 0) {
      try {
        const linterRegistry = KotlinCC.unitmesh.agent.linter.JsLinterRegistry;
        const linterSummary = await linterRegistry.getLinterSummaryForFiles(filePaths);

        if (linterSummary.totalLinters > 0 && linterSummary.availableLinters.length > 0) {
          console.log(semanticChalk.info('🔍 Detecting linters...'));
          console.log(semanticChalk.success(`✅ Available Linters (${linterSummary.availableLinters.length}):`));
          linterSummary.availableLinters.forEach((linter: any) => {
            console.log(semanticChalk.muted(`  - ${linter.name}`));
          });
          console.log();
        }
      } catch (error: any) {
        console.log(semanticChalk.warning(`Linter detection failed: ${error.message}`));
      }
    }

    // ===== STEP 3: Use Kotlin's CodeReviewAgent (Proper Implementation) =====
    console.log(semanticChalk.info('🤖 Running AI code review...'));
    console.log();

    // Create review task
    const task = new KotlinCC.unitmesh.agent.JsReviewTask(
      filePaths,
      reviewType,
      projectPath,
      diffContent.length > 5000 ? `Git diff (first 5000 chars):\n${diffContent.substring(0, 5000)}...` : `Git diff:\n${diffContent}`
    );

    // Create code review agent (uses proper prompt renderer)
    const reviewAgent = new KotlinCC.unitmesh.agent.JsCodeReviewAgent(
      projectPath,
      llmService,
      50,
      renderer || null,
      null,
      null
    );

    const llmStartTime = Date.now();

    // Execute review (uses CodeReviewAgentExecutor and proper prompts)
    const result = await reviewAgent.executeTask(task);

    const llmDuration = Date.now() - llmStartTime;
    const totalDuration = Date.now() - startTime;

    console.log();
    console.log(semanticChalk.success('✅ Code review complete!'));
    console.log(semanticChalk.muted(`⏱️  Total: ${totalDuration}ms (AI: ${llmDuration}ms)`));
    console.log();

    // Display findings
    if (result.findings && result.findings.length > 0) {
      displayKotlinFindings(result.findings);
    } else {
      console.log(semanticChalk.success('✨ No issues found! Code looks good.'));
      console.log();
    }

    return {
      success: result.success,
      message: result.message,
      findings: result.findings ? result.findings.map((f: any) => ({
        severity: f.severity,
        category: f.category,
        description: f.description,
        filePath: f.filePath,
        lineNumber: f.lineNumber,
        suggestion: f.suggestion
      })) : [],
      analysisOutput: result.message
    };

  } catch (error: any) {
    console.error(semanticChalk.error(`Review failed: ${error.message}`));
    console.error(error.stack);
    return {
      success: false,
      message: `Review failed: ${error.message}`,
      findings: []
    };
  }
}

// ==================== Helper Functions ====================

/**
 * Fetch git diff and extract file paths
 */
async function fetchGitDiff(options: ReviewOptions): Promise<{ diffContent: string; filePaths: string[] }> {
  const { projectPath, diff, commitHash, baseBranch, compareWith } = options;
  let diffContent = diff;

  if (!diffContent) {
    console.log(semanticChalk.info('📥 Fetching git diff...'));

    try {
      const { execSync } = await import('child_process');

      if (commitHash) {
        diffContent = execSync(`git show ${commitHash}`, {
          cwd: projectPath,
          encoding: 'utf-8'
        });
      } else if (baseBranch && compareWith) {
        diffContent = execSync(`git diff ${baseBranch}...${compareWith}`, {
          cwd: projectPath,
          encoding: 'utf-8'
        });
      } else {
        // Default: review the last commit (HEAD)
        diffContent = execSync('git show HEAD', {
          cwd: projectPath,
          encoding: 'utf-8'
        });
      }
    } catch (error: any) {
      throw new Error(`Failed to get git diff: ${error.message}`);
    }
  }

  if (!diffContent || diffContent.trim().length === 0) {
    return { diffContent: '', filePaths: [] };
  }

  // Parse diff to get changed files
  console.log(semanticChalk.info('📝 Parsing diff...'));

  try {
    const filePaths = KotlinCC.unitmesh.agent.JsDiffUtils.extractFilePaths(diffContent);
    console.log(semanticChalk.success(`Found ${filePaths.length} changed files`));

    filePaths.forEach((file: string) => {
      console.log(semanticChalk.muted(`  - ${file}`));
    });
    console.log();

    return { diffContent, filePaths };
  } catch (error: any) {
    throw new Error(`Failed to parse diff: ${error.message}`);
  }
}

/**
 * Display findings from Kotlin CodeReviewAgent
 */
function displayKotlinFindings(findings: any[]): void {
  console.log(semanticChalk.info(`📋 Found ${findings.length} findings:`));
  console.log();

  // Group by severity
  const critical = findings.filter(f => f.severity === 'CRITICAL');
  const high = findings.filter(f => f.severity === 'HIGH');
  const medium = findings.filter(f => f.severity === 'MEDIUM');
  const low = findings.filter(f => f.severity === 'LOW' || f.severity === 'INFO');

  if (critical.length > 0) {
    console.log(semanticChalk.error(`🔴 CRITICAL (${critical.length}):`));
    critical.forEach(f => {
      const location = f.lineNumber ? `${f.filePath}:${f.lineNumber}` : f.filePath || 'N/A';
      console.log(semanticChalk.error(`  - ${f.description}`));
      console.log(semanticChalk.muted(`    📍 ${location}`));
      if (f.suggestion) {
        console.log(semanticChalk.muted(`    💡 ${f.suggestion.substring(0, 100)}${f.suggestion.length > 100 ? '...' : ''}`));
      }
    });
    console.log();
  }

  if (high.length > 0) {
    console.log(semanticChalk.warning(`🟠 HIGH (${high.length}):`));
    high.forEach(f => {
      const location = f.lineNumber ? `${f.filePath}:${f.lineNumber}` : f.filePath || 'N/A';
      console.log(semanticChalk.warning(`  - ${f.description}`));
      console.log(semanticChalk.muted(`    📍 ${location}`));
      if (f.suggestion) {
        console.log(semanticChalk.muted(`    💡 ${f.suggestion.substring(0, 100)}${f.suggestion.length > 100 ? '...' : ''}`));
      }
    });
    console.log();
  }

  if (medium.length > 0) {
    console.log(semanticChalk.info(`🟡 MEDIUM (${medium.length}):`));
    medium.forEach(f => {
      const location = f.lineNumber ? `${f.filePath}:${f.lineNumber}` : f.filePath || 'N/A';
      console.log(semanticChalk.info(`  - ${f.description}`));
      console.log(semanticChalk.muted(`    📍 ${location}`));
    });
    console.log();
  }

  if (low.length > 0) {
    console.log(semanticChalk.muted(`🟢 LOW (${low.length}):`));
    low.forEach(f => {
      const location = f.lineNumber ? `${f.filePath}:${f.lineNumber}` : f.filePath || 'N/A';
      console.log(semanticChalk.muted(`  - ${f.description}`));
      console.log(semanticChalk.muted(`    📍 ${location}`));
    });
    console.log();
  }
}


