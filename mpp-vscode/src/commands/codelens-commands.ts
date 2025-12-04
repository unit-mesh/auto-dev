/**
 * CodeLens 命令实现
 * 
 * 实现 CodeLens 点击后的各种操作
 */

import * as vscode from 'vscode';
import { CodeElement } from '../providers/code-element-parser';
import { executeAutoComment, executeAutoTest, executeAutoMethod, ActionContext } from '../actions/auto-actions';
import { ModelConfig } from '../bridge/mpp-core';

export class CodeLensCommands {
  constructor(
    private log: (message: string) => void,
    private getChatViewProvider: () => any | undefined,
    private getModelConfig: () => ModelConfig | undefined
  ) {}

  /**
   * 注册所有 CodeLens 命令
   */
  register(context: vscode.ExtensionContext): vscode.Disposable[] {
    return [
      // Quick Chat - 将代码发送到聊天
      vscode.commands.registerCommand(
        'autodev.codelens.quickChat',
        async (document: vscode.TextDocument, element: CodeElement) => {
          await this.handleQuickChat(document, element);
        }
      ),

      // Explain Code - 解释代码
      vscode.commands.registerCommand(
        'autodev.codelens.explainCode',
        async (document: vscode.TextDocument, element: CodeElement) => {
          await this.handleExplainCode(document, element);
        }
      ),

      // Optimize Code - 优化代码
      vscode.commands.registerCommand(
        'autodev.codelens.optimizeCode',
        async (document: vscode.TextDocument, element: CodeElement) => {
          await this.handleOptimizeCode(document, element);
        }
      ),

      // AutoComment - 生成文档注释
      vscode.commands.registerCommand(
        'autodev.codelens.autoComment',
        async (document: vscode.TextDocument, element: CodeElement) => {
          await this.handleAutoComment(document, element);
        }
      ),

      // AutoTest - 生成测试
      vscode.commands.registerCommand(
        'autodev.codelens.autoTest',
        async (document: vscode.TextDocument, element: CodeElement) => {
          await this.handleAutoTest(document, element);
        }
      ),

      // AutoMethod - 方法补全
      vscode.commands.registerCommand(
        'autodev.codelens.autoMethod',
        async (document: vscode.TextDocument, element: CodeElement) => {
          await this.handleAutoMethod(document, element);
        }
      ),

      // Show Menu - 显示折叠菜单
      vscode.commands.registerCommand(
        'autodev.codelens.showMenu',
        async (commands: vscode.Command[]) => {
          await this.handleShowMenu(commands);
        }
      )
    ];
  }

  /**
   * Quick Chat: 将代码发送到聊天
   */
  private async handleQuickChat(document: vscode.TextDocument, element: CodeElement) {
    this.log(`Quick Chat: ${element.type} ${element.name}`);
    
    const chatView = this.getChatViewProvider();
    if (!chatView) {
      vscode.window.showWarningMessage('Chat view not available');
      return;
    }

    await vscode.commands.executeCommand('autodev.chatView.focus');
    const codeContext = this.buildCodeContext(document, element);
    chatView.sendCodeContext(codeContext);
  }

  /**
   * Explain Code: 解释代码
   */
  private async handleExplainCode(document: vscode.TextDocument, element: CodeElement) {
    this.log(`Explain Code: ${element.type} ${element.name}`);
    
    const chatView = this.getChatViewProvider();
    if (!chatView) {
      vscode.window.showWarningMessage('Chat view not available');
      return;
    }

    await vscode.commands.executeCommand('autodev.chatView.focus');
    const codeContext = this.buildCodeContext(document, element);
    chatView.sendCodeContext(codeContext);
    
    setTimeout(() => {
      chatView.sendMessage('Explain this code in detail, including:\n1. What it does\n2. How it works\n3. Any potential issues or improvements');
    }, 300);
  }

  /**
   * Optimize Code: 优化代码
   */
  private async handleOptimizeCode(document: vscode.TextDocument, element: CodeElement) {
    this.log(`Optimize Code: ${element.type} ${element.name}`);
    
    const chatView = this.getChatViewProvider();
    if (!chatView) {
      vscode.window.showWarningMessage('Chat view not available');
      return;
    }

    await vscode.commands.executeCommand('autodev.chatView.focus');
    const codeContext = this.buildCodeContext(document, element);
    chatView.sendCodeContext(codeContext);
    
    setTimeout(() => {
      chatView.sendMessage('Optimize this code for better performance, readability, and maintainability');
    }, 300);
  }

  /**
   * AutoComment: 生成文档注释
   */
  private async handleAutoComment(document: vscode.TextDocument, element: CodeElement) {
    this.log(`AutoComment: ${element.type} ${element.name}`);
    
    const config = this.getModelConfig();
    if (!config) {
      vscode.window.showWarningMessage('Please configure a model first');
      return;
    }

    const context: ActionContext = {
      document,
      element,
      config,
      log: this.log
    };

    await executeAutoComment(context);
  }

  /**
   * AutoTest: 生成测试
   */
  private async handleAutoTest(document: vscode.TextDocument, element: CodeElement) {
    this.log(`AutoTest: ${element.type} ${element.name}`);
    
    const config = this.getModelConfig();
    if (!config) {
      vscode.window.showWarningMessage('Please configure a model first');
      return;
    }

    const context: ActionContext = {
      document,
      element,
      config,
      log: this.log
    };

    await executeAutoTest(context);
  }

  /**
   * AutoMethod: 方法补全
   */
  private async handleAutoMethod(document: vscode.TextDocument, element: CodeElement) {
    this.log(`AutoMethod: ${element.type} ${element.name}`);
    
    const config = this.getModelConfig();
    if (!config) {
      vscode.window.showWarningMessage('Please configure a model first');
      return;
    }

    const context: ActionContext = {
      document,
      element,
      config,
      log: this.log
    };

    await executeAutoMethod(context);
  }

  /**
   * Show Menu: 显示折叠菜单
   */
  private async handleShowMenu(commands: vscode.Command[]) {
    const items = commands.map(cmd => ({
      label: cmd.title,
      description: cmd.tooltip,
      command: cmd
    }));

    const selected = await vscode.window.showQuickPick(items, {
      placeHolder: 'Select an action'
    });

    if (selected && selected.command.command) {
      await vscode.commands.executeCommand(
        selected.command.command,
        ...(selected.command.arguments || [])
      );
    }
  }

  /**
   * 构建代码上下文
   */
  private buildCodeContext(document: vscode.TextDocument, element: CodeElement) {
    return {
      filepath: document.uri.fsPath,
      language: document.languageId,
      element: {
        type: element.type,
        name: element.name,
        code: element.code,
        range: {
          start: {
            line: element.bodyRange.start.line,
            character: element.bodyRange.start.character
          },
          end: {
            line: element.bodyRange.end.line,
            character: element.bodyRange.end.character
          }
        }
      }
    };
  }
}
