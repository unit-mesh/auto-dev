/**
 * WebSocket polyfill for browser
 * The 'ws' package is a Node.js WebSocket library used by ktor-client.
 * In browsers, we use the native WebSocket API instead.
 */

// Export browser's native WebSocket as the default
export default typeof window !== 'undefined' ? window.WebSocket : class {};

// Also provide named export for compatibility
export const WebSocket = typeof window !== 'undefined' ? window.WebSocket : class {};

