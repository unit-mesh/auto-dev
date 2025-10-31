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

export type JsDevInsResult = {
  success: boolean;
  output: string;
  errorMessage: string | null;
  hasCommand: boolean;
};

/**
 * Completion manager instance (singleton)
 */
let completionManager: any = null;

/**
 * DevIns compiler instance (singleton)
 */
let devinsCompiler: any = null;

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
 * Initialize the DevIns compiler
 * Will be loaded from Kotlin/JS build output
 */
export async function initDevInsCompiler() {
  if (!devinsCompiler) {
    try {
      // Dynamic import from build output
      // @ts-ignore - Runtime import, path is correct after build
      const mppCore = await import('@autodev/mpp-core/autodev-mpp-core.js');
      const exports = mppCore['module.exports'] || mppCore.default || mppCore;
      if (exports?.cc?.unitmesh?.llm?.JsDevInsCompiler) {
        devinsCompiler = new exports.cc.unitmesh.llm.JsDevInsCompiler();
        console.log('✅ DevInsCompiler initialized');
      } else {
        console.error('❌ JsDevInsCompiler not found in exports');
      }
    } catch (error) {
      console.error('❌ Failed to initialize DevInsCompiler:', error);
      console.log('💡 Make sure to build mpp-core first: npm run build:kotlin');
    }
  }
  return devinsCompiler;
}

/**
 * Compile DevIns source code
 * This processes commands like /read-file:path, @agent, $variable, etc.
 */
export async function compileDevIns(source: string, variables?: Record<string, any>): Promise<JsDevInsResult | null> {
  const compiler = await initDevInsCompiler();
  if (!compiler) return null;
  
  try {
    const result = await compiler.compile(source, variables);
    return result;
  } catch (error) {
    console.error('❌ Error compiling DevIns:', error);
    return {
      success: false,
      output: '',
      errorMessage: error instanceof Error ? error.message : 'Unknown error',
      hasCommand: false
    };
  }
}

/**
 * Check if text contains DevIns commands that need compilation
 * Returns true if text contains /command or @agent syntax
 */
export function hasDevInsCommands(text: string): boolean {
  // Check for /command: syntax (most common)
  if (/\/[a-z-]+:/i.test(text)) return true;
  
  // Check for @agent syntax
  if (/@[a-z-]+/i.test(text)) return true;
  
  // Check for $variable syntax
  if (/\$[a-z_][a-z0-9_]*/i.test(text)) return true;
  
  return false;
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
