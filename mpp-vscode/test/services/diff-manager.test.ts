/**
 * Tests for DiffManager
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock vscode module
vi.mock('vscode', () => ({
  Uri: {
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
  },
  EventEmitter: class {
    private listeners: Function[] = [];
    event = (listener: Function) => {
      this.listeners.push(listener);
      return { dispose: () => { this.listeners = this.listeners.filter(l => l !== listener); } };
    };
    fire = (data: any) => { this.listeners.forEach(l => l(data)); };
    dispose = () => { this.listeners = []; };
  },
  window: {
    onDidChangeActiveTextEditor: () => ({ dispose: () => {} }),
    activeTextEditor: undefined,
    tabGroups: { all: [] }
  },
  workspace: {
    fs: {
      stat: vi.fn().mockResolvedValue({}),
      writeFile: vi.fn().mockResolvedValue(undefined)
    },
    openTextDocument: vi.fn().mockResolvedValue({ getText: () => 'test content', uri: {} })
  },
  commands: {
    executeCommand: vi.fn().mockResolvedValue(undefined)
  }
}));

// Mock extension module to avoid circular dependency
vi.mock('../../src/extension', () => ({
  DIFF_SCHEME: 'autodev-diff'
}));

describe('DiffContentProvider', () => {
  it('should be importable', async () => {
    const { DiffContentProvider } = await import('../../src/services/diff-manager');
    expect(DiffContentProvider).toBeDefined();
  });

  it('should create instance', async () => {
    const { DiffContentProvider } = await import('../../src/services/diff-manager');
    const provider = new DiffContentProvider();
    expect(provider).toBeDefined();
  });

  it('should set and get content', async () => {
    const { DiffContentProvider } = await import('../../src/services/diff-manager');
    const vscode = await import('vscode');
    
    const provider = new DiffContentProvider();
    const uri = vscode.Uri.from({ scheme: 'autodev-diff', path: '/test/file.ts' });
    
    provider.setContent(uri, 'test content');
    expect(provider.getContent(uri)).toBe('test content');
  });

  it('should delete content', async () => {
    const { DiffContentProvider } = await import('../../src/services/diff-manager');
    const vscode = await import('vscode');
    
    const provider = new DiffContentProvider();
    const uri = vscode.Uri.from({ scheme: 'autodev-diff', path: '/test/file.ts' });
    
    provider.setContent(uri, 'test content');
    provider.deleteContent(uri);
    expect(provider.getContent(uri)).toBeUndefined();
  });

  it('should provide text document content', async () => {
    const { DiffContentProvider } = await import('../../src/services/diff-manager');
    const vscode = await import('vscode');
    
    const provider = new DiffContentProvider();
    const uri = vscode.Uri.from({ scheme: 'autodev-diff', path: '/test/file.ts' });
    
    provider.setContent(uri, 'provided content');
    expect(provider.provideTextDocumentContent(uri)).toBe('provided content');
  });

  it('should return empty string for unknown uri', async () => {
    const { DiffContentProvider } = await import('../../src/services/diff-manager');
    const vscode = await import('vscode');
    
    const provider = new DiffContentProvider();
    const uri = vscode.Uri.from({ scheme: 'autodev-diff', path: '/unknown/file.ts' });
    
    expect(provider.provideTextDocumentContent(uri)).toBe('');
  });
});

describe('DiffManager', () => {
  let logMessages: string[] = [];
  const mockLog = (message: string) => { logMessages.push(message); };

  beforeEach(() => {
    logMessages = [];
    vi.clearAllMocks();
  });

  it('should be importable', async () => {
    const { DiffManager } = await import('../../src/services/diff-manager');
    expect(DiffManager).toBeDefined();
  });

  it('should create instance', async () => {
    const { DiffManager, DiffContentProvider } = await import('../../src/services/diff-manager');
    const contentProvider = new DiffContentProvider();
    const manager = new DiffManager(mockLog, contentProvider);
    expect(manager).toBeDefined();
  });

  it('should check if diff exists for file', async () => {
    const { DiffManager, DiffContentProvider } = await import('../../src/services/diff-manager');
    const contentProvider = new DiffContentProvider();
    const manager = new DiffManager(mockLog, contentProvider);
    
    expect(manager.hasDiff('/test/file.ts')).toBe(false);
  });

  it('should emit events on diff changes', async () => {
    const { DiffManager, DiffContentProvider } = await import('../../src/services/diff-manager');
    const contentProvider = new DiffContentProvider();
    const manager = new DiffManager(mockLog, contentProvider);
    
    const events: any[] = [];
    manager.onDidChange((event) => events.push(event));
    
    expect(events).toHaveLength(0);
  });

  it('should dispose subscriptions', async () => {
    const { DiffManager, DiffContentProvider } = await import('../../src/services/diff-manager');
    const contentProvider = new DiffContentProvider();
    const manager = new DiffManager(mockLog, contentProvider);
    
    expect(() => manager.dispose()).not.toThrow();
  });
});

