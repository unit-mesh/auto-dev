/**
 * Timeline types - mirrors mpp-core's TimelineItem structure
 */

export type MessageRole = 'user' | 'assistant' | 'system';

export interface Message {
  role: MessageRole;
  content: string;
  timestamp?: number;
}

export interface ToolCallInfo {
  toolName: string;
  description: string;
  params: string;
  fullParams?: string;
  filePath?: string;
  toolType?: string;
  success?: boolean | null;
  summary?: string;
  output?: string;
  fullOutput?: string;
  executionTimeMs?: number;
}

export interface TerminalOutput {
  command: string;
  output: string;
  exitCode: number;
  executionTimeMs: number;
}

export interface TaskInfo {
  taskName: string;
  status: 'WORKING' | 'COMPLETED' | 'CANCELLED' | 'BLOCKED';
  summary: string;
  timestamp?: number;
}

// Timeline item types
export type TimelineItemType = 
  | 'message'
  | 'tool_call'
  | 'terminal_output'
  | 'task_complete'
  | 'error';

export interface BaseTimelineItem {
  type: TimelineItemType;
  timestamp: number;
}

export interface MessageTimelineItem extends BaseTimelineItem {
  type: 'message';
  message: Message;
  isStreaming?: boolean;
}

export interface ToolCallTimelineItem extends BaseTimelineItem {
  type: 'tool_call';
  toolCall: ToolCallInfo;
}

export interface TerminalTimelineItem extends BaseTimelineItem {
  type: 'terminal_output';
  terminal: TerminalOutput;
}

export interface TaskCompleteTimelineItem extends BaseTimelineItem {
  type: 'task_complete';
  success: boolean;
  message: string;
}

export interface ErrorTimelineItem extends BaseTimelineItem {
  type: 'error';
  message: string;
}

export type TimelineItem = 
  | MessageTimelineItem
  | ToolCallTimelineItem
  | TerminalTimelineItem
  | TaskCompleteTimelineItem
  | ErrorTimelineItem;

// Agent state
export interface AgentState {
  timeline: TimelineItem[];
  currentStreamingContent: string;
  isProcessing: boolean;
  currentIteration: number;
  maxIterations: number;
  tasks: TaskInfo[];
  errorMessage?: string;
}

