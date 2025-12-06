/**
 * NanoIR Types for VSCode Webview
 * 
 * TypeScript types matching the Kotlin NanoIR data classes.
 * Used for rendering NanoUI components in the VSCode webview.
 * 
 * @see xuiper-ui/src/main/kotlin/cc/unitmesh/xuiper/ir/NanoIR.kt
 */

/**
 * State variable definition
 */
export interface NanoStateVarIR {
  type: string;
  defaultValue?: any;
}

/**
 * State definition in IR format
 */
export interface NanoStateIR {
  variables: Record<string, NanoStateVarIR>;
}

/**
 * Action definition in IR format
 */
export interface NanoActionIR {
  type: string;
  payload?: Record<string, any>;
}

/**
 * Binding definition in IR format
 */
export interface NanoBindingIR {
  /** "subscribe" for << or "twoWay" for := */
  mode: string;
  expression: string;
}

/**
 * Loop definition for list rendering
 */
export interface NanoLoopIR {
  variable: string;
  iterable: string;
}

/**
 * NanoIR - JSON Intermediate Representation
 * 
 * Platform-agnostic representation of NanoDSL components.
 * This is the format sent to renderers.
 */
export interface NanoIR {
  /** Component type (VStack, Card, Text, Button, etc.) */
  type: string;
  
  /** Component properties */
  props: Record<string, any>;
  
  /** Child components */
  children?: NanoIR[];
  
  /** State definitions (only for Component type) */
  state?: NanoStateIR;
  
  /** Actions associated with this component */
  actions?: Record<string, NanoActionIR>;
  
  /** Binding information */
  bindings?: Record<string, NanoBindingIR>;
  
  /** Condition for conditional rendering */
  condition?: string;
  
  /** Loop information for list rendering */
  loop?: NanoLoopIR;
}

/**
 * Render context for stateful rendering
 */
export interface NanoRenderContext {
  /** Current state values */
  state: Record<string, any>;
  
  /** Action dispatcher */
  dispatch?: (action: NanoRenderAction) => void;
  
  /** Theme configuration */
  theme: NanoTheme;
}

/**
 * Action dispatched during rendering
 */
export type NanoRenderAction =
  | { type: 'StateMutation'; path: string; value: any }
  | { type: 'Navigate'; to: string }
  | { type: 'Fetch'; url: string; method?: string }
  | { type: 'ShowToast'; message: string };

/**
 * Theme configuration for NanoUI
 */
export interface NanoTheme {
  spacing: NanoSpacingScale;
  colors: NanoColorScheme;
}

/**
 * Spacing scale following design tokens
 */
export interface NanoSpacingScale {
  xs: number;
  sm: number;
  md: number;
  lg: number;
  xl: number;
}

/**
 * Color scheme for NanoUI theming
 */
export interface NanoColorScheme {
  primary: string;
  secondary: string;
  background: string;
  surface: string;
  error: string;
  onPrimary: string;
  onSecondary: string;
  onBackground: string;
  onSurface: string;
  onError: string;
}

/**
 * Default theme configuration
 */
export const DEFAULT_THEME: NanoTheme = {
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
  },
  colors: {
    primary: '#6200EE',
    secondary: '#03DAC6',
    background: '#FFFFFF',
    surface: '#FFFFFF',
    error: '#B00020',
    onPrimary: '#FFFFFF',
    onSecondary: '#000000',
    onBackground: '#000000',
    onSurface: '#000000',
    onError: '#FFFFFF',
  },
};

