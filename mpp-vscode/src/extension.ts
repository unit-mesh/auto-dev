/**
 * AutoDev VSCode Extension - Entry Point
 * 
 * Based on Kotlin Multiplatform (KMP) technology, reusing mpp-core capabilities.
 */

import * as vscode from 'vscode';
import { IDEServer } from './services/ide-server';
import { DiffManager, DiffContentProvider } from './services/diff-manager';
import { ChatViewProvider } from './providers/chat-view';
import { StatusBarManager } from './services/status-bar';
import { registerDevInsCompletionProvider } from './providers/devins-completion';
import { createLogger } from './utils/logger';

export const DIFF_SCHEME = 'autodev-diff';

let ideServer: IDEServer | undefined;
let statusBar: StatusBarManager | undefined;
let logger: vscode.OutputChannel;
let log: (message: string) => void = () => {};

/**
 * Extension activation
 */
export async function activate(context: vscode.ExtensionContext) {
  logger = vscode.window.createOutputChannel('AutoDev');
  log = createLogger(context, logger);
  log('AutoDev extension activated');

  // Initialize Status Bar
  statusBar = new StatusBarManager();
  context.subscriptions.push({ dispose: () => statusBar?.dispose() });

  // Initialize Diff Manager
  const diffContentProvider = new DiffContentProvider();
  const diffManager = new DiffManager(log, diffContentProvider);

  // Register Diff Content Provider
  context.subscriptions.push(
    vscode.workspace.registerTextDocumentContentProvider(DIFF_SCHEME, diffContentProvider)
  );

  // Handle diff document close
  context.subscriptions.push(
    vscode.workspace.onDidCloseTextDocument((doc) => {
      if (doc.uri.scheme === DIFF_SCHEME) {
        diffManager.cancelDiff(doc.uri);
      }
    })
  );

  // Register diff commands
  context.subscriptions.push(
    vscode.commands.registerCommand('autodev.acceptDiff', (uri?: vscode.Uri) => {
      const docUri = uri ?? vscode.window.activeTextEditor?.document.uri;
      if (docUri && docUri.scheme === DIFF_SCHEME) {
        diffManager.acceptDiff(docUri);
      }
    }),
    vscode.commands.registerCommand('autodev.cancelDiff', (uri?: vscode.Uri) => {
      const docUri = uri ?? vscode.window.activeTextEditor?.document.uri;
      if (docUri && docUri.scheme === DIFF_SCHEME) {
        diffManager.cancelDiff(docUri);
      }
    })
  );

  // Initialize Chat View Provider
  const chatViewProvider = new ChatViewProvider(context, log);
  context.subscriptions.push(
    vscode.window.registerWebviewViewProvider('autodev.chatView', chatViewProvider, {
      webviewOptions: { retainContextWhenHidden: true }
    })
  );

  // Register chat command
  context.subscriptions.push(
    vscode.commands.registerCommand('autodev.chat', () => {
      vscode.commands.executeCommand('autodev.chatView.focus');
    })
  );

  // Register run agent command
  context.subscriptions.push(
    vscode.commands.registerCommand('autodev.runAgent', async () => {
      const workspaceFolders = vscode.workspace.workspaceFolders;
      if (!workspaceFolders || workspaceFolders.length === 0) {
        vscode.window.showWarningMessage('Please open a folder to run AutoDev Agent.');
        return;
      }

      const input = await vscode.window.showInputBox({
        prompt: 'Enter your coding task',
        placeHolder: 'e.g., Add a new API endpoint for user authentication'
      });

      if (input) {
        chatViewProvider.sendMessage(input);
      }
    })
  );

  // Initialize IDE Server (MCP protocol)
  const config = vscode.workspace.getConfiguration('autodev');
  const serverPort = config.get<number>('ideServer.port', 23120);

  ideServer = new IDEServer(log, diffManager, serverPort);
  try {
    await ideServer.start(context);
    log(`IDE Server started on port ${serverPort}`);
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    log(`Failed to start IDE server: ${message}`);
  }

  // Handle workspace folder changes
  context.subscriptions.push(
    vscode.workspace.onDidChangeWorkspaceFolders(() => {
      ideServer?.syncEnvVars();
    })
  );

  // Register DevIns language completion provider
  context.subscriptions.push(registerDevInsCompletionProvider(context));
  log('DevIns language support registered');

  // Show welcome message on first install
  const welcomeShownKey = 'autodev.welcomeShown';
  if (!context.globalState.get(welcomeShownKey)) {
    vscode.window.showInformationMessage(
      'AutoDev extension installed successfully! Press Cmd+Shift+A to open chat.'
    );
    context.globalState.update(welcomeShownKey, true);
  }

  log('AutoDev extension initialization complete');
}

/**
 * Extension deactivation
 */
export async function deactivate(): Promise<void> {
  log('AutoDev extension deactivating');
  
  try {
    if (ideServer) {
      await ideServer.stop();
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    log(`Failed to stop IDE server: ${message}`);
  } finally {
    if (logger) {
      logger.dispose();
    }
  }
}

