/**
 * Tests for ChatViewProvider
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock vscode module
vi.mock('vscode', () => ({
  Uri: {
    joinPath: vi.fn().mockReturnValue({ fsPath: '/mock/path' }),
    file: vi.fn().mockReturnValue({ fsPath: '/mock/path' })
  },
  window: {
    createOutputChannel: vi.fn().mockReturnValue({
      appendLine: vi.fn(),
      show: vi.fn()
    })
  },
  workspace: {
    getConfiguration: vi.fn().mockReturnValue({
      get: vi.fn().mockImplementation((key: string, defaultValue: unknown) => defaultValue)
    })
  }
}));

// Mock mpp-core
vi.mock('../../src/bridge/mpp-core', () => ({
  LLMService: vi.fn().mockImplementation(() => ({
    streamMessage: vi.fn().mockResolvedValue(undefined),
    clearHistory: vi.fn()
  })),
  ModelConfig: {}
}));

describe('ChatViewProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should be importable', async () => {
    const { ChatViewProvider } = await import('../../src/providers/chat-view');
    expect(ChatViewProvider).toBeDefined();
  });

  it('should create instance', async () => {
    const { ChatViewProvider } = await import('../../src/providers/chat-view');
    const mockContext = {
      extensionUri: { fsPath: '/mock/extension' },
      subscriptions: []
    };
    const mockLog = vi.fn();
    
    const provider = new ChatViewProvider(mockContext as any, mockLog);
    expect(provider).toBeDefined();
  });

  it('should have resolveWebviewView method', async () => {
    const { ChatViewProvider } = await import('../../src/providers/chat-view');
    const mockContext = {
      extensionUri: { fsPath: '/mock/extension' },
      subscriptions: []
    };
    const mockLog = vi.fn();
    
    const provider = new ChatViewProvider(mockContext as any, mockLog);
    expect(typeof provider.resolveWebviewView).toBe('function');
  });

  it('should have sendMessage method', async () => {
    const { ChatViewProvider } = await import('../../src/providers/chat-view');
    const mockContext = {
      extensionUri: { fsPath: '/mock/extension' },
      subscriptions: []
    };
    const mockLog = vi.fn();
    
    const provider = new ChatViewProvider(mockContext as any, mockLog);
    expect(typeof provider.sendMessage).toBe('function');
  });

  it('should have postMessage method', async () => {
    const { ChatViewProvider } = await import('../../src/providers/chat-view');
    const mockContext = {
      extensionUri: { fsPath: '/mock/extension' },
      subscriptions: []
    };
    const mockLog = vi.fn();
    
    const provider = new ChatViewProvider(mockContext as any, mockLog);
    expect(typeof provider.postMessage).toBe('function');
  });
});

describe('Webview HTML Generation', () => {
  it('should generate HTML with React bundle references', async () => {
    const { ChatViewProvider } = await import('../../src/providers/chat-view');
    const mockContext = {
      extensionUri: { fsPath: '/mock/extension' },
      subscriptions: []
    };
    const mockLog = vi.fn();
    
    const provider = new ChatViewProvider(mockContext as any, mockLog);
    
    // Access private method via any cast for testing
    const getHtmlContent = (provider as any).getHtmlContent.bind(provider);
    const mockWebview = {
      asWebviewUri: vi.fn().mockReturnValue('mock-uri'),
      cspSource: 'mock-csp'
    };
    
    const html = getHtmlContent(mockWebview);
    
    expect(html).toContain('<!DOCTYPE html>');
    expect(html).toContain('<div id="root"></div>');
    expect(html).toContain('Content-Security-Policy');
  });

  it('should include fallback inline HTML', async () => {
    const { ChatViewProvider } = await import('../../src/providers/chat-view');
    const mockContext = {
      extensionUri: { fsPath: '/mock/extension' },
      subscriptions: []
    };
    const mockLog = vi.fn();
    
    const provider = new ChatViewProvider(mockContext as any, mockLog);
    
    const getHtmlContent = (provider as any).getHtmlContent.bind(provider);
    const mockWebview = {
      asWebviewUri: vi.fn().mockReturnValue('mock-uri'),
      cspSource: 'mock-csp'
    };
    
    const html = getHtmlContent(mockWebview);
    
    // Should have fallback code
    expect(html).toContain('hasChildNodes');
    expect(html).toContain('acquireVsCodeApi');
  });
});

