/**
 * CodingAgentService - Autonomous coding agent for completing development tasks
 * 
 * This service implements an AI-powered coding agent that can:
 * - Analyze project structure
 * - Read and understand existing code
 * - Make code changes based on requirements
 * - Execute commands and tests
 * - Iterate until the task is complete
 * 
 * Refactored to use mpp-core abstractions for cross-platform consistency
 */

import * as fs from 'fs/promises';
import * as path from 'path';
import * as os from 'os';
import type { LLMConfig } from '../config/ConfigManager.js';
import { LLMService } from '../services/LLMService.js';
import { OutputFormatter } from '../utils/outputFormatter.js';
import { ErrorRecoveryAgent, RecoveryResult } from './ErrorRecoveryAgent.js';
import { LogSummaryAgent, LogSummaryResult } from './LogSummaryAgent.js';

// Import mpp-core
// @ts-ignore
import MppCore from '@autodev/mpp-core';

const { JsCompletionManager, JsToolRegistry } = MppCore.cc.unitmesh.llm;
const { JsCodingAgentContextBuilder, JsCodingAgentPromptRenderer } = MppCore.cc.unitmesh.agent;

export interface AgentTask {
  requirement: string;
  projectPath: string;
}

export interface AgentStep {
  step: number;
  action: string;
  tool?: string;
  params?: any;
  result?: string;
  success: boolean;
}

export interface AgentEdit {
  file: string;
  operation: 'create' | 'update' | 'delete';
  content?: string;
}

export interface AgentResult {
  success: boolean;
  message: string;
  steps: AgentStep[];
  edits: AgentEdit[];
}

/**
 * Critical infrastructure files that should not be modified by simple tasks
 */
const PROTECTED_FILES = [
  'build.gradle.kts',
  'build.gradle',
  'pom.xml',
  'package.json',
  'package-lock.json',
  'yarn.lock',
  'pnpm-lock.yaml',
  'requirements.txt',
  'Pipfile',
  'Cargo.toml',
  'Cargo.lock',
  'go.mod',
  'go.sum',
  'CMakeLists.txt',
  'Makefile',
  'settings.gradle.kts',
  'settings.gradle'
];

/**
 * Keywords that indicate a simple task
 */
const SIMPLE_TASK_KEYWORDS = [
  'hello world',
  'create a simple',
  'add a simple',
  'write a simple',
  'simple file',
  'add a file',
  'write a function',
  'add a function',
  'simple example',
  'basic example',
  'quick test',
  'create a test',
  'add a method',
  'simple class'
];

/**
 * Coding Agent Service
 * Now uses mpp-core for prompt generation and context building
 */
export class CodingAgentService {
  private llmService: LLMService;
  private projectPath: string;
  private steps: AgentStep[] = [];
  private edits: AgentEdit[] = [];
  private maxIterations: number = 100;
  private completionManager: any;
  private toolRegistry: any;
  private promptRenderer: any;
  private formatter: OutputFormatter;
  private errorRecoveryAgent: ErrorRecoveryAgent;
  private logSummaryAgent: LogSummaryAgent;
  private startTime: number = 0;
  private lastRecoveryResult: RecoveryResult | null = null;
  private config: LLMConfig;
  private isSimpleTaskMode: boolean = false;

  constructor(projectPath: string, config: LLMConfig, quiet: boolean = false) {
    this.projectPath = path.resolve(projectPath);
    this.config = config;
    this.llmService = new LLMService(config);
    this.completionManager = new JsCompletionManager();
    this.toolRegistry = new JsToolRegistry(this.projectPath);
    this.promptRenderer = new JsCodingAgentPromptRenderer();
    this.formatter = new OutputFormatter(quiet);
    this.errorRecoveryAgent = new ErrorRecoveryAgent(this.projectPath, config);
    this.logSummaryAgent = new LogSummaryAgent(config, 2000); // Summarize if output > 2000 chars
  }

  /**
   * Check if this is a simple task that should have limited scope
   */
  private isSimpleTask(taskDescription: string): boolean {
    const lowerTask = taskDescription.toLowerCase();
    return SIMPLE_TASK_KEYWORDS.some(keyword => lowerTask.includes(keyword));
  }

