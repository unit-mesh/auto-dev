import { useCallback, useState } from 'react';

// VSCode API type
interface VSCodeAPI {
  postMessage: (message: unknown) => void;
  getState: () => unknown;
  setState: (state: unknown) => void;
}

// Declare the global acquireVsCodeApi function
declare function acquireVsCodeApi(): VSCodeAPI;

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
    // Error and control
    | 'error'
    | 'historyCleared';
  content?: string;
  data?: Record<string, unknown>;
}

// Message types to extension
export interface WebviewMessage {
  type: 'sendMessage' | 'clearHistory' | 'action' | 'openConfig';
  content?: string;
  action?: string;
  data?: Record<string, unknown>;
}

// Singleton VSCode API instance
let vscodeApi: VSCodeAPI | null = null;

function getVSCodeAPI(): VSCodeAPI | null {
  if (vscodeApi) return vscodeApi;
  
  try {
    vscodeApi = acquireVsCodeApi();
    return vscodeApi;
  } catch {
    // Running outside VSCode (e.g., in browser for development)
    console.warn('VSCode API not available, running in standalone mode');
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
    } else {
      console.log('Would send to VSCode:', message);
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

