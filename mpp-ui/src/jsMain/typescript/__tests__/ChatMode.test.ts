/**
 * ChatMode Tests
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ChatMode } from '../modes/ChatMode.js';
import type { ModeContext } from '../modes/Mode.js';

// Mock dependencies
vi.mock('../agents/LLMService.js', () => ({
  LLMService: vi.fn().mockImplementation(() => ({
    chat: vi.fn().mockResolvedValue({
      content: 'Mock LLM response',
      usage: { totalTokens: 100 }
    }),
    streamMessage: vi.fn().mockImplementation(async (query, onChunk) => {
      // Simulate streaming response
      const chunks = ['Mock ', 'LLM ', 'response'];
      for (const chunk of chunks) {
        onChunk(chunk);
      }
    })
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

vi.mock('../processors/InputRouter.js', () => ({
  InputRouter: vi.fn().mockImplementation(() => ({
    register: vi.fn(),
    route: vi.fn().mockImplementation((input, context) => {
      if (input === '/help') {
        return Promise.resolve({
          type: 'handled',
          output: `📚 AutoDev CLI Help

Commands:
  /help       - Show this help message
  /clear      - Clear chat history
  /exit       - Exit the application
  /config     - Show current configuration
  /model      - Change AI model

Agents (use @ to invoke):
  @code       - Code generation and refactoring
  @test       - Test generation
  @doc        - Documentation generation
  @review     - Code review
  @debug      - Debugging assistance

Shortcuts:
  Ctrl+C      - Exit
  Ctrl+L      - Clear screen
  Tab         - Auto-complete
  ↑/↓         - Navigate history`
        });
      }
      if (input === '/clear') {
        // Call clearMessages if available in context
        if (context && context.clearMessages) {
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
          output: 'Command processed'
        });
      }
      return Promise.resolve({
        type: 'llm-query',
        query: input
      });
    })
  }))
}));

describe('ChatMode', () => {
  let chatMode: ChatMode;
  let mockContext: ModeContext;

  beforeEach(() => {
    chatMode = new ChatMode();
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
        apiKey: 'test-key',
        temperature: 0.7,
        maxTokens: 4096,
        baseUrl: 'https://api.deepseek.com'
      }
    };
  });

  describe('initialization', () => {
    it('should initialize successfully with valid config', async () => {
      await expect(chatMode.initialize(mockContext)).resolves.not.toThrow();
      expect(mockContext.logger.info).toHaveBeenCalledWith('[ChatMode] Initializing chat mode...');
      expect(mockContext.logger.info).toHaveBeenCalledWith('[ChatMode] Chat mode initialized successfully');
      expect(mockContext.addMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'system',
          content: expect.stringContaining('Chat Mode Activated')
        })
      );
    });

    it('should fail initialization without LLM config', async () => {
      const contextWithoutConfig = { ...mockContext, llmConfig: null };
      await expect(chatMode.initialize(contextWithoutConfig)).rejects.toThrow(
        'LLM configuration is required for chat mode'
      );
    });
  });

  describe('input handling', () => {
    beforeEach(async () => {
      await chatMode.initialize(mockContext);
    });

    it('should handle slash commands through router', async () => {
      const result = await chatMode.handleInput('/init', mockContext);

      expect(result.success).toBe(true);
      // Should add user message for the command
      expect(mockContext.addMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'user',
          content: '/init'
        })
      );
    });

    it('should handle help command', async () => {
      const result = await chatMode.handleInput('/help', mockContext);

      expect(result.success).toBe(true);
      // Should add user message and system response
      expect(mockContext.addMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'user',
          content: '/help'
        })
      );
      // Should also add help response
      expect(mockContext.addMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'system',
          content: expect.stringContaining('AutoDev CLI Help')
        })
      );
    });

    it('should handle clear command', async () => {
      const result = await chatMode.handleInput('/clear', mockContext);
      
      expect(result.success).toBe(true);
      expect(mockContext.clearMessages).toHaveBeenCalled();
    });

    it('should reject empty input', async () => {
      const result = await chatMode.handleInput('', mockContext);
      
      expect(result.success).toBe(false);
      expect(result.error).toBe('Please enter a message');
    });

    it('should reject whitespace-only input', async () => {
      const result = await chatMode.handleInput('   ', mockContext);
      
      expect(result.success).toBe(false);
      expect(result.error).toBe('Please enter a message');
    });

    it('should handle regular chat messages', async () => {
      const result = await chatMode.handleInput('Hello, how are you?', mockContext);

      expect(result.success).toBe(true);
      // Should add user message
      expect(mockContext.addMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'user',
          content: 'Hello, how are you?'
        })
      );
      // Should also add LLM response (mocked)
      expect(mockContext.addMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'assistant',
          content: 'Mock LLM response'
        })
      );
    });

    it('should handle errors gracefully', async () => {
      // Mock the router to throw an error
      const errorRouter = {
        route: vi.fn().mockRejectedValue(new Error('Router error'))
      };

      // Create a new ChatMode instance and replace its router
      const errorMode = new ChatMode();
      await errorMode.initialize(mockContext);
      (errorMode as any).router = errorRouter;

      const result = await errorMode.handleInput('test input', mockContext);

      expect(result.success).toBe(false);
      expect(result.error).toBeDefined();
      expect(mockContext.addMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'system',
          content: expect.stringContaining('❌ Error:')
        })
      );
    });
  });

  describe('mode properties', () => {
    it('should have correct mode properties', () => {
      expect(chatMode.name).toBe('chat');
      expect(chatMode.displayName).toBe('Chat');
      expect(chatMode.description).toBe('Interactive chat with AI assistant');
      expect(chatMode.icon).toBe('💬');
    });

    it('should return correct status', () => {
      expect(chatMode.getStatus()).toBe('Ready for conversation');
    });
  });

  describe('cleanup', () => {
    it('should cleanup successfully', async () => {
      await chatMode.initialize(mockContext);
      await expect(chatMode.cleanup()).resolves.not.toThrow();
    });
  });
});
