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
  private isExecuting = false;
  private messages: Array<{ role: string; content: string }> = [];

  constructor(
    private readonly context: vscode.ExtensionContext,
    private readonly log: (message: string) => void
  ) {}

  resolveWebviewView(
    webviewView: vscode.WebviewView,
    _context: vscode.WebviewViewResolveContext,
    _token: vscode.CancellationToken
  ): void {
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
      }
    });

    // Initialize agent from config file
    this.initializeFromConfig();
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
   */
  private createRenderer(): any {
    const self = this;
    return {
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

    const trimmedContent = content.trim();
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
      // Initialize agent if needed
      const agent = this.initializeCodingAgent();
      const workspacePath = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath || process.cwd();

      // Create task and execute - same pattern as AgentMode.ts
      const task = new KotlinCC.agent.JsAgentTask(trimmedContent, workspacePath);
      const result = await agent.executeTask(task);

      // Add completion message
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

    // Use nonce for security
    const nonce = this.getNonce();

    // Try to use React build, fallback to inline HTML
    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src ${webview.cspSource} 'unsafe-inline'; script-src 'nonce-${nonce}';">
  <link rel="stylesheet" href="${styleUri}">
  <title>AutoDev Chat</title>
</head>
<body>
  <div id="root"></div>
  <script nonce="${nonce}" src="${scriptUri}"></script>
  <script nonce="${nonce}">
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

  private getNonce(): string {
    let text = '';
    const possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    for (let i = 0; i < 32; i++) {
      text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return text;
  }
}

