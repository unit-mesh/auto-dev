/**
 * CodeLens Provider - 在函数/类上方显示操作按钮
 * 
 * 提供的操作：
 * - Quick Chat: 将代码发送到聊天
 * - Explain Code: 解释代码
 * - Optimize Code: 优化代码
 * - AutoComment: 生成文档注释
 * - AutoTest: 生成测试代码
 * - AutoMethod: 方法补全
 */

import * as vscode from 'vscode';
import { CodeElementParser, CodeElement, CodeElementType } from './code-element-parser';

export type CodeLensAction = 
  | 'quickChat' 
  | 'explainCode' 
  | 'optimizeCode' 
  | 'autoComment' 
  | 'autoTest' 
  | 'autoMethod';

export class AutoDevCodeLensProvider implements vscode.CodeLensProvider {
  private parser: CodeElementParser;
  private _onDidChangeCodeLenses: vscode.EventEmitter<void> = new vscode.EventEmitter<void>();
  public readonly onDidChangeCodeLenses: vscode.Event<void> = this._onDidChangeCodeLenses.event;

  constructor(
    private log: (message: string) => void,
    extensionPath?: string
  ) {
    this.parser = new CodeElementParser(log, extensionPath);
  }

  /**
   * 刷新 CodeLens
   */
  public refresh(): void {
    this._onDidChangeCodeLenses.fire();
  }

  /**
   * 提供 CodeLens
   */
  async provideCodeLenses(
    document: vscode.TextDocument,
    token: vscode.CancellationToken
  ): Promise<vscode.CodeLens[]> {
    const config = vscode.workspace.getConfiguration('autodev');
    
    // 检查是否启用
    const enabled = config.get<boolean>('codelens.enable', true);
    if (!enabled) {
      return [];
    }

    // 检查文件大小限制 (避免大文件解析)
    if (document.lineCount > 10000) {
      this.log(`File too large (${document.lineCount} lines), skipping CodeLens`);
      return [];
    }

    try {
      // 解析代码元素
      const elements = await this.parser.parseDocument(document);
      if (token.isCancellationRequested || elements.length === 0) {
        return [];
      }

      // 获取配置的显示项
      const displayMode = config.get<string>('codelens.displayMode', 'expand');
      const displayItems = new Set(config.get<CodeLensAction[]>('codelens.items', [
        'quickChat',
        'autoTest',
        'autoComment'
      ]));

      // 构建 CodeLens 组
      const groups = this.buildCodeLensGroups(elements, displayItems, document);
      
      // 根据显示模式返回
      if (displayMode === 'collapse') {
        return groups.map(group => this.buildCollapsedCodeLens(group));
      }
      
      return groups.flat();
    } catch (error) {
      this.log(`Error providing CodeLens: ${error instanceof Error ? error.message : String(error)}`);
      return [];
    }
  }

  /**
   * 构建 CodeLens 组（每个元素一组）
   */
  private buildCodeLensGroups(
    elements: CodeElement[],
    displayItems: Set<CodeLensAction>,
    document: vscode.TextDocument
  ): vscode.CodeLens[][] {
    const groups: vscode.CodeLens[][] = [];

    for (const element of elements) {
      const codeLenses: vscode.CodeLens[] = [];

      for (const action of displayItems) {
        // AutoTest 只在非测试文件中显示
        if (action === 'autoTest' && this.isTestFile(document.fileName)) {
          continue;
        }

        // AutoMethod 只在方法中显示
        if (action === 'autoMethod' && element.type !== CodeElementType.Method) {
          continue;
        }

        const lens = this.createCodeLens(element, action, document);
        if (lens) {
          codeLenses.push(lens);
        }
      }

      if (codeLenses.length > 0) {
        groups.push(codeLenses);
      }
    }

    return groups;
  }

  /**
   * 创建单个 CodeLens
   */
  private createCodeLens(
    element: CodeElement,
    action: CodeLensAction,
    document: vscode.TextDocument
  ): vscode.CodeLens | null {
    const range = element.nameRange;

    switch (action) {
      case 'quickChat':
        return new vscode.CodeLens(range, {
          title: '💬 Quick Chat',
          command: 'autodev.codelens.quickChat',
          arguments: [document, element]
        });

      case 'explainCode':
        return new vscode.CodeLens(range, {
          title: '📖 Explain',
          command: 'autodev.codelens.explainCode',
          arguments: [document, element]
        });

      case 'optimizeCode':
        return new vscode.CodeLens(range, {
          title: '⚡ Optimize',
          command: 'autodev.codelens.optimizeCode',
          arguments: [document, element]
        });

      case 'autoComment':
        return new vscode.CodeLens(range, {
          title: '📝 AutoComment',
          command: 'autodev.codelens.autoComment',
          arguments: [document, element]
        });

      case 'autoTest':
        return new vscode.CodeLens(range, {
          title: '🧪 AutoTest',
          command: 'autodev.codelens.autoTest',
          arguments: [document, element]
        });

      case 'autoMethod':
        return new vscode.CodeLens(range, {
          title: '✨ AutoMethod',
          command: 'autodev.codelens.autoMethod',
          arguments: [document, element]
        });

      default:
        return null;
    }
  }

  /**
   * 构建折叠模式的 CodeLens（显示下拉菜单）
   */
  private buildCollapsedCodeLens(group: vscode.CodeLens[]): vscode.CodeLens {
    const [first] = group;
    const commands = group.map(lens => lens.command!);

    return new vscode.CodeLens(first.range, {
      title: '$(autodev-icon) $(chevron-down)',
      command: 'autodev.codelens.showMenu',
      arguments: [commands],
      tooltip: 'AutoDev Actions'
    });
  }

  /**
   * 判断是否为测试文件
   */
  private isTestFile(fileName: string): boolean {
    const testPatterns = [
      /\.test\./,
      /\.spec\./,
      /_test\./,
      /_spec\./,
      /test_.*\.py$/,
      /.*Test\.java$/,
      /.*Test\.kt$/,
      /.*Tests\.cs$/,
      /_test\.go$/
    ];

    return testPatterns.some(pattern => pattern.test(fileName));
  }

  dispose() {
    this._onDidChangeCodeLenses.dispose();
  }
}


