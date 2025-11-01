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
import { CodingAgentService } from './agents/CodingAgentService';
import * as path from 'path';
import * as fs from 'fs';

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

    if (!quiet) {
      console.log(`\n🚀 AutoDev Coding Agent`);
      console.log(`📦 Provider: ${activeConfig.provider}`);
      console.log(`🤖 Model: ${activeConfig.model}\n`);
    }

    // Create and run coding agent
    const agent = new CodingAgentService(resolvedPath, activeConfig, quiet);
    const result = await agent.executeTask({
      requirement: task,
      projectPath: resolvedPath,
    });

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
    .version('0.1.1');

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
    .option('-m, --max-iterations <number>', 'Maximum iterations', '10')
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

