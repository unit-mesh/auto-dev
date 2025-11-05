/**
 * Chat Mode - 聊天模式
 * 
 * 封装现有的聊天逻辑，支持与 LLM 的对话交互
 */

import type { Mode, ModeContext, ModeResult, ModeFactory } from './Mode.js';
import type { Message } from '../ui/App.js';
import { LLMService } from '../agents/LLMService.js';
import { InputRouter } from '../processors/InputRouter.js';
import { SlashCommandProcessor } from '../processors/SlashCommandProcessor.js';
import { AtCommandProcessor } from '../processors/AtCommandProcessor.js';
import { VariableProcessor } from '../processors/VariableProcessor.js';
import { compileDevIns, hasDevInsCommands } from '../utils/commandUtils.js';
import { findLastSafeSplitPoint } from '../utils/markdownSplitter.js';

/**
 * Chat 模式实现
 */
export class ChatMode implements Mode {
  readonly name = 'chat';
  readonly displayName = 'Chat';
  readonly description = 'Interactive chat with AI assistant';
  readonly icon = '💬';

  private llmService: LLMService | null = null;
  private router: InputRouter | null = null;

  async initialize(context: ModeContext): Promise<void> {
    context.logger.info('[ChatMode] Initializing chat mode...');
    
    try {
      // 初始化 LLM 服务
      if (!context.llmConfig) {
        throw new Error('LLM configuration is required for chat mode');
      }
      
      this.llmService = new LLMService(context.llmConfig);

      // 初始化输入路由器
      this.router = new InputRouter();
      
      // 注册处理器（排除模式切换命令，因为它们由外部处理）
      const slashProcessor = new SlashCommandProcessor();
      this.router.register(slashProcessor, 100);
      this.router.register(new AtCommandProcessor(), 50);
      this.router.register(new VariableProcessor(), 30);

      context.logger.info('[ChatMode] Chat mode initialized successfully');
      
      // 显示欢迎消息
      const welcomeMessage: Message = {
        role: 'system',
        content: `💬 **Chat Mode Activated**\n\nI'm ready to chat and help you with questions, code review, explanations, and more.\n\nType \`/agent\` to switch to autonomous agent mode, or \`/help\` for more commands.`,
        timestamp: Date.now(),
        showPrefix: true
      };
      
      context.addMessage(welcomeMessage);

    } catch (error) {
      context.logger.error('[ChatMode] Failed to initialize chat mode:', error);
      throw error;
    }
  }