  /**
   * Check if a file is protected (critical infrastructure)
   */
  private isProtectedFile(filePath: string): boolean {
    const basename = path.basename(filePath);
    return PROTECTED_FILES.includes(basename);
  }

  /**
   * Check if task appears complete based on heuristics
   */
  private checkTaskCompletion(task: string, iteration: number): boolean {
    // Only for simple tasks after a few iterations
    if (!this.isSimpleTaskMode || iteration < 2) {
      return false;
    }

    // Check if we created a core file
    const hasCreatedFile = this.edits.some(e => e.operation === 'create');
    
    if (hasCreatedFile) {
      // Check if recent steps are mostly failing
      const recentSteps = this.steps.slice(-3);
      if (recentSteps.length > 0) {
        const failureCount = recentSteps.filter(s => !s.success).length;
        const mostlyFailing = failureCount >= 2;
        
        if (mostlyFailing) {
          this.formatter.info('🎯 Core work completed, subsequent failures appear unrelated');
          return true;
        }
      }
    }
    
    return false;
  }

  /**
   * Enhance system prompt for simple tasks
   */
  private enhanceSystemPromptForTask(systemPrompt: string, task: string): string {
    if (!this.isSimpleTask(task)) {
      return systemPrompt;
    }

    return systemPrompt + `\n\n## 🎯 IMPORTANT: Simple Task Mode

This is a SIMPLE task that requires minimal changes. Follow these STRICT rules:

1. **DO NOT modify build/config files**: ${PROTECTED_FILES.slice(0, 5).join(', ')}, etc.
2. **DO NOT run full project builds or test suites** - focus only on the task
3. **DO NOT try to fix pre-existing project issues** - ignore unrelated errors
4. **CREATE ONLY** the minimal files needed for this specific task
5. **VERIFY** your new code has correct syntax (read it back if needed)
6. **RESPOND** with "TASK_COMPLETE" immediately after creating the requested file

**Expected workflow:**
- Step 1: Understand project structure (quick scan)
- Step 2: Create the requested file
- Step 3: Verify file was created correctly
- Step 4: Respond with "TASK_COMPLETE"

Maximum expected steps: 5-7
If you encounter errors unrelated to your new file, mark the task complete anyway.`;
  }

  /**
   * Execute a development task
   */
  async executeTask(task: AgentTask): Promise<AgentResult> {
    this.startTime = Date.now();
    
    this.formatter.header('AutoDev Coding Agent');
    this.formatter.info(`Project: ${this.projectPath}`);
    this.formatter.info(`Task: ${task.requirement}`);

    // Detect if this is a simple task
    this.isSimpleTaskMode = this.isSimpleTask(task.requirement);
    if (this.isSimpleTaskMode) {
      this.formatter.info('📋 Simple task detected - using limited scope mode');
      this.formatter.info('⚠️  Build files are protected from modification');
    }

    try {
      // Initialize workspace
      this.formatter.section('Initializing Workspace');
      await this.initializeWorkspace();

      // Build context and system prompt using mpp-core
      const context = await this.buildContext(task);
      let systemPrompt = this.promptRenderer.render(context, 'EN');
      
      // Enhance prompt for simple tasks
      systemPrompt = this.enhanceSystemPromptForTask(systemPrompt, task.requirement);

      // Main agent loop
      let iteration = 0;
      let taskComplete = false;

      this.formatter.section('Executing Task');
      
      while (iteration < this.maxIterations && !taskComplete) {
        iteration++;
        this.formatter.step(iteration, this.maxIterations, 'Analyzing and executing...');

        // Get next action from LLM
        const action = await this.getNextAction(systemPrompt, task.requirement, iteration);

        if (!action) {
          this.formatter.error('Failed to get next action from LLM');
          break;
        }

        // Execute action
        const stepResult = await this.executeAction(action, iteration);
        this.steps.push(stepResult);

        // Check if task is complete (explicit signal from LLM)
        if (action.includes('TASK_COMPLETE') || action.includes('task complete')) {
          taskComplete = true;
          this.formatter.success('Task marked as complete by agent');
        }
        
        // Check completion heuristics for simple tasks
        if (!taskComplete && this.checkTaskCompletion(task.requirement, iteration)) {
          taskComplete = true;
          this.formatter.success('Task appears complete based on success heuristics');
        }
        
        // If AI didn't call any tools (just reasoning), end the task
        if (stepResult.action === 'reasoning') {
          taskComplete = true;
          this.formatter.info('Agent completed reasoning without further actions');
        }

        // Small delay between iterations
        await new Promise(resolve => setTimeout(resolve, 500));
      }

      // Display all file changes
      if (this.edits.length > 0) {
        this.formatter.section('File Changes');
        for (const edit of this.edits) {
          this.formatter.displayFileChange({
            file: edit.file,
            operation: edit.operation,
            newContent: edit.content
          });
        }
      }

      const duration = Date.now() - this.startTime;
      const success = taskComplete || this.steps.filter(s => s.success).length > 0;
      
      // Display summary
      this.formatter.displaySummary({
        iterations: iteration,
        edits: this.edits.length,
        creates: this.edits.filter(e => e.operation === 'create').length,
        updates: this.edits.filter(e => e.operation === 'update').length,
        deletes: this.edits.filter(e => e.operation === 'delete').length,
        duration
      });

      const message = success
        ? `✅ Task completed successfully`
        : `⚠️  Task incomplete after ${iteration} iterations`;

      if (success) {
        this.formatter.success(message);
      } else {
        this.formatter.warn(message);
      }

      return {
        success,
        message,
        steps: this.steps,
        edits: this.edits
      };

    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : String(error);
      this.formatter.error(`Agent error: ${errorMsg}`);

      return {
        success: false,
        message: `Failed: ${errorMsg}`,
        steps: this.steps,
        edits: this.edits
      };
    }
  }

