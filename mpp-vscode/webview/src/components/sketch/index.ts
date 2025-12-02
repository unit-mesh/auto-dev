/**
 * Sketch Renderer Components
 * 
 * Mirrors mpp-ui's SketchRenderer architecture for VSCode Webview
 * Provides specialized renderers for different content types:
 * - SketchRenderer: Main dispatcher
 * - MarkdownRenderer: Markdown/text content
 * - CodeBlockRenderer: Code with syntax highlighting
 * - DiffRenderer: Diff/patch with accept/reject
 * - ThinkingRenderer: Collapsible thinking blocks
 * - TerminalRenderer: Terminal commands and output
 * - ToolCallRenderer: Tool call information
 */

export { SketchRenderer } from './SketchRenderer';
export { MarkdownRenderer } from './MarkdownRenderer';
export { CodeBlockRenderer } from './CodeBlockRenderer';
export { DiffRenderer } from './DiffRenderer';
export { ThinkingRenderer } from './ThinkingRenderer';
export { TerminalRenderer } from './TerminalRenderer';
export { ToolCallRenderer } from './ToolCallRenderer';