  async handleInput(input: string, context: ModeContext): Promise<ModeResult> {
    if (!this.llmService || !this.router) {
      return {
        success: false,
        error: 'Chat mode not initialized'
      };
    }

    const trimmedInput = input.trim();
    if (!trimmedInput) {
      return {
        success: false,
        error: 'Please enter a message'
      };
    }

    try {
      // 添加用户消息
      const userMessage: Message = {
        role: 'user',
        content: trimmedInput,
        timestamp: Date.now(),
        showPrefix: true
      };
      context.addMessage(userMessage);

      // 处理 DevIns 命令编译
      let processedContent = trimmedInput;
      
      if (hasDevInsCommands(trimmedInput)) {
        context.setIsCompiling(true);
        context.setPendingMessage({
          role: 'compiling',
          content: '🔧 Compiling DevIns commands...',
          timestamp: Date.now(),
          showPrefix: true,
        });

        const compileResult = await compileDevIns(trimmedInput);
        
        if (compileResult) {
          if (compileResult.success) {
            processedContent = compileResult.output;
            
            if (compileResult.hasCommand && compileResult.output !== trimmedInput) {
              const compileMessage: Message = {
                role: 'system',
                content: `📝 Compiled output:\n${compileResult.output}`,
                timestamp: Date.now(),
                showPrefix: true,
              };
              context.addMessage(compileMessage);
            }
          } else {
            const errorMessage: Message = {
              role: 'system',
              content: `⚠️  DevIns compilation error: ${compileResult.errorMessage}`,
              timestamp: Date.now(),
              showPrefix: true,
            };
            context.addMessage(errorMessage);
          }
        }
        
        context.setIsCompiling(false);
        context.setPendingMessage(null);
      }

      // 路由输入到处理器
      const routerContext = {
        clearMessages: context.clearMessages,
        logger: context.logger,
        readFile: async (path: string) => {
          const compileResult = await compileDevIns(`/file:${path}`);
          if (compileResult?.success) {
            return compileResult.output;
          }
          throw new Error(compileResult?.errorMessage || 'Failed to read file');
        }
      };

      const result = await this.router.route(processedContent, routerContext);

      // 处理路由结果
      switch (result.type) {
        case 'handled':
          if (result.output) {
            const outputMessage: Message = {
              role: 'system',
              content: result.output,
              timestamp: Date.now(),
              showPrefix: true
            };
            context.addMessage(outputMessage);
          }
          return { success: true };

        case 'compile':
          // 已经在上面处理了编译
          return { success: true };

        case 'error':
          const errorMessage: Message = {
            role: 'system',
            content: `❌ ${result.message}`,
            timestamp: Date.now(),
            showPrefix: true
          };
          context.addMessage(errorMessage);
          return { success: false, error: result.message };

        case 'llm-query':
          // 发送到 LLM
          return await this.handleLLMQuery(result.query, context);

        default:
          // 默认发送到 LLM
          return await this.handleLLMQuery(processedContent, context);
      }

    } catch (error) {
      context.logger.error('[ChatMode] Error handling input:', error);
      
      const errorMessage: Message = {
        role: 'system',
        content: `❌ Error: ${error instanceof Error ? error.message : String(error)}`,
        timestamp: Date.now(),
        showPrefix: true
      };
      context.addMessage(errorMessage);

      return {
        success: false,
        error: error instanceof Error ? error.message : String(error)
      };
    }
  }

  private async handleLLMQuery(query: string, context: ModeContext): Promise<ModeResult> {
    if (!this.llmService) {
      return { success: false, error: 'LLM service not available' };
    }

    try {
      // 创建待处理消息用于流式输出
      context.setPendingMessage({
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
      });

      // 流式响应处理
      let assistantContent = '';
      const startTimestamp = Date.now();
      let isFirstBlock = true;

      await this.llmService.streamMessage(query, (chunk) => {
        assistantContent += chunk;
        
        // 查找安全分割点
        const splitPoint = findLastSafeSplitPoint(assistantContent);
        
        if (splitPoint === assistantContent.length) {
          // 没有完整块，只更新待处理消息
          context.setPendingMessage({
            role: 'assistant',
            content: assistantContent,
            timestamp: startTimestamp,
            showPrefix: isFirstBlock,
          });
        } else {
          // 找到完整块，分割处理
          const completedContent = assistantContent.substring(0, splitPoint);
          const pendingContent = assistantContent.substring(splitPoint);
          
          // 移动完成的内容到历史
          context.addMessage({
            role: 'assistant',
            content: completedContent,
            timestamp: startTimestamp,
            showPrefix: isFirstBlock,
          });
          
          // 保留待处理内容
          context.setPendingMessage({
            role: 'assistant',
            content: pendingContent,
            timestamp: startTimestamp,
            showPrefix: false,
          });
          
          assistantContent = pendingContent;
          isFirstBlock = false;
        }
      });

      // 清除待处理消息
      context.setPendingMessage(null);
      
      // 移动剩余内容到历史
      if (assistantContent.trim()) {
        context.addMessage({
          role: 'assistant',
          content: assistantContent,
          timestamp: startTimestamp,
          showPrefix: isFirstBlock,
        });
      }

      return { success: true };

    } catch (error) {
      context.setPendingMessage(null);
      return {
        success: false,
        error: error instanceof Error ? error.message : String(error)
      };
    }
  }

  async cleanup(): Promise<void> {
    this.llmService = null;
    this.router = null;
  }

  getStatus(): string {
    return 'Ready for conversation';
  }
}

/**
 * Chat 模式工厂
 */
export class ChatModeFactory implements ModeFactory {
  readonly type = 'chat';

  createMode(): Mode {
    return new ChatMode();
  }
}
