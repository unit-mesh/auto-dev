/**
 * Tests for DevIns Completion Provider
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock vscode module
vi.mock('vscode', () => ({
  CompletionItem: vi.fn().mockImplementation((label, kind) => ({
    label,
    kind,
    detail: '',
    insertText: '',
    documentation: null
  })),
  CompletionItemKind: {
    Function: 3,
    Class: 7,
    Variable: 6
  },
  MarkdownString: vi.fn().mockImplementation((value) => ({ value })),
  languages: {
    registerCompletionItemProvider: vi.fn().mockReturnValue({ dispose: () => {} })
  }
}));

// Mock mpp-core
vi.mock('../../src/bridge/mpp-core', () => ({
  CompletionManager: vi.fn().mockImplementation(() => ({
    initWorkspace: vi.fn().mockResolvedValue(true),
    getCompletions: vi.fn().mockReturnValue([])
  }))
}));

describe('DevInsCompletionProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should be importable', async () => {
    const { DevInsCompletionProvider } = await import('../../src/providers/devins-completion');
    expect(DevInsCompletionProvider).toBeDefined();
  });

  it('should create instance', async () => {
    const { DevInsCompletionProvider } = await import('../../src/providers/devins-completion');
    const provider = new DevInsCompletionProvider();
    expect(provider).toBeDefined();
  });

  it('should have provideCompletionItems method', async () => {
    const { DevInsCompletionProvider } = await import('../../src/providers/devins-completion');
    const provider = new DevInsCompletionProvider();
    expect(typeof provider.provideCompletionItems).toBe('function');
  });

  it('should provide command completions for /', async () => {
    const { DevInsCompletionProvider } = await import('../../src/providers/devins-completion');
    const provider = new DevInsCompletionProvider();
    
    const mockDocument = {
      lineAt: vi.fn().mockReturnValue({ text: '/' })
    };
    const mockPosition = { character: 1 };
    const mockToken = { isCancellationRequested: false };
    const mockContext = {};

    const items = await provider.provideCompletionItems(
      mockDocument as any,
      mockPosition as any,
      mockToken as any,
      mockContext as any
    );

    expect(Array.isArray(items)).toBe(true);
    expect(items.length).toBeGreaterThan(0);
  });

  it('should provide agent completions for @', async () => {
    const { DevInsCompletionProvider } = await import('../../src/providers/devins-completion');
    const provider = new DevInsCompletionProvider();
    
    const mockDocument = {
      lineAt: vi.fn().mockReturnValue({ text: '@' })
    };
    const mockPosition = { character: 1 };
    const mockToken = { isCancellationRequested: false };
    const mockContext = {};

    const items = await provider.provideCompletionItems(
      mockDocument as any,
      mockPosition as any,
      mockToken as any,
      mockContext as any
    );

    expect(Array.isArray(items)).toBe(true);
    expect(items.length).toBeGreaterThan(0);
  });

  it('should provide variable completions for $', async () => {
    const { DevInsCompletionProvider } = await import('../../src/providers/devins-completion');
    const provider = new DevInsCompletionProvider();
    
    const mockDocument = {
      lineAt: vi.fn().mockReturnValue({ text: '$' })
    };
    const mockPosition = { character: 1 };
    const mockToken = { isCancellationRequested: false };
    const mockContext = {};

    const items = await provider.provideCompletionItems(
      mockDocument as any,
      mockPosition as any,
      mockToken as any,
      mockContext as any
    );

    expect(Array.isArray(items)).toBe(true);
    expect(items.length).toBeGreaterThan(0);
  });

  it('should return empty array for regular text', async () => {
    const { DevInsCompletionProvider } = await import('../../src/providers/devins-completion');
    const provider = new DevInsCompletionProvider();
    
    const mockDocument = {
      lineAt: vi.fn().mockReturnValue({ text: 'hello world' })
    };
    const mockPosition = { character: 11 };
    const mockToken = { isCancellationRequested: false };
    const mockContext = {};

    const items = await provider.provideCompletionItems(
      mockDocument as any,
      mockPosition as any,
      mockToken as any,
      mockContext as any
    );

    expect(Array.isArray(items)).toBe(true);
    expect(items.length).toBe(0);
  });
});

describe('registerDevInsCompletionProvider', () => {
  it('should be importable', async () => {
    const { registerDevInsCompletionProvider } = await import('../../src/providers/devins-completion');
    expect(registerDevInsCompletionProvider).toBeDefined();
  });

  it('should return disposable', async () => {
    const { registerDevInsCompletionProvider } = await import('../../src/providers/devins-completion');
    const mockContext = { subscriptions: [] };
    const disposable = registerDevInsCompletionProvider(mockContext as any);
    expect(disposable).toBeDefined();
    expect(typeof disposable.dispose).toBe('function');
  });
});

