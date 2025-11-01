/**
 * CodingAgentService - Autonomous coding agent for completing development tasks
 * 
 * This service implements an AI-powered coding agent that can:
 * - Analyze project structure
 * - Read and understand existing code
 * - Make code changes based on requirements
 * - Execute commands and tests
 * - Iterate until the task is complete
 */

import * as fs from 'fs/promises';
import * as path from 'path';
import type { LLMConfig } from '../config/ConfigManager.js';
import { LLMService } from './LLMService.js';

// Import mpp-core
// @ts-ignore
import MppCore from '@autodev/mpp-core';

const { JsCompletionManager, JsToolRegistry } = MppCore.cc.unitmesh.llm;

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
 * Coding Agent Service
 */
export class CodingAgentService {
  private llmService: LLMService;
  private projectPath: string;
  private steps: AgentStep[] = [];
  private edits: AgentEdit[] = [];
  private maxIterations: number = 10;
  private completionManager: any;
  private toolRegistry: any;

  constructor(projectPath: string, config: LLMConfig) {
    this.projectPath = path.resolve(projectPath);
    this.llmService = new LLMService(config);
    this.completionManager = new JsCompletionManager();
    this.toolRegistry = new JsToolRegistry(this.projectPath);
  }

  /**
   * Execute a development task
   */
  async executeTask(task: AgentTask): Promise<AgentResult> {
    console.log(`\n🤖 Starting AutoDev Agent...`);
    console.log(`📁 Project: ${this.projectPath}`);
    console.log(`📝 Task: ${task.requirement}\n`);

    try {
      // Initialize workspace
      await this.initializeWorkspace();

      // Build system prompt
      const systemPrompt = await this.buildSystemPrompt(task);

      // Main agent loop
      let iteration = 0;
      let taskComplete = false;

      while (iteration < this.maxIterations && !taskComplete) {
        iteration++;
        console.log(`\n--- Iteration ${iteration}/${this.maxIterations} ---`);

        // Get next action from LLM
        const action = await this.getNextAction(systemPrompt, task.requirement, iteration);

        if (!action) {
          console.log('❌ Failed to get next action from LLM');
          break;
        }

        // Execute action
        const stepResult = await this.executeAction(action, iteration);
        this.steps.push(stepResult);

        // Check if task is complete
        if (action.includes('TASK_COMPLETE') || action.includes('task complete')) {
          taskComplete = true;
          console.log('\n✅ Task marked as complete by agent');
        }

        // Small delay between iterations
        await new Promise(resolve => setTimeout(resolve, 500));
      }

      const success = taskComplete || this.steps.filter(s => s.success).length > 0;
      const message = success
        ? `✅ Task completed successfully in ${iteration} iterations`
        : `⚠️  Task incomplete after ${iteration} iterations`;

      return {
        success,
        message,
        steps: this.steps,
        edits: this.edits
      };

    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : String(error);
      console.error(`\n❌ Agent error: ${errorMsg}`);

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
      console.log(`✓ Workspace initialized: ${this.projectPath}`);
    } catch (error) {
      console.warn(`⚠️  Failed to initialize workspace: ${error}`);
    }
  }

