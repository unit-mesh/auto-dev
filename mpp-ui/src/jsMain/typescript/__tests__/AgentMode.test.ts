/**
 * AgentMode Tests
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AgentMode } from '../modes/AgentMode.js';
import type { ModeContext } from '../modes/Mode.js';

// Mock dependencies
vi.mock('../config/ConfigManager.js', () => ({
  ConfigManager: {
    load: vi.fn().mockResolvedValue({
      getActiveConfig: vi.fn().mockReturnValue({
        provider: 'deepseek',
        model: 'deepseek-chat',
        apiKey: 'test-key',
        temperature: 0.7,
        maxTokens: 8192,
        baseUrl: 'https://api.deepseek.com'
      }),
      getMcpServers: vi.fn().mockReturnValue({})
    })
  }
}));

vi.mock('@autodev/mpp-core', () => ({
  default: {
    cc: {
      unitmesh: {
        llm: {
          JsKoogLLMService: vi.fn(),
          JsModelConfig: vi.fn()
        },
        agent: {
          JsCodingAgent: vi.fn().mockImplementation(() => ({
            executeTask: vi.fn().mockResolvedValue({
              success: true,
              message: 'Task completed successfully'
            })
          })),
          JsAgentTask: vi.fn()
        }
      }
    }
  }
}));

vi.mock('../agents/render/TuiRenderer.js', () => ({
  TuiRenderer: vi.fn().mockImplementation(() => ({
    renderIterationHeader: vi.fn(),
    renderLLMResponseStart: vi.fn(),
    renderLLMResponseChunk: vi.fn(),
    renderLLMResponseEnd: vi.fn(),
    renderToolCall: vi.fn(),
    renderToolResult: vi.fn(),
    renderTaskComplete: vi.fn(),
    renderFinalResult: vi.fn(),
    renderError: vi.fn(),
    renderRepeatWarning: vi.fn(),
    renderRecoveryAdvice: vi.fn()
  }))
}));

vi.mock('node:fs', () => ({
  existsSync: vi.fn().mockReturnValue(true),
  readFileSync: vi.fn(),
  writeFileSync: vi.fn(),
  mkdirSync: vi.fn(),
  statSync: vi.fn().mockReturnValue({ isDirectory: () => true })
}));

vi.mock('fs', () => ({
  existsSync: vi.fn().mockReturnValue(true),
  readFileSync: vi.fn(),
  writeFileSync: vi.fn(),
  mkdirSync: vi.fn(),
  statSync: vi.fn().mockReturnValue({ isDirectory: () => true })
}));

// Mock path module
vi.mock('path', () => ({
  resolve: vi.fn((path) => path),
  join: vi.fn((...args) => args.join('/')),
  basename: vi.fn((path) => path.split('/').pop() || path)
}));

vi.mock('../utils/commandUtils.js', () => ({
  compileDevIns: vi.fn().mockResolvedValue({
    success: true,
    output: 'compiled output'
  })
}));

// Mock SlashCommandProcessor
vi.mock('../processors/SlashCommandProcessor.js', () => ({
  SlashCommandProcessor: vi.fn().mockImplementation(() => ({
    name: 'SlashCommandProcessor',
    canHandle: vi.fn((input) => input.startsWith('/')),
    process: vi.fn().mockImplementation((input) => {
      if (input === '/help') {
        return Promise.resolve({
          type: 'handled',
          output: '📚 AutoDev CLI Help\n\nCommands:\n  /help       - Show this help message'
        });
      }
      if (input === '/clear') {
        return Promise.resolve({
          type: 'handled',
          output: '✓ Chat history cleared'
        });
      }
      return Promise.resolve({
        type: 'handled',
        output: 'Command processed successfully'
      });
    })
  }))
}));

// Mock InputRouter
vi.mock('../processors/InputRouter.js', () => ({
  InputRouter: vi.fn().mockImplementation(() => ({
    register: vi.fn(),
    route: vi.fn().mockImplementation((input, context) => {
      if (input.startsWith('/help')) {
        return Promise.resolve({
          type: 'handled',
          output: '📚 AutoDev CLI Help\n\nCommands:\n  /help       - Show this help message'
        });
      }
      if (input.startsWith('/clear')) {
        // Call the clearMessages function from context
        if (context.clearMessages) {
          context.clearMessages();
        }
        return Promise.resolve({
          type: 'handled',
          output: '✓ Chat history cleared'
        });
      }
      if (input.startsWith('/')) {
        return Promise.resolve({
          type: 'handled',
          output: 'Command processed successfully'
        });
      }
      // For non-slash commands, return skip so they go to the agent
      return Promise.resolve({
        type: 'skip'
      });
    })
  }))
}));

// Mock domain dict utils
vi.mock('../utils/domainDictUtils.js', () => ({
  DomainDictService: {
    create: vi.fn().mockReturnValue({
      exists: vi.fn().mockResolvedValue(false),
      generateAndSave: vi.fn().mockResolvedValue({
        success: true,
        content: 'test,测试,测试内容'
      })
    })
  },
  getCurrentProjectPath: vi.fn().mockReturnValue('/test/project'),
  isValidProjectPath: vi.fn().mockReturnValue(true)
}));

describe('AgentMode', () => {
  let agentMode: AgentMode;
  let mockContext: ModeContext;

  beforeEach(async () => {
    // Ensure fs.existsSync returns true for test paths
    const fs = await import('fs');
    vi.mocked(fs.existsSync).mockReturnValue(true);

    agentMode = new AgentMode();
    mockContext = {
      addMessage: vi.fn(),
      setPendingMessage: vi.fn(),
      setIsCompiling: vi.fn(),
      clearMessages: vi.fn(),
      logger: {
        info: vi.fn(),
        warn: vi.fn(),
        error: vi.fn(),
      },
      projectPath: '/test/project',
      llmConfig: {
        provider: 'deepseek',
        model: 'deepseek-chat',
        apiKey: 'test-key'
      }
    };

    vi.clearAllMocks();
  });

  describe('initialization', () => {
    it('should initialize successfully with valid config', async () => {
      await expect(agentMode.initialize(mockContext)).resolves.not.toThrow();
      expect(mockContext.logger.info).toHaveBeenCalledWith('[AgentMode] Initializing agent mode...');
      expect(mockContext.logger.info).toHaveBeenCalledWith('[AgentMode] Agent mode initialized successfully');
      expect(mockContext.addMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'system',
          content: expect.stringContaining('AI Agent Mode Activated')
        })
      );
    });

    it('should handle missing project path', async () => {
      const contextWithoutPath = { ...mockContext, projectPath: undefined };
      await expect(agentMode.initialize(contextWithoutPath)).resolves.not.toThrow();
    });

    it('should fail with invalid project path', async () => {
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockReturnValue(false);

      await expect(agentMode.initialize(mockContext)).rejects.toThrow(
        'Project path does not exist'
      );
    });
  });

  describe('input handling', () => {
    beforeEach(async () => {
      await agentMode.initialize(mockContext);
    });

    it('should handle slash commands through router', async () => {
      await agentMode.initialize(mockContext);
      const result = await agentMode.handleInput('/init', mockContext);

      // Should be handled by router, not sent to agent
      expect(result.success).toBe(true);
      expect(mockContext.addMessage).toHaveBeenCalled();
    });

    it('should handle help command', async () => {
      await agentMode.initialize(mockContext);
      const result = await agentMode.handleInput('/help', mockContext);

      expect(result.success).toBe(true);
      expect(mockContext.addMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'system',
          content: expect.stringContaining('AutoDev CLI Help')
        })
      );
    });

    it('should handle clear command', async () => {
      await agentMode.initialize(mockContext);
      const result = await agentMode.handleInput('/clear', mockContext);

      expect(result.success).toBe(true);
      expect(mockContext.clearMessages).toHaveBeenCalled();
    });

    it('should send regular tasks to agent', async () => {
      await agentMode.initialize(mockContext);
      const result = await agentMode.handleInput('Create a hello world function', mockContext);

      expect(result.success).toBe(true);
      expect(mockContext.addMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'user',
          content: 'Create a hello world function'
        })
      );
    });

    it('should reject empty input', async () => {
      await agentMode.initialize(mockContext);
      const result = await agentMode.handleInput('', mockContext);

      expect(result.success).toBe(false);
      expect(result.error).toBe('Please provide a task description');
    });

    it('should prevent concurrent execution', async () => {
      await agentMode.initialize(mockContext);

      // Start first task
      const promise1 = agentMode.handleInput('Task 1', mockContext);

      // Try to start second task while first is running
      const result2 = await agentMode.handleInput('Task 2', mockContext);

      expect(result2.success).toBe(false);
      expect(result2.error).toContain('Agent is already executing');

      // Wait for first task to complete
      await promise1;
    });
  });

  describe('mode properties', () => {
    it('should have correct mode properties', () => {
      expect(agentMode.name).toBe('agent');
      expect(agentMode.displayName).toBe('AI Agent');
      expect(agentMode.description).toBe('Autonomous coding agent that completes development tasks');
      expect(agentMode.icon).toBe('🤖');
    });

    it('should return correct status', () => {
      // AgentMode returns status with project info
      expect(agentMode.getStatus()).toContain('Ready');
    });
  });

  describe('cleanup', () => {
    it('should cleanup successfully', async () => {
      await agentMode.initialize(mockContext);
      await expect(agentMode.cleanup()).resolves.not.toThrow();
    });
  });
});
