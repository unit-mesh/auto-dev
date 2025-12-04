/**
 * Code Element Parser using Tree-sitter
 *
 * Based on autodev-vscode's NamedElementBuilder implementation.
 * Uses web-tree-sitter for accurate AST-based code parsing.
 */

import * as vscode from 'vscode';
import type { Language, Query, SyntaxNode, Tree } from 'web-tree-sitter';

// Dynamic import for web-tree-sitter (CommonJS module)
let Parser: any = null;
async function getParser(): Promise<any> {
  if (!Parser) {
    Parser = require('web-tree-sitter');
  }
  return Parser;
}

export enum CodeElementType {
  Structure = 'structure',
  Method = 'method',
  Function = 'function'
}

export interface CodeElement {
  type: CodeElementType;
  name: string;
  nameRange: vscode.Range;
  bodyRange: vscode.Range;
  code: string;
}

interface TextInRange {
  text: string;
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
}

/**
 * Memoized query for caching compiled tree-sitter queries
 */
class MemoizedQuery {
  private readonly queryStr: string;
  private compiledQuery: Query | undefined;

  constructor(queryStr: string) {
    this.queryStr = queryStr;
  }

  query(language: Language): Query {
    if (this.compiledQuery) {
      return this.compiledQuery;
    }
    this.compiledQuery = language.query(this.queryStr);
    return this.compiledQuery;
  }
}

/**
 * Language profile for tree-sitter parsing
 */
interface LanguageProfile {
  languageIds: string[];
  classQuery: MemoizedQuery;
  methodQuery: MemoizedQuery;
  autoSelectInsideParent: string[];
}

// Language profiles based on autodev-vscode
const LANGUAGE_PROFILES: Record<string, LanguageProfile> = {
  typescript: {
    languageIds: ['typescript', 'typescriptreact'],
    classQuery: new MemoizedQuery(`
      (class_declaration
        (type_identifier) @name.definition.class) @definition.class
    `),
    methodQuery: new MemoizedQuery(`
      (function_declaration
        (identifier) @name.definition.method) @definition.method

      (generator_function_declaration
        name: (identifier) @name.identifier.method
      ) @definition.method

      (export_statement
        declaration: (lexical_declaration
          (variable_declarator
            name: (identifier) @name.identifier.method
            value: (arrow_function)
          )
        ) @definition.method
      )

      (class_declaration
        name: (type_identifier)
        body: (class_body
          ((method_definition
            name: (property_identifier) @name.definition.method
          ) @definition.method)
        )
      )
    `),
    autoSelectInsideParent: ['export_statement']
  },
  javascript: {
    languageIds: ['javascript', 'javascriptreact'],
    classQuery: new MemoizedQuery(`
      (class_declaration
        (identifier) @name.definition.class) @definition.class
    `),
    methodQuery: new MemoizedQuery(`
      (function_declaration
        (identifier) @name.definition.method) @definition.method

      (generator_function_declaration
        name: (identifier) @name.identifier.method
      ) @definition.method

      (export_statement
        declaration: (lexical_declaration
          (variable_declarator
            name: (identifier) @name.identifier.method
            value: (arrow_function)
          )
        ) @definition.method
      )

      (class_declaration
        name: (identifier)
        body: (class_body
          ((method_definition
            name: (property_identifier) @name.definition.method
          ) @definition.method)
        )
      )
    `),
    autoSelectInsideParent: ['export_statement']
  },
  python: {
    languageIds: ['python'],
    classQuery: new MemoizedQuery(`
      (class_definition
        (identifier) @type_identifier) @type_declaration
    `),
    methodQuery: new MemoizedQuery(`
      (function_definition
        name: (identifier) @name.definition.method
      ) @definition.method
    `),
    autoSelectInsideParent: []
  },
  java: {
    languageIds: ['java'],
    classQuery: new MemoizedQuery(`
      (class_declaration
        name: (identifier) @name.definition.class) @definition.class
    `),
    methodQuery: new MemoizedQuery(`
      (method_declaration
        name: (identifier) @name.definition.method) @definition.method
    `),
    autoSelectInsideParent: []
  },
  kotlin: {
    languageIds: ['kotlin'],
    classQuery: new MemoizedQuery(`
      (class_declaration
        (type_identifier) @name.definition.class) @definition.class
    `),
    methodQuery: new MemoizedQuery(`
      (function_declaration
        (simple_identifier) @name.definition.method) @definition.method
    `),
    autoSelectInsideParent: []
  },
  go: {
    languageIds: ['go'],
    classQuery: new MemoizedQuery(`
      (type_declaration
        (type_spec
          name: (_) @type-name
          type: (struct_type)
        )
      ) @type_declaration
    `),
    methodQuery: new MemoizedQuery(`
      (function_declaration
        name: (identifier) @function-name) @function-body

      (method_declaration
        receiver: (_)? @receiver-struct-name
        name: (_)? @method-name) @method-body
    `),
    autoSelectInsideParent: []
  },
  rust: {
    languageIds: ['rust'],
    classQuery: new MemoizedQuery(`
      (struct_item (type_identifier) @type_identifier) @type_declaration
    `),
    methodQuery: new MemoizedQuery(`
      (function_item (identifier) @name.definition.method) @definition.method
    `),
    autoSelectInsideParent: []
  }
};

