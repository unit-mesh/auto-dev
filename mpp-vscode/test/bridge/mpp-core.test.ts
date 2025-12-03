/**
 * Tests for mpp-core bridge
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ProviderTypes } from '../../src/bridge/mpp-core';

// Mock the mpp-core module
vi.mock('@autodev/mpp-core', () => ({
  default: {
    cc: {
      unitmesh: {
        llm: {
          JsKoogLLMService: vi.fn().mockImplementation(() => ({
            streamPrompt: vi.fn().mockResolvedValue(undefined),
            sendPrompt: vi.fn().mockResolvedValue('test response'),
            getLastTokenInfo: vi.fn().mockReturnValue({ totalTokens: 100, inputTokens: 50, outputTokens: 50 })
          })),
          JsModelConfig: vi.fn(),
          JsMessage: vi.fn(),
          JsModelRegistry: {
            getAvailableModels: vi.fn().mockReturnValue(['gpt-4', 'gpt-3.5-turbo']),
            getAllProviders: vi.fn().mockReturnValue(['OPENAI', 'ANTHROPIC'])
          },
          JsCompletionManager: vi.fn().mockImplementation(() => ({
            initWorkspace: vi.fn().mockResolvedValue(true),
            getCompletions: vi.fn().mockReturnValue([])
          })),
          JsDevInsCompiler: vi.fn().mockImplementation(() => ({
            compile: vi.fn().mockResolvedValue({ success: true, output: 'compiled', errorMessage: null, hasCommand: true }),
            compileToString: vi.fn().mockResolvedValue('compiled output')
          })),
          JsToolRegistry: vi.fn().mockImplementation(() => ({
            readFile: vi.fn().mockResolvedValue({ success: true, output: 'file content', errorMessage: null, metadata: {} }),
            writeFile: vi.fn().mockResolvedValue({ success: true, output: '', errorMessage: null, metadata: {} }),
            glob: vi.fn().mockResolvedValue({ success: true, output: '["file1.ts", "file2.ts"]', errorMessage: null, metadata: {} }),
            grep: vi.fn().mockResolvedValue({ success: true, output: 'match found', errorMessage: null, metadata: {} }),
            shell: vi.fn().mockResolvedValue({ success: true, output: 'command output', errorMessage: null, metadata: {} }),
            getAvailableTools: vi.fn().mockReturnValue(['read-file', 'write-file', 'glob', 'grep', 'shell']),
            formatToolListForAI: vi.fn().mockReturnValue('Tool list for AI')
          })),
          JsCompressionConfig: vi.fn()
        },
        agent: {
          JsCodingAgent: vi.fn().mockImplementation(() => ({
            executeTask: vi.fn().mockResolvedValue({
              success: true,
              message: 'Task completed',
              steps: [],
              edits: []
            }),
            initializeWorkspace: vi.fn().mockResolvedValue(undefined),
            getConversationHistory: vi.fn().mockReturnValue([])
          })),
          JsAgentTask: vi.fn()
        }
      }
    }
  }
}));

describe('ProviderTypes', () => {
  it('should map provider names correctly', () => {
    expect(ProviderTypes['openai']).toBe('OPENAI');
    expect(ProviderTypes['anthropic']).toBe('ANTHROPIC');
    expect(ProviderTypes['google']).toBe('GOOGLE');
    expect(ProviderTypes['deepseek']).toBe('DEEPSEEK');
    expect(ProviderTypes['ollama']).toBe('OLLAMA');
    expect(ProviderTypes['openrouter']).toBe('OPENROUTER');
    expect(ProviderTypes['custom-openai-base']).toBe('CUSTOM_OPENAI_BASE');
  });
});

describe('LLMService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should be importable', async () => {
    const { LLMService } = await import('../../src/bridge/mpp-core');
    expect(LLMService).toBeDefined();
  });

  it('should create instance with config', async () => {
    const { LLMService } = await import('../../src/bridge/mpp-core');
    const service = new LLMService({
      provider: 'openai',
      model: 'gpt-4',
      apiKey: 'test-key'
    });
    expect(service).toBeDefined();
  });

  it('should have empty history initially', async () => {
    const { LLMService } = await import('../../src/bridge/mpp-core');
    const service = new LLMService({
      provider: 'openai',
      model: 'gpt-4',
      apiKey: 'test-key'
    });
    expect(service.getHistory()).toEqual([]);
  });

  it('should clear history', async () => {
    const { LLMService } = await import('../../src/bridge/mpp-core');
    const service = new LLMService({
      provider: 'openai',
      model: 'gpt-4',
      apiKey: 'test-key'
    });
    service.clearHistory();
    expect(service.getHistory()).toEqual([]);
  });
});

describe('CompletionManager', () => {
  it('should be importable', async () => {
    const { CompletionManager } = await import('../../src/bridge/mpp-core');
    expect(CompletionManager).toBeDefined();
  });

  it('should create instance', async () => {
    const { CompletionManager } = await import('../../src/bridge/mpp-core');
    const manager = new CompletionManager();
    expect(manager).toBeDefined();
  });
});

describe('DevInsCompiler', () => {
  it('should be importable', async () => {
    const { DevInsCompiler } = await import('../../src/bridge/mpp-core');
    expect(DevInsCompiler).toBeDefined();
  });
});

describe('ToolRegistry', () => {
  it('should be importable', async () => {
    const { ToolRegistry } = await import('../../src/bridge/mpp-core');
    expect(ToolRegistry).toBeDefined();
  });

  it('should create instance with project path', async () => {
    const { ToolRegistry } = await import('../../src/bridge/mpp-core');
    const registry = new ToolRegistry('/test/project');
    expect(registry).toBeDefined();
  });
});

describe('CodingAgent', () => {
  it('should be importable', async () => {
    const { CodingAgent } = await import('../../src/bridge/mpp-core');
    expect(CodingAgent).toBeDefined();
  });
});

describe('Helper functions', () => {
  it('should get available models', async () => {
    const { getAvailableModels } = await import('../../src/bridge/mpp-core');
    const models = getAvailableModels('openai');
    expect(Array.isArray(models)).toBe(true);
  });

  it('should get all providers', async () => {
    const { getAllProviders } = await import('../../src/bridge/mpp-core');
    const providers = getAllProviders();
    expect(Array.isArray(providers)).toBe(true);
  });
});

