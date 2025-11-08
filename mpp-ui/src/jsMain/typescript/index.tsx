#!/usr/bin/env node

/**
 * AutoDev CLI - Main entry point
 *
 * This file serves as the entry point for the @autodev/cli application.
 * It supports both interactive TUI mode and non-interactive coding agent mode.
 */

import React from 'react';
import { render } from 'ink';
import { Command } from 'commander';
import { App } from './ui/App.js';
import { ConfigManager } from './config/ConfigManager.js';
import { CliRenderer } from './agents/render/CliRenderer.js';
import mppCore from '@autodev/mpp-core';
import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';

const { cc: KotlinCC } = mppCore;

/**
 * Save chat history to log file
 */
async function saveChatHistoryToLog(conversationHistory: any[]): Promise<void> {
  try {
    // Create log directory if it doesn't exist
    const logDir = path.join(os.homedir(), '.autodev', 'logs');
    if (!fs.existsSync(logDir)) {
      fs.mkdirSync(logDir, { recursive: true });
    }

    // Generate timestamp for filename
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const logFilePath = path.join(logDir, `chat-history-${timestamp}.json`);

    // Format conversation history
    const formattedHistory = {
      timestamp: new Date().toISOString(),
      messages: conversationHistory.map((msg: any) => ({
        role: msg.role,
        content: msg.content
      }))
    };

    // Write to file
    fs.writeFileSync(logFilePath, JSON.stringify(formattedHistory, null, 2), 'utf-8');
    console.log(`💾 Chat history saved to: ${logFilePath}`);
  } catch (error) {
    console.error('Failed to save chat history:', error);
    throw error;
  }
}

/**
 * Run in coding agent mode
 */
