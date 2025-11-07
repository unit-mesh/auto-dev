/**
 * Theme Helpers - 主题辅助工具
 * 
 * 提供便捷的方法来访问当前主题的颜色和工具函数
 */

import { inkColorMap, chalkColorMap } from './colors.js';
import chalk from 'chalk';

/**
 * Chalk 颜色辅助类
 * 使用设计系统中定义的语义化颜色
 */
export const semanticChalk = {
  // Primary and Accent
  primary: chalk.blue,
  accent: chalk.cyan,
  
  // Semantic colors
  success: chalk.green,
  warning: chalk.yellow,
  error: chalk.red,
  info: chalk.blue,
  
  // Text variants
  muted: chalk.gray,
  dim: chalk.dim,
  bold: chalk.bold,
  
  // Combined styles
  successBold: chalk.green.bold,
  errorBold: chalk.red.bold,
  warningBold: chalk.yellow.bold,
  infoBold: chalk.blue.bold,
  primaryBold: chalk.blue.bold,
  accentBold: chalk.cyan.bold,
} as const;

/**
 * Ink 颜色辅助 - 用于 <Text> 组件
 * 返回 Ink 支持的颜色字符串
 */
export const semanticInk = {
  primary: inkColorMap.primary,
  accent: inkColorMap.accent,
  success: inkColorMap.success,
  warning: inkColorMap.warning,
  error: inkColorMap.error,
  info: inkColorMap.info,
  muted: inkColorMap.muted,
} as const;

/**
 * 语义化颜色类型
 */
export type SemanticColor = 'primary' | 'accent' | 'success' | 'warning' | 'error' | 'info' | 'muted';

/**
 * 获取 Ink 颜色（用于 <Text color={...}> ）
 */
export function getInkColor(semantic: SemanticColor): string {
  return semanticInk[semantic];
}

/**
 * 获取 Chalk 颜色函数
 */
export function getChalkColor(semantic: SemanticColor): typeof chalk {
  return semanticChalk[semantic];
}

/**
 * 状态指示器
 */
export const statusIndicators = {
  success: '✓',
  error: '✗',
  warning: '⚠',
  info: 'ℹ',
  loading: '⏳',
  processing: '🔄',
} as const;

/**
 * 带颜色的状态指示器
 */
export function coloredStatus(status: keyof typeof statusIndicators, message: string): string {
  const indicator = statusIndicators[status];
  
  switch (status) {
    case 'success':
      return chalk.green(`${indicator} ${message}`);
    case 'error':
      return chalk.red(`${indicator} ${message}`);
    case 'warning':
      return chalk.yellow(`${indicator} ${message}`);
    case 'info':
      return chalk.blue(`${indicator} ${message}`);
    case 'loading':
    case 'processing':
      return chalk.cyan(`${indicator} ${message}`);
    default:
      return `${indicator} ${message}`;
  }
}

/**
 * 分隔线样式
 */
export const dividers = {
  solid: (length: number = 60) => chalk.gray('─'.repeat(length)),
  double: (length: number = 60) => chalk.gray('═'.repeat(length)),
  bold: (length: number = 60) => chalk.bold('─'.repeat(length)),
} as const;

/**
 * 高亮文本
 */
export function highlight(text: string, color: SemanticColor = 'accent'): string {
  const chalkFn = getChalkColor(color);
  return chalkFn.bold(text);
}

/**
 * 代码块样式
 */
export function codeBlock(code: string, language?: string): string {
  const header = language ? chalk.gray(`[${language}]`) : '';
  const lines = code.split('\n').map(line => chalk.gray('│ ') + line);
  
  return [
    header,
    chalk.gray('┌' + '─'.repeat(58) + '┐'),
    ...lines,
    chalk.gray('└' + '─'.repeat(58) + '┘')
  ].filter(Boolean).join('\n');
}

export default {
  semanticChalk,
  semanticInk,
  getInkColor,
  getChalkColor,
  statusIndicators,
  coloredStatus,
  dividers,
  highlight,
  codeBlock,
};

