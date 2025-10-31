// Kotlin exports will be available after build
// Import types from Kotlin/JS output
type JsCompletionItem = {
  text: string;
  displayText: string;
  description: string | null;
  icon: string | null;
  triggerType: string;
};

/**
 * Completion manager instance (singleton)
 */
let completionManager: any = null;

/**
 * Initialize the completion manager
 * Will be loaded from Kotlin/JS build output
 */
export async function initCompletionManager() {
  if (!completionManager) {
    try {
      // Dynamic import from build output
      const mppCore = await import('../../../../mpp-core/build/dist/js/productionLibrary/mpp-core.mjs');
      completionManager = new mppCore.JsCompletionManager();
      console.log('✅ CompletionManager initialized');
    } catch (error) {
      console.error('❌ Failed to initialize CompletionManager:', error);
      console.log('💡 Make sure to build mpp-core first: ./gradlew :mpp-core:jsProductionLibraryCompileSync');
    }
  }
  return completionManager;
}

/**
 * Get completion suggestions from Kotlin CompletionManager
 */
export async function getCompletionSuggestions(text: string, cursorPosition: number): Promise<JsCompletionItem[]> {
  const manager = await initCompletionManager();
  if (!manager) return [];
  
  try {
    const items = manager.getCompletions(text, cursorPosition);
    return Array.from(items || []);
  } catch (error) {
    console.error('❌ Error getting completions:', error);
    return [];
  }
}

/**
 * Check if a character should trigger completion
 */
export async function shouldTriggerCompletion(char: string): Promise<boolean> {
  const manager = await initCompletionManager();
  if (!manager) return false;
  
  try {
    return manager.shouldTrigger(char);
  } catch (error) {
    return false;
  }
}

/**
 * Get the trigger type from a character
 */
export function getTriggerType(char: string): string | null {
  switch (char) {
    case '@': return 'AGENT';
    case '/': return 'COMMAND';
    case '$': return 'VARIABLE';
    case ':': return 'COMMAND_VALUE';
    default: return null;
  }
}

/**
 * Format completion item for display
 */
export function formatCompletionItem(item: JsCompletionItem): string {
  const icon = item.icon || '';
  const display = item.displayText || item.text;
  const desc = item.description ? ` - ${item.description}` : '';
  return `${icon} ${display}${desc}`.trim();
}

// Legacy functions (kept for backward compatibility but delegate to Kotlin)

export function isAtCommand(text: string): boolean {
  return text.trim().startsWith('@');
}

export function isSlashCommand(text: string): boolean {
  return text.trim().startsWith('/');
}

export function extractCommand(text: string): string {
  const trimmed = text.trim();
  if (trimmed.startsWith('@') || trimmed.startsWith('/')) {
    return trimmed.substring(1).split(/\s+/)[0];
  }
  return '';
}
