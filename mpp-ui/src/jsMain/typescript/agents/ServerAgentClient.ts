/**
 * ServerAgentClient - Client for connecting to mpp-server
 *
 * Provides both synchronous and streaming (SSE) execution modes
 */

import fetch from 'node-fetch';

export interface LLMConfig {
  provider: string;
  modelName: string;
  apiKey: string;
  baseUrl?: string;
}

export interface AgentRequest {
  projectId: string;
  task: string;
  llmConfig?: LLMConfig;
}

export interface AgentStepInfo {
  step: number;
  action: string;
  tool: string;
  success: boolean;
}

export interface AgentEditInfo {
  file: string;
  operation: string;
  content: string;
}

export interface AgentResponse {
  success: boolean;
  message: string;
  output?: string;
  iterations: number;
  steps: AgentStepInfo[];
  edits: AgentEditInfo[];
}

export type AgentEvent =
  | { type: 'iteration'; current: number; max: number }
  | { type: 'llm_chunk'; chunk: string }
  | { type: 'tool_call'; toolName: string; params: string }
  | { type: 'tool_result'; toolName: string; success: boolean; output?: string }
  | { type: 'error'; message: string }
  | { type: 'complete'; success: boolean; message: string; iterations: number; steps: AgentStepInfo[]; edits: AgentEditInfo[] };

export class ServerAgentClient {
  private baseUrl: string;

  constructor(baseUrl: string = 'http://localhost:8080') {
    this.baseUrl = baseUrl;
  }

  /**
   * Execute agent task synchronously
   */
  async executeSync(request: AgentRequest): Promise<AgentResponse> {
    const response = await fetch(`${this.baseUrl}/api/agent/run`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Server error: ${response.status} - ${error}`);
    }

    return await response.json() as AgentResponse;
  }

  /**
   * Execute agent task with SSE streaming
   */
  async *executeStream(request: AgentRequest): AsyncGenerator<AgentEvent> {
    const url = `${this.baseUrl}/api/agent/stream`;

    // Use fetch for SSE (EventSource doesn't support POST)
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Server error: ${response.status} - ${error}`);
    }

    if (!response.body) {
      throw new Error('Response body is null');
    }

    // Parse SSE stream
    const decoder = new TextDecoder();
    let buffer = '';

    // node-fetch returns a Node.js Readable stream
    for await (const chunk of response.body as any) {
      // Decode chunk and add to buffer
      buffer += decoder.decode(chunk, { stream: true });

      // Process complete events in buffer
      const lines = buffer.split('\n');
      buffer = lines.pop() || ''; // Keep incomplete line in buffer

      let currentEvent: { type?: string; data?: string } = {};

      for (const line of lines) {
        if (line.startsWith('event:')) {
          currentEvent.type = line.substring(6).trim();
        } else if (line.startsWith('data:')) {
          currentEvent.data = line.substring(5).trim();
        } else if (line.trim() === '' && currentEvent.type && currentEvent.data) {
          // Complete event - parse and yield
          const event = this.parseSSEEvent(currentEvent.type, currentEvent.data);
          if (event) {
            yield event;

            // Check if complete
            if (event.type === 'complete') {
              return;
            }
          }
          currentEvent = {};
        }
      }
    }
  }

  private parseSSEEvent(type: string, data: string): AgentEvent | null {
    try {
      const parsed = JSON.parse(data);

      switch (type) {
        case 'iteration':
          return { type: 'iteration', current: parsed.current, max: parsed.max };
        case 'llm_chunk':
          return { type: 'llm_chunk', chunk: parsed.chunk };
        case 'tool_call':
          return { type: 'tool_call', toolName: parsed.toolName, params: parsed.params };
        case 'tool_result':
          return { type: 'tool_result', toolName: parsed.toolName, success: parsed.success, output: parsed.output };
        case 'error':
          return { type: 'error', message: parsed.message };
        case 'complete':
          return {
            type: 'complete',
            success: parsed.success,
            message: parsed.message,
            iterations: parsed.iterations,
            steps: parsed.steps,
            edits: parsed.edits
          };
        default:
          console.warn(`Unknown SSE event type: ${type}`);
          return null;
      }
    } catch (e) {
      console.error(`Failed to parse SSE event: ${e}`);
      return null;
    }
  }

  /**
   * Get list of available projects
   */
  async getProjects(): Promise<{ id: string; name: string; path: string; description: string }[]> {
    const response = await fetch(`${this.baseUrl}/api/projects`);

    if (!response.ok) {
      throw new Error(`Failed to get projects: ${response.status}`);
    }

    const data = await response.json() as any;
    return data.projects || [];
  }

  /**
   * Health check
   */
  async healthCheck(): Promise<{ status: string }> {
    const response = await fetch(`${this.baseUrl}/health`);

    if (!response.ok) {
      throw new Error(`Health check failed: ${response.status}`);
    }

    return await response.json() as { status: string };
  }
}

