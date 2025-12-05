/**
 * mpp-core Bridge - TypeScript wrapper for Kotlin/JS compiled mpp-core module
 * 
 * This module provides a TypeScript-friendly interface to the mpp-core
 * Kotlin Multiplatform library.
 */

// @ts-ignore - Kotlin/JS generated module
import MppCore from '@autodev/mpp-core';

// Access the exported Kotlin/JS classes
const {
  JsKoogLLMService,
  JsModelConfig,
  JsMessage,
  JsModelRegistry,
  JsCompletionManager,
  JsDevInsCompiler,
  JsToolRegistry
} = MppCore.cc.unitmesh.llm;

const { JsCodingAgent, JsAgentTask } = MppCore.cc.unitmesh.agent;

// Provider type mapping
export const ProviderTypes: Record<string, string> = {
  'openai': 'OPENAI',
  'anthropic': 'ANTHROPIC',
  'google': 'GOOGLE',
  'deepseek': 'DEEPSEEK',
  'ollama': 'OLLAMA',
  'openrouter': 'OPENROUTER',
  'custom-openai-base': 'CUSTOM_OPENAI_BASE'
};

/**
 * Model configuration interface
 */
export interface ModelConfig {
  provider: string;
  model: string;
  apiKey: string;
  temperature?: number;
  maxTokens?: number;
  baseUrl?: string;
}

/**
 * Message interface for chat history
 */
export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

/**
 * LLM Service wrapper - provides streaming and non-streaming LLM calls
 */
export class LLMService {
  private koogService: any;
  private chatHistory: ChatMessage[] = [];

  constructor(private config: ModelConfig) {
    const providerName = ProviderTypes[config.provider.toLowerCase()] || config.provider.toUpperCase();
    
    const modelConfig = new JsModelConfig(
      providerName,
      config.model,
      config.apiKey,
      config.temperature ?? 0.7,
      config.maxTokens ?? 8192,
      config.baseUrl ?? ''
    );

    this.koogService = new JsKoogLLMService(modelConfig);
  }

  /**
   * Stream a message and receive chunks via callback
   */
  async streamMessage(
    message: string,
    onChunk: (chunk: string) => void
  ): Promise<string> {
    this.chatHistory.push({ role: 'user', content: message });

    const historyMessages = this.chatHistory.slice(0, -1).map(msg =>
      new JsMessage(msg.role, msg.content)
    );

    let fullResponse = '';

    await this.koogService.streamPrompt(
      message,
      historyMessages,
      (chunk: string) => {
        fullResponse += chunk;
        onChunk(chunk);
      },
      (error: any) => {
        throw new Error(`LLM Error: ${error.message || error}`);
      },
      () => { /* complete */ }
    );

    this.chatHistory.push({ role: 'assistant', content: fullResponse });
    return fullResponse;
  }

  /**
   * Send a prompt and get complete response (non-streaming)
   */
  async sendPrompt(prompt: string): Promise<string> {
    return await this.koogService.sendPrompt(prompt);
  }

  /**
   * Clear chat history
   */
  clearHistory(): void {
    this.chatHistory = [];
  }

  /**
   * Get chat history
   */
  getHistory(): ChatMessage[] {
    return [...this.chatHistory];
  }

  /**
   * Get token info from last request
   */
  getLastTokenInfo(): { totalTokens: number; inputTokens: number; outputTokens: number } {
    const info = this.koogService.getLastTokenInfo();
    return {
      totalTokens: info.totalTokens,
      inputTokens: info.inputTokens,
      outputTokens: info.outputTokens
    };
  }
}

/**
 * Completion Manager - provides auto-completion for @agent, /command, $variable
 */
export class CompletionManager {
  private manager: any;

  constructor() {
    this.manager = new JsCompletionManager();
  }

  /**
   * Initialize workspace for file path completion
   */
  async initWorkspace(workspacePath: string): Promise<boolean> {
    return await this.manager.initWorkspace(workspacePath);
  }

  /**
   * Get completion suggestions
   */
  getCompletions(text: string, cursorPosition: number): CompletionItem[] {
    const items = this.manager.getCompletions(text, cursorPosition);
    return Array.from(items).map((item: any) => ({
      text: item.text,
      displayText: item.displayText,
      description: item.description,
      icon: item.icon,
      triggerType: item.triggerType,
      index: item.index
    }));
  }
}

export interface CompletionItem {
  text: string;
  displayText: string;
  description: string | null;
  icon: string | null;
  triggerType: string;
  index: number;
}

/**
 * DevIns Compiler - compiles DevIns code (e.g., "/read-file:path")
 */
export class DevInsCompiler {
  private compiler: any;

  constructor() {
    this.compiler = new JsDevInsCompiler();
  }

  /**
   * Compile DevIns source code
   */
  async compile(source: string): Promise<DevInsResult> {
    const result = await this.compiler.compile(source);
    return {
      success: result.success,
      output: result.output,
      errorMessage: result.errorMessage,
      hasCommand: result.hasCommand
    };
  }

  /**
   * Compile and return just the output string
   */
  async compileToString(source: string): Promise<string> {
    return await this.compiler.compileToString(source);
  }
}

