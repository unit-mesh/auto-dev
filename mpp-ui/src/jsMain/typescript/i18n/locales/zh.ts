/**
 * Chinese Translations (简体中文)
 */

import type { TranslationKeys } from '../types.js';

export const zh: TranslationKeys = {
  common: {
    save: '保存',
    cancel: '取消',
    back: '返回',
    confirm: '确认',
    error: '错误',
    success: '成功',
    loading: '加载中',
    yes: '是',
    no: '否',
    continue: '继续',
    exit: '退出',
  },
  
  welcome: {
    title: '🚀 欢迎使用 AutoDev CLI！',
    subtitle: '让我们配置您的 AI 设置。您可以稍后在 ~/.autodev/config.yaml 中添加更多配置',
    configPrompt: '配置您的 LLM 模型以开始使用',
    exitHint: '按 Ctrl+C 退出',
  },
  
  modelConfig: {
    title: '🤖 配置 LLM 模型',
    stepInfo: '步骤 1/2',
    nextStepInfo: '您将在下一步中为此配置命名',
    selectProvider: '选择您的 LLM 提供商：',
    enterModel: '输入模型名称',
    defaultHint: '默认：{{default}}',
    enterApiKey: '输入您的 API Key：',
    enterBaseUrl: '输入自定义 Base URL（可选）：',
    customBaseUrl: '输入自定义 Base URL',
    ollamaUrl: '输入 Ollama 服务器 URL：',
    leaveEmpty: '留空使用默认值',
    backHint: '按 Ctrl+B 返回',
    summary: '配置摘要：',
    nameConfig: '💾 为配置命名',
    namePrompt: '为此配置起一个名字（例如："工作"、"个人"、"gpt4"）：',
    nameHint: '按 Enter 保存',
    providers: {
      openai: '🔹 OpenAI (GPT-4, GPT-3.5)',
      anthropic: '🔹 Anthropic (Claude)',
      google: '🔹 Google (Gemini)',
      deepseek: '🔹 DeepSeek',
      ollama: '🔹 Ollama (本地)',
      openrouter: '🔹 OpenRouter',
    },
    fields: {
      provider: '提供商',
      model: '模型',
      apiKey: 'API Key',
      baseUrl: 'Base URL',
    },
  },
  
  chat: {
    title: '🤖 AutoDev CLI - AI 编程助手',
    emptyHint: '💬 输入您的消息开始编程',
    startHint: '💡 尝试 /help 或 @code 开始使用',
    inputPlaceholder: '输入您的消息...（或输入 /help 查看命令）',
    exitHint: '按 Ctrl+C 退出',
    helpHint: '输入 /help 查看命令',
    prefixes: {
      you: '👤 您',
      ai: '🤖 AI',
      system: 'ℹ️  系统',
    },
  },
  
  commands: {
    help: {
      description: '显示帮助信息',
    },
    clear: {
      description: '清空聊天历史',
      success: '✓ 聊天历史已清空',
    },
    exit: {
      description: '退出应用程序',
    },
    config: {
      description: '显示配置信息',
      output: '📋 配置信息：\n  • 模型：{{model}}\n  • 输入 /help 查看更多命令',
    },
    model: {
      description: '切换 AI 模型',
      available: '可用模型：{{models}}',
      current: '当前：{{model}}',
      usage: '用法：/model <模型名>',
    },
    init: {
      description: '初始化项目域字典',
      starting: '🚀 开始生成域字典...',
      analyzing: '📊 正在分析项目代码...',
      generating: '🤖 正在使用 AI 生成域字典...',
      saving: '💾 正在保存域字典到 prompts/domain.csv...',
      success: '✅ 域字典生成成功！文件已保存到 prompts/domain.csv',
      error: '❌ 域字典生成失败：{{error}}',
      noCode: '⚠️  未找到可分析的代码文件',
      usage: '用法：/init [--force] - 生成项目域字典',
    },
    unknown: '未知命令：{{command}}。输入 /help 查看可用命令。',
    usage: '需要命令名称。用法：/command [参数]',
    executionError: '命令执行失败：{{error}}',
  },
  
  messages: {
    configSaving: '⏳ 正在保存配置...',
    configSaved: '✓ 配置已保存！',
    configLoadError: '加载配置失败：{{error}}',
    configSaveError: '保存配置失败：{{error}}',
    starting: '正在启动 AutoDev CLI...',
    goodbye: '👋 再见！祝编程愉快！',
    inputError: '处理输入时出错：{{error}}',
    compilationError: 'DevIns 编译错误：{{error}}',
  },
};



