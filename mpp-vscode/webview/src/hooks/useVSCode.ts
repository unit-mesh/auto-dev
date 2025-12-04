import { useCallback, useState } from 'react';

// VSCode API type
interface VSCodeAPI {
  postMessage: (message: unknown) => void;
  getState: () => unknown;
  setState: (state: unknown) => void;
}

// Declare the global VSCode API on window (acquired in HTML before React loads)
declare global {
  interface Window {
    vscodeApi?: VSCodeAPI;
    acquireVsCodeApi?: () => VSCodeAPI;
  }
}

// Message types from extension
// Mirrors mpp-ui's ComposeRenderer events
export interface ExtensionMessage {
  type:
    // Message events
    | 'userMessage'
    | 'startResponse'
    | 'responseChunk'
    | 'endResponse'
    // Tool events
    | 'toolCall'
    | 'toolResult'
    // Terminal events
    | 'terminalOutput'
    // Task events
    | 'taskComplete'
    | 'iterationUpdate'
    // Config events
    | 'configUpdate'
    // Token events
    | 'tokenUpdate'
    // Prompt optimization events
    | 'promptOptimized'
    | 'promptOptimizeFailed'
    // File search events
    | 'searchFilesResult'
    | 'recentFilesResult'
    | 'fileContentResult'
    // Context events
    | 'activeFileChanged'
    // Completion events
    | 'completionsResult'
    | 'completionApplied'
    // Plan events
    | 'planUpdate'
    | 'planCleared'
    // Error and control
    | 'error'
    | 'historyCleared';
  content?: string;
  data?: Record<string, unknown>;
}

// Message types to extension
export interface WebviewMessage {
  type: 'sendMessage' | 'clearHistory' | 'action' | 'openConfig' | 'stopExecution' | 'selectConfig' | 'searchFiles' | 'getRecentFiles' | 'readFileContent' | 'requestConfig' | 'getActiveFile' | 'getCompletions' | 'applyCompletion';
  content?: string;
  action?: string;
  data?: Record<string, unknown>;
}

// Singleton VSCode API instance
let vscodeApi: VSCodeAPI | null = null;

function getVSCodeAPI(): VSCodeAPI | null {
  if (vscodeApi) return vscodeApi;

  // First, check if API was already acquired and stored on window (by HTML script)
  if (window.vscodeApi) {
    vscodeApi = window.vscodeApi;
    return vscodeApi;
  }

  // Fallback: try to acquire it ourselves (only works if not already acquired)
  try {
    const acquireFn = window.acquireVsCodeApi;
    if (typeof acquireFn === 'function') {
      vscodeApi = acquireFn();
      return vscodeApi;
    }
    return null;
  } catch {
    // Running outside VSCode (e.g., in browser for development)
    return null;
  }
}

/**
 * Hook for communicating with VSCode extension
 */
export function useVSCode() {
  const [api] = useState<VSCodeAPI | null>(() => getVSCodeAPI());

  const postMessage = useCallback((message: WebviewMessage) => {
    if (api) {
      api.postMessage(message);
    }
  }, [api]);

  const onMessage = useCallback((handler: (message: ExtensionMessage) => void) => {
    const listener = (event: MessageEvent<ExtensionMessage>) => {
      handler(event.data);
    };

    window.addEventListener('message', listener);
    return () => window.removeEventListener('message', listener);
  }, []);

  return { postMessage, onMessage, isVSCode: !!api };
}

/**
 * Hook for persisting state in VSCode
 */
export function useVSCodeState<T>(key: string, initialValue: T): [T, (value: T) => void] {
  const api = getVSCodeAPI();
  
  const [state, setState] = useState<T>(() => {
    if (api) {
      const savedState = api.getState() as Record<string, unknown> | null;
      if (savedState && key in savedState) {
        return savedState[key] as T;
      }
    }
    return initialValue;
  });

  const setStateAndPersist = useCallback((value: T) => {
    setState(value);
    if (api) {
      const currentState = (api.getState() as Record<string, unknown>) || {};
      api.setState({ ...currentState, [key]: value });
    }
  }, [api, key]);

  return [state, setStateAndPersist];
}

