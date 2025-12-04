/**
 * Auto Actions - AutoComment, AutoTest, AutoMethod implementations
 * 
 * Based on autodev-vscode's action executors, adapted for mpp-vscode.
 */

import * as vscode from 'vscode';
import { CodeElement } from '../providers/code-element-parser';
import { LLMService, ModelConfig } from '../bridge/mpp-core';
import { DiffManager } from '../services/diff-manager';
import {
  generateAutoDocPrompt,
  generateAutoTestPrompt,
  generateAutoMethodPrompt,
  parseCodeBlock,
  LANGUAGE_COMMENT_MAP,
  getTestFramework,
  getTestFilePath,
  AutoDocContext,
  AutoTestContext,
  AutoMethodContext
} from '../prompts/prompt-templates';

export interface ActionContext {
  document: vscode.TextDocument;
  element: CodeElement;
  config: ModelConfig;
  log: (message: string) => void;
}

/**
 * Execute AutoComment action - generates documentation comments
 */
export async function executeAutoComment(context: ActionContext): Promise<void> {
  const { document, element, config, log } = context;
  const language = document.languageId;
  
  log(`AutoComment: Generating documentation for ${element.name}`);
  
  const commentSymbols = LANGUAGE_COMMENT_MAP[language] || { start: '/**', end: '*/' };
  
  const promptContext: AutoDocContext = {
    language,
    code: element.code,
    startSymbol: commentSymbols.start,
    endSymbol: commentSymbols.end,
  };
  
  const prompt = generateAutoDocPrompt(promptContext);
  
  try {
    await vscode.window.withProgress({
      location: vscode.ProgressLocation.Notification,
      title: `Generating documentation for ${element.name}...`,
      cancellable: true
    }, async (progress, token) => {
      const llmService = new LLMService(config);
      
      let response = '';
      await llmService.streamMessage(prompt, (chunk) => {
        response += chunk;
        progress.report({ message: 'Generating...' });
      });
      
      if (token.isCancellationRequested) return;
      
      const docComment = parseCodeBlock(response, language);
      if (!docComment) {
        vscode.window.showWarningMessage('Failed to generate documentation');
        return;
      }
      
      const formattedDoc = formatDocComment(docComment, document, element);
      const insertPosition = new vscode.Position(element.bodyRange.start.line, 0);
      
      const diffManager = new DiffManager(log);
      const originalContent = document.getText();
      const newContent = insertTextAtPosition(originalContent, formattedDoc, document.offsetAt(insertPosition));
      
      await diffManager.showDiff(document.uri.fsPath, originalContent, newContent);
      log(`AutoComment: Documentation generated for ${element.name}`);
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    log(`AutoComment error: ${message}`);
    vscode.window.showErrorMessage(`Failed to generate documentation: ${message}`);
  }
}

/**
 * Execute AutoTest action - generates unit tests
 */
export async function executeAutoTest(context: ActionContext): Promise<void> {
  const { document, element, config, log } = context;
  const language = document.languageId;
  
  log(`AutoTest: Generating tests for ${element.name}`);
  
  const promptContext: AutoTestContext = {
    language,
    sourceCode: element.code,
    className: element.type === 'structure' ? element.name : undefined,
    methodName: element.type === 'method' ? element.name : undefined,
    testFramework: getTestFramework(language),
    isNewFile: true,
  };
  
  const prompt = generateAutoTestPrompt(promptContext);
  
  try {
    await vscode.window.withProgress({
      location: vscode.ProgressLocation.Notification,
      title: `Generating tests for ${element.name}...`,
      cancellable: true
    }, async (progress, token) => {
      const llmService = new LLMService(config);
      
      let response = '';
      await llmService.streamMessage(prompt, (chunk) => {
        response += chunk;
        progress.report({ message: 'Generating...' });
      });
      
      if (token.isCancellationRequested) return;
      
      const testCode = parseCodeBlock(response, language);
      if (!testCode) {
        vscode.window.showWarningMessage('Failed to generate test code');
        return;
      }
      
      const testFilePath = getTestFilePath(document.uri.fsPath, language);
      const testFileUri = vscode.Uri.file(testFilePath);
      
      let existingContent = '';
      try {
        const existingDoc = await vscode.workspace.openTextDocument(testFileUri);
        existingContent = existingDoc.getText();
      } catch { /* File doesn't exist */ }
      
      const diffManager = new DiffManager(log);
      if (existingContent) {
        const newContent = existingContent + '\n\n' + testCode;
        await diffManager.showDiff(testFilePath, existingContent, newContent);
      } else {
        await diffManager.showDiff(testFilePath, '', testCode);
      }
      
      log(`AutoTest: Tests generated for ${element.name}`);
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    log(`AutoTest error: ${message}`);
    vscode.window.showErrorMessage(`Failed to generate tests: ${message}`);
  }
}

/**
 * Execute AutoMethod action - generates method implementation
 */
export async function executeAutoMethod(context: ActionContext): Promise<void> {
  const { document, element, config, log } = context;
  const language = document.languageId;
  
  log(`AutoMethod: Generating implementation for ${element.name}`);
  
  const promptContext: AutoMethodContext = {
    language,
    code: element.code,
    methodSignature: extractMethodSignature(element.code),
    className: findContainingClass(document, element),
  };
  
  const prompt = generateAutoMethodPrompt(promptContext);
  
  try {
    await vscode.window.withProgress({
      location: vscode.ProgressLocation.Notification,
      title: `Generating implementation for ${element.name}...`,
      cancellable: true
    }, async (progress, token) => {
      const llmService = new LLMService(config);
      
      let response = '';
      await llmService.streamMessage(prompt, (chunk) => {
        response += chunk;
        progress.report({ message: 'Generating...' });
      });
      
      if (token.isCancellationRequested) return;
      
      const methodCode = parseCodeBlock(response, language);
      if (!methodCode) {
        vscode.window.showWarningMessage('Failed to generate method implementation');
        return;
      }
      
      const diffManager = new DiffManager(log);
      const originalContent = document.getText();
      const newContent = replaceMethodBody(originalContent, element, methodCode, document);
      
      await diffManager.showDiff(document.uri.fsPath, originalContent, newContent);
      log(`AutoMethod: Implementation generated for ${element.name}`);
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    log(`AutoMethod error: ${message}`);
    vscode.window.showErrorMessage(`Failed to generate implementation: ${message}`);
  }
}

// Helper functions

function formatDocComment(docComment: string, document: vscode.TextDocument, element: CodeElement): string {
  const line = document.lineAt(element.bodyRange.start.line);
  const indent = line.text.substring(0, line.firstNonWhitespaceCharacterIndex);
  
  let formatted = docComment.trim();
  if (!formatted.endsWith('\n')) formatted += '\n';
  
  const lines = formatted.split('\n');
  return lines.map(l => l ? indent + l : l).join('\n');
}

function insertTextAtPosition(content: string, text: string, position: number): string {
  return content.substring(0, position) + text + content.substring(position);
}

function extractMethodSignature(code: string): string {
  const lines = code.split('\n');
  const signatureLines: string[] = [];
  for (const line of lines) {
    signatureLines.push(line);
    if (line.includes('{') || line.includes(':')) break;
  }
  return signatureLines.join('\n').trim();
}

function findContainingClass(document: vscode.TextDocument, element: CodeElement): string | undefined {
  const text = document.getText(new vscode.Range(new vscode.Position(0, 0), element.bodyRange.start));
  const classMatch = text.match(/class\s+(\w+)/g);
  if (classMatch && classMatch.length > 0) {
    const lastMatch = classMatch[classMatch.length - 1];
    const nameMatch = lastMatch.match(/class\s+(\w+)/);
    return nameMatch ? nameMatch[1] : undefined;
  }
  return undefined;
}

function replaceMethodBody(content: string, element: CodeElement, newMethodCode: string, document: vscode.TextDocument): string {
  const startOffset = document.offsetAt(element.bodyRange.start);
  const endOffset = document.offsetAt(element.bodyRange.end);
  return content.substring(0, startOffset) + newMethodCode + content.substring(endOffset);
}
