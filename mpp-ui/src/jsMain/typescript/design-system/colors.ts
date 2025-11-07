/**
 * AutoDev Design System - Color Palette
 * 
 * 基于色彩心理学设计的双主题（亮色/暗色）色彩系统
 * 
 * 设计原则：
 * 1. 主色（Intelligent Indigo）- 融合蓝色的稳定和紫色的创造力
 * 2. 辅色（Spark Cyan）- AI 的"火花"，用于关键操作
 * 3. 暗黑模式避免纯黑纯白，使用去饱和化颜色
 */

// ============================================================================
// Color Scales - 基础色阶
// ============================================================================

/**
 * Indigo Scale - 主色调（智能靛蓝）
 * 在暗黑模式下使用较浅、较低饱和度的变体
 */
export const indigo = {
  50: '#eef2ff',
  100: '#e0e7ff',
  200: '#c7d2fe',
  300: '#a5b4fc',  // 暗黑模式主色
  400: '#818cf8',  // 暗黑模式悬停
  500: '#6366f1',
  600: '#4f46e5',  // 亮色模式主色
  700: '#4338ca',  // 亮色模式悬停
  800: '#3730a3',
  900: '#312e81',
} as const;

/**
 * Cyan Scale - 辅助色（活力青色）
 * AI "火花"，用于强调和关键操作
 */
export const cyan = {
  50: '#ecfeff',
  100: '#cffafe',
  200: '#a5f3fc',
  300: '#67e8f9',
  400: '#22d3ee',  // 暗黑模式辅色
  500: '#06b6d4',  // 亮色模式辅色
  600: '#0891b2',
  700: '#0e7490',
  800: '#155e75',
  900: '#164e63',
} as const;

/**
 * Neutral Scale - 中性色（灰度）
 * 精心设计的 10 级灰度，用于界面层次
 */
export const neutral = {
  50: '#fafafa',   // 亮色模式背景
  100: '#f5f5f5',  // 暗黑模式主文本
  200: '#e5e5e5',  // 亮色模式边框
  300: '#d4d4d4',  // 暗黑模式辅文本
  400: '#a3a3a3',
  500: '#737373',
  600: '#525252',
  700: '#404040',  // 暗黑模式边框
  800: '#262626',  // 暗黑模式卡片
  900: '#171717',  // 暗黑模式背景（避免纯黑）
} as const;

/**
 * Semantic Colors - 语义化颜色
 */

// Success - 成功状态（绿色）
export const green = {
  50: '#f0fdf4',
  100: '#dcfce7',
  200: '#bbf7d0',
  300: '#86efac',  // 暗黑模式成功色
  400: '#4ade80',
  500: '#22c55e',
  600: '#16a34a',  // 亮色模式成功色
  700: '#15803d',
  800: '#166534',
  900: '#14532d',
} as const;

// Warning - 警告状态（琥珀色）
export const amber = {
  50: '#fffbeb',
  100: '#fef3c7',
  200: '#fde68a',
  300: '#fcd34d',  // 暗黑模式警告色
  400: '#fbbf24',
  500: '#f59e0b',  // 亮色模式警告色
  600: '#d97706',
  700: '#b45309',
  800: '#92400e',
  900: '#78350f',
} as const;

// Error/Danger - 错误状态（红色）
export const red = {
  50: '#fef2f2',
  100: '#fee2e2',
  200: '#fecaca',
  300: '#fca5a5',  // 暗黑模式错误色
  400: '#f87171',
  500: '#ef4444',
  600: '#dc2626',  // 亮色模式错误色
  700: '#b91c1c',
  800: '#991b1b',
  900: '#7f1d1d',
} as const;

// Info - 信息状态（蓝色）
export const blue = {
  50: '#eff6ff',
  100: '#dbeafe',
  200: '#bfdbfe',
  300: '#93c5fd',  // 暗黑模式信息色
  400: '#60a5fa',
  500: '#3b82f6',  // 亮色模式信息色
  600: '#2563eb',
  700: '#1d4ed8',
  800: '#1e40af',
  900: '#1e3a8a',
} as const;

// ============================================================================
// Theme Modes - 主题模式
// ============================================================================

/**
 * 亮色模式色彩令牌
 */
