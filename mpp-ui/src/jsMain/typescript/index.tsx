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

const { cc: KotlinCC } = mppCore;

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
    try {
      if (!quiet) {
        console.log('🔍 Enhancing task prompt...');
      }

      // Create file system
      const fileSystem = KotlinCC.unitmesh.devins.filesystem.JsFileSystemFactory.Companion.createFileSystem(resolvedPath);

      // Create domain dict service
      const domainDictService = new KotlinCC.unitmesh.llm.JsDomainDictService(fileSystem);

      // Create prompt enhancer
      const enhancer = new KotlinCC.unitmesh.llm.JsPromptEnhancer(
        llmService,
        fileSystem,
        domainDictService
      );

      // Enhance the task - CLI mode uses Chinese template for English translation but Chinese response
      enhancedTask = await enhancer.enhance(task, 'zh');

      if (enhancedTask !== task && !quiet) {
        console.log(`✨ Enhanced task: ${enhancedTask}`);
        console.log();
      }

    } catch (error) {
      if (!quiet) {
        console.warn('⚠️ Failed to enhance task prompt, using original:', error);
      }
      // Continue with original task on error
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

