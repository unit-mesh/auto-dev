/**
 * Chat View Provider - Webview for chat interface
 */

import * as vscode from 'vscode';
import * as path from 'path';
import { LLMService, ModelConfig } from '../bridge/mpp-core';

/**
 * Chat View Provider for the sidebar webview
 */
export class ChatViewProvider implements vscode.WebviewViewProvider {
  private webviewView: vscode.WebviewView | undefined;
  private llmService: LLMService | undefined;
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
      }
    });

    // Initialize LLM service
    this.initializeLLMService();
  }

  /**
   * Send a message programmatically
   */
  async sendMessage(content: string): Promise<void> {
    if (this.webviewView) {
      // Show the chat view
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

  private initializeLLMService(): void {
    const config = vscode.workspace.getConfiguration('autodev');
    
    const modelConfig: ModelConfig = {
      provider: config.get<string>('provider', 'openai'),
      model: config.get<string>('model', 'gpt-4'),
      apiKey: config.get<string>('apiKey', ''),
      baseUrl: config.get<string>('baseUrl', '')
    };

    if (!modelConfig.apiKey) {
      this.postMessage({
        type: 'error',
        content: 'Please configure your API key in settings (autodev.apiKey)'
      });
      return;
    }

    try {
      this.llmService = new LLMService(modelConfig);
      this.log(`LLM Service initialized: ${modelConfig.provider}/${modelConfig.model}`);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.log(`Failed to initialize LLM service: ${message}`);
      this.postMessage({ type: 'error', content: message });
    }
  }

  private async handleUserMessage(content: string): Promise<void> {
    if (!this.llmService) {
      this.initializeLLMService();
      if (!this.llmService) {
        return;
      }
    }

    // Add user message
    this.messages.push({ role: 'user', content });
    this.postMessage({ type: 'userMessage', content });

    // Start streaming response
    this.postMessage({ type: 'startResponse' });

    try {
      let fullResponse = '';
      
      await this.llmService.streamMessage(content, (chunk) => {
        fullResponse += chunk;
        this.postMessage({ type: 'responseChunk', content: chunk });
      });

      this.messages.push({ role: 'assistant', content: fullResponse });
      this.postMessage({ type: 'endResponse' });
      
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.log(`Error in chat: ${message}`);
      this.postMessage({ type: 'error', content: message });
    }
  }

  private clearHistory(): void {
    this.messages = [];
    this.llmService?.clearHistory();
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