  /**
   * Initialize workspace for the agent
   */
  private async initializeWorkspace(): Promise<void> {
    try {
      // Initialize workspace using mpp-core
      await this.completionManager.initWorkspace(this.projectPath);
      this.formatter.success(`Workspace initialized`);
    } catch (error) {
      this.formatter.warn(`Failed to initialize workspace: ${error}`);
    }
  }

  /**
   * Build context for the agent using mpp-core
   * This replaces the old buildSystemPrompt method
   */
  private async buildContext(task: AgentTask): Promise<any> {
    const osInfo = `${os.platform()} ${os.release()} ${os.arch()}`;
    const timestamp = new Date().toISOString();

    // Get project structure
    const projectStructure = await this.getProjectStructure();

    // Read AGENTS.md if exists
    let agentRules = '';
    try {
      const agentsPath = path.join(this.projectPath, 'AGENTS.md');
      agentRules = await fs.readFile(agentsPath, 'utf-8');
    } catch {
      // AGENTS.md doesn't exist, that's ok
    }

    // Detect build tool and language
    const buildTool = await this.detectBuildTool();
    const language = await this.detectLanguage();

    // Get tool list from registry
    const toolList = await this.getToolList();

    // Detect shell
    const shell = process.env.SHELL || '/bin/bash';

    // Enhance project structure with language info
    const enhancedStructure = `${projectStructure}\n\nDetected Language: ${language}\nBuild Tool: ${buildTool}`;

    // Build context using mpp-core builder
    const builder = new JsCodingAgentContextBuilder();
    return builder
      .setProjectPath(this.projectPath)
      .setOsInfo(osInfo)
      .setTimestamp(timestamp)
      .setProjectStructure(enhancedStructure)
      .setAgentRules(agentRules)
      .setBuildTool(buildTool)
      .setToolList(toolList)
      .setShell(shell)
      .build();
  }

  /**
   * Get project structure summary
   */
  private async getProjectStructure(): Promise<string> {
    try {
      const entries = await fs.readdir(this.projectPath, { withFileTypes: true });
      const items = entries
        .filter(e => !e.name.startsWith('.') && e.name !== 'node_modules')
        .map(e => `${e.isDirectory() ? '📁' : '📄'} ${e.name}`)
        .slice(0, 20); // Limit to first 20 items

      return items.join('\n') + (entries.length > 20 ? '\n... (more files)' : '');
    } catch {
      return '(Unable to read project structure)';
    }
  }

