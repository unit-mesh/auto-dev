/**
 * Chat View Provider - Webview for chat interface
 *
 * Uses mpp-core's CodingAgent for agent-based interactions
 * Mirrors mpp-ui's AgentChatInterface architecture
 */

import * as vscode from 'vscode';
import { CodingAgent, ToolRegistry, ModelConfig, VSCodeRenderer } from '../bridge/mpp-core';

/**
 * Chat View Provider for the sidebar webview
 */
export class ChatViewProvider implements vscode.WebviewViewProvider {
  private webviewView: vscode.WebviewView | undefined;
  private codingAgent: CodingAgent | undefined;
  private toolRegistry: ToolRegistry | undefined;
  private renderer: VSCodeRenderer | undefined;
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
      }
    });

    // Initialize agent
    this.initializeAgent();
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

  private initializeAgent(): void {
    const config = vscode.workspace.getConfiguration('autodev');
    const workspacePath = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath || '';

    const modelConfig: ModelConfig = {
      provider: config.get<string>('provider', 'openai'),
      model: config.get<string>('model', 'gpt-4'),
      apiKey: config.get<string>('apiKey', ''),
      baseUrl: config.get<string>('baseUrl', '')
    };

    // Create renderer that forwards events to webview
    this.renderer = new VSCodeRenderer(this);

    try {
      // Initialize tool registry with VSCode tools
      this.toolRegistry = new ToolRegistry(workspacePath);

      // Initialize coding agent (will use config from mpp-core)
      this.codingAgent = new CodingAgent(
        modelConfig,
        this.toolRegistry,
        this.renderer,
        workspacePath
      );

      this.log(`CodingAgent initialized for workspace: ${workspacePath}`);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.log(`Failed to initialize CodingAgent: ${message}`);
      // Don't show error - agent will be initialized on first message if needed
    }
  }

  private async handleUserMessage(content: string): Promise<void> {
    // Initialize agent if not ready
    if (!this.codingAgent) {
      this.initializeAgent();
    }

    // Add user message to timeline
    this.messages.push({ role: 'user', content });
    this.postMessage({ type: 'userMessage', content });

    // Check if this is a DevIns command
    const isDevInsCommand = content.startsWith('/') || content.startsWith('@');

    try {
      if (this.codingAgent && isDevInsCommand) {
        // Use CodingAgent for DevIns commands
        await this.codingAgent.execute(content);
      } else {
        // Simple chat mode - stream response directly
        this.postMessage({ type: 'startResponse' });

        // For now, echo back a helpful message
        const response = this.getHelpfulResponse(content);
        this.postMessage({ type: 'responseChunk', content: response });
        this.postMessage({ type: 'endResponse' });

        this.messages.push({ role: 'assistant', content: response });
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.log(`Error in chat: ${message}`);
      this.postMessage({ type: 'error', content: message });
    }
  }

  private getHelpfulResponse(content: string): string {
    // Provide helpful guidance for users
    return `I'm AutoDev, your AI coding assistant. Here's how to use me:

**Commands (start with /):**
- \`/file:path\` - Read a file
- \`/write:path\` - Write to a file
- \`/run:command\` - Run a shell command
- \`/commit\` - Generate commit message

**Agents (start with @):**
- \`@code\` - Code generation
- \`@test\` - Test generation
- \`@doc\` - Documentation
- \`@review\` - Code review

**Variables (use $):**
- \`$selection\` - Current selection
- \`$file\` - Current file
- \`$language\` - Current language

Try: \`@code write a function to sort an array\`

Your message: "${content}"`;
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

