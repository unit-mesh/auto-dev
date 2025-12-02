/**
 * Diff Manager - Manages file diff views in VSCode
 * 
 * Handles showing, accepting, and canceling file diffs.
 */

import * as vscode from 'vscode';
import * as path from 'path';
import { DIFF_SCHEME } from '../extension';

/**
 * Provides content for diff documents
 */
export class DiffContentProvider implements vscode.TextDocumentContentProvider {
  private content = new Map<string, string>();
  private onDidChangeEmitter = new vscode.EventEmitter<vscode.Uri>();

  get onDidChange(): vscode.Event<vscode.Uri> {
    return this.onDidChangeEmitter.event;
  }

  provideTextDocumentContent(uri: vscode.Uri): string {
    return this.content.get(uri.toString()) ?? '';
  }

  setContent(uri: vscode.Uri, content: string): void {
    this.content.set(uri.toString(), content);
    this.onDidChangeEmitter.fire(uri);
  }

  deleteContent(uri: vscode.Uri): void {
    this.content.delete(uri.toString());
  }

  getContent(uri: vscode.Uri): string | undefined {
    return this.content.get(uri.toString());
  }
}

/**
 * Information about an open diff view
 */
interface DiffInfo {
  originalFilePath: string;
  newContent: string;
  rightDocUri: vscode.Uri;
}

/**
 * Event types for diff changes
 */
export interface DiffEvent {
  type: 'accepted' | 'closed';
  filePath: string;
  content: string;
}

/**
 * Manages diff view lifecycle
 */
export class DiffManager {
  private readonly onDidChangeEmitter = new vscode.EventEmitter<DiffEvent>();
  readonly onDidChange = this.onDidChangeEmitter.event;
  
  private diffDocuments = new Map<string, DiffInfo>();
  private readonly subscriptions: vscode.Disposable[] = [];

  constructor(
    private readonly log: (message: string) => void,
    private readonly diffContentProvider: DiffContentProvider
  ) {
    this.subscriptions.push(
      vscode.window.onDidChangeActiveTextEditor((editor) => {
        this.onActiveEditorChange(editor);
      })
    );
    this.onActiveEditorChange(vscode.window.activeTextEditor);
  }

  dispose(): void {
    for (const subscription of this.subscriptions) {
      subscription.dispose();
    }
  }

  /**
   * Show a diff view for a file
   */
  async showDiff(filePath: string, newContent: string): Promise<void> {
    const fileUri = vscode.Uri.file(filePath);

    const rightDocUri = vscode.Uri.from({
      scheme: DIFF_SCHEME,
      path: filePath,
      query: `rand=${Math.random()}`
    });
    
    this.diffContentProvider.setContent(rightDocUri, newContent);
    this.diffDocuments.set(rightDocUri.toString(), {
      originalFilePath: filePath,
      newContent,
      rightDocUri
    });

    const diffTitle = `${path.basename(filePath)} ↔ Modified`;
    await vscode.commands.executeCommand('setContext', 'autodev.diff.isVisible', true);

    let leftDocUri: vscode.Uri;
    try {
      await vscode.workspace.fs.stat(fileUri);
      leftDocUri = fileUri;
    } catch {
      // File doesn't exist, use untitled scheme
      leftDocUri = vscode.Uri.from({
        scheme: 'untitled',
        path: filePath
      });
    }

    await vscode.commands.executeCommand(
      'vscode.diff',
      leftDocUri,
      rightDocUri,
      diffTitle,
      { preview: false, preserveFocus: true }
    );

    this.log(`Showing diff for: ${filePath}`);
  }

  /**
   * Accept changes in a diff view
   */
  async acceptDiff(rightDocUri: vscode.Uri): Promise<void> {
    const diffInfo = this.diffDocuments.get(rightDocUri.toString());
    if (!diffInfo) {
      return;
    }

    const rightDoc = await vscode.workspace.openTextDocument(rightDocUri);
    const modifiedContent = rightDoc.getText();
    
    // Write the content to the original file
    const fileUri = vscode.Uri.file(diffInfo.originalFilePath);
    await vscode.workspace.fs.writeFile(fileUri, Buffer.from(modifiedContent, 'utf8'));
    
    await this.closeDiffEditor(rightDocUri);

    this.onDidChangeEmitter.fire({
      type: 'accepted',
      filePath: diffInfo.originalFilePath,
      content: modifiedContent
    });

    this.log(`Accepted diff for: ${diffInfo.originalFilePath}`);
  }

  /**
   * Cancel a diff view
   */
  async cancelDiff(rightDocUri: vscode.Uri): Promise<void> {
    const diffInfo = this.diffDocuments.get(rightDocUri.toString());
    if (!diffInfo) {
      await this.closeDiffEditor(rightDocUri);
      return;
    }

    const rightDoc = await vscode.workspace.openTextDocument(rightDocUri);
    const modifiedContent = rightDoc.getText();
    await this.closeDiffEditor(rightDocUri);

    this.onDidChangeEmitter.fire({
      type: 'closed',
      filePath: diffInfo.originalFilePath,
      content: modifiedContent
    });

    this.log(`Cancelled diff for: ${diffInfo.originalFilePath}`);
  }

  /**
   * Close a diff by file path
   */
  async closeDiffByPath(filePath: string): Promise<string | undefined> {
    let uriToClose: vscode.Uri | undefined;

    for (const [uriString, diffInfo] of this.diffDocuments.entries()) {
      if (diffInfo.originalFilePath === filePath) {
        uriToClose = vscode.Uri.parse(uriString);
        break;
      }
    }

    if (uriToClose) {
      const rightDoc = await vscode.workspace.openTextDocument(uriToClose);
      const modifiedContent = rightDoc.getText();
      await this.closeDiffEditor(uriToClose);

      this.onDidChangeEmitter.fire({
        type: 'closed',
        filePath,
        content: modifiedContent
      });

      return modifiedContent;
    }

    return undefined;
  }

  /**
   * Check if a diff is open for a file
   */
  hasDiff(filePath: string): boolean {
    for (const diffInfo of this.diffDocuments.values()) {
      if (diffInfo.originalFilePath === filePath) {
        return true;
      }
    }
    return false;
  }

  private async onActiveEditorChange(editor: vscode.TextEditor | undefined): Promise<void> {
    let isVisible = false;

    if (editor) {
      isVisible = this.diffDocuments.has(editor.document.uri.toString());
      if (!isVisible) {
        for (const document of this.diffDocuments.values()) {
          if (document.originalFilePath === editor.document.uri.fsPath) {
            isVisible = true;
            break;
          }
        }
      }
    }

    await vscode.commands.executeCommand('setContext', 'autodev.diff.isVisible', isVisible);
  }

  private async closeDiffEditor(rightDocUri: vscode.Uri): Promise<void> {
    const diffInfo = this.diffDocuments.get(rightDocUri.toString());
    await vscode.commands.executeCommand('setContext', 'autodev.diff.isVisible', false);

    if (diffInfo) {
      this.diffDocuments.delete(rightDocUri.toString());
      this.diffContentProvider.deleteContent(rightDocUri);
    }

    // Find and close the tab
    for (const tabGroup of vscode.window.tabGroups.all) {
      for (const tab of tabGroup.tabs) {
        const input = tab.input as { modified?: vscode.Uri; original?: vscode.Uri };
        if (input && input.modified?.toString() === rightDocUri.toString()) {
          await vscode.window.tabGroups.close(tab);
          return;
        }
      }
    }
  }
}

