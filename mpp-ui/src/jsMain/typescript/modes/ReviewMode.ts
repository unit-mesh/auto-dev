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

    // ===== STEP 5: Extract Commit Message =====
    console.log(semanticChalk.info('📝 Extracting commit information...'));
    const { commitMessage, commitId, repoUrl } = await extractCommitInfo(projectPath, options);
    console.log();

    // Create code review agent
    const reviewAgent = new KotlinCC.unitmesh.agent.JsCodeReviewAgent(
      projectPath,
      llmService,
      50,
      renderer || null,
      null,
      null
    );

    // ===== STEP 6: Intent Analysis (if commit message exists) =====
    let intentAnalysisOutput = '';
    let mermaidDiagram: string | null = null;
    
    if (commitMessage && commitMessage.trim().length > 10) {
      console.log(semanticChalk.info('🎯 Analyzing commit intent...'));
      console.log(semanticChalk.muted(`📄 Commit: ${commitId || 'HEAD'}`));
      console.log(semanticChalk.muted(`💬 Message: ${commitMessage.substring(0, 80)}${commitMessage.length > 80 ? '...' : ''}`));
      console.log();
      
      const llmStartTime = Date.now();
      
      try {
        // Build code changes map from diff content
        const codeChangesMap = buildCodeChangesMap(diffContent, filePaths, codeContent);
        
        console.log(semanticChalk.info('⚡ Streaming intent analysis...'));
        console.log();
        
        // Call analyzeIntent with tool-driven approach
        const intentResult = await reviewAgent.analyzeIntent(
          commitMessage,
          commitId,
          codeChangesMap,
          repoUrl,
          '', // issueToken - could be added as an option
          true, // useTools
          'EN',
          (chunk: string) => {
            process.stdout.write(chunk);
          }
        );
        
        intentAnalysisOutput = intentResult.content;
        mermaidDiagram = intentResult.mermaidDiagram || null;
        
        const llmDuration = Date.now() - llmStartTime;
        
        console.log();
        console.log();
        console.log(semanticChalk.success('✅ Intent analysis complete!'));
        console.log(semanticChalk.muted(`⏱️  Time: ${llmDuration}ms`));
        
        // Display mermaid diagram if present
        if (mermaidDiagram) {
          console.log();
          console.log(semanticChalk.info('📊 Intent Flow Diagram:'));
          console.log();
          console.log(semanticChalk.muted('```mermaid'));
          console.log(mermaidDiagram);
          console.log(semanticChalk.muted('```'));
        }
        
        console.log();
        console.log(semanticChalk.muted('─'.repeat(80)));
        console.log();
      } catch (error: any) {
        console.log(semanticChalk.warning(`⚠️  Intent analysis failed: ${error.message}`));
        console.log(semanticChalk.muted('Continuing with technical review...'));
        console.log();
      }
    }

    // ===== STEP 7: Technical Code Review (if intent analysis was done) =====
    let technicalReviewOutput = '';
    
    if (intentAnalysisOutput) {
      console.log(semanticChalk.info('🔧 Performing technical code review...'));
      console.log();
      
      // Build diff context
      const diffContext = diffContent.length > 2000 
        ? `\n## What Changed\n\`\`\`diff\n${diffContent.substring(0, 2000)}...\n\`\`\`` 
        : `\n## What Changed\n\`\`\`diff\n${diffContent}\n\`\`\``;

      const promptLength = JSON.stringify(codeContent).length + JSON.stringify(lintResults).length + diffContext.length;
      console.log(semanticChalk.muted(`📊 Prompt: ${promptLength} chars (~${Math.floor(promptLength / 4)} tokens)`));
      console.log(semanticChalk.info('⚡ Streaming technical review...'));
      console.log();

      const llmStartTime = Date.now();

      // Use Data-Driven analysis for technical review
      technicalReviewOutput = await reviewAgent.analyzeWithDataDriven(
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
      console.log();
      console.log();
      console.log(semanticChalk.success('✅ Technical review complete!'));
      console.log(semanticChalk.muted(`⏱️  Time: ${llmDuration}ms`));
    } else {
      // No commit message, just do technical review
      console.log(semanticChalk.info('🤖 Analyzing with AI...'));
      
      const diffContext = diffContent.length > 2000 
        ? `\n## What Changed\n\`\`\`diff\n${diffContent.substring(0, 2000)}...\n\`\`\`` 
        : `\n## What Changed\n\`\`\`diff\n${diffContent}\n\`\`\``;

      const promptLength = JSON.stringify(codeContent).length + JSON.stringify(lintResults).length + diffContext.length;
      console.log(semanticChalk.muted(`📊 Prompt: ${promptLength} chars (~${Math.floor(promptLength / 4)} tokens)`));
      console.log(semanticChalk.info('⚡ Streaming AI response...'));
      console.log();

      const llmStartTime = Date.now();

      technicalReviewOutput = await reviewAgent.analyzeWithDataDriven(
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
      console.log();
      console.log();
      console.log(semanticChalk.success('✅ Code review complete!'));
      console.log(semanticChalk.muted(`⏱️  Time: ${llmDuration}ms`));
    }

    const totalDuration = Date.now() - startTime;

    // Combine outputs
    const analysisOutput = intentAnalysisOutput 
      ? `# 🎯 Intent Analysis\n\n${intentAnalysisOutput}\n\n---\n\n# 🔧 Technical Review\n\n${technicalReviewOutput}`
      : technicalReviewOutput;

    console.log();
    console.log();
    console.log(semanticChalk.success('✅ Complete review finished!'));
    console.log(semanticChalk.muted(`⏱️  Total: ${totalDuration}ms`));
    console.log();

    // Parse findings from technical review only (not from intent analysis)
    const findings = parseMarkdownFindings(technicalReviewOutput || analysisOutput);

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
 * Enhanced review finding with more details
 */
interface EnhancedReviewFinding extends ReviewFinding {
  methodName?: string;
  className?: string;
  codeSnippet?: string;
  source?: string;  // "Linter (detekt: ...)" or "Manual Analysis"
}

/**
 * Parse structured findings from markdown output
 * Looks for issues in format: #### #{number}. {title}
 */
function parseMarkdownFindings(markdown: string): EnhancedReviewFinding[] {
  const findings: EnhancedReviewFinding[] = [];
  
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

    // Extract location with method/class name
    const locationMatch = issueText.match(/\*\*Location\*\*:\s*`([^`]+)`(?:\s+in\s+`([^`]+)`)?/i);
    let filePath: string | undefined;
    let lineNumber: number | undefined;
    let methodName: string | undefined;
    let className: string | undefined;
    
    if (locationMatch) {
      const location = locationMatch[1].trim();
      const parts = location.split(':');
      filePath = parts[0];
      if (parts.length > 1) {
        const lineStr = parts[1].replace(/[^\d]/g, ''); // Extract just the number
        lineNumber = parseInt(lineStr, 10) || undefined;
      }
      
      // Extract method/class name
      if (locationMatch[2]) {
        const nameInfo = locationMatch[2].trim();
        // Could be "MethodName" or "ClassName" or "MethodName / ClassName"
        if (nameInfo.includes('/')) {
          const [method, cls] = nameInfo.split('/').map(s => s.trim());
          methodName = method;
          className = cls;
        } else {
          // Assume it's a method name if it starts with lowercase, else class name
          if (nameInfo[0] === nameInfo[0].toLowerCase()) {
            methodName = nameInfo;
          } else {
            className = nameInfo;
          }
        }
      }
    }

    // Extract source (Linter or Manual Analysis)
    const sourceMatch = issueText.match(/\*\*Source\*\*:\s*(.+?)(?:\n|\*\*)/i);
    const source = sourceMatch ? sourceMatch[1].trim() : undefined;

    // Extract problem description
    const problemMatch = issueText.match(/\*\*Problem\*\*:\s*(.+?)(?=\*\*Code\*\*|\*\*Impact\*\*|\*\*|$)/is);
    const description = problemMatch ? problemMatch[1].trim() : issueTitle;

    // Extract code snippet
    const codeMatch = issueText.match(/\*\*Code\*\*:\s*```[\w]*\n([\s\S]+?)```/i);
    const codeSnippet = codeMatch ? codeMatch[1].trim() : undefined;

    // Extract suggested fix
    const fixMatch = issueText.match(/\*\*Suggested Fix\*\*:\s*(.+?)(?=---|\*\*|####|$)/is);
    const suggestion = fixMatch ? fixMatch[1].trim() : undefined;

    findings.push({
      severity,
      category,
      description: `${issueTitle}${description !== issueTitle && description !== issueTitle + ':' ? ': ' + description : ''}`,
      filePath,
      lineNumber,
      methodName,
      className,
      codeSnippet,
      source,
      suggestion
    });
  }

  return findings;
}

/**
 * Extract commit information (message, ID, repo URL)
 */
async function extractCommitInfo(
  projectPath: string,
  options: ReviewOptions
): Promise<{ commitMessage: string; commitId: string; repoUrl: string }> {
  try {
    const { execSync } = await import('child_process');
    
    // Get commit ID
    let commitId = options.commitHash || '';
    if (!commitId) {
      try {
        commitId = execSync('git rev-parse HEAD', {
          cwd: projectPath,
          encoding: 'utf-8'
        }).trim();
      } catch {
        commitId = 'HEAD';
      }
    }
    
    // Get commit message
    let commitMessage = '';
    try {
      const command = options.commitHash 
        ? `git log -1 --format=%B ${options.commitHash}`
        : 'git log -1 --format=%B HEAD';
      
      commitMessage = execSync(command, {
        cwd: projectPath,
        encoding: 'utf-8'
      }).trim();
    } catch (error: any) {
      console.log(semanticChalk.muted(`Unable to get commit message: ${error.message}`));
    }
    
    // Get repo URL
    let repoUrl = '';
    try {
      const remoteUrl = execSync('git config --get remote.origin.url', {
        cwd: projectPath,
        encoding: 'utf-8'
      }).trim();
      
      // Convert SSH URL to HTTPS if needed
      if (remoteUrl.startsWith('git@github.com:')) {
        repoUrl = remoteUrl.replace('git@github.com:', 'https://github.com/').replace(/\.git$/, '');
      } else {
        repoUrl = remoteUrl.replace(/\.git$/, '');
      }
    } catch {
      // Repo URL is optional
    }
    
    return { commitMessage, commitId, repoUrl };
  } catch (error: any) {
    console.log(semanticChalk.muted(`Unable to extract commit info: ${error.message}`));
    return { commitMessage: '', commitId: '', repoUrl: '' };
  }
}

/**
 * Build code changes map from diff content
 * Returns a map of file paths to their diff content
 */
function buildCodeChangesMap(
  diffContent: string,
  filePaths: string[],
  codeContent: Record<string, string>
): Record<string, string> {
  const codeChangesMap: Record<string, string> = {};
  
  // Try to extract per-file diffs
  const lines = diffContent.split('\n');
  let currentFile = '';
  let currentDiff: string[] = [];
  
  for (const line of lines) {
    if (line.startsWith('diff --git ')) {
      // Save previous file's diff
      if (currentFile && currentDiff.length > 0) {
        codeChangesMap[currentFile] = currentDiff.join('\n');
      }
      
      // Start new file
      const match = line.match(/diff --git a\/(.*?) b\/(.*?)$/);
      if (match) {
        currentFile = match[2];
        currentDiff = [line];
      }
    } else if (currentFile) {
      currentDiff.push(line);
    }
  }
  
  // Save last file's diff
  if (currentFile && currentDiff.length > 0) {
    codeChangesMap[currentFile] = currentDiff.join('\n');
  }
  
  // If no per-file diffs were extracted, use full code content as fallback
  if (Object.keys(codeChangesMap).length === 0) {
    filePaths.forEach(filePath => {
      if (codeContent[filePath]) {
        codeChangesMap[filePath] = codeContent[filePath];
      }
    });
  }
  
  return codeChangesMap;
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
 * Display findings from Kotlin CodeReviewAgent with enhanced formatting
 */
function displayKotlinFindings(findings: EnhancedReviewFinding[]): void {
  console.log(semanticChalk.info(`📋 Found ${findings.length} findings:`));
  console.log();

  // Group by severity
  const critical = findings.filter(f => f.severity === 'CRITICAL');
  const high = findings.filter(f => f.severity === 'HIGH');
  const medium = findings.filter(f => f.severity === 'MEDIUM');
  const low = findings.filter(f => f.severity === 'LOW' || f.severity === 'INFO');

  // Helper to format a single finding
  const formatFinding = (f: EnhancedReviewFinding, color: (str: string) => string) => {
    // Build location string with method/class info
    let location = f.filePath || 'N/A';
    if (f.lineNumber) {
      location += `:${f.lineNumber}`;
    }
    if (f.methodName || f.className) {
      const context = [f.methodName, f.className].filter(Boolean).join(' / ');
      location += ` in ${context}`;
    }
    
    console.log(color(`  • ${f.description}`));
    console.log(semanticChalk.muted(`    📍 ${location}`));
    
    // Show source if available
    if (f.source) {
      const sourceIcon = f.source.toLowerCase().includes('linter') ? '🔍' : '👁️';
      console.log(semanticChalk.muted(`    ${sourceIcon} ${f.source}`));
    }
    
    // Show code snippet if available
    if (f.codeSnippet) {
      console.log(semanticChalk.muted(`    📝 Code:`));
      f.codeSnippet.split('\n').forEach((line, idx) => {
        if (idx < 5) { // Show max 5 lines
          console.log(semanticChalk.muted(`       ${line}`));
        }
      });
    }
    
    // Show suggestion
    if (f.suggestion) {
      const shortSuggestion = f.suggestion.length > 150 
        ? f.suggestion.substring(0, 150) + '...' 
        : f.suggestion;
      console.log(semanticChalk.muted(`    💡 ${shortSuggestion}`));
    }
    console.log();
  };

  if (critical.length > 0) {
    console.log(semanticChalk.error(`🔴 CRITICAL (${critical.length}):`));
    console.log();
    critical.forEach(f => formatFinding(f, semanticChalk.error));
  }

  if (high.length > 0) {
    console.log(semanticChalk.warning(`🟠 HIGH (${high.length}):`));
    console.log();
    high.forEach(f => formatFinding(f, semanticChalk.warning));
  }

  if (medium.length > 0) {
    console.log(semanticChalk.info(`🟡 MEDIUM (${medium.length}):`));
    console.log();
    medium.forEach(f => formatFinding(f, semanticChalk.info));
  }

  if (low.length > 0) {
    console.log(semanticChalk.muted(`🟢 LOW/INFO (${low.length}):`));
    console.log();
    low.forEach(f => formatFinding(f, semanticChalk.muted));
  }
}


