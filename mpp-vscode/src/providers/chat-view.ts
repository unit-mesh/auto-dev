/**
 * Chat View Provider - Webview for chat interface
 *
 * Uses mpp-core's JsCodingAgent for agent-based interactions
 * Configuration is loaded from ~/.autodev/config.yaml (same as CLI and Desktop)
 *
 * Architecture mirrors:
 * - mpp-ui/src/jsMain/typescript/modes/AgentMode.ts
 * - mpp-idea/src/main/kotlin/cc/unitmesh/devins/idea/toolwindow/IdeaAgentViewModel.kt
 */

import * as vscode from 'vscode';
import { ConfigManager, AutoDevConfigWrapper, LLMConfig } from '../services/config-manager';

// @ts-ignore - Kotlin/JS generated module
import MppCore from '@autodev/mpp-core';

// Access Kotlin/JS exports - same pattern as AgentMode.ts
const KotlinCC = MppCore.cc.unitmesh;

/**
 * Chat View Provider for the sidebar webview
 */
export class ChatViewProvider implements vscode.WebviewViewProvider {
  private webviewView: vscode.WebviewView | undefined;
  private codingAgent: any = null;
  private llmService: any = null;
  private configWrapper: AutoDevConfigWrapper | null = null;
  private completionManager: any = null;
  private isExecuting = false;
  private messages: Array<{ role: string; content: string }> = [];
  private editorChangeDisposable: vscode.Disposable | undefined;

  constructor(
    private readonly context: vscode.ExtensionContext,
    private readonly log: (message: string) => void
  ) {
    // Initialize completion manager
    try {
      if (KotlinCC?.llm?.JsCompletionManager) {
        this.completionManager = new KotlinCC.llm.JsCompletionManager();
        this.log('CompletionManager initialized');
      }
    } catch (error) {
      this.log(`Failed to initialize CompletionManager: ${error}`);
    }

    // Listen to active editor changes
    this.editorChangeDisposable = vscode.window.onDidChangeActiveTextEditor((editor) => {
      if (editor && this.webviewView) {
        this.sendActiveFileUpdate(editor.document);
      }
    });
  }

  /**
   * Send active file update to webview
   */
  private sendActiveFileUpdate(document: vscode.TextDocument): void {
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) return;

    const relativePath = vscode.workspace.asRelativePath(document.uri, false);
    const fileName = document.fileName.split('/').pop() || document.fileName.split('\\').pop() || '';
    const isDirectory = false;

    // Skip binary files and non-file schemes
    if (document.uri.scheme !== 'file') return;
    const binaryExtensions = ['jar', 'class', 'exe', 'dll', 'so', 'dylib', 'png', 'jpg', 'jpeg', 'gif', 'ico', 'pdf', 'zip', 'tar', 'gz', 'rar', '7z'];
    const ext = fileName.split('.').pop()?.toLowerCase() || '';
    if (binaryExtensions.includes(ext)) return;

