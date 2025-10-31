// Kotlin exports will be available after build
// Import types from Kotlin/JS output
export type JsCompletionItem = {
  text: string;
  displayText: string;
  description: string | null;
  icon: string | null;
  triggerType: string;
  index: number;
};

export type JsInsertResult = {
  newText: string;
  newCursorPosition: number;
  shouldTriggerNextCompletion: boolean;
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
      // @ts-ignore - Runtime import, path is correct after build
      const mppCore = await import('@autodev/mpp-core/autodev-mpp-core.js');
      const exports = mppCore['module.exports'] || mppCore.default || mppCore;
      if (exports?.cc?.unitmesh?.llm?.JsCompletionManager) {
        completionManager = new exports.cc.unitmesh.llm.JsCompletionManager();
        console.log('✅ CompletionManager initialized');
        
        // Initialize workspace with current directory
        try {
          const workspacePath = process.cwd();
          const success = await completionManager.initWorkspace(workspacePath);
          if (success) {
            console.log(`✅ Workspace initialized: ${workspacePath}`);
          } else {
            console.warn('⚠️  Workspace initialization failed');
          }
        } catch (error) {
          console.warn('⚠️  Failed to initialize workspace:', error);
        }
      } else {
        console.error('❌ JsCompletionManager not found in exports');
      }
    } catch (error) {
      console.error('❌ Failed to initialize CompletionManager:', error);
      console.log('💡 Make sure to build mpp-core first: npm run build:kotlin');
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
 * Apply a completion item using the Kotlin insert handler
 * This properly handles special cases like adding ":" after commands
 */
export async function applyCompletionItem(
  text: string, 
  cursorPosition: number, 
  completionIndex: number
): Promise<JsInsertResult | null> {
  const manager = await initCompletionManager();
  if (!manager) return null;
  
  try {
    const result = manager.applyCompletion(text, cursorPosition, completionIndex);
    return result || null;
  } catch (error) {
    console.error('❌ Error applying completion:', error);
    return null;
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
