/**
 * English Translations
 */

import type { TranslationKeys } from '../types.js';

export const en: TranslationKeys = {
  common: {
    save: 'Save',
    cancel: 'Cancel',
    back: 'Back',
    confirm: 'Confirm',
    error: 'Error',
    success: 'Success',
    loading: 'Loading',
    yes: 'Yes',
    no: 'No',
    continue: 'Continue',
    exit: 'Exit',
  },
  
  welcome: {
    title: '🚀 Welcome to AutoDev CLI!',
    subtitle: "Let's set up your AI configuration. You can add more later in ~/.autodev/config.yaml",
    configPrompt: 'Configure your LLM model to get started',
    exitHint: 'Press Ctrl+C to exit',
  },
  
  modelConfig: {
    title: '🤖 Configure LLM Model',
    stepInfo: 'Step 1/2',
    nextStepInfo: "You'll name this configuration in the next step",
    selectProvider: 'Select your LLM provider:',
    enterModel: 'Enter model name',
    defaultHint: 'default: {{default}}',
    enterApiKey: 'Enter your API key:',
    enterBaseUrl: 'Enter custom base URL (optional):',
    customBaseUrl: 'Enter custom base URL',
    ollamaUrl: 'Enter Ollama server URL:',
    leaveEmpty: 'Leave empty to use default',
    backHint: 'Press Ctrl+B to go back',
    summary: 'Configuration Summary:',
    nameConfig: '💾 Name Your Configuration',
    namePrompt: 'Give this configuration a name (e.g., "work", "personal", "gpt4"):',
    nameHint: 'Press Enter to save',
    providers: {
      openai: '🔹 OpenAI (GPT-4, GPT-3.5)',
      anthropic: '🔹 Anthropic (Claude)',
      google: '🔹 Google (Gemini)',
      deepseek: '🔹 DeepSeek',
      ollama: '🔹 Ollama (Local)',
      openrouter: '🔹 OpenRouter',
    },
    fields: {
      provider: 'Provider',
      model: 'Model',
      apiKey: 'API Key',
      baseUrl: 'Base URL',
    },
  },
  
  chat: {
    title: '🤖 AutoDev CLI - AI Coding Assistant',
    emptyHint: '💬 Type your message to start coding',
    startHint: '💡 Try /help or @code to get started',
    inputPlaceholder: 'Type your message... (or /help for commands)',
    exitHint: 'Press Ctrl+C to exit',
    helpHint: 'Type /help for commands',
    prefixes: {
      you: '👤 You',
      ai: '🤖 AI',
      system: 'ℹ️  System',
    },
  },
  
  commands: {
    help: {
      description: 'Show help information',
    },
    clear: {
      description: 'Clear chat history',
      success: '✓ Chat history cleared',
    },
    exit: {
      description: 'Exit the application',
    },
    config: {
      description: 'Show configuration',
      output: '📋 Configuration:\n  • Model: {{model}}\n  • Type /help for more commands',
    },
    model: {
      description: 'Change AI model',
      available: 'Available models: {{models}}',
      current: 'Current: {{model}}',
      usage: 'Usage: /model <name>',
    },
    init: {
      description: 'Initialize project domain dictionary',
      starting: '🚀 Starting domain dictionary generation...',
      analyzing: '📊 Analyzing project code...',
      generating: '🤖 Generating domain dictionary with AI...',
      saving: '💾 Saving domain dictionary to prompts/domain.csv...',
      success: '✅ Domain dictionary generated successfully! File saved to prompts/domain.csv',
      error: '❌ Domain dictionary generation failed: {{error}}',
      noCode: '⚠️  No analyzable code files found',
      usage: 'Usage: /init [--force] - Generate project domain dictionary',
    },
    unknown: 'Unknown command: {{command}}. Type /help for available commands.',
    usage: 'Command name is required. Usage: /command [args]',
    executionError: 'Command execution failed: {{error}}',
  },
  
  messages: {
    configSaving: '⏳ Saving configuration...',
    configSaved: '✓ Configuration saved!',
    configLoadError: 'Failed to load configuration: {{error}}',
    configSaveError: 'Failed to save configuration: {{error}}',
    starting: 'Starting AutoDev CLI...',
    goodbye: '👋 Goodbye! Happy coding!',
    inputError: 'Error processing input: {{error}}',
    compilationError: 'DevIns compilation error: {{error}}',
  },
};



