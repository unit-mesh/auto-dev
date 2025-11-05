/**
 * TUI Integration Tests
 * 测试整个 TUI 应用的集成流程
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ModeManager } from '../modes/ModeManager.js';
import { ChatModeFactory, AgentModeFactory } from '../modes/index.js';
import type { ModeContext } from '../modes/Mode.js';

// Mock all external dependencies
vi.mock('../config/ConfigManager.js', () => ({
  ConfigManager: {
    load: vi.fn().mockResolvedValue({
      isValid: vi.fn().mockReturnValue(true),
      toJSON: vi.fn().mockReturnValue({
        provider: 'deepseek',
        model: 'deepseek-chat',
        apiKey: 'test-key'
      }),
      getActiveConfig: vi.fn().mockReturnValue({
        provider: 'deepseek',
        model: 'deepseek-chat',
        apiKey: 'test-key'
      }),
      getMcpServers: vi.fn().mockReturnValue({})
    })
  }
}));

vi.mock('../agents/LLMService.js', () => ({
  LLMService: vi.fn().mockImplementation(() => ({
    chat: vi.fn().mockResolvedValue({
      content: 'Mock LLM response',
      usage: { totalTokens: 100 }
    })
  }))
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
              message: 'Task completed'
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

vi.mock('../utils/commandUtils.js', () => ({
  compileDevIns: vi.fn().mockResolvedValue({
    success: true,
    output: 'compiled output',
    hasCommand: false
  }),
  hasDevInsCommands: vi.fn().mockReturnValue(false)
}));

// Mock SlashCommandProcessor
vi.mock('../processors/SlashCommandProcessor.js', () => ({
  SlashCommandProcessor: vi.fn().mockImplementation(() => ({
    name: 'SlashCommandProcessor',
    canHandle: vi.fn((input) => input.startsWith('/')),
    process: vi.fn().mockResolvedValue({
      type: 'handled',
      output: 'Command processed successfully'
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

// Mock AtCommandProcessor
vi.mock('../processors/AtCommandProcessor.js', () => ({
  AtCommandProcessor: vi.fn().mockImplementation(() => ({
    name: 'AtCommandProcessor',
    canHandle: vi.fn((input) => input.startsWith('@')),
    process: vi.fn().mockResolvedValue({
      type: 'skip'
    })
  }))
}));

// Mock VariableProcessor
vi.mock('../processors/VariableProcessor.js', () => ({
  VariableProcessor: vi.fn().mockImplementation(() => ({
    name: 'VariableProcessor',
    canHandle: vi.fn(() => false),
    process: vi.fn().mockResolvedValue({
      type: 'skip'
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

vi.mock('fs', () => ({
  existsSync: vi.fn().mockReturnValue(true),
  readFileSync: vi.fn(),
  writeFileSync: vi.fn(),
  mkdirSync: vi.fn(),
  statSync: vi.fn().mockReturnValue({ isDirectory: () => true })
}));

vi.mock('node:fs', () => ({
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

describe('TUI Integration Tests', () => {
  let modeManager: ModeManager;
  let mockContext: ModeContext;

  beforeEach(async () => {
    // Setup fs mocks for project path validation
    const fs = await import('fs');
    vi.mocked(fs.existsSync).mockImplementation((path) => {
      if (path === '/test/project') return true;
      if (path === '/test/project/package.json') return true;
      return false;
    });

    modeManager = new ModeManager();
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
  });

  describe('Mode Management', () => {
    it('should register and switch between modes', async () => {
      // Register modes
      modeManager.registerMode(new ChatModeFactory());
      modeManager.registerMode(new AgentModeFactory());

      expect(modeManager.getAvailableModes()).toContain('chat');
      expect(modeManager.getAvailableModes()).toContain('agent');

      // Switch to chat mode
      const chatSuccess = await modeManager.switchToMode('chat', mockContext);
      expect(chatSuccess).toBe(true);
      expect(modeManager.getCurrentMode()?.type).toBe('chat');

      // Switch to agent mode
      const agentSuccess = await modeManager.switchToMode('agent', mockContext);
      expect(agentSuccess).toBe(true);
      expect(modeManager.getCurrentMode()?.type).toBe('agent');
    });

    it('should handle mode change events', async () => {
      const changeListener = vi.fn();
      modeManager.onModeChange(changeListener);

      modeManager.registerMode(new ChatModeFactory());
      await modeManager.switchToMode('chat', mockContext);

      expect(changeListener).toHaveBeenCalledWith(
        expect.objectContaining({
          currentMode: 'chat',
          previousMode: null
        })
      );
    });
  });

  describe('Slash Command Integration', () => {
    beforeEach(async () => {
      modeManager.registerMode(new ChatModeFactory());
      modeManager.registerMode(new AgentModeFactory());
    });

    it('should handle /init command in chat mode', async () => {
      await modeManager.switchToMode('chat', mockContext);
      
      const result = await modeManager.handleInput('/init');
      expect(result).toBe(true);
      
      // Should add user message
      expect(mockContext.addMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'user',
          content: '/init'
        })
      );
    });

    it('should handle /init command in agent mode', async () => {
      await modeManager.switchToMode('agent', mockContext);
      
      const result = await modeManager.handleInput('/init');
      expect(result).toBe(true);
      
      // Should be handled by router, not sent to agent
      expect(mockContext.addMessage).toHaveBeenCalled();
    });

    it('should handle /help command in both modes', async () => {
      // Test in chat mode
      await modeManager.switchToMode('chat', mockContext);
      let result = await modeManager.handleInput('/help');
      expect(result).toBe(true);

      // Test in agent mode
      await modeManager.switchToMode('agent', mockContext);
      result = await modeManager.handleInput('/help');
      expect(result).toBe(true);
    });

    it('should handle /clear command in both modes', async () => {
      // Test in chat mode
      await modeManager.switchToMode('chat', mockContext);
      let result = await modeManager.handleInput('/clear');
      expect(result).toBe(true);
      expect(mockContext.clearMessages).toHaveBeenCalled();

      // Reset mock
      vi.clearAllMocks();

      // Test in agent mode
      await modeManager.switchToMode('agent', mockContext);
      result = await modeManager.handleInput('/clear');
      expect(result).toBe(true);
      expect(mockContext.clearMessages).toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    it('should handle invalid mode switching', async () => {
      const result = await modeManager.switchToMode('invalid-mode', mockContext);
      expect(result).toBe(false);
    });

    it('should handle input without active mode', async () => {
      const result = await modeManager.handleInput('/test');
      expect(result).toBe(false);
    });

    it('should handle mode initialization errors', async () => {
      // Mock a mode that fails to initialize
      const failingModeFactory = {
        type: 'failing',
        createMode: () => ({
          name: 'failing',
          displayName: 'Failing Mode',
          description: 'A mode that fails',
          icon: '❌',
          initialize: vi.fn().mockRejectedValue(new Error('Init failed')),
          handleInput: vi.fn(),
          getStatus: vi.fn().mockReturnValue('Error'),
          cleanup: vi.fn()
        })
      };

      modeManager.registerMode(failingModeFactory);
      const result = await modeManager.switchToMode('failing', mockContext);
      expect(result).toBe(false);
    });
  });
});