export interface DevInsResult {
  success: boolean;
  output: string;
  errorMessage: string | null;
  hasCommand: boolean;
}

/**
 * Tool Registry - provides access to built-in tools
 */
export class ToolRegistry {
  private registry: any;

  constructor(projectPath: string) {
    this.registry = new JsToolRegistry(projectPath);
  }

  /**
   * Read a file
   */
  async readFile(path: string, startLine?: number, endLine?: number): Promise<ToolResult> {
    const result = await this.registry.readFile(path, startLine, endLine);
    return this.toToolResult(result);
  }

  /**
   * Write a file
   */
  async writeFile(path: string, content: string, createDirectories = true): Promise<ToolResult> {
    const result = await this.registry.writeFile(path, content, createDirectories);
    return this.toToolResult(result);
  }

  /**
   * Glob pattern matching
   */
  async glob(pattern: string, path = '.', includeFileInfo = false): Promise<ToolResult> {
    const result = await this.registry.glob(pattern, path, includeFileInfo);
    return this.toToolResult(result);
  }

  /**
   * Grep search
   */
  async grep(
    pattern: string,
    path = '.',
    options?: { include?: string; exclude?: string; recursive?: boolean; caseSensitive?: boolean }
  ): Promise<ToolResult> {
    const result = await this.registry.grep(
      pattern,
      path,
      options?.include,
      options?.exclude,
      options?.recursive ?? true,
      options?.caseSensitive ?? true
    );
    return this.toToolResult(result);
  }

  /**
   * Execute shell command
   */
  async shell(command: string, workingDirectory?: string, timeoutMs = 30000): Promise<ToolResult> {
    const result = await this.registry.shell(command, workingDirectory, timeoutMs);
    return this.toToolResult(result);
  }

  /**
   * Get available tools
   */
  getAvailableTools(): string[] {
    return Array.from(this.registry.getAvailableTools());
  }

  /**
   * Format tool list for AI consumption
   */
  formatToolListForAI(): string {
    return this.registry.formatToolListForAI();
  }

  private toToolResult(result: any): ToolResult {
    return {
      success: result.success,
      output: result.output,
      errorMessage: result.errorMessage,
      metadata: result.metadata
    };
  }
}

export interface ToolResult {
  success: boolean;
  output: string;
  errorMessage: string | null;
  metadata: Record<string, string>;
}

/**
 * VSCode Renderer - Forwards agent events to webview
 * Implements the JsCodingAgentRenderer interface
 */
export class VSCodeRenderer {
  constructor(private chatProvider: { postMessage: (msg: any) => void }) {}

  renderIterationHeader(current: number, max: number): void {
    this.chatProvider.postMessage({
      type: 'iterationUpdate',
      data: { current, max }
    });
  }

  renderLLMResponseStart(): void {
    this.chatProvider.postMessage({ type: 'startResponse' });
  }

  renderLLMResponseChunk(chunk: string): void {
    this.chatProvider.postMessage({ type: 'responseChunk', content: chunk });
  }

  renderLLMResponseEnd(): void {
    this.chatProvider.postMessage({ type: 'endResponse' });
  }

  renderToolCall(toolName: string, paramsStr: string): void {
    this.chatProvider.postMessage({
      type: 'toolCall',
      data: {
        toolName,
        params: paramsStr,
        description: `Calling ${toolName}`,
        success: null
      }
    });
  }

  renderToolResult(toolName: string, success: boolean, output: string | null, fullOutput: string | null): void {
    this.chatProvider.postMessage({
      type: 'toolResult',
      data: {
        toolName,
        success,
        output,
        fullOutput,
        summary: success ? 'Completed' : 'Failed'
      }
    });
  }

  renderTaskComplete(executionTimeMs: number = 0, toolsUsedCount: number = 0): void {
    const parts: string[] = [];

    if (executionTimeMs > 0) {
      const seconds = (executionTimeMs / 1000).toFixed(2);
      parts.push(`${seconds}s`);
    }

    if (toolsUsedCount > 0) {
      parts.push(`${toolsUsedCount} tools`);
    }

    const info = parts.length > 0 ? ` (${parts.join(', ')})` : '';
    const message = `Task completed${info}`;
    this.chatProvider.postMessage({
      type: 'taskComplete',
      data: { success: true, message }
    });
  }

  renderFinalResult(success: boolean, message: string, iterations: number): void {
    this.chatProvider.postMessage({
      type: 'taskComplete',
      data: { success, message: `${message} (${iterations} iterations)` }
    });
  }

  renderError(message: string): void {
    this.chatProvider.postMessage({ type: 'error', content: message });
  }

  renderRepeatWarning(toolName: string, count: number): void {
    this.chatProvider.postMessage({
      type: 'error',
      content: `Warning: ${toolName} called ${count} times - consider different approach`
    });
  }

  renderRecoveryAdvice(advice: string): void {
    this.chatProvider.postMessage({
      type: 'responseChunk',
      content: `\n\n💡 **Suggestion**: ${advice}\n`
    });
  }

