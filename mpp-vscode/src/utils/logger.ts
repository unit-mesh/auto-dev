/**
 * Logger utility for AutoDev extension
 */

import * as vscode from 'vscode';

/**
 * Create a logger function that writes to both output channel and console
 */
export function createLogger(
  context: vscode.ExtensionContext,
  outputChannel: vscode.OutputChannel
): (message: string) => void {
  const isDevelopment = context.extensionMode === vscode.ExtensionMode.Development;

  return (message: string) => {
    const timestamp = new Date().toISOString();
    const formattedMessage = `[${timestamp}] ${message}`;
    
    outputChannel.appendLine(formattedMessage);
    
    if (isDevelopment) {
      console.log(`[AutoDev] ${formattedMessage}`);
    }
  };
}