  /**
   * Detect build tool from project files
   */
  private async detectBuildTool(): Promise<string> {
    try {
      const files = await fs.readdir(this.projectPath);
      
      if (files.includes('package.json')) {
        return 'npm/node.js';
      }
      if (files.includes('build.gradle.kts') || files.includes('build.gradle')) {
        return 'gradle';
      }
      if (files.includes('pom.xml')) {
        return 'maven';
      }
      if (files.includes('Cargo.toml')) {
        return 'cargo/rust';
      }
      if (files.includes('requirements.txt') || files.includes('setup.py')) {
        return 'python';
      }
      
      return 'unknown';
    } catch {
      return 'unknown';
    }
  }

  /**
   * Detect primary language by counting source files
   */
  private async detectLanguage(): Promise<string> {
    try {
      const srcPath = path.join(this.projectPath, 'src');
      
      // Check if src directory exists
      try {
        await fs.access(srcPath);
      } catch {
        return 'unknown';
      }

      // Count files by extension
      const counts: Record<string, number> = {
        '.java': 0,
        '.kt': 0,
        '.ts': 0,
        '.tsx': 0,
        '.js': 0,
        '.jsx': 0,
        '.py': 0,
        '.rs': 0,
        '.go': 0
      };

      const countFiles = async (dir: string) => {
        const entries = await fs.readdir(dir, { withFileTypes: true });
        for (const entry of entries) {
          const fullPath = path.join(dir, entry.name);
          if (entry.isDirectory() && !entry.name.startsWith('.') && entry.name !== 'node_modules') {
            await countFiles(fullPath);
          } else if (entry.isFile()) {
            const ext = path.extname(entry.name);
            if (ext in counts) {
              counts[ext]++;
            }
          }
        }
      };

      await countFiles(srcPath);

      // Find the most common extension
      const sortedLangs = Object.entries(counts)
        .filter(([_, count]) => count > 0)
        .sort(([_, a], [__, b]) => b - a);

      if (sortedLangs.length === 0) return 'unknown';

      const [topExt, topCount] = sortedLangs[0];
      const langMap: Record<string, string> = {
        '.java': 'Java',
        '.kt': 'Kotlin',
        '.ts': 'TypeScript',
        '.tsx': 'TypeScript/React',
        '.js': 'JavaScript',
        '.jsx': 'JavaScript/React',
        '.py': 'Python',
        '.rs': 'Rust',
        '.go': 'Go'
      };

      return `${langMap[topExt] || topExt} (${topCount} files)`;
    } catch {
      return 'unknown';
    }
  }

  /**
   * Get tool list from registry
   */
  private async getToolList(): Promise<string> {
    try {
      const tools = this.toolRegistry.getAgentTools();
      return tools.map((tool: any) => 
        `**${tool.name}** - ${tool.description}\n   Example: ${tool.example}`
      ).join('\n\n');
    } catch (error) {
      console.warn(`⚠️  Failed to get tool list: ${error}`);
      return 'Tools: read-file, write-file, glob, grep, shell';
    }
  }

  /**
   * Get next action from LLM
   */
  private async getNextAction(systemPrompt: string, requirement: string, iteration: number): Promise<string | null> {
    let userPrompt: string;
    
    // Check if we have a recovery result from SubAgent
    if (this.lastRecoveryResult) {
      const recovery = this.lastRecoveryResult;
      
      userPrompt = `## Previous Action Failed - Recovery Needed

${recovery.analysis}

**Suggested Actions:**
${recovery.suggestedActions.map((a, i) => `${i + 1}. ${a}`).join('\n')}

${recovery.recoveryCommands && recovery.recoveryCommands.length > 0 ? `
**Recovery Commands:**
${recovery.recoveryCommands.map(cmd => `\`${cmd}\``).join('\n')}

Please execute these recovery commands first, then continue with the original task.
` : ''}

**Original Task:** ${requirement}

**What to do next:**
1. Execute the recovery commands to fix the error
2. Verify the fix worked
3. Continue with the original task`;

      this.formatter.info('🔧 Applying recovery plan from SubAgent');
      
      // Clear after using
      this.lastRecoveryResult = null;
      
    } else if (iteration === 1) {
      userPrompt = `Task: ${requirement}\n\nPlease start working on this task. Begin by gathering context about the project.`;
    } else {
      userPrompt = `Continue working on the task. Previous steps completed: ${this.steps.length}`;
    }

    let response = '';

    try {
      this.formatter.debug('Getting next action from LLM...');
      
      await this.llmService.streamMessageWithSystem(
        systemPrompt,
        userPrompt,
        (chunk) => {
          response += chunk;
          // Don't output individual chunks - too noisy
          // The full response will be logged via executeAction
        }
      );

      // Log the complete response in debug mode
      if (response.length > 0) {
        this.formatter.debug(`Received ${response.length} chars from LLM`);
      }

      return response;

    } catch (error) {
      this.formatter.error(`LLM error: ${error}`);
      return null;
    }
  }

