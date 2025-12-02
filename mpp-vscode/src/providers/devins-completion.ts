/**
 * DevIns Completion Provider - Auto-completion for DevIns language
 */

import * as vscode from 'vscode';
import { CompletionManager } from '../bridge/mpp-core';

/**
 * Built-in DevIns commands
 */
const BUILTIN_COMMANDS = [
  { name: '/file', description: 'Read file content', args: ':path' },
  { name: '/write', description: 'Write content to file', args: ':path' },
  { name: '/run', description: 'Run shell command', args: ':command' },
  { name: '/patch', description: 'Apply patch to file', args: ':path' },
  { name: '/commit', description: 'Create git commit', args: ':message' },
  { name: '/symbol', description: 'Find symbol in codebase', args: ':name' },
  { name: '/rev', description: 'Review code changes', args: '' },
  { name: '/refactor', description: 'Refactor code', args: ':instruction' },
  { name: '/test', description: 'Generate tests', args: '' },
  { name: '/doc', description: 'Generate documentation', args: '' },
  { name: '/help', description: 'Show available commands', args: '' }
];

/**
 * Built-in agents
 */
const BUILTIN_AGENTS = [
  { name: '@code', description: 'Code generation agent' },
  { name: '@test', description: 'Test generation agent' },
  { name: '@doc', description: 'Documentation agent' },
  { name: '@review', description: 'Code review agent' },
  { name: '@refactor', description: 'Refactoring agent' },
  { name: '@explain', description: 'Code explanation agent' }
];

/**
 * Built-in variables
 */
const BUILTIN_VARIABLES = [
  { name: '$selection', description: 'Current editor selection' },
  { name: '$file', description: 'Current file path' },
  { name: '$fileName', description: 'Current file name' },
  { name: '$language', description: 'Current file language' },
  { name: '$workspace', description: 'Workspace root path' },
  { name: '$clipboard', description: 'Clipboard content' }
];

/**
 * DevIns Completion Provider
 */
export class DevInsCompletionProvider implements vscode.CompletionItemProvider {
  private completionManager: CompletionManager | undefined;

  constructor() {
    try {
      this.completionManager = new CompletionManager();
    } catch (e) {
      // mpp-core not available, use built-in completions only
    }
  }

  async provideCompletionItems(
    document: vscode.TextDocument,
    position: vscode.Position,
    _token: vscode.CancellationToken,
    _context: vscode.CompletionContext
  ): Promise<vscode.CompletionItem[]> {
    const linePrefix = document.lineAt(position).text.substring(0, position.character);
    const items: vscode.CompletionItem[] = [];

    // Command completion (starts with /)
    if (linePrefix.endsWith('/') || /\/[a-zA-Z]*$/.test(linePrefix)) {
      items.push(...this.getCommandCompletions(linePrefix));
    }

    // Agent completion (starts with @)
    if (linePrefix.endsWith('@') || /@[a-zA-Z]*$/.test(linePrefix)) {
      items.push(...this.getAgentCompletions(linePrefix));
    }

    // Variable completion (starts with $)
    if (linePrefix.endsWith('$') || /\$[a-zA-Z]*$/.test(linePrefix)) {
      items.push(...this.getVariableCompletions(linePrefix));
    }

    return items;
  }

  private getCommandCompletions(linePrefix: string): vscode.CompletionItem[] {
    const prefix = linePrefix.match(/\/([a-zA-Z]*)$/)?.[1] || '';
    
    return BUILTIN_COMMANDS
      .filter(cmd => cmd.name.substring(1).startsWith(prefix))
      .map(cmd => {
        const item = new vscode.CompletionItem(
          cmd.name,
          vscode.CompletionItemKind.Function
        );
        item.detail = cmd.description;
        item.insertText = cmd.name.substring(1) + cmd.args;
        item.documentation = new vscode.MarkdownString(`**${cmd.name}**\n\n${cmd.description}`);
        return item;
      });
  }

  private getAgentCompletions(linePrefix: string): vscode.CompletionItem[] {
    const prefix = linePrefix.match(/@([a-zA-Z]*)$/)?.[1] || '';
    
    return BUILTIN_AGENTS
      .filter(agent => agent.name.substring(1).startsWith(prefix))
      .map(agent => {
        const item = new vscode.CompletionItem(
          agent.name,
          vscode.CompletionItemKind.Class
        );
        item.detail = agent.description;
        item.insertText = agent.name.substring(1);
        item.documentation = new vscode.MarkdownString(`**${agent.name}**\n\n${agent.description}`);
        return item;
      });
  }

  private getVariableCompletions(linePrefix: string): vscode.CompletionItem[] {
    const prefix = linePrefix.match(/\$([a-zA-Z]*)$/)?.[1] || '';
    
    return BUILTIN_VARIABLES
      .filter(v => v.name.substring(1).startsWith(prefix))
      .map(v => {
        const item = new vscode.CompletionItem(
          v.name,
          vscode.CompletionItemKind.Variable
        );
        item.detail = v.description;
        item.insertText = v.name.substring(1);
        item.documentation = new vscode.MarkdownString(`**${v.name}**\n\n${v.description}`);
        return item;
      });
  }
}

/**
 * Register DevIns completion provider
 */
export function registerDevInsCompletionProvider(
  context: vscode.ExtensionContext
): vscode.Disposable {
  const provider = new DevInsCompletionProvider();
  
  return vscode.languages.registerCompletionItemProvider(
    { language: 'DevIns', scheme: 'file' },
    provider,
    '/', '@', '$'
  );
}

