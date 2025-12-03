/**
 * AutoDev VSCode Webview App
 *
 * Main application component using Timeline-based architecture
 * Mirrors mpp-ui's ComposeRenderer and AgentChatInterface
 */

import React, { useState, useEffect, useCallback } from 'react';
import { Timeline } from './components/Timeline';
import { ChatInput } from './components/ChatInput';
import { ModelConfig } from './components/ModelSelector';
import { useVSCode, ExtensionMessage } from './hooks/useVSCode';
import type { AgentState, ToolCallInfo, TerminalOutput, ToolCallTimelineItem } from './types/timeline';
import './App.css';

interface ConfigState {
  availableConfigs: ModelConfig[];
  currentConfigName: string | null;
}

const App: React.FC = () => {
  // Agent state - mirrors ComposeRenderer's state
  const [agentState, setAgentState] = useState<AgentState>({
    timeline: [],
    currentStreamingContent: '',
    isProcessing: false,
    currentIteration: 0,
    maxIterations: 10,
    tasks: [],
  });

  // Config state - mirrors IdeaAgentViewModel's config management
  const [configState, setConfigState] = useState<ConfigState>({
    availableConfigs: [],
    currentConfigName: null
  });

  const { postMessage, onMessage, isVSCode } = useVSCode();

  // Handle messages from extension
  const handleExtensionMessage = useCallback((msg: ExtensionMessage) => {
    switch (msg.type) {
      // User message
      case 'userMessage':
        setAgentState(prev => ({
          ...prev,
          timeline: [...prev.timeline, {
            type: 'message',
            timestamp: Date.now(),
            message: { role: 'user', content: msg.content || '' }
          }]
        }));
        break;

      // LLM response start
      case 'startResponse':
        setAgentState(prev => ({
          ...prev,
          isProcessing: true,
          currentStreamingContent: ''
        }));
        break;

      // LLM response chunk (streaming)
      case 'responseChunk':
        setAgentState(prev => ({
          ...prev,
          currentStreamingContent: prev.currentStreamingContent + (msg.content || '')
        }));
        break;

      // LLM response end
      case 'endResponse':
        setAgentState(prev => ({
          ...prev,
          isProcessing: false,
          timeline: [...prev.timeline, {
            type: 'message',
            timestamp: Date.now(),
            message: { role: 'assistant', content: prev.currentStreamingContent }
          }],
          currentStreamingContent: ''
        }));
        break;

      // Tool call
      case 'toolCall':
        if (msg.data) {
          const toolCall: ToolCallInfo = {
            toolName: (msg.data.toolName as string) || 'unknown',
            description: (msg.data.description as string) || '',
            params: (msg.data.params as string) || '',
            success: msg.data.success as boolean | null | undefined
          };
          setAgentState(prev => ({
            ...prev,
            timeline: [...prev.timeline, {
              type: 'tool_call' as const,
              timestamp: Date.now(),
              toolCall
            }]
          }));
        }
        break;

      // Tool result - update the last tool call
      case 'toolResult':
        if (msg.data) {
          setAgentState(prev => {
            const timeline = [...prev.timeline];
            // Find the last tool call and update it
            for (let i = timeline.length - 1; i >= 0; i--) {
              if (timeline[i].type === 'tool_call') {
                const item = timeline[i] as ToolCallTimelineItem;
                item.toolCall = {
                  ...item.toolCall,
                  success: msg.data?.success as boolean | undefined,
                  output: msg.data?.output as string | undefined,
                  summary: msg.data?.summary as string | undefined
                };
                break;
              }
            }
            return { ...prev, timeline };
          });
        }
        break;

      // Terminal output
      case 'terminalOutput':
        if (msg.data) {
          const terminal: TerminalOutput = {
            command: (msg.data.command as string) || '',
            output: (msg.data.output as string) || '',
            exitCode: (msg.data.exitCode as number) || 0,
            executionTimeMs: (msg.data.executionTimeMs as number) || 0
          };
          setAgentState(prev => ({
            ...prev,
            timeline: [...prev.timeline, {
              type: 'terminal_output' as const,
              timestamp: Date.now(),
              terminal
            }]
          }));
        }
        break;

      // Task complete
      case 'taskComplete':
        setAgentState(prev => ({
          ...prev,
          isProcessing: false,
          timeline: [...prev.timeline, {
            type: 'task_complete' as const,
            timestamp: Date.now(),
            success: Boolean(msg.data?.success ?? true),
            message: String(msg.data?.message || 'Task completed')
          }]
        }));
        break;

      // Error
      case 'error':
        setAgentState(prev => ({
          ...prev,
          isProcessing: false,
          currentStreamingContent: '',
          timeline: [...prev.timeline, {
            type: 'error',
            timestamp: Date.now(),
            message: msg.content || 'An error occurred'
          }],
          errorMessage: msg.content
        }));
        break;

      // Clear history
      case 'historyCleared':
        setAgentState({
          timeline: [],
          currentStreamingContent: '',
          isProcessing: false,
          currentIteration: 0,
          maxIterations: 10,
          tasks: [],
        });
        break;

      // Iteration update
      case 'iterationUpdate':
        setAgentState(prev => ({
          ...prev,
          currentIteration: Number(msg.data?.current) || prev.currentIteration,
          maxIterations: Number(msg.data?.max) || prev.maxIterations
        }));
        break;

      // Config update from extension
      case 'configUpdate':
        if (msg.data) {
          setConfigState({
            availableConfigs: (msg.data.availableConfigs as ModelConfig[]) || [],
            currentConfigName: (msg.data.currentConfigName as string) || null
          });
        }
        break;
    }
  }, []);

  // Subscribe to extension messages
  useEffect(() => {
    return onMessage(handleExtensionMessage);
  }, [onMessage, handleExtensionMessage]);

  // Send message to extension
  const handleSend = useCallback((content: string) => {
    postMessage({ type: 'sendMessage', content });
  }, [postMessage]);

  // Clear history
  const handleClear = useCallback(() => {
    postMessage({ type: 'clearHistory' });
  }, [postMessage]);

  // Handle actions from sketch renderers
  const handleAction = useCallback((action: string, data: any) => {
    postMessage({ type: 'action', action, data });
  }, [postMessage]);

  // Handle open config
  const handleOpenConfig = useCallback(() => {
    postMessage({ type: 'openConfig' });
  }, [postMessage]);

  // Handle stop execution
  const handleStop = useCallback(() => {
    postMessage({ type: 'stopExecution' });
  }, [postMessage]);

  // Handle config selection
  const handleConfigSelect = useCallback((config: ModelConfig) => {
    postMessage({ type: 'selectConfig', data: { configName: config.name } });
  }, [postMessage]);

  // Check if we need to show config prompt
  const needsConfig = agentState.timeline.length === 0 &&
    agentState.currentStreamingContent.includes('No configuration found') ||
    agentState.currentStreamingContent.includes('Configuration Required');

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-title">
          <span className="header-icon">✨</span>
          <span>AutoDev</span>
        </div>
        {agentState.isProcessing && (
          <div className="header-status">
            <span className="status-dot"></span>
            <span>Processing...</span>
            {agentState.currentIteration > 0 && (
              <span className="iteration-badge">
                {agentState.currentIteration}/{agentState.maxIterations}
              </span>
            )}
          </div>
        )}
        <button className="header-config-btn" onClick={handleOpenConfig} title="Open Config">
          ⚙️
        </button>
        {!isVSCode && (
          <span className="dev-badge">Dev Mode</span>
        )}
      </header>

      <div className="app-content">
        <Timeline
          items={agentState.timeline}
          currentStreamingContent={agentState.isProcessing ? agentState.currentStreamingContent : undefined}
          onAction={handleAction}
        />

        {/* Show Open Config button when config is needed */}
        {needsConfig && (
          <div className="config-prompt">
            <button className="config-btn" onClick={handleOpenConfig}>
              📝 Open Config File
            </button>
          </div>
        )}
      </div>

      <ChatInput
        onSend={handleSend}
        onClear={handleClear}
        onStop={handleStop}
        onConfigSelect={handleConfigSelect}
        onConfigureClick={handleOpenConfig}
        disabled={agentState.isProcessing}
        isExecuting={agentState.isProcessing}
        placeholder="Ask AutoDev anything... (use / for commands, @ for agents)"
        availableConfigs={configState.availableConfigs}
        currentConfigName={configState.currentConfigName}
      />
    </div>
  );
};

export default App;

