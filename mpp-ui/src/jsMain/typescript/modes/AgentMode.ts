/**
 * Agent Mode - AI 代理模式
 *
 * 集成 CodingAgent 逻辑，类似于 CLI 中的 runCodingAgent
 * 用户输入被视为开发任务，由 AI 代理自主完成
 */

import type { Mode, ModeContext, ModeResult, ModeFactory } from './Mode.js';
import type { Message } from '../ui/App.js';
import { ConfigManager } from '../config/ConfigManager.js';
import { TuiRenderer } from '../agents/render/TuiRenderer.js';
import { InputRouter } from '../processors/InputRouter.js';
import { SlashCommandProcessor } from '../processors/SlashCommandProcessor.js';
import { compileDevIns } from '../utils/commandUtils.js';
import mppCore from '@autodev/mpp-core';
import * as path from 'path';
import * as fs from 'fs';

const { cc: KotlinCC } = mppCore;

/**
 * Agent 模式实现
 */
export class AgentMode implements Mode {
  readonly name = 'agent';
  readonly displayName = 'AI Agent';
  readonly description = 'Autonomous coding agent that completes development tasks';
  readonly icon = '🤖';

  private agent: any = null;
  private renderer: TuiRenderer | null = null;
  private router: InputRouter | null = null;
  private isExecuting = false;
  private projectPath = process.cwd();

  async initialize(context: ModeContext): Promise<void> {
    context.logger.info('[AgentMode] Initializing agent mode...');

    try {
      // 设置项目路径
      if (context.projectPath) {
        this.projectPath = path.resolve(context.projectPath);
      }

      // 检查项目路径是否存在
      if (!fs.existsSync(this.projectPath)) {
        throw new Error(`Project path does not exist: ${this.projectPath}`);
      }

      // 加载配置
      const config = await ConfigManager.load();
      const activeConfig = config.getActiveConfig();

      if (!activeConfig) {
        throw new Error('No active LLM configuration found. Please configure your LLM provider first.');
      }

      // 创建 TUI 渲染器
      this.renderer = new TuiRenderer(context);

      // 初始化输入路由器
      this.router = new InputRouter();

      // 注册斜杠命令处理器（高优先级）
      const slashProcessor = new SlashCommandProcessor();
      this.router.register(slashProcessor, 100);

      // 创建 LLM 服务
      const llmService = new KotlinCC.unitmesh.llm.JsKoogLLMService(
        new KotlinCC.unitmesh.llm.JsModelConfig(
          activeConfig.provider,
          activeConfig.model,
          activeConfig.apiKey || '',
          activeConfig.temperature || 0.7,
          activeConfig.maxTokens || 8192,
          activeConfig.baseUrl || ''
        )
      );

      // 加载 MCP 服务器配置
      const mcpServers = config.getMcpServers();
      const enabledMcpServers: Record<string, any> = {};

      for (const [name, serverConfig] of Object.entries(mcpServers)) {
        if ((serverConfig as any).enabled) {
          enabledMcpServers[name] = {
            command: (serverConfig as any).command,
            args: (serverConfig as any).args || [],
            env: (serverConfig as any).env || {}
          };
        }
      }

      // 加载工具配置 - 使用默认配置
      const toolConfig = null; // ConfigManager 可能没有 getToolConfig 方法

      // 创建 CodingAgent
      this.agent = new KotlinCC.unitmesh.agent.JsCodingAgent(
        this.projectPath,
        llmService,
        10, // maxIterations
        this.renderer,
        Object.keys(enabledMcpServers).length > 0 ? enabledMcpServers : null,
        toolConfig
      );

      context.logger.info('[AgentMode] Agent mode initialized successfully');

      // 显示欢迎消息
      const welcomeMessage: Message = {
        role: 'system',
        content: `🤖 **AI Agent Mode Activated**\n\nProject: \`${this.projectPath}\`\n\nI'm ready to help you with development tasks. Just describe what you want me to do, and I'll work autonomously to complete it.\n\nType \`/chat\` to switch to chat mode, or \`/help\` for more commands.`,
        timestamp: Date.now(),
        showPrefix: true
      };

      context.addMessage(welcomeMessage);

    } catch (error) {
      context.logger.error('[AgentMode] Failed to initialize agent mode:', error);
      throw error;
    }
  }

