/**
 * NanoUI Renderer Components for VSCode Webview
 * 
 * Provides React components for rendering NanoIR to the VSCode webview.
 * 
 * @see xuiper-ui/src/main/kotlin/cc/unitmesh/xuiper/render/NanoRenderer.kt
 */

export { NanoRenderer } from './NanoRenderer';
export type { 
  NanoIR, 
  NanoRenderContext, 
  NanoRenderAction,
  NanoTheme,
  NanoSpacingScale,
  NanoColorScheme,
} from '../../types/nano';
export { DEFAULT_THEME } from '../../types/nano';

