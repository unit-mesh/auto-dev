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

    // ===== STEP 3: Read Code Files =====
    console.log(semanticChalk.info('📖 Reading code files...'));
    const codeContent = await readCodeFiles(filePaths, projectPath);
    console.log(semanticChalk.success(`✅ Read ${Object.keys(codeContent).length} files`));
    console.log();

    // ===== STEP 4: Run Linters and Collect Results =====
    const lintResults: Record<string, string> = {};
    if (!skipLint && filePaths.length > 0) {
      try {
        const collectedLintResults = await runLinters(filePaths, projectPath);
        Object.assign(lintResults, collectedLintResults);
      } catch (error: any) {
        console.log(semanticChalk.warning(`⚠️  Linter execution failed: ${error.message}`));
      }
    }

    // ===== STEP 5: Use Data-Driven Analysis (Optimized) =====
    console.log(semanticChalk.info('🤖 Analyzing with AI...'));
    
    // Create code review agent
    const reviewAgent = new KotlinCC.unitmesh.agent.JsCodeReviewAgent(
      projectPath,
      llmService,
      50,
      renderer || null,
      null,
      null
    );

    // Build diff context (simplified)
    const diffContext = diffContent.length > 2000 
      ? `\n## What Changed\n\`\`\`diff\n${diffContent.substring(0, 2000)}...\n\`\`\`` 
      : `\n## What Changed\n\`\`\`diff\n${diffContent}\n\`\`\``;

    const promptLength = JSON.stringify(codeContent).length + JSON.stringify(lintResults).length + diffContext.length;
    console.log(semanticChalk.muted(`📊 Prompt: ${promptLength} chars (~${Math.floor(promptLength / 4)} tokens)`));
    console.log(semanticChalk.info('⚡ Streaming AI response...'));
    console.log();

    const llmStartTime = Date.now();
    let analysisOutput = '';

    // Use Data-Driven analysis (single LLM call)
    analysisOutput = await reviewAgent.analyzeWithDataDriven(
      reviewType,
      filePaths,
      codeContent,
      lintResults,
      diffContext,
      'EN',
      (chunk: string) => {
        process.stdout.write(chunk);
      }
    );

    const llmDuration = Date.now() - llmStartTime;
    const totalDuration = Date.now() - startTime;

    console.log();
    console.log();
    console.log(semanticChalk.success('✅ Code review complete!'));
    console.log(semanticChalk.muted(`⏱️  Total: ${totalDuration}ms (AI: ${llmDuration}ms)`));
    console.log();

    // Parse findings from markdown output
    const findings = parseMarkdownFindings(analysisOutput);

    if (findings.length > 0) {
      displayKotlinFindings(findings);
    } else {
      console.log(semanticChalk.success('✨ No significant issues found! Code looks good.'));
      console.log();
    }

    return {
      success: true,
      message: analysisOutput,
      findings: findings,
      analysisOutput: analysisOutput
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
 * Read code files into memory
 */
async function readCodeFiles(filePaths: string[], projectPath: string): Promise<Record<string, string>> {
  const fs = await import('fs/promises');
  const path = await import('path');
  const codeContent: Record<string, string> = {};

  for (const filePath of filePaths) {
    try {
      const fullPath = path.join(projectPath, filePath);
      const content = await fs.readFile(fullPath, 'utf-8');
      codeContent[filePath] = content;
    } catch (error: any) {
      console.log(semanticChalk.warning(`⚠️  Failed to read ${filePath}: ${error.message}`));
    }
  }

  return codeContent;
}

/**
 * Run linters on files and collect results
 */
async function runLinters(filePaths: string[], projectPath: string): Promise<Record<string, string>> {
  const lintResults: Record<string, string> = {};
  
  try {
    const linterRegistry = KotlinCC.unitmesh.agent.linter.JsLinterRegistry;
    const linters = linterRegistry.findLintersForFiles(filePaths);
    
    if (linters.length === 0) {
      return lintResults;
    }

    console.log(semanticChalk.info(`🔍 Running linters: ${linters.join(', ')}...`));

    // Run linters using child_process
    const { execSync } = await import('child_process');
    
    for (const linter of linters) {
      try {
        let command = '';
        let output = '';

        // Build linter command based on linter name
        switch (linter) {
          case 'biome':
            command = `npx --yes @biomejs/biome check --diagnostic-level=info ${filePaths.join(' ')}`;
            break;
          case 'eslint':
            command = `npx eslint --format json ${filePaths.join(' ')}`;
            break;
          case 'detekt':
            command = `./gradlew detekt`;
            break;
          default:
            continue;
        }

        try {
          output = execSync(command, {
            cwd: projectPath,
            encoding: 'utf-8',
            stdio: ['pipe', 'pipe', 'pipe']
          });
        } catch (error: any) {
          // Linters often exit with non-zero on issues found
          output = error.stdout || error.message;
        }

        if (output && output.trim().length > 0) {
          lintResults[linter] = output;
        }
      } catch (error: any) {
        console.log(semanticChalk.warning(`⚠️  ${linter} failed: ${error.message}`));
      }
    }

    if (Object.keys(lintResults).length > 0) {
      console.log(semanticChalk.success(`✅ Collected lint results from ${Object.keys(lintResults).length} linters`));
    } else {
      console.log(semanticChalk.muted('ℹ️  No lint issues found'));
    }
  } catch (error: any) {
    console.log(semanticChalk.warning(`⚠️  Linter execution error: ${error.message}`));
  }

  return lintResults;
}

/**
 * Parse structured findings from markdown output
 * Looks for issues in format: #### #{number}. {title}
 */
function parseMarkdownFindings(markdown: string): ReviewFinding[] {
  const findings: ReviewFinding[] = [];
  
  // Split by issue markers (#### #1., #### #2., etc.)
  const issuePattern = /####\s*#(\d+)\.\s*(.+?)(?=####\s*#\d+\.|$)/gs;
  const matches = Array.from(markdown.matchAll(issuePattern));

  for (const match of matches) {
    const issueText = match[0];
    const issueNumber = match[1];
    const issueTitle = match[2].trim();

    // Extract severity
    const severityMatch = issueText.match(/\*\*Severity\*\*:\s*(CRITICAL|HIGH|MEDIUM|LOW|INFO)/i);
    const severity = severityMatch ? severityMatch[1].toUpperCase() : 'MEDIUM';

    // Extract category
    const categoryMatch = issueText.match(/\*\*Category\*\*:\s*(.+?)(?:\n|\*\*)/i);
    const category = categoryMatch ? categoryMatch[1].trim() : 'General';

    // Extract location
    const locationMatch = issueText.match(/\*\*Location\*\*:\s*`?([^`\n]+?)(?:`|\n)/i);
    let filePath: string | undefined;
    let lineNumber: number | undefined;
    
    if (locationMatch) {
      const location = locationMatch[1].trim();
      const parts = location.split(':');
      filePath = parts[0];
      if (parts.length > 1) {
        lineNumber = parseInt(parts[1], 10);
      }
    }

    // Extract problem description
    const problemMatch = issueText.match(/\*\*Problem\*\*:\s*(.+?)(?=\*\*|$)/is);
    const description = problemMatch ? problemMatch[1].trim() : issueTitle;

    // Extract suggested fix
    const fixMatch = issueText.match(/\*\*Suggested Fix\*\*:\s*(.+?)(?=---|\*\*|$)/is);
    const suggestion = fixMatch ? fixMatch[1].trim() : undefined;

    findings.push({
      severity,
      category,
      description: `${issueTitle}${description !== issueTitle ? ': ' + description : ''}`,
      filePath,
      lineNumber,
      suggestion
    });
  }

  return findings;
}

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


