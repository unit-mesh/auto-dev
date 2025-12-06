/**
 * NanoUI Action Handler for React/TypeScript
 * 
 * Handles NanoUI actions in a React environment.
 * Provides platform-specific implementations for navigation, toast, and fetch.
 * 
 * @see xuiper-ui/src/main/kotlin/cc/unitmesh/xuiper/action/NanoActionHandler.kt
 */

// Types are defined locally to avoid circular dependencies

// ============================================================================
// Action Types
// ============================================================================

export type NanoAction = 
  | { type: 'StateMutation'; path: string; operation: MutationOp; value: any }
  | { type: 'Navigate'; to: string }
  | { type: 'Fetch'; url: string; method?: string; body?: Record<string, any>; headers?: Record<string, string> }
  | { type: 'ShowToast'; message: string }
  | { type: 'Sequence'; actions: NanoAction[] }
  | { type: 'Custom'; name: string; payload: Record<string, any> };

export type MutationOp = 'SET' | 'ADD' | 'SUBTRACT' | 'APPEND' | 'REMOVE';

export type ActionResult = 
  | { success: true; value?: any }
  | { success: false; error: string };

// ============================================================================
// Action Context
// ============================================================================

export interface NanoActionContext {
  get(path: string): any;
  set(path: string, value: any): void;
  mutate(path: string, operation: MutationOp, value: any): void;
  getState(): Record<string, any>;
}

// ============================================================================
// Action Handler Interface
// ============================================================================

export interface NanoActionHandler {
  handleAction(action: NanoAction, context: NanoActionContext): Promise<ActionResult>;
  handleStateMutation(action: Extract<NanoAction, { type: 'StateMutation' }>, context: NanoActionContext): ActionResult;
  handleNavigate(action: Extract<NanoAction, { type: 'Navigate' }>, context: NanoActionContext): ActionResult;
  handleFetch(action: Extract<NanoAction, { type: 'Fetch' }>, context: NanoActionContext): Promise<ActionResult>;
  handleShowToast(action: Extract<NanoAction, { type: 'ShowToast' }>, context: NanoActionContext): ActionResult;
  handleSequence(action: Extract<NanoAction, { type: 'Sequence' }>, context: NanoActionContext): Promise<ActionResult>;
  handleCustomAction(name: string, payload: Record<string, any>, context: NanoActionContext): Promise<ActionResult>;
}

export type CustomActionHandler = (
  payload: Record<string, any>, 
  context: NanoActionContext
) => Promise<ActionResult> | ActionResult;

// ============================================================================
// React Action Handler Implementation
// ============================================================================

export interface ReactActionHandlerOptions {
  onNavigate?: (to: string) => void;
  onToast?: (message: string) => void;
  onFetchComplete?: (url: string, success: boolean, data?: any) => void;
  customActions?: Record<string, CustomActionHandler>;
}

export class ReactActionHandler implements NanoActionHandler {
  private customActions: Record<string, CustomActionHandler> = {};
  private options: ReactActionHandlerOptions;

  constructor(options: ReactActionHandlerOptions = {}) {
    this.options = options;
    if (options.customActions) {
      this.customActions = { ...options.customActions };
    }
  }

  registerCustomAction(name: string, handler: CustomActionHandler): void {
    this.customActions[name] = handler;
  }

  async handleAction(action: NanoAction, context: NanoActionContext): Promise<ActionResult> {
    switch (action.type) {
      case 'StateMutation':
        return this.handleStateMutation(action, context);
      case 'Navigate':
        return this.handleNavigate(action, context);
      case 'Fetch':
        return this.handleFetch(action, context);
      case 'ShowToast':
        return this.handleShowToast(action, context);
      case 'Sequence':
        return this.handleSequence(action, context);
      case 'Custom':
        return this.handleCustomAction(action.name, action.payload, context);
      default:
        return { success: false, error: `Unknown action type: ${(action as any).type}` };
    }
  }

  handleStateMutation(
    action: Extract<NanoAction, { type: 'StateMutation' }>, 
    context: NanoActionContext
  ): ActionResult {
    try {
      context.mutate(action.path, action.operation, action.value);
      return { success: true };
    } catch (e) {
      return { success: false, error: `State mutation failed: ${e}` };
    }
  }

  handleNavigate(
    action: Extract<NanoAction, { type: 'Navigate' }>,
    _context: NanoActionContext
  ): ActionResult {
    try {
      if (this.options.onNavigate) {
        this.options.onNavigate(action.to);
      } else {
        // Default: use window.location for external URLs
        if (action.to.startsWith('http://') || action.to.startsWith('https://')) {
          window.open(action.to, '_blank');
        } else {
          window.location.href = action.to;
        }
      }
      return { success: true };
    } catch (e) {
      return { success: false, error: `Navigation failed: ${e}` };
    }
  }