  renderUserConfirmationRequest(toolName: string, params: Record<string, any>): void {
    // Auto-approve for now
    this.chatProvider.postMessage({
      type: 'toolCall',
      data: {
        toolName,
        params: JSON.stringify(params),
        description: `Tool '${toolName}' needs approval (auto-approved)`,
        success: null
      }
    });
  }

  forceStop(): void {
    this.chatProvider.postMessage({
      type: 'taskComplete',
      data: { success: false, message: 'Stopped by user' }
    });
  }
}

/**
 * Coding Agent - AI-powered coding assistant
 * Wraps mpp-core's JsCodingAgent
 */
export class CodingAgent {
  private agent: any;
  private renderer: VSCodeRenderer;

  constructor(
    config: ModelConfig,
    toolRegistry: ToolRegistry,
    renderer: VSCodeRenderer,
    projectPath: string,
    options?: {
      maxIterations?: number;
      mcpServers?: Record<string, any>;
    }
  ) {
    this.renderer = renderer;

    // Create model config
    const providerName = ProviderTypes[config.provider.toLowerCase()] || config.provider.toUpperCase();
    const modelConfig = new JsModelConfig(
      providerName,
      config.model,
      config.apiKey,
      config.temperature ?? 0.7,
      config.maxTokens ?? 8192,
      config.baseUrl ?? ''
    );

    // Create LLM service
    const llmService = new JsKoogLLMService(modelConfig);

    // Create agent with renderer
    this.agent = new JsCodingAgent(
      projectPath,
      llmService,
      options?.maxIterations ?? 100,
      this.createKotlinRenderer(),
      options?.mcpServers ?? null,
      null  // toolConfig
    );
  }

  /**
   * Create a Kotlin-compatible renderer object
   */
  private createKotlinRenderer(): any {
    const renderer = this.renderer;
    return {
      renderIterationHeader: (c: number, m: number) => renderer.renderIterationHeader(c, m),
      renderLLMResponseStart: () => renderer.renderLLMResponseStart(),
      renderLLMResponseChunk: (chunk: string) => renderer.renderLLMResponseChunk(chunk),
      renderLLMResponseEnd: () => renderer.renderLLMResponseEnd(),
      renderToolCall: (name: string, params: string) => renderer.renderToolCall(name, params),
      renderToolResult: (name: string, success: boolean, output: string | null, full: string | null) =>
        renderer.renderToolResult(name, success, output, full),
      renderTaskComplete: (executionTimeMs?: number, toolsUsedCount?: number) => renderer.renderTaskComplete(executionTimeMs, toolsUsedCount),
      renderFinalResult: (success: boolean, msg: string, iters: number) =>
        renderer.renderFinalResult(success, msg, iters),
      renderError: (msg: string) => renderer.renderError(msg),
      renderRepeatWarning: (name: string, count: number) => renderer.renderRepeatWarning(name, count),
      renderRecoveryAdvice: (advice: string) => renderer.renderRecoveryAdvice(advice),
      renderUserConfirmationRequest: (name: string, params: any) =>
        renderer.renderUserConfirmationRequest(name, params),
      forceStop: () => renderer.forceStop()
    };
  }

  /**
   * Execute a DevIns command or natural language task
   */
  async execute(input: string): Promise<void> {
    try {
      await this.agent.execute(input);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.renderer.renderError(message);
    }
  }

  /**
   * Execute a coding task
   */
  async executeTask(requirement: string, projectPath: string): Promise<AgentResult> {
    const task = new JsAgentTask(requirement, projectPath);
    const result = await this.agent.executeTask(task);

    return {
      success: result.success,
      message: result.message,
      steps: Array.from(result.steps).map((step: any) => ({
        step: step.step,
        action: step.action,
        tool: step.tool,
        params: step.params,
        result: step.result,
        success: step.success
      })),
      edits: Array.from(result.edits).map((edit: any) => ({
        file: edit.file,
        operation: edit.operation,
        content: edit.content
      }))
    };
  }

  /**
   * Clear conversation history
   */
  clearHistory(): void {
    this.agent.clearHistory?.();
  }

  /**
   * Get conversation history
   */
  getConversationHistory(): ChatMessage[] {
    const history = this.agent.getConversationHistory?.() || [];
    return Array.from(history).map((msg: any) => ({
      role: msg.role as 'user' | 'assistant' | 'system',
      content: msg.content
    }));
  }
}

export interface AgentResult {
  success: boolean;
  message: string;
  steps: AgentStep[];
  edits: AgentEdit[];
}

export interface AgentStep {
  step: number;
  action: string;
  tool: string | null;
  params: string | null;
  result: string | null;
  success: boolean;
}

export interface AgentEdit {
  file: string;
  operation: string;
  content: string | null;
}

/**
 * Get available models for a provider
 */
export function getAvailableModels(provider: string): string[] {
  const providerName = ProviderTypes[provider.toLowerCase()] || provider.toUpperCase();
  return Array.from(JsModelRegistry.getAvailableModels(providerName));
}

/**
 * Get all supported providers
 */
export function getAllProviders(): string[] {
  return Array.from(JsModelRegistry.getAllProviders());
}