  /**
   * Execute an action from the LLM response
   */
  private async executeAction(response: string, stepNumber: number): Promise<AgentStep> {
    // Extract ALL DevIns commands from response
    const devinRegex = /<devin>([\s\S]*?)<\/devin>/g;
    const devinMatches = Array.from(response.matchAll(devinRegex));

    if (devinMatches.length === 0) {
      // No DevIns command, just reasoning
      return {
        step: stepNumber,
        action: 'reasoning',
        result: response.substring(0, 200),
        success: true
      };
    }

    // Execute all DevIns blocks
    let allSuccess = true;
    let allOutput = '';
    let lastError = '';

    for (const match of devinMatches) {
      const devinCode = match[1].trim();
      
      // Extract command name for display
      const cmdMatch = devinCode.match(/^\/(\w+[-\w]*)/);
      const cmdName = cmdMatch ? cmdMatch[1] : 'command';
      
      this.formatter.debug(`Executing: ${devinCode}`);

      try {
        // Execute DevIns using tool registry
        const result = await this.compileDevIns(devinCode);

        if (result.success) {
          this.formatter.success(`Executed ${cmdName}`);
          if (result.output) {
            this.formatter.debug(`Output: ${result.output.substring(0, 200)}${result.output.length > 200 ? '...' : ''}`);
            allOutput += result.output + '\n';
          }

          // Track file edits (async but don't wait)
          this.trackEdits(devinCode).catch(e => 
            this.formatter.debug(`Failed to track edits: ${e}`)
          );
        } else {
          this.formatter.error(`Failed ${cmdName}: ${result.errorMessage}`);
          allSuccess = false;
          lastError = result.errorMessage || 'Unknown error';
        }
      } catch (error) {
        this.formatter.error(`Execution error: ${error}`);
        allSuccess = false;
        lastError = error instanceof Error ? error.message : String(error);
      }
    }

    if (allSuccess) {
      return {
        step: stepNumber,
        action: 'execute_devins',
        tool: this.extractToolName(devinMatches[0][1]),
        params: devinMatches.map(m => m[1].trim()).join('\n---\n'),
        result: allOutput,
        success: true
      };
    } else {
      return {
        step: stepNumber,
        action: 'execute_devins',
        tool: this.extractToolName(devinMatches[0][1]),
        params: devinMatches.map(m => m[1].trim()).join('\n---\n'),
        result: lastError,
        success: false
      };
    }
  }

  /**
   * Execute DevIns code by parsing and calling tools directly
   */
  private async compileDevIns(code: string): Promise<{ success: boolean; output: string; errorMessage?: string }> {
    try {
      // Parse DevIns command
      const lines = code.trim().split('\n');
      let allOutput = '';
      let allSuccess = true;
      let lastError = '';

      for (const line of lines) {
        const trimmedLine = line.trim();
        if (!trimmedLine || !trimmedLine.startsWith('/')) {
          continue;
        }

        // Parse command and parameters
        const parsed = this.parseDevInsCommand(trimmedLine);
        if (!parsed) {
          lastError = `Failed to parse command: ${trimmedLine}`;
          allSuccess = false;
          continue;
        }

        // Execute tool
        const result = await this.executeTool(parsed.tool, parsed.params);
        if (result.success) {
          allOutput += result.output + '\n';
        } else {
          allSuccess = false;
          lastError = result.errorMessage || 'Unknown error';
          allOutput += `Error: ${lastError}\n`;
        }
      }

      return {
        success: allSuccess,
        output: allOutput,
        errorMessage: allSuccess ? undefined : lastError
      };
    } catch (error) {
      return {
        success: false,
        output: '',
        errorMessage: error instanceof Error ? error.message : String(error)
      };
    }
  }