  async handleFetch(
    action: Extract<NanoAction, { type: 'Fetch' }>,
    _context: NanoActionContext
  ): Promise<ActionResult> {
    try {
      const response = await fetch(action.url, {
        method: action.method || 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...action.headers,
        },
        body: action.body ? JSON.stringify(action.body) : undefined,
      });

      const data = await response.json().catch(() => response.text());

      if (response.ok) {
        this.options.onFetchComplete?.(action.url, true, data);
        return { success: true, value: data };
      } else {
        const error = `HTTP ${response.status}: ${data}`;
        this.options.onFetchComplete?.(action.url, false, error);
        return { success: false, error };
      }
    } catch (e) {
      const error = `Fetch failed: ${e}`;
      this.options.onFetchComplete?.(action.url, false, error);
      return { success: false, error };
    }
  }

  handleShowToast(
    action: Extract<NanoAction, { type: 'ShowToast' }>,
    _context: NanoActionContext
  ): ActionResult {
    try {
      if (this.options.onToast) {
        this.options.onToast(action.message);
      } else {
        // Default: use alert (not ideal but works)
        alert(action.message);
      }
      return { success: true };
    } catch (e) {
      return { success: false, error: `Toast failed: ${e}` };
    }
  }

  async handleSequence(
    action: Extract<NanoAction, { type: 'Sequence' }>,
    context: NanoActionContext
  ): Promise<ActionResult> {
    for (const subAction of action.actions) {
      const result = await this.handleAction(subAction, context);
      if (!result.success) {
        return result;
      }
    }
    return { success: true };
  }

  async handleCustomAction(
    name: string,
    payload: Record<string, any>,
    context: NanoActionContext
  ): Promise<ActionResult> {
    const handler = this.customActions[name];
    if (!handler) {
      return { success: false, error: `Unknown custom action: ${name}` };
    }

    try {
      const result = await handler(payload, context);
      return result;
    } catch (e) {
      return { success: false, error: `Custom action '${name}' failed: ${e}` };
    }
  }
}

// ============================================================================
// State Context Implementation
// ============================================================================

export class ReactStateContext implements NanoActionContext {
  private state: Record<string, any>;
  private setState: React.Dispatch<React.SetStateAction<Record<string, any>>>;

  constructor(
    state: Record<string, any>,
    setState: React.Dispatch<React.SetStateAction<Record<string, any>>>
  ) {
    this.state = state;
    this.setState = setState;
  }

  get(path: string): any {
    const parts = path.split('.');
    let current: any = this.state;

    for (const part of parts) {
      if (current == null) return undefined;
      current = current[part];
    }

    return current;
  }

  set(path: string, value: any): void {
    this.setState(prev => {
      const newState = { ...prev };
      this.setNestedValue(newState, path.split('.'), value);
      return newState;
    });
  }

  mutate(path: string, operation: MutationOp, value: any): void {
    const currentValue = this.get(path);
    let newValue: any;

    switch (operation) {
      case 'SET':
        newValue = value;
        break;
      case 'ADD':
        if (typeof currentValue === 'number' && typeof value === 'number') {
          newValue = currentValue + value;
        } else if (typeof currentValue === 'string') {
          newValue = currentValue + String(value);
        } else {
          newValue = value;
        }
        break;
      case 'SUBTRACT':
        if (typeof currentValue === 'number' && typeof value === 'number') {
          newValue = currentValue - value;
        } else {
          newValue = currentValue;
        }
        break;
      case 'APPEND':
        if (Array.isArray(currentValue)) {
          newValue = [...currentValue, value];
        } else {
          newValue = [value];
        }
        break;
      case 'REMOVE':
        if (Array.isArray(currentValue)) {
          newValue = currentValue.filter(item => item !== value);
        } else {
          newValue = currentValue;
        }
        break;
      default:
        newValue = value;
    }

    this.set(path, newValue);
  }

  getState(): Record<string, any> {
    return { ...this.state };
  }

  private setNestedValue(obj: Record<string, any>, parts: string[], value: any): void {
    if (parts.length === 0) return;

    const key = parts[0];

    if (parts.length === 1) {
      obj[key] = value;
    } else {
      if (!(key in obj) || typeof obj[key] !== 'object') {
        obj[key] = {};
      }
      obj[key] = { ...obj[key] };
      this.setNestedValue(obj[key], parts.slice(1), value);
    }
  }
}

// ============================================================================
// Helper: Parse NanoIR action to NanoAction
// ============================================================================

export function parseNanoAction(actionIR: { type: string; payload?: Record<string, any> }): NanoAction | null {
  switch (actionIR.type) {
    case 'StateMutation':
      return {
        type: 'StateMutation',
        path: actionIR.payload?.path ?? '',
        operation: (actionIR.payload?.operation ?? 'SET') as MutationOp,
        value: actionIR.payload?.value,
      };
    case 'Navigate':
      return { type: 'Navigate', to: actionIR.payload?.to ?? '' };
    case 'Fetch':
      return {
        type: 'Fetch',
        url: actionIR.payload?.url ?? '',
        method: actionIR.payload?.method,
        body: actionIR.payload?.body,
        headers: actionIR.payload?.headers,
      };
    case 'ShowToast':
      return { type: 'ShowToast', message: actionIR.payload?.message ?? '' };
    default:
      // Treat as custom action
      return { type: 'Custom', name: actionIR.type, payload: actionIR.payload ?? {} };
  }
}