export class CodeElementParser {
  private static parserInstance: any = null;
  private static ParserClass: any = null;
  private static languages: Map<string, Language> = new Map();
  private static initPromise: Promise<void> | null = null;
  private static extensionPath: string | undefined;

  constructor(
    private log: (message: string) => void,
    extensionPath?: string
  ) {
    if (extensionPath && !CodeElementParser.extensionPath) {
      CodeElementParser.extensionPath = extensionPath;
    }
  }

  /**
   * Initialize tree-sitter parser
   */
  private async initialize(): Promise<void> {
    if (CodeElementParser.parserInstance) {
      return;
    }

    if (CodeElementParser.initPromise) {
      return CodeElementParser.initPromise;
    }

    CodeElementParser.initPromise = this.doInitialize();
    return CodeElementParser.initPromise;
  }

  private async doInitialize(): Promise<void> {
    try {
      CodeElementParser.ParserClass = await getParser();
      await CodeElementParser.ParserClass.init();
      CodeElementParser.parserInstance = new CodeElementParser.ParserClass();
      this.log('Tree-sitter parser initialized');
    } catch (error) {
      this.log(`Failed to initialize tree-sitter: ${error}`);
      throw error;
    }
  }

  /**
   * Get or load language grammar
   */
  private async getLanguage(langId: string): Promise<Language | null> {
    const cached = CodeElementParser.languages.get(langId);
    if (cached) {
      return cached;
    }

    try {
      // Try to load from extension's bundled WASM files
      const wasmPath = this.getWasmPath(langId);
      if (!wasmPath) {
        return null;
      }

      const language = await CodeElementParser.ParserClass.Language.load(wasmPath);
      CodeElementParser.languages.set(langId, language);
      return language;
    } catch (error) {
      this.log(`Failed to load language ${langId}: ${error}`);
      return null;
    }
  }

  /**
   * Get WASM file path for language
   */
  private getWasmPath(langId: string): string | null {
    // Map language IDs to tree-sitter grammar names
    const grammarMap: Record<string, string> = {
      'typescript': 'tree-sitter-typescript',
      'typescriptreact': 'tree-sitter-tsx',
      'javascript': 'tree-sitter-javascript',
      'javascriptreact': 'tree-sitter-javascript',
      'python': 'tree-sitter-python',
      'java': 'tree-sitter-java',
      'kotlin': 'tree-sitter-kotlin',
      'go': 'tree-sitter-go',
      'rust': 'tree-sitter-rust'
    };

    const grammarName = grammarMap[langId];
    if (!grammarName) {
      return null;
    }

    // Try multiple path resolution strategies
    // 1. Try extension's dist/wasm folder (for packaged extension)
    if (CodeElementParser.extensionPath) {
      const path = require('path');
      const wasmPath = path.join(CodeElementParser.extensionPath, 'dist', 'wasm', `${grammarName}.wasm`);
      const fs = require('fs');
      if (fs.existsSync(wasmPath)) {
        this.log(`Using WASM from extension: ${wasmPath}`);
        return wasmPath;
      }
    }

    // 2. Try node_modules (for development)
    try {
      const wasmPath = require.resolve(`@unit-mesh/treesitter-artifacts/wasm/${grammarName}.wasm`);
      this.log(`Using WASM from node_modules: ${wasmPath}`);
      return wasmPath;
    } catch (error) {
      this.log(`WASM file not found for ${langId}: ${error}`);
      return null;
    }
  }