export const lightTheme = {
  // Primary Colors
  primary: indigo[600],
  primaryHover: indigo[700],
  primaryActive: indigo[800],
  
  // Accent Colors (AI Spark)
  accent: cyan[500],
  accentHover: cyan[600],
  
  // Text Colors
  textPrimary: neutral[900],
  textSecondary: neutral[700],
  textTertiary: neutral[500],
  textInverse: neutral[50],
  
  // Surface Colors
  surfaceBg: neutral[50],
  surfaceCard: '#ffffff',
  surfaceHover: neutral[100],
  surfaceActive: neutral[200],
  
  // Border Colors
  border: neutral[200],
  borderHover: neutral[300],
  borderFocus: indigo[600],
  
  // Semantic Colors
  success: green[600],
  successLight: green[100],
  warning: amber[500],
  warningLight: amber[100],
  error: red[600],
  errorLight: red[100],
  info: blue[500],
  infoLight: blue[100],
} as const;

/**
 * 暗色模式色彩令牌
 * 注意：所有颜色都经过去饱和处理，避免视觉振动
 */
export const darkTheme = {
  // Primary Colors (去饱和，更浅的色调)
  primary: indigo[300],
  primaryHover: indigo[400],
  primaryActive: indigo[500],
  
  // Accent Colors (AI Spark)
  accent: cyan[400],
  accentHover: cyan[500],
  
  // Text Colors (避免纯白)
  textPrimary: neutral[100],
  textSecondary: neutral[300],
  textTertiary: neutral[500],
  textInverse: neutral[900],
  
  // Surface Colors (避免纯黑)
  surfaceBg: neutral[900],
  surfaceCard: neutral[800],
  surfaceHover: neutral[700],
  surfaceActive: neutral[600],
  
  // Border Colors
  border: neutral[700],
  borderHover: neutral[600],
  borderFocus: indigo[300],
  
  // Semantic Colors (去饱和)
  success: green[300],
  successLight: green[900],
  warning: amber[300],
  warningLight: amber[900],
  error: red[300],
  errorLight: red[900],
  info: blue[300],
  infoLight: blue[900],
} as const;

// ============================================================================
// Theme Type Definitions
// ============================================================================

export type ThemeMode = 'light' | 'dark';
export type ColorTheme = {
  readonly primary: string;
  readonly primaryHover: string;
  readonly primaryActive: string;
  readonly accent: string;
  readonly accentHover: string;
  readonly textPrimary: string;
  readonly textSecondary: string;
  readonly textTertiary: string;
  readonly textInverse: string;
  readonly surfaceBg: string;
  readonly surfaceCard: string;
  readonly surfaceHover: string;
  readonly surfaceActive: string;
  readonly border: string;
  readonly borderHover: string;
  readonly borderFocus: string;
  readonly success: string;
  readonly successLight: string;
  readonly warning: string;
  readonly warningLight: string;
  readonly error: string;
  readonly errorLight: string;
  readonly info: string;
  readonly infoLight: string;
};

// ============================================================================
// Theme Context and Utilities
// ============================================================================

/**
 * 获取当前主题
 * 默认为暗黑模式（开发者首选）
 */
export function getTheme(mode: ThemeMode = 'dark'): ColorTheme {
  return mode === 'light' ? lightTheme : darkTheme;
}

/**
 * CLI 颜色映射（用于 Ink Text 组件）
 * 将设计令牌映射到 Ink 的颜色名称
 */
export const inkColorMap = {
  primary: 'blue' as const,      // Indigo 在终端中显示为 blue
  accent: 'cyan' as const,
  success: 'green' as const,
  warning: 'yellow' as const,
  error: 'red' as const,
  info: 'blue' as const,
  muted: 'gray' as const,
} as const;

/**
 * Chalk 颜色映射（用于终端输出）
 */
export const chalkColorMap = {
  primary: 'blue',
  accent: 'cyan',
  success: 'green',
  warning: 'yellow',
  error: 'red',
  info: 'blue',
  muted: 'gray',
} as const;

// ============================================================================
// Exports
// ============================================================================

export const colors = {
  // Color scales
  indigo,
  cyan,
  neutral,
  green,
  amber,
  red,
  blue,
  
  // Themes
  light: lightTheme,
  dark: darkTheme,
  
  // Utilities
  getTheme,
  inkColorMap,
  chalkColorMap,
} as const;

export default colors;