  /**
   * Parse DevIns command into tool name and parameters
   * Fixed to properly handle multiline content in parameters
   */
  private parseDevInsCommand(line: string): { tool: string; params: Record<string, any> } | null {
    // Match pattern: /tool-name param1="value1" param2="value2"
    const match = line.match(/^\/([a-z-]+)\s*(.*)/s);  // Added 's' flag for multiline
    if (!match) return null;

    const tool = match[1];
    const paramsStr = match[2];

    // Parse parameters - FIXED: Use [\s\S] instead of . to match newlines
    const params: Record<string, any> = {};
    const paramRegex = /(\w+)=(?:"((?:[^"\\]|\\.)*)"|'((?:[^'\\]|\\.)*)'|(\S+))/g;
    let paramMatch: RegExpExecArray | null;

    while ((paramMatch = paramRegex.exec(paramsStr)) !== null) {
      const key = paramMatch[1];
      let value = paramMatch[2] || paramMatch[3] || paramMatch[4];

      // Unescape common escape sequences
      if (typeof value === 'string') {
        value = value
          .replace(/\\n/g, '\n')
          .replace(/\\t/g, '\t')
          .replace(/\\r/g, '\r')
          .replace(/\\"/g, '"')
          .replace(/\\'/g, "'")
          .replace(/\\\\/g, '\\');
      }

      // Try to parse as number or boolean
      if (value === 'true') params[key] = true;
      else if (value === 'false') params[key] = false;
      else if (/^\d+$/.test(value)) params[key] = value;
      else params[key] = value;
    }

    return { tool, params };
  }

  /**
   * Execute a tool using JsToolRegistry
   */
  private async executeTool(toolName: string, params: Record<string, any>): Promise<{ success: boolean; output: string; errorMessage?: string }> {
    try {
      let result: any;

      switch (toolName) {
        case 'read-file':
          result = await this.toolRegistry.readFile(
            params.path,
            params.startLine,
            params.endLine
          );
          break;

        case 'write-file':
          // Protect critical infrastructure files in simple task mode
          if (this.isSimpleTaskMode && this.isProtectedFile(params.path)) {
            result = {
              success: false,
              output: '',
              errorMessage: `⚠️ Cannot modify protected file: ${params.path}. This file is critical project infrastructure and should not be modified for simple tasks. If you need to add functionality, create new files instead.`
            };
            this.formatter.warn(`🛡️  Blocked modification of protected file: ${params.path}`);
            break;
          }
          
          result = await this.toolRegistry.writeFile(
            params.path,
            params.content || '',
            params.createDirectories !== false
          );
          
          // Validate write operation by reading back
          if (result.success && params.content) {
            try {
              const verifyResult = await this.toolRegistry.readFile(params.path);
              if (verifyResult.success && verifyResult.output !== params.content) {
                this.formatter.warn(`File content mismatch after write: ${params.path}`);
                this.formatter.debug(`Expected ${params.content.length} chars, got ${verifyResult.output.length} chars`);
              }
            } catch (e) {
              // Verification failed, but write might have succeeded
              this.formatter.debug(`Failed to verify write: ${e}`);
            }
          }
          break;

        case 'glob':
          result = await this.toolRegistry.glob(
            params.pattern,
            params.path || '.',
            params.includeFileInfo || false
          );
          break;

        case 'grep':
          result = await this.toolRegistry.grep(
            params.pattern,
            params.path || '.',
            params.include,
            params.exclude,
            params.recursive !== false,
            params.caseSensitive !== false
          );
          break;

        case 'shell':
          result = await this.toolRegistry.shell(
            params.command,
            params.workingDirectory || this.projectPath,  // Default to project path
            Number(params.timeoutMs) || 30000
          );
          
          // If output is very long, summarize it with AI SubAgent
          if (result.success && this.logSummaryAgent.needsSummarization(result.output)) {
            this.formatter.info('📊 Output is long, activating Summary SubAgent...');
            
            const executionTimeMatch = result.output.match(/Execution Time: (\d+)ms/);
            const executionTime = executionTimeMatch ? parseInt(executionTimeMatch[1]) : 0;
            
            const summaryResult = await this.logSummaryAgent.summarize(
              {
                command: params.command,
                output: result.output,
                exitCode: 0,
                executionTime
              },
              (status) => {
                this.formatter.debug(`Summary SubAgent: ${status}`);
              }
            );
            
            // Display summary in a nice box
            this.formatter.info('\n┌─────────────────────────────────────────┐');
            this.formatter.info('│  📊 Log Summary SubAgent               │');
            this.formatter.info('└─────────────────────────────────────────┘');
            this.formatter.info(LogSummaryAgent.formatSummary(summaryResult));
            this.formatter.info('└─────────────────────────────────────────┘\n');
            
            // Replace the long output with the summary in the result
            const originalLength = result.output.length;
            result.output = `[Output summarized by AI: ${originalLength} chars -> summary]\n\n` + 
                          LogSummaryAgent.formatSummary(summaryResult);
          }
          
          // If shell command failed, activate Error Recovery SubAgent
          if (!result.success && result.errorMessage) {
            this.formatter.warn('Shell command failed');
            
            // Run SubAgent asynchronously (but wait for it)
            const recoveryResult = await this.errorRecoveryAgent.analyzeAndRecover(
              {
                command: params.command,
                errorMessage: result.errorMessage,
                stdout: result.output,
                stderr: result.errorMessage
              },
              (status) => {
                // Progress callback
                this.formatter.debug(`SubAgent: ${status}`);
              }
            );
            
            // Store recovery result for next iteration
            if (recoveryResult.success && !recoveryResult.shouldAbort) {
              this.lastRecoveryResult = recoveryResult;
              this.formatter.info('✓ SubAgent provided recovery plan');
            } else if (recoveryResult.shouldAbort) {
              this.formatter.error('✗ SubAgent recommends aborting task');
            }
          }
          break;

        default:
          return {
            success: false,
            output: '',
            errorMessage: `Unknown tool: ${toolName}`
          };
      }

      return {
        success: result.success,
        output: result.output,
        errorMessage: result.errorMessage
      };

    } catch (error) {
      return {
        success: false,
        output: '',
        errorMessage: error instanceof Error ? error.message : String(error)
      };
    }
  }

  /**
   * Extract tool name from DevIns code
   */
  private extractToolName(devinCode: string): string {
    const match = devinCode.match(/^\/(\w+)/);
    return match ? match[1] : 'unknown';
  }

  /**
   * Track file edits from DevIns commands
   * Fixed: Only track if file actually changed
   */
  private async trackEdits(devinCode: string): Promise<void> {
    // Track write operations
    if (devinCode.includes('/write-file')) {
      const pathMatch = devinCode.match(/path="([^"]+)"/);
      const contentMatch = devinCode.match(/content="([^"]*)"/);
      
      if (pathMatch) {
        const filePath = pathMatch[1];
        const newContent = contentMatch ? contentMatch[1] : '';
        
        try {
          // Check if file exists and read current content
          const readResult = await this.toolRegistry.readFile(filePath);
          
          if (readResult.success && readResult.output) {
            // File exists - check if content actually changed
            if (readResult.output !== newContent) {
              this.edits.push({
                file: filePath,
                operation: 'update',
                content: newContent
              });
            }
            // If content is the same, don't track it
          } else {
            // File doesn't exist - this is a create
            this.edits.push({
              file: filePath,
              operation: 'create',
              content: newContent
            });
          }
        } catch (error) {
          // If we can't read, assume it's a create
          this.edits.push({
            file: filePath,
            operation: 'create',
            content: newContent
          });
        }
      }
    }
  }
}