async function runCodingAgent(projectPath: string, task: string, quiet: boolean = false) {
  try {
    // Resolve project path
    const resolvedPath = path.resolve(projectPath);

    // Check if project path exists
    if (!fs.existsSync(resolvedPath)) {
      console.error(`❌ Project path does not exist: ${resolvedPath}`);
      process.exit(1);
    }

    // Load configuration
    const config = await ConfigManager.load();
    const activeConfig = config.getActiveConfig();

    if (!activeConfig) {
      console.error('❌ No active LLM configuration found.');
      console.error('Please run the interactive mode first to configure your LLM provider.');
      process.exit(1);
    }

    // Load tool configuration (includes MCP tool settings)
    const toolConfig = await KotlinCC.unitmesh.agent.config.JsToolConfigManager.loadToolConfig();

    // Debug: Log tool config details
    if (!quiet) {
      console.log('🔍 Debug: Tool config loaded');
      console.log('  Enabled builtin tools:', toolConfig.enabledBuiltinTools.length);
      console.log('  Enabled MCP tools:', toolConfig.enabledMcpTools.length);
      console.log('  MCP servers in tool config:', Object.keys(toolConfig.mcpServers || {}).length);
    }

    // Load MCP servers configuration from main config
    const mcpServers = config.getMcpServers();

    if (!quiet) {
      console.log('  MCP servers in main config:', Object.keys(mcpServers).length);
    }

    // Merge MCP servers from both sources (tool config takes precedence)
    const allMcpServers = { ...mcpServers, ...toolConfig.mcpServers };
    const enabledMcpServers = Object.fromEntries(
      Object.entries(allMcpServers).filter(([_, server]: [string, any]) => !server.disabled)
    );

    if (!quiet) {
      console.log('  Total MCP servers after merge:', Object.keys(allMcpServers).length);
      console.log('  Enabled MCP servers:', Object.keys(enabledMcpServers).length);
    }

    if (!quiet) {
      console.log(`\n🚀 AutoDev Coding Agent`);
      console.log(`📦 Provider: ${activeConfig.provider}`);
      console.log(`🤖 Model: ${activeConfig.model}`);
      console.log(`🔧 Enabled builtin tools: ${toolConfig.enabledBuiltinTools.length}`);
      console.log(`🔌 Enabled MCP tools: ${toolConfig.enabledMcpTools.length}`);
      if (Object.keys(enabledMcpServers).length > 0) {
        console.log(`🔌 MCP Servers: ${Object.keys(enabledMcpServers).join(', ')}`);
      }
      console.log();
    }

    // Create Kotlin LLM service first
    const llmService = KotlinCC.unitmesh.llm.JsKoogLLMService.Companion.create(
      new KotlinCC.unitmesh.llm.JsModelConfig(
        activeConfig.provider,
        activeConfig.model,
        activeConfig.apiKey || '',
        activeConfig.temperature || 0.7,
        activeConfig.maxTokens || 8192,
        activeConfig.baseUrl || ''
      )
    );

    // Enhance the task prompt automatically in CLI mode
    let enhancedTask = task;

    // Temporarily disable prompt enhancement due to cross-platform issues
    // TODO: Re-enable after fixing Kotlin/JS interface type handling
    if (!quiet) {
      console.log('📝 Using original task prompt (enhancement temporarily disabled)');
    }

    // Create CLI renderer
    const renderer = new CliRenderer();

    // Create and run Kotlin CodingAgent with custom renderer, MCP servers, and tool config
    const agent = new KotlinCC.unitmesh.agent.JsCodingAgent(
      resolvedPath,
      llmService,
      10, // maxIterations
      renderer, // custom renderer
      Object.keys(enabledMcpServers).length > 0 ? enabledMcpServers : null, // MCP servers
      toolConfig // tool configuration
    );

    // Create task object with enhanced task
    const taskObj = new KotlinCC.unitmesh.agent.JsAgentTask(
      enhancedTask,
      resolvedPath
    );

    const result = await agent.executeTask(taskObj);

    if (!quiet) {
      console.log(result.success ? '✅ Task completed successfully' : '❌ Task failed');
      if (result.message) {
        console.log(result.message);
      }

      // Save conversation history to log file
      try {
        const conversationHistory = agent.getConversationHistory();
        await saveChatHistoryToLog(conversationHistory);
      } catch (error) {
        console.error('⚠️  Failed to save chat history:', error);
      }
    }

    process.exit(result.success ? 0 : 1);

  } catch (error) {
    console.error('❌ Fatal error:', error);
    process.exit(1);
  }
}

/**
 * Run in interactive TUI mode
 */
async function runInteractive() {
  try {
    // Render the Ink application
    const { waitUntilExit } = render(<App />);

    // Wait for the app to exit
    await waitUntilExit();

  } catch (error) {
    console.error('Fatal error:', error);
    process.exit(1);
  }
}

// Main entry point
async function main() {
  const program = new Command();

  program
    .name('autodev')
    .description('AutoDev CLI - AI-powered development assistant')
    .version('0.1.3');

  // Interactive mode (default)
  program
    .command('chat', { isDefault: true })
    .description('Start interactive chat mode (default)')
    .action(runInteractive);

  // Coding agent mode
  program
    .command('code')
    .description('Run autonomous coding agent to complete a task')
    .requiredOption('-p, --path <path>', 'Project path (e.g., /path/to/project or . for current directory)')
    .requiredOption('-t, --task <task>', 'Development task or requirement to complete')
    .option('-m, --max-iterations <number>', 'Maximum iterations', '20')
    .option('-q, --quiet', 'Quiet mode - only show important messages', false)
    .option('-v, --verbose', 'Verbose mode - show all debug information', false)
    .action(async (options) => {
      const projectPath = options.path === '.' ? process.cwd() : options.path;
      await runCodingAgent(projectPath, options.task, options.quiet && !options.verbose);
    });

  // Parse arguments
  await program.parseAsync(process.argv);
}

// Run the application
main().catch((error) => {
  console.error('Unhandled error:', error);
  process.exit(1);
});