  async handleInput(input: string, context: ModeContext): Promise<ModeResult> {
    if (!this.agent || !this.renderer || !this.router) {
      return {
        success: false,
        error: 'Agent not initialized'
      };
    }

    const trimmedInput = input.trim();
    if (!trimmedInput) {
      return {
        success: false,
        error: 'Please provide a task description'
      };
    }

    try {
      // 首先尝试通过路由器处理输入（处理斜杠命令等）
      const routerContext = {
        clearMessages: context.clearMessages,
        logger: context.logger,
        addMessage: (role: string, content: string) => {
          const message: Message = {
            role: role as any,
            content,
            timestamp: Date.now(),
            showPrefix: true
          };
          context.addMessage(message);
        },
        setLoading: (loading: boolean) => {
          // TUI 模式下可以忽略加载状态
        },
        readFile: async (path: string) => {
          const compileResult = await compileDevIns(`/file:${path}`);
          if (compileResult?.success) {
            return compileResult.output;
          }
          throw new Error(compileResult?.errorMessage || 'Failed to read file');
        }
      };

      const routeResult = await this.router.route(trimmedInput, routerContext);

      // 处理路由结果
      switch (routeResult.type) {
        case 'handled':
          // 斜杠命令已处理
          if (routeResult.output) {
            const outputMessage: Message = {
              role: 'system',
              content: routeResult.output,
              timestamp: Date.now(),
              showPrefix: true
            };
            context.addMessage(outputMessage);
          }
          return { success: true };

        case 'error':
          const errorMessage: Message = {
            role: 'system',
            content: `❌ ${routeResult.message}`,
            timestamp: Date.now(),
            showPrefix: true
          };
          context.addMessage(errorMessage);
          return { success: false, error: routeResult.message };

        case 'compile':
          // DevIns 编译命令，委托给 Agent 处理
          break;

        case 'llm-query':
          // LLM 查询，委托给 Agent 处理
          break;

        case 'skip':
          // 继续处理
          break;
      }

      // 如果路由器没有处理，或者返回了需要 Agent 处理的结果，则使用 Agent
      if (this.isExecuting) {
        return {
          success: false,
          error: 'Agent is already executing a task. Please wait for completion.'
        };
      }

      this.isExecuting = true;
      context.logger.info(`[AgentMode] Executing task: ${trimmedInput}`);

      // 添加用户消息
      const userMessage: Message = {
        role: 'user',
        content: trimmedInput,
        timestamp: Date.now(),
        showPrefix: true
      };
      context.addMessage(userMessage);

      // 创建任务对象
      const taskObj = new KotlinCC.unitmesh.agent.JsAgentTask(
        trimmedInput,
        this.projectPath
      );

      // 执行任务
      const result = await this.agent.executeTask(taskObj);

      // 添加完成消息
      const completionMessage: Message = {
        role: 'system',
        content: result.success
          ? '✅ **Task completed successfully**'
          : '❌ **Task failed**',
        timestamp: Date.now(),
        showPrefix: true
      };
      context.addMessage(completionMessage);

      if (result.message) {
        const resultMessage: Message = {
          role: 'assistant',
          content: result.message,
          timestamp: Date.now(),
          showPrefix: true
        };
        context.addMessage(resultMessage);
      }

      this.isExecuting = false;
      return {
        success: result.success,
        message: result.message
      };

    } catch (error) {
      this.isExecuting = false;
      context.logger.error('[AgentMode] Task execution failed:', error);

      const errorMessage: Message = {
        role: 'system',
        content: `❌ **Task execution failed**: ${error instanceof Error ? error.message : String(error)}`,
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

  async cleanup(): Promise<void> {
    this.isExecuting = false;
    this.agent = null;
    this.renderer = null;
  }

  getStatus(): string {
    if (this.isExecuting) {
      return 'Executing task...';
    }
    return `Ready (Project: ${path.basename(this.projectPath)})`;
  }
}

/**
 * Agent 模式工厂
 */
export class AgentModeFactory implements ModeFactory {
  readonly type = 'agent';

  createMode(): Mode {
    return new AgentMode();
  }
}