  /**
   * Get language profile for a language ID
   */
  private getProfile(langId: string): LanguageProfile | null {
    // Check direct match
    if (LANGUAGE_PROFILES[langId]) {
      return LANGUAGE_PROFILES[langId];
    }

    // Check if any profile includes this language ID
    for (const profile of Object.values(LANGUAGE_PROFILES)) {
      if (profile.languageIds.includes(langId)) {
        return profile;
      }
    }

    return null;
  }

  /**
   * Parse document and extract code elements
   */
  async parseDocument(document: vscode.TextDocument): Promise<CodeElement[]> {
    const langId = document.languageId;
    const profile = this.getProfile(langId);

    if (!profile) {
      this.log(`Language ${langId} not supported for CodeLens`);
      return [];
    }

    try {
      await this.initialize();

      const language = await this.getLanguage(langId);
      if (!language || !CodeElementParser.parserInstance) {
        // Fallback to regex-based parsing
        this.log(`Tree-sitter not available for ${langId}, using regex fallback`);
        return this.parseWithRegex(document);
      }

      CodeElementParser.parserInstance.setLanguage(language);
      const tree = CodeElementParser.parserInstance.parse(document.getText());
      const elements: CodeElement[] = [];

      // Parse classes/structures
      const classElements = this.buildBlock(
        tree.rootNode,
        profile.classQuery,
        language,
        CodeElementType.Structure,
        profile.autoSelectInsideParent,
        document
      );
      elements.push(...classElements);

      // Parse methods/functions
      const methodElements = this.buildBlock(
        tree.rootNode,
        profile.methodQuery,
        language,
        CodeElementType.Method,
        profile.autoSelectInsideParent,
        document
      );
      elements.push(...methodElements);

      return elements;
    } catch (error) {
      this.log(`Error parsing document: ${error}`);
      return this.parseWithRegex(document);
    }
  }

  /**
   * Build code elements from tree-sitter query matches
   */
  private buildBlock(
    rootNode: SyntaxNode,
    memoizedQuery: MemoizedQuery,
    language: Language,
    elementType: CodeElementType,
    autoSelectInsideParent: string[],
    document: vscode.TextDocument
  ): CodeElement[] {
    try {
      const query = memoizedQuery.query(language);
      const matches = query.matches(rootNode);

      return matches.flatMap(match => {
        let blockNode = match.captures[0]?.node;
        const idNode = match.captures[1]?.node;

        if (!blockNode || !idNode) {
          return [];
        }

        // Handle autoSelectInsideParent
        if (autoSelectInsideParent.length > 0) {
          for (const nodeType of autoSelectInsideParent) {
            if (blockNode.parent?.type === nodeType) {
              blockNode = blockNode.parent;
            }
          }
        }

        const nameRange = new vscode.Range(
          new vscode.Position(idNode.startPosition.row, idNode.startPosition.column),
          new vscode.Position(idNode.endPosition.row, idNode.endPosition.column)
        );

        const bodyRange = new vscode.Range(
          new vscode.Position(blockNode.startPosition.row, blockNode.startPosition.column),
          new vscode.Position(blockNode.endPosition.row, blockNode.endPosition.column)
        );

        return [{
          type: elementType,
          name: idNode.text,
          nameRange,
          bodyRange,
          code: blockNode.text
        }];
      });
    } catch (error) {
      this.log(`Error building block: ${error}`);
      return [];
    }
  }

