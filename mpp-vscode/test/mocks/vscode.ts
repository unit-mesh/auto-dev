/**
 * VSCode API Mock for testing
 */

export const Uri = {
  file: (path: string) => ({ scheme: 'file', fsPath: path, path, toString: () => `file://${path}` }),
  from: (components: { scheme: string; path: string; query?: string }) => ({
    scheme: components.scheme,
    path: components.path,
    query: components.query,
    fsPath: components.path,
    toString: () => `${components.scheme}://${components.path}${components.query ? '?' + components.query : ''}`
  }),
  parse: (value: string) => {
    const url = new URL(value);
    return { scheme: url.protocol.replace(':', ''), path: url.pathname, fsPath: url.pathname, toString: () => value };
  }
};

export const EventEmitter = class {
  private listeners: Function[] = [];
  event = (listener: Function) => {
    this.listeners.push(listener);
    return { dispose: () => { this.listeners = this.listeners.filter(l => l !== listener); } };
  };
  fire = (data: any) => { this.listeners.forEach(l => l(data)); };
  dispose = () => { this.listeners = []; };
};

export const window = {
  createOutputChannel: (name: string) => ({
    appendLine: (message: string) => console.log(`[${name}] ${message}`),
    dispose: () => {}
  }),
  showInformationMessage: async (message: string) => console.log(`[INFO] ${message}`),
  showWarningMessage: async (message: string) => console.log(`[WARN] ${message}`),
  showErrorMessage: async (message: string) => console.log(`[ERROR] ${message}`),
  showInputBox: async () => undefined,
  activeTextEditor: undefined,
  onDidChangeActiveTextEditor: () => ({ dispose: () => {} }),
  registerWebviewViewProvider: () => ({ dispose: () => {} }),
  tabGroups: { all: [] }
};

export const workspace = {
  workspaceFolders: undefined,
  getConfiguration: (section: string) => ({
    get: <T>(key: string, defaultValue?: T) => defaultValue
  }),
  fs: {
    stat: async (uri: any) => ({}),
    readFile: async (uri: any) => Buffer.from(''),
    writeFile: async (uri: any, content: Uint8Array) => {}
  },
  onDidCloseTextDocument: () => ({ dispose: () => {} }),
  onDidChangeWorkspaceFolders: () => ({ dispose: () => {} }),
  registerTextDocumentContentProvider: () => ({ dispose: () => {} }),
  openTextDocument: async (uri: any) => ({ getText: () => '', uri })
};

export const commands = {
  registerCommand: (command: string, callback: Function) => ({ dispose: () => {} }),
  executeCommand: async (command: string, ...args: any[]) => {}
};

export const ExtensionMode = {
  Development: 1,
  Test: 2,
  Production: 3
};

export default {
  Uri,
  EventEmitter,
  window,
  workspace,
  commands,
  ExtensionMode
};