  /**
   * Build system prompt for the agent
   */
  async buildSystemPrompt(task: AgentTask): Promise<string> {
    const osInfo = `${process.platform} ${process.arch}`;
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

    return `You are AutoDev, an autonomous AI coding agent designed to complete development tasks.

## Environment Information
- OS: ${osInfo}
- Project Path: ${this.projectPath}
- Current Time: ${timestamp}

## Project Structure
${projectStructure}

## Available Tools
You have access to the following tools through DevIns commands:

1. **read-file** - Read file content
   Examples:
   /read-file path="src/main.kt"
   /read-file path="README.md" startLine=1 endLine=10

2. **write-file** - Write content to file
   Examples:
   /write-file path="output.txt" content="Hello, World!"
   /write-file path="config.json" content="{\"key\": \"value\"}" createDirectories=true

3. **glob** - List files matching pattern
   Examples:
   /glob pattern="*.kt" path="src"
   /glob pattern="**/*.{ts,js}" includeFileInfo=true

4. **grep** - Search for pattern in files
   Examples:
   /grep pattern="function.*main" path="src" include="*.kt"
   /grep pattern="TODO|FIXME" recursive=true caseSensitive=false

5. **shell** - Execute shell command
   Examples:
   /shell command="ls -la"
   /shell command="npm test" workingDirectory="frontend" timeoutMs=60000

## Task Execution Guidelines

1. **Gather Context First**: Before making changes, use /read-file and /glob to understand the codebase
2. **Plan Your Approach**: Think step-by-step about what needs to be done
3. **Make Incremental Changes**: Make one change at a time and verify it works
4. **Test Your Changes**: Run tests or build commands to verify changes
5. **Signal Completion**: When done, respond with "TASK_COMPLETE" in your message

## Response Format

For each step, respond with:
1. Your reasoning about what to do next
2. The DevIns command(s) to execute (wrapped in <devin></devin> tags)
3. What you expect to happen

Example:
I need to check the existing implementation first.
<devin>
/read-file path="src/main.ts"
</devin>

${agentRules ? `\n## Project-Specific Rules\n${agentRules}` : ''}

Remember: You are autonomous. Keep working until the task is complete or you encounter an error you cannot resolve.
`;
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
   * Get next action from LLM
   */
  private async getNextAction(systemPrompt: string, requirement: string, iteration: number): Promise<string | null> {
    const userPrompt = iteration === 1
      ? `Task: ${requirement}\n\nPlease start working on this task. Begin by gathering context about the project.`
      : `Continue working on the task. Previous steps completed: ${this.steps.length}`;

    let response = '';

    try {
      await this.llmService.streamMessageWithSystem(
        systemPrompt,
        userPrompt,
        (chunk) => {
          response += chunk;
          process.stdout.write(chunk);
        }
      );

      console.log('\n'); // New line after streaming
      return response;

    } catch (error) {
      console.error(`\n❌ LLM error: ${error}`);
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
      console.log(`\n🔧 Executing DevIns:\n${devinCode}\n`);

      try {
        // Compile and execute DevIns using mpp-core
        const result = await this.compileDevIns(devinCode);

        if (result.success) {
          console.log(`✓ DevIns executed successfully`);
          if (result.output) {
            console.log(`Output:\n${result.output.substring(0, 500)}${result.output.length > 500 ? '...' : ''}`);
            allOutput += result.output + '\n';
          }

          // Track file edits
          this.trackEdits(devinCode);
        } else {
          console.error(`✗ DevIns failed: ${result.errorMessage}`);
          allSuccess = false;
          lastError = result.errorMessage || 'Unknown error';
        }
      } catch (error) {
        console.error(`✗ DevIns execution error: ${error}`);
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
   */
  private parseDevInsCommand(line: string): { tool: string; params: Record<string, any> } | null {
    // Match pattern: /tool-name param1="value1" param2="value2"
    const match = line.match(/^\/([a-z-]+)\s*(.*)/);
    if (!match) return null;

    const tool = match[1];
    const paramsStr = match[2];

    // Parse parameters
    const params: Record<string, any> = {};
    const paramRegex = /(\w+)=(?:"([^"]*)"|'([^']*)'|(\S+))/g;
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
      else if (/^\d+$/.test(value)) params[key] = parseInt(value);
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
          result = await this.toolRegistry.writeFile(
            params.path,
            params.content || '',
            params.createDirectories !== false
          );
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
            params.workingDirectory,
            Number(params.timeoutMs) || 30000
          );
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
   */
  private trackEdits(devinCode: string): void {
    // Track write operations
    if (devinCode.includes('/write:')) {
      const match = devinCode.match(/\/write:([^:]+)/);
      if (match) {
        this.edits.push({
          file: match[1],
          operation: 'update'
        });
      }
    }
  }
}