  /**
   * Fallback regex-based parsing for when tree-sitter is not available
   */
  private parseWithRegex(document: vscode.TextDocument): CodeElement[] {
    const language = document.languageId;
    const text = document.getText();

    switch (language) {
      case 'typescript':
      case 'javascript':
      case 'typescriptreact':
      case 'javascriptreact':
        return this.parseTypeScriptRegex(text, document);
      case 'python':
        return this.parsePythonRegex(text, document);
      case 'java':
      case 'kotlin':
        return this.parseJavaLikeRegex(text, document);
      case 'go':
        return this.parseGoRegex(text, document);
      case 'rust':
        return this.parseRustRegex(text, document);
      default:
        return [];
    }
  }

  private parseTypeScriptRegex(text: string, document: vscode.TextDocument): CodeElement[] {
    const elements: CodeElement[] = [];
    const classRegex = /^\s*(export\s+)?(abstract\s+)?class\s+(\w+)/gm;
    let match;
    while ((match = classRegex.exec(text)) !== null) {
      const name = match[3];
      const startPos = document.positionAt(match.index);
      const nameStartPos = document.positionAt(match.index + match[0].lastIndexOf(name));
      const bodyRange = this.findBlockRange(document, startPos);
      elements.push({
        type: CodeElementType.Structure,
        name,
        nameRange: new vscode.Range(nameStartPos, nameStartPos.translate(0, name.length)),
        bodyRange,
        code: document.getText(bodyRange)
      });
    }

    const functionRegex = /^\s*(export\s+)?(async\s+)?(?:function\s+(\w+)|(?:const|let|var)\s+(\w+)\s*=\s*(?:async\s+)?\([^)]*\)\s*=>)/gm;
    while ((match = functionRegex.exec(text)) !== null) {
      const name = match[3] || match[4];
      if (!name) continue;
      const startPos = document.positionAt(match.index);
      const nameStartPos = document.positionAt(match.index + match[0].lastIndexOf(name));
      const bodyRange = this.findBlockRange(document, startPos);
      elements.push({
        type: CodeElementType.Method,
        name,
        nameRange: new vscode.Range(nameStartPos, nameStartPos.translate(0, name.length)),
        bodyRange,
        code: document.getText(bodyRange)
      });
    }
    return elements;
  }

  private parsePythonRegex(text: string, document: vscode.TextDocument): CodeElement[] {
    const elements: CodeElement[] = [];
    const classRegex = /^class\s+(\w+)/gm;
    let match;
    while ((match = classRegex.exec(text)) !== null) {
      const name = match[1];
      const startPos = document.positionAt(match.index);
      const nameStartPos = document.positionAt(match.index + match[0].indexOf(name));
      const bodyRange = this.findPythonBlockRange(document, startPos);
      elements.push({
        type: CodeElementType.Structure,
        name,
        nameRange: new vscode.Range(nameStartPos, nameStartPos.translate(0, name.length)),
        bodyRange,
        code: document.getText(bodyRange)
      });
    }

    const functionRegex = /^(\s*)def\s+(\w+)\s*\(/gm;
    while ((match = functionRegex.exec(text)) !== null) {
      const name = match[2];
      const startPos = document.positionAt(match.index);
      const nameStartPos = document.positionAt(match.index + match[0].indexOf(name));
      const bodyRange = this.findPythonBlockRange(document, startPos);
      elements.push({
        type: CodeElementType.Method,
        name,
        nameRange: new vscode.Range(nameStartPos, nameStartPos.translate(0, name.length)),
        bodyRange,
        code: document.getText(bodyRange)
      });
    }
    return elements;
  }

  private parseJavaLikeRegex(text: string, document: vscode.TextDocument): CodeElement[] {
    const elements: CodeElement[] = [];
    const classRegex = /^\s*(public|private|protected)?\s*(abstract|final)?\s*(class|interface)\s+(\w+)/gm;
    let match;
    while ((match = classRegex.exec(text)) !== null) {
      const name = match[4];
      const startPos = document.positionAt(match.index);
      const nameStartPos = document.positionAt(match.index + match[0].lastIndexOf(name));
      const bodyRange = this.findBlockRange(document, startPos);
      elements.push({
        type: CodeElementType.Structure,
        name,
        nameRange: new vscode.Range(nameStartPos, nameStartPos.translate(0, name.length)),
        bodyRange,
        code: document.getText(bodyRange)
      });
    }

    const methodRegex = /^\s*(public|private|protected)?\s*(static|final|abstract)?\s*(\w+)\s+(\w+)\s*\([^)]*\)\s*{/gm;
    while ((match = methodRegex.exec(text)) !== null) {
      const name = match[4];
      const startPos = document.positionAt(match.index);
      const nameStartPos = document.positionAt(match.index + match[0].lastIndexOf(name));
      const bodyRange = this.findBlockRange(document, startPos);
      elements.push({
        type: CodeElementType.Method,
        name,
        nameRange: new vscode.Range(nameStartPos, nameStartPos.translate(0, name.length)),
        bodyRange,
        code: document.getText(bodyRange)
      });
    }
    return elements;
  }

  private parseGoRegex(text: string, document: vscode.TextDocument): CodeElement[] {
    const elements: CodeElement[] = [];
    const functionRegex = /^func\s+(?:\([^)]+\)\s+)?(\w+)\s*\(/gm;
    let match;
    while ((match = functionRegex.exec(text)) !== null) {
      const name = match[1];
      const startPos = document.positionAt(match.index);
      const nameStartPos = document.positionAt(match.index + match[0].lastIndexOf(name));
      const bodyRange = this.findBlockRange(document, startPos);
      elements.push({
        type: CodeElementType.Method,
        name,
        nameRange: new vscode.Range(nameStartPos, nameStartPos.translate(0, name.length)),
        bodyRange,
        code: document.getText(bodyRange)
      });
    }
    return elements;
  }

  private parseRustRegex(text: string, document: vscode.TextDocument): CodeElement[] {
    const elements: CodeElement[] = [];
    const functionRegex = /^\s*(pub\s+)?fn\s+(\w+)\s*\(/gm;
    let match;
    while ((match = functionRegex.exec(text)) !== null) {
      const name = match[2];
      const startPos = document.positionAt(match.index);
      const nameStartPos = document.positionAt(match.index + match[0].lastIndexOf(name));
      const bodyRange = this.findBlockRange(document, startPos);
      elements.push({
        type: CodeElementType.Method,
        name,
        nameRange: new vscode.Range(nameStartPos, nameStartPos.translate(0, name.length)),
        bodyRange,
        code: document.getText(bodyRange)
      });
    }
    return elements;
  }

  private findBlockRange(document: vscode.TextDocument, startPos: vscode.Position): vscode.Range {
    const text = document.getText();
    let offset = document.offsetAt(startPos);
    while (offset < text.length && text[offset] !== '{') {
      offset++;
    }
    if (offset >= text.length) {
      return new vscode.Range(startPos, startPos);
    }
    let depth = 1;
    offset++;
    while (offset < text.length && depth > 0) {
      if (text[offset] === '{') depth++;
      else if (text[offset] === '}') depth--;
      offset++;
    }
    return new vscode.Range(startPos, document.positionAt(offset));
  }

  private findPythonBlockRange(document: vscode.TextDocument, startPos: vscode.Position): vscode.Range {
    const startLine = startPos.line;
    const startIndent = document.lineAt(startLine).firstNonWhitespaceCharacterIndex;
    let endLine = startLine + 1;
    while (endLine < document.lineCount) {
      const line = document.lineAt(endLine);
      if (line.isEmptyOrWhitespace) {
        endLine++;
        continue;
      }
      const indent = line.firstNonWhitespaceCharacterIndex;
      if (indent <= startIndent) {
        break;
      }
      endLine++;
    }
    return new vscode.Range(startPos, new vscode.Position(endLine - 1, document.lineAt(endLine - 1).text.length));
  }
}


