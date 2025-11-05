/**
 * i18n Type Definitions
 * 
 * Defines types for internationalization support
 */

export type SupportedLanguage = 'en' | 'zh';

export interface TranslationKeys {
  common: {
    save: string;
    cancel: string;
    back: string;
    confirm: string;
    error: string;
    success: string;
    loading: string;
    yes: string;
    no: string;
    continue: string;
    exit: string;
  };
  
  welcome: {
    title: string;
    subtitle: string;
    configPrompt: string;
    exitHint: string;
  };
  
  modelConfig: {
    title: string;
    stepInfo: string;
    nextStepInfo: string;
    selectProvider: string;
    enterModel: string;
    defaultHint: string;
    enterApiKey: string;
    enterBaseUrl: string;
    customBaseUrl: string;
    ollamaUrl: string;
    leaveEmpty: string;
    backHint: string;
    summary: string;
    nameConfig: string;
    namePrompt: string;
    nameHint: string;
    providers: {
      openai: string;
      anthropic: string;
      google: string;
      deepseek: string;
      ollama: string;
      openrouter: string;
    };
    fields: {
      provider: string;
      model: string;
      apiKey: string;
      baseUrl: string;
    };
  };
  
  chat: {
    title: string;
    emptyHint: string;
    startHint: string;
    inputPlaceholder: string;
    exitHint: string;
    helpHint: string;
    prefixes: {
      you: string;
      ai: string;
      system: string;
    };
  };
  
  commands: {
    help: {
      description: string;
    };
    clear: {
      description: string;
      success: string;
    };
    exit: {
      description: string;
    };
    config: {
      description: string;
      output: string;
    };
    model: {
      description: string;
      available: string;
      current: string;
      usage: string;
    };
    init: {
      description: string;
      starting: string;
      analyzing: string;
      generating: string;
      saving: string;
      success: string;
      error: string;
      noCode: string;
      usage: string;
    };
    unknown: string;
    usage: string;
    executionError: string;
  };
  
  messages: {
    configSaving: string;
    configSaved: string;
    configLoadError: string;
    configSaveError: string;
    starting: string;
    goodbye: string;
    inputError: string;
    compilationError: string;
  };
}

export type TranslationFunction = (key: string, params?: Record<string, any>) => string;