    this.postMessage({
      type: 'activeFileChanged',
      data: {
        path: relativePath,
        name: fileName,
        isDirectory
      }
    });
  }

  /**
   * Send current active file to webview
   */
  private sendCurrentActiveFile(): void {
    const editor = vscode.window.activeTextEditor;
    if (editor) {
      this.sendActiveFileUpdate(editor.document);
    }
  }

  async resolveWebviewView(
    webviewView: vscode.WebviewView,
    _context: vscode.WebviewViewResolveContext,
    _token: vscode.CancellationToken
  ): Promise<void> {
    this.webviewView = webviewView;

    webviewView.webview.options = {
      enableScripts: true,
      localResourceRoots: [
        vscode.Uri.joinPath(this.context.extensionUri, 'dist', 'webview')
      ]
    };

    webviewView.webview.html = this.getHtmlContent(webviewView.webview);

    // Handle messages from webview
    webviewView.webview.onDidReceiveMessage(async (message) => {
      this.log(`Received message from webview: ${JSON.stringify(message)}`);
      switch (message.type) {
        case 'sendMessage':
          await this.handleUserMessage(message.content);
          break;
        case 'clearHistory':
          this.clearHistory();
          break;
        case 'action':
          await this.handleAction(message.action, message.data);
          break;
        case 'openConfig':
          await this.openConfigFile();
          break;
        case 'stopExecution':
          this.stopExecution();
          break;
        case 'selectConfig':
          await this.selectConfig(message.data?.configName as string);
          break;
        case 'searchFiles':
          await this.handleSearchFiles(message.data?.query as string);
          break;
        case 'getRecentFiles':
          await this.handleGetRecentFiles();
          break;
        case 'readFileContent':
          await this.handleReadFileContent(message.data?.path as string);
          break;
        case 'requestConfig':
          // Webview is ready and requesting config
          this.sendConfigUpdate();
          // Also send current active file
          this.sendCurrentActiveFile();
          break;
        case 'getActiveFile':
          // Get current active file
          this.sendCurrentActiveFile();
          break;
        case 'getCompletions':
          // Get completion suggestions from mpp-core
          await this.handleGetCompletions(
            message.data?.text as string,
            message.data?.cursorPosition as number
          );
          break;
        case 'applyCompletion':
          // Apply a completion item
          await this.handleApplyCompletion(
            message.data?.text as string,
            message.data?.cursorPosition as number,
            message.data?.completionIndex as number
          );
          break;
      }
    });

    // Initialize agent from config file
    await this.initializeFromConfig();
  }

  /**
   * Stop current execution
   */
  private stopExecution(): void {
    if (this.isExecuting) {
      this.isExecuting = false;
      this.postMessage({ type: 'taskComplete', data: { success: false, message: 'Stopped by user' } });
      this.log('Execution stopped by user');
    }
  }

  /**
   * Select a different config
   */
  private async selectConfig(configName: string): Promise<void> {
    if (!this.configWrapper || !configName) return;

    const configs = this.configWrapper.getAllConfigs();
    const selectedConfig = configs.find(c => c.name === configName);

    if (selectedConfig) {
      // Recreate LLM service with new config
      this.llmService = this.createLLMService(selectedConfig);
      this.codingAgent = null; // Reset agent to use new LLM service

      this.log(`Switched to config: ${configName}`);

      // Send updated config state to webview
      this.sendConfigUpdate(configName);

      this.postMessage({
        type: 'responseChunk',
        content: `✨ Switched to: \`${selectedConfig.name}\` (${selectedConfig.provider}/${selectedConfig.model})`
      });
    }
  }

  /**
   * Send a message programmatically
   */
  async sendMessage(content: string): Promise<void> {
    if (this.webviewView) {
      this.webviewView.show(true);
    }
    await this.handleUserMessage(content);
  }

  /**
   * Post a message to the webview
   */
  postMessage(message: any): void {
    this.webviewView?.webview.postMessage(message);
  }

  /**
   * Send config update to webview
   */
  private sendConfigUpdate(currentConfigName?: string): void {
    if (!this.configWrapper) return;

    const configs = this.configWrapper.getAllConfigs();
    const availableConfigs = configs.map(c => ({
      name: c.name,
      provider: c.provider,
      model: c.model
    }));

    this.postMessage({
      type: 'configUpdate',
      data: {
        availableConfigs,
        currentConfigName: currentConfigName || this.configWrapper.getActiveConfig()?.name || null
      }
    });
  }

  /**
   * Initialize from ~/.autodev/config.yaml
   * Mirrors IdeaAgentViewModel.loadConfiguration()
   */
  private async initializeFromConfig(): Promise<void> {
    try {
      this.configWrapper = await ConfigManager.load();
      const activeConfig = this.configWrapper.getActiveConfig();

      // Send config state to webview
      this.sendConfigUpdate();

      if (!activeConfig || !this.configWrapper.isValid()) {
        this.log('No valid configuration found in ~/.autodev/config.yaml');
        // Show welcome message with config instructions
        this.postMessage({
          type: 'responseChunk',
          content: this.getWelcomeMessage()
        });
        return;
      }

      // Create LLM service
      this.llmService = this.createLLMService(activeConfig);
      this.log(`LLM Service initialized: ${activeConfig.provider}/${activeConfig.model}`);

      // Show ready message
      this.postMessage({
        type: 'responseChunk',
        content: `✨ **AutoDev Ready**\n\nUsing: \`${activeConfig.name}\` (${activeConfig.provider}/${activeConfig.model})\n\nType a message or use DevIns commands like \`/file:path\` or \`@code\`.`
      });

    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.log(`Failed to load configuration: ${message}`);
    }
  }

  /**
   * Create LLM service from config
   * Same pattern as AgentMode.ts
   */
  private createLLMService(config: LLMConfig): any {
    const modelConfig = new KotlinCC.llm.JsModelConfig(
      config.provider,  // Use lowercase provider name like AgentMode.ts
      config.model,
      config.apiKey || '',
      config.temperature ?? 0.7,
      config.maxTokens ?? 8192,
      config.baseUrl || ''
    );
    return new KotlinCC.llm.JsKoogLLMService(modelConfig);
  }

  /**
   * Initialize CodingAgent (lazy initialization)
   * Same pattern as AgentMode.ts
   */
  private initializeCodingAgent(): any {
    if (this.codingAgent) {
      return this.codingAgent;
    }

    if (!this.llmService) {
      throw new Error('LLM service not configured. Please configure ~/.autodev/config.yaml');
    }

    const workspacePath = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath || process.cwd();
    const mcpServers = this.configWrapper?.getEnabledMcpServers() || {};

    // Create renderer that forwards events to webview
    const renderer = this.createRenderer();

    // Create CodingAgent - same constructor as AgentMode.ts
    this.codingAgent = new KotlinCC.agent.JsCodingAgent(
      workspacePath,
      this.llmService,
      10, // maxIterations
      renderer,
      Object.keys(mcpServers).length > 0 ? mcpServers : null,
      null // toolConfig
    );

    this.log(`CodingAgent initialized for workspace: ${workspacePath}`);
    return this.codingAgent;
  }

  /**
   * Create renderer that forwards events to webview
   * Mirrors TuiRenderer from mpp-ui
   * Must implement JsCodingAgentRenderer interface including __doNotUseOrImplementIt
   */
  private createRenderer(): any {
    const self = this;
    return {
      // Required by Kotlin JS export interface
      __doNotUseOrImplementIt: {},

      renderIterationHeader: (current: number, max: number) => {
        self.postMessage({ type: 'iterationUpdate', data: { current, max } });
      },
      renderLLMResponseStart: () => {
        self.postMessage({ type: 'startResponse' });
      },
      renderLLMResponseChunk: (chunk: string) => {
        self.postMessage({ type: 'responseChunk', content: chunk });
      },
      renderLLMResponseEnd: () => {
        self.postMessage({ type: 'endResponse' });
      },
      renderToolCall: (toolName: string, params: string) => {
        self.postMessage({
          type: 'toolCall',
          data: { toolName, params, description: `Calling ${toolName}` }
        });
      },
      renderToolResult: (toolName: string, success: boolean, output: string | null, fullOutput: string | null) => {
        self.postMessage({
          type: 'toolResult',
          data: { toolName, success, output, fullOutput }
        });
      },
      renderTaskComplete: () => {
        self.postMessage({ type: 'taskComplete', data: { success: true, message: 'Task completed' } });
      },
      renderFinalResult: (success: boolean, message: string, iterations: number) => {
        self.postMessage({
          type: 'taskComplete',
          data: { success, message: `${message} (${iterations} iterations)` }
        });
      },
      renderError: (message: string) => {
        self.postMessage({ type: 'error', content: message });
      },
      renderRepeatWarning: (toolName: string, count: number) => {
        self.postMessage({
          type: 'error',
          content: `Warning: ${toolName} called ${count} times`
        });
      },
      renderRecoveryAdvice: (advice: string) => {
        self.postMessage({ type: 'responseChunk', content: `\n💡 ${advice}\n` });
      },
      renderUserConfirmationRequest: () => {},
      addLiveTerminal: () => {},
      forceStop: () => {
        self.postMessage({ type: 'taskComplete', data: { success: false, message: 'Stopped' } });
      }
    };
  }

  /**
   * Handle user message
   * Mirrors IdeaAgentViewModel.executeTask()
   */
  private async handleUserMessage(content: string): Promise<void> {
    if (this.isExecuting) {
      this.postMessage({ type: 'error', content: 'Already executing a task. Please wait.' });
      return;
    }

    const trimmedContent = content?.trim();
    if (!trimmedContent) return;

    // Add user message to timeline
    this.messages.push({ role: 'user', content: trimmedContent });
    this.postMessage({ type: 'userMessage', content: trimmedContent });

    // Check if LLM is configured
    if (!this.llmService) {
      this.postMessage({
        type: 'responseChunk',
        content: this.getConfigRequiredMessage()
      });
      return;
    }

    this.isExecuting = true;

    try {
      const agent = this.initializeCodingAgent();
      const workspacePath = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath || process.cwd();
      const task = new KotlinCC.agent.JsAgentTask(trimmedContent, workspacePath);
      const result = await agent.executeTask(task);

      if (result && result.message) {
        this.messages.push({ role: 'assistant', content: result.message });
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.log(`Error in chat: ${message}`);
      this.postMessage({ type: 'error', content: message });
    } finally {
      this.isExecuting = false;
    }
  }

  private getWelcomeMessage(): string {
    return `# Welcome to AutoDev! 🚀

No configuration found. Please create \`~/.autodev/config.yaml\`:

\`\`\`yaml
active: default
configs:
  - name: default
    provider: openai  # or anthropic, deepseek, ollama, etc.
    apiKey: your-api-key
    model: gpt-4
\`\`\`

**Supported Providers:**
- \`openai\` - OpenAI (GPT-4, GPT-3.5)
- \`anthropic\` - Anthropic (Claude)
- \`deepseek\` - DeepSeek
- \`ollama\` - Ollama (local models)
- \`openrouter\` - OpenRouter

Click **Open Config** below to create the file.`;
  }

  private getConfigRequiredMessage(): string {
    return `⚠️ **Configuration Required**

Please configure your LLM provider in \`~/.autodev/config.yaml\`.

Click **Open Config** to edit the configuration file.`;
  }

  /**
   * Open config file in editor
   */
  private async openConfigFile(): Promise<void> {
    const configPath = ConfigManager.getConfigPath();
    const uri = vscode.Uri.file(configPath);

    try {
      await vscode.workspace.fs.stat(uri);
    } catch {
      // File doesn't exist, create it with template
      const template = `# AutoDev Configuration
# See: https://github.com/phodal/auto-dev

active: default
configs:
  - name: default
    provider: openai
    apiKey: your-api-key-here
    model: gpt-4
    # baseUrl: https://api.openai.com/v1  # Optional
    # temperature: 0.7
    # maxTokens: 8192

# MCP Servers (optional)
# mcpServers:
#   filesystem:
#     command: npx
#     args: ["-y", "@anthropic/mcp-server-filesystem"]
`;
      await vscode.workspace.fs.writeFile(uri, Buffer.from(template, 'utf-8'));
    }

    const doc = await vscode.workspace.openTextDocument(uri);
    await vscode.window.showTextDocument(doc);
  }

  private async handleAction(action: string, data: any): Promise<void> {
    this.log(`Action: ${action}, data: ${JSON.stringify(data)}`);

    switch (action) {
      case 'insert':
        // Insert code at cursor
        const editor = vscode.window.activeTextEditor;
        if (editor && data.code) {
          await editor.edit(editBuilder => {
            editBuilder.insert(editor.selection.active, data.code);
          });
        }
        break;

      case 'apply':
        // Apply code changes (show diff)
        if (data.code) {
          // TODO: Show diff view
          vscode.window.showInformationMessage('Apply code: ' + data.code.substring(0, 50) + '...');
        }
        break;

      case 'accept-diff':
        // Accept diff changes
        // TODO: Apply diff
        break;

      case 'reject-diff':
        // Reject diff changes
        break;

      case 'run-command':
        // Run terminal command
        if (data.command) {
          const terminal = vscode.window.createTerminal('AutoDev');
          terminal.show();
          terminal.sendText(data.command);
        }
        break;

      case 'view-diff':
        // Open diff view
        break;

      case 'rerun-tool':
        // Rerun a tool
        break;

      case 'optimizePrompt':
        // Optimize prompt using LLM
        await this.handlePromptOptimize(data?.prompt as string);
        break;

      case 'openMcpConfig':
        // Open MCP configuration
        await this.openMcpConfig();
        break;
    }
  }

  /**
   * Open MCP configuration file
   */
  private async openMcpConfig(): Promise<void> {
    try {
      const homeDir = process.env.HOME || process.env.USERPROFILE || '';
      const mcpConfigPath = `${homeDir}/.autodev/mcp.json`;

      // Check if file exists, create if not
      const fs = await import('fs').then(m => m.promises);
      try {
        await fs.access(mcpConfigPath);
      } catch {
        // Create default MCP config
        const defaultConfig = {
          mcpServers: {}
        };
        await fs.mkdir(`${homeDir}/.autodev`, { recursive: true });
        await fs.writeFile(mcpConfigPath, JSON.stringify(defaultConfig, null, 2));
      }

      // Open the file in VSCode
      const uri = vscode.Uri.file(mcpConfigPath);
      await vscode.window.showTextDocument(uri);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.log(`Failed to open MCP config: ${message}`);
      vscode.window.showErrorMessage(`Failed to open MCP config: ${message}`);
    }
  }

  /**
   * Handle prompt optimization request
   * Uses LLM to enhance the user's prompt
   */
  private async handlePromptOptimize(prompt: string): Promise<void> {
    if (!prompt || !this.llmService) {
      this.postMessage({ type: 'promptOptimizeFailed', data: { error: 'No prompt or LLM service' } });
      return;
    }

    try {
      const systemPrompt = `You are a prompt optimization assistant. Your task is to enhance the user's prompt to be more clear, specific, and effective for an AI coding assistant.

Rules:
1. Keep the original intent and meaning
2. Add clarity and specificity where needed
3. Structure the prompt for better understanding
4. Keep it concise - don't make it unnecessarily long
5. Return ONLY the optimized prompt, no explanations

User's original prompt:`;

      const response = await this.llmService.chat([
        { role: 'system', content: systemPrompt },
        { role: 'user', content: prompt }
      ]);

      if (response) {
        this.postMessage({ type: 'promptOptimized', data: { optimizedPrompt: response.trim() } });
      } else {
        this.postMessage({ type: 'promptOptimizeFailed', data: { error: 'Empty response' } });
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.log(`Prompt optimization failed: ${message}`);
      this.postMessage({ type: 'promptOptimizeFailed', data: { error: message } });
    }
  }

  /**
   * Handle file search request from webview
   * Searches for files matching the query in the workspace
   */
  private async handleSearchFiles(query: string): Promise<void> {
    if (!query || query.length < 2) {
      this.postMessage({ type: 'searchFilesResult', data: { files: [], folders: [] } });
      return;
    }

    try {
      const workspaceFolders = vscode.workspace.workspaceFolders;
      if (!workspaceFolders) {
        this.postMessage({ type: 'searchFilesResult', data: { files: [], folders: [] } });
        return;
      }

      const lowerQuery = query.toLowerCase();

      // Search for files matching the query
      const files = await vscode.workspace.findFiles(
        `**/*${query}*`,
        '**/node_modules/**',
        50
      );

      const fileResults: Array<{ name: string; path: string; relativePath: string; isDirectory: boolean }> = [];
      const folderPaths = new Set<string>();

      for (const file of files) {
        const relativePath = vscode.workspace.asRelativePath(file, false);
        const name = file.path.split('/').pop() || '';

        // Skip binary files
        const ext = name.split('.').pop()?.toLowerCase() || '';
        const binaryExts = ['jar', 'class', 'exe', 'dll', 'so', 'dylib', 'png', 'jpg', 'jpeg', 'gif', 'ico', 'pdf', 'zip', 'tar', 'gz'];
        if (binaryExts.includes(ext)) continue;

        fileResults.push({
          name,
          path: relativePath, // Use relative path for consistency with activeFileChanged
          relativePath,
          isDirectory: false
        });

        // Collect parent folders that match the query
        const parts = relativePath.split('/');
        let currentPath = '';
        for (let i = 0; i < parts.length - 1; i++) {
          currentPath = currentPath ? `${currentPath}/${parts[i]}` : parts[i];
          if (parts[i].toLowerCase().includes(lowerQuery)) {
            folderPaths.add(currentPath);
          }
        }
      }

      const folderResults = Array.from(folderPaths).slice(0, 10).map(p => ({
        name: p.split('/').pop() || p,
        path: p, // Use relative path for consistency
        relativePath: p,
        isDirectory: true
      }));

      this.postMessage({
        type: 'searchFilesResult',
        data: {
          files: fileResults.slice(0, 30),
          folders: folderResults
        }
      });
    } catch (error) {
      this.log(`Error searching files: ${error}`);
      this.postMessage({ type: 'searchFilesResult', data: { files: [], folders: [] } });
    }
  }

  /**
   * Handle get recent files request from webview
   * Returns recently opened files in the workspace
   */
  private async handleGetRecentFiles(): Promise<void> {
    try {
      // Get recently opened text documents
      const recentFiles: Array<{ name: string; path: string; relativePath: string; isDirectory: boolean }> = [];

      // Get visible text editors first (most recently used)
      for (const editor of vscode.window.visibleTextEditors) {
        const doc = editor.document;
        if (doc.uri.scheme === 'file') {
          const relativePath = vscode.workspace.asRelativePath(doc.uri, false);
          recentFiles.push({
            name: doc.fileName.split('/').pop() || '',
            path: relativePath, // Use relative path for consistency
            relativePath,
            isDirectory: false
          });
        }
      }

      // Add other open documents
      for (const doc of vscode.workspace.textDocuments) {
        const relativePath = vscode.workspace.asRelativePath(doc.uri, false);
        if (doc.uri.scheme === 'file' && !recentFiles.some(f => f.path === relativePath)) {
          recentFiles.push({
            name: doc.fileName.split('/').pop() || '',
            path: relativePath, // Use relative path for consistency
            relativePath,
            isDirectory: false
          });
        }
      }

      this.postMessage({
        type: 'recentFilesResult',
        data: { files: recentFiles.slice(0, 20) }
      });
    } catch (error) {
      this.log(`Error getting recent files: ${error}`);
      this.postMessage({ type: 'recentFilesResult', data: { files: [] } });
    }
  }

  /**
   * Handle read file content request from webview
   * filePath can be either relative or absolute path
   */
  private async handleReadFileContent(filePath: string): Promise<void> {
    if (!filePath) {
      this.postMessage({ type: 'fileContentResult', data: { content: null, error: 'No path provided' } });
      return;
    }

    try {
      // Convert relative path to absolute if needed
      let uri: vscode.Uri;
      if (filePath.startsWith('/') || filePath.match(/^[a-zA-Z]:\\/)) {
        // Already absolute path
        uri = vscode.Uri.file(filePath);
      } else {
        // Relative path - resolve against workspace
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        if (!workspaceFolder) {
          this.postMessage({ type: 'fileContentResult', data: { content: null, error: 'No workspace folder' } });
          return;
        }
        uri = vscode.Uri.joinPath(workspaceFolder.uri, filePath);
      }

      const content = await vscode.workspace.fs.readFile(uri);
      const text = new TextDecoder().decode(content);

      this.postMessage({
        type: 'fileContentResult',
        data: { path: filePath, content: text }
      });
    } catch (error) {
      this.log(`Error reading file: ${error}`);
      this.postMessage({
        type: 'fileContentResult',
        data: { path: filePath, content: null, error: String(error) }
      });
    }
  }

  /**
   * Handle get completions request from webview
   * Uses mpp-core's CompletionManager
   */
  private async handleGetCompletions(text: string, cursorPosition: number): Promise<void> {
    if (!this.completionManager) {
      this.postMessage({ type: 'completionsResult', data: { items: [] } });
      return;
    }

    try {
      const items = this.completionManager.getCompletions(text, cursorPosition);
      const itemsArray = Array.from(items || []).map((item: any, index: number) => ({
        text: item.text,
        displayText: item.displayText,
        description: item.description,
        icon: item.icon,
        triggerType: item.triggerType,
        index
      }));

      this.postMessage({ type: 'completionsResult', data: { items: itemsArray } });
    } catch (error) {
      this.log(`Error getting completions: ${error}`);
      this.postMessage({ type: 'completionsResult', data: { items: [] } });
    }
  }

  /**
   * Handle apply completion request from webview
   * Uses mpp-core's CompletionManager insert handler
   */
  private async handleApplyCompletion(
    text: string,
    cursorPosition: number,
    completionIndex: number
  ): Promise<void> {
    if (!this.completionManager) {
      this.postMessage({ type: 'completionApplied', data: null });
      return;
    }

    try {
      const result = this.completionManager.applyCompletion(text, cursorPosition, completionIndex);
      if (result) {
        this.postMessage({
          type: 'completionApplied',
          data: {
            newText: result.newText,
            newCursorPosition: result.newCursorPosition,
            shouldTriggerNextCompletion: result.shouldTriggerNextCompletion
          }
        });
      } else {
        this.postMessage({ type: 'completionApplied', data: null });
      }
    } catch (error) {
      this.log(`Error applying completion: ${error}`);
      this.postMessage({ type: 'completionApplied', data: null });
    }
  }

  private clearHistory(): void {
    this.messages = [];
    this.codingAgent?.clearHistory();
    this.postMessage({ type: 'historyCleared' });
  }

  private getHtmlContent(webview: vscode.Webview): string {
    // Check if React build exists
    const webviewPath = vscode.Uri.joinPath(this.context.extensionUri, 'dist', 'webview');
    const scriptUri = webview.asWebviewUri(vscode.Uri.joinPath(webviewPath, 'assets', 'index.js'));
    const styleUri = webview.asWebviewUri(vscode.Uri.joinPath(webviewPath, 'assets', 'index.css'));

    // Try to use React build, fallback to inline HTML
    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src ${webview.cspSource} 'unsafe-inline'; script-src ${webview.cspSource} 'unsafe-inline';">
  <link rel="stylesheet" href="${styleUri}">
  <title>AutoDev Chat</title>
  <script>
    // Acquire VSCode API ONCE and store on window BEFORE React loads
    (function() {
      if (typeof acquireVsCodeApi === 'function') {
        window.vscodeApi = acquireVsCodeApi();
        console.log('[AutoDev] VSCode API acquired and stored on window.vscodeApi');
      }
    })();
  </script>
</head>
<body>
  <div id="root"></div>
  <script src="${scriptUri}"></script>
  <script>
    // Fallback if React bundle not loaded
    if (!document.getElementById('root').hasChildNodes()) {
      document.getElementById('root').innerHTML = \`
        <div style="height: 100vh; display: flex; flex-direction: column; font-family: var(--vscode-font-family); color: var(--vscode-foreground); background: var(--vscode-editor-background);">
          <div id="messages" style="flex: 1; overflow-y: auto; padding: 12px;"></div>
          <div style="padding: 12px; border-top: 1px solid var(--vscode-panel-border); display: flex; gap: 8px;">
            <textarea id="input" rows="2" placeholder="Ask AutoDev..." style="flex: 1; padding: 8px; border: 1px solid var(--vscode-input-border); background: var(--vscode-input-background); color: var(--vscode-input-foreground); border-radius: 4px; resize: none;"></textarea>
            <button id="send" style="padding: 8px 16px; background: var(--vscode-button-background); color: var(--vscode-button-foreground); border: none; border-radius: 4px; cursor: pointer;">Send</button>
          </div>
        </div>
      \`;

      const vscode = acquireVsCodeApi();
      const messagesEl = document.getElementById('messages');
      const inputEl = document.getElementById('input');
      const sendBtn = document.getElementById('send');
      let currentResponse = null;

      function addMessage(role, content) {
        const div = document.createElement('div');
        div.className = 'message ' + role;
        div.style.cssText = 'margin-bottom: 12px; padding: 8px 12px; border-radius: 6px; max-width: 90%;';
        if (role === 'user') div.style.cssText += 'background: var(--vscode-button-background); margin-left: auto;';
        else if (role === 'assistant') div.style.cssText += 'background: var(--vscode-editor-inactiveSelectionBackground);';
        else div.style.cssText += 'background: var(--vscode-inputValidation-errorBackground); color: var(--vscode-inputValidation-errorForeground);';
        div.textContent = content;
        messagesEl.appendChild(div);
        messagesEl.scrollTop = messagesEl.scrollHeight;
        return div;
      }

      sendBtn.onclick = () => {
        const content = inputEl.value.trim();
        if (content) {
          vscode.postMessage({ type: 'sendMessage', content });
          inputEl.value = '';
        }
      };

      inputEl.onkeydown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
          e.preventDefault();
          sendBtn.click();
        }
      };

      window.addEventListener('message', (e) => {
        const msg = e.data;
        switch (msg.type) {
          case 'userMessage': addMessage('user', msg.content); break;
          case 'startResponse': currentResponse = addMessage('assistant', ''); break;
          case 'responseChunk': if (currentResponse) { currentResponse.textContent += msg.content; messagesEl.scrollTop = messagesEl.scrollHeight; } break;
          case 'endResponse': currentResponse = null; break;
          case 'error': addMessage('error', msg.content); break;
          case 'historyCleared': messagesEl.innerHTML = ''; break;
        }
      });
    }
  </script>
</body>
</html>`;
  }
}

