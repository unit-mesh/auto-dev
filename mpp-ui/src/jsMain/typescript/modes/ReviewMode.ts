/**
 * Review Mode - Automated code review with linting and AI analysis
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

export interface ReviewResult {
  success: boolean;
  message: string;
  findings: Array<{
    severity: string;
    category: string;
    description: string;
    filePath?: string;
    lineNumber?: number;
    suggestion?: string;
  }>;
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
}

/**
 * Run code review on a project or specific changes
 */
export async function runReview(
  options: ReviewOptions,
  llmService: any,
  renderer?: any
): Promise<ReviewResult> {
  const { projectPath, diff, reviewType = 'COMPREHENSIVE', skipLint = false } = options;

  console.log(semanticChalk.info(`\n🔍 Starting Code Review`));
  console.log(semanticChalk.muted(`Project: ${projectPath}`));
  console.log(semanticChalk.muted(`Review Type: ${reviewType}`));
  console.log();

  try {
    // Step 1: Get diff content
    let diffContent = diff;
    let filePaths: string[] = [];

    if (!diffContent) {
      // Get diff from git
      console.log(semanticChalk.info('📥 Fetching git diff...'));

      try {
        const { execSync } = await import('child_process');

        if (options.commitHash) {
          // Get diff for specific commit
          diffContent = execSync(`git show ${options.commitHash}`, {
            cwd: projectPath,
            encoding: 'utf-8'
          });
        } else if (options.baseBranch && options.compareWith) {
          // Compare branches
          diffContent = execSync(`git diff ${options.baseBranch}...${options.compareWith}`, {
            cwd: projectPath,
            encoding: 'utf-8'
          });
        } else {
          // Default: review the last commit (HEAD)
          // This shows what was changed in the most recent commit
          diffContent = execSync('git show HEAD', {
            cwd: projectPath,
            encoding: 'utf-8'
          });
        }
      } catch (error: any) {
        console.error(semanticChalk.error(`Failed to get git diff: ${error.message}`));
        return {
          success: false,
          message: `Failed to get git diff: ${error.message}`,
          findings: []
        };
      }
    }

    if (!diffContent || diffContent.trim().length === 0) {
      console.log(semanticChalk.warning('⚠️  No changes found to review'));
      return {
        success: true,
        message: 'No changes found to review',
        findings: []
      };
    }

    // Step 2: Parse diff to get changed files
    console.log(semanticChalk.info('📝 Parsing diff...'));

    try {
      filePaths = KotlinCC.unitmesh.agent.JsDiffUtils.extractFilePaths(diffContent);
      console.log(semanticChalk.success(`Found ${filePaths.length} changed files`));

      filePaths.forEach((file: string) => {
        console.log(semanticChalk.muted(`  - ${file}`));
      });
      console.log();
    } catch (error: any) {
      console.error(semanticChalk.error(`Failed to parse diff: ${error.message}`));
      return {
        success: false,
        message: `Failed to parse diff: ${error.message}`,
        findings: []
      };
    }

    // Step 3: Run linters (optional)
    let lintResults: any[] = [];

    if (!skipLint && filePaths.length > 0) {
      console.log(semanticChalk.info('🔍 Running linters...'));

      try {
        const linterRegistry = new KotlinCC.unitmesh.agent.JsLinterRegistry();
        const linterNames = linterRegistry.findLintersForFiles(filePaths);

        if (linterNames.length > 0) {
          console.log(semanticChalk.muted(`Available linters: ${linterNames.join(', ')}`));
          console.log(semanticChalk.warning('Note: Linter execution requires linters to be installed'));
          console.log();
        } else {
          console.log(semanticChalk.warning('No suitable linters found for the changed files'));
          console.log();
        }
      } catch (error: any) {
        console.log(semanticChalk.warning(`Linter check failed: ${error.message}`));
        console.log();
      }
    }

    // Step 4: Run AI code review
    console.log(semanticChalk.info('🤖 Running AI code review...'));
    console.log();

    try {
      // Create code review agent
      const reviewAgent = new KotlinCC.unitmesh.agent.JsCodeReviewAgent(
        projectPath,
        llmService,
        50,
        renderer || null,
        null,
        null
      );

      // Create review task
      const task = new KotlinCC.unitmesh.agent.JsReviewTask(
        filePaths,
        reviewType,
        projectPath,
        `Git diff:\n${diffContent.substring(0, 2000)}...` // Include snippet of diff
      );

      // Execute review
      const result = await reviewAgent.executeTask(task);

      console.log();
      console.log(semanticChalk.success('✅ Code review complete!'));
      console.log();

      // Display findings
      if (result.findings.length > 0) {
        console.log(semanticChalk.info(`📋 Found ${result.findings.length} findings:`));
        console.log();

        // Group by severity
        const critical = result.findings.filter((f: any) => f.severity === 'CRITICAL');
        const high = result.findings.filter((f: any) => f.severity === 'HIGH');
        const medium = result.findings.filter((f: any) => f.severity === 'MEDIUM');
        const low = result.findings.filter((f: any) => f.severity === 'LOW');

        if (critical.length > 0) {
          console.log(semanticChalk.error(`🔴 CRITICAL (${critical.length}):`));
          critical.forEach((f: any) => {
            console.log(semanticChalk.error(`  - ${f.description}`));
            if (f.suggestion) {
              console.log(semanticChalk.muted(`    💡 ${f.suggestion}`));
            }
          });
          console.log();
        }

        if (high.length > 0) {
          console.log(semanticChalk.warning(`🟠 HIGH (${high.length}):`));
          high.forEach((f: any) => {
            console.log(semanticChalk.warning(`  - ${f.description}`));
            if (f.suggestion) {
              console.log(semanticChalk.muted(`    💡 ${f.suggestion}`));
            }
          });
          console.log();
        }

        if (medium.length > 0) {
          console.log(semanticChalk.info(`🟡 MEDIUM (${medium.length}):`));
          medium.forEach((f: any) => {
            console.log(semanticChalk.info(`  - ${f.description}`));
          });
          console.log();
        }

        if (low.length > 0) {
          console.log(semanticChalk.muted(`🟢 LOW (${low.length}):`));
          low.forEach((f: any) => {
            console.log(semanticChalk.muted(`  - ${f.description}`));
          });
          console.log();
        }
      } else {
        console.log(semanticChalk.success('✨ No issues found! Code looks good.'));
        console.log();
      }

      return {
        success: result.success,
        message: result.message,
        findings: result.findings,
        lintResults
      };

    } catch (error: any) {
      console.error(semanticChalk.error(`AI review failed: ${error.message}`));
      return {
        success: false,
        message: `AI review failed: ${error.message}`,
        findings: []
      };
    }

  } catch (error: any) {
    console.error(semanticChalk.error(`Review failed: ${error.message}`));
    return {
      success: false,
      message: `Review failed: ${error.message}`,
      findings: []
    };
  }
}

