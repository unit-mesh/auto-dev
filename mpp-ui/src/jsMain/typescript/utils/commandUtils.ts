/**
 * Command parsing and validation utilities
 */

/**
 * Checks if a query string represents an '@' command (agent invocation).
 * @param query The input query string.
 * @returns True if the query looks like an '@' command, false otherwise.
 */
export const isAtCommand = (query: string): boolean =>
  query.trim().startsWith('@') || /\s@\S/.test(query);

/**
 * Checks if a query string represents an '/' command.
 * @param query The input query string.
 * @returns True if the query looks like an '/' command, false otherwise.
 */
export const isSlashCommand = (query: string): boolean => {
  const trimmed = query.trim();
  
  if (!trimmed.startsWith('/')) {
    return false;
  }

  // Exclude line comments that start with '//'
  if (trimmed.startsWith('//')) {
    return false;
  }

  // Exclude block comments that start with '/*'
  if (trimmed.startsWith('/*')) {
    return false;
  }

  return true;
};

/**
 * Extract command name from a query
 */
export const extractCommand = (query: string): string | null => {
  const trimmed = query.trim();
  
  if (isSlashCommand(trimmed)) {
    const match = trimmed.match(/^\/(\w+)/);
    return match ? match[1] : null;
  }
  
  if (isAtCommand(trimmed)) {
    const match = trimmed.match(/@(\w+)/);
    return match ? match[1] : null;
  }
  
  return null;
};

/**
 * Available slash commands
 */
export const SLASH_COMMANDS = [
  { name: 'help', description: 'Show help information' },
  { name: 'clear', description: 'Clear chat history' },
  { name: 'exit', description: 'Exit the application' },
  { name: 'config', description: 'Show configuration' },
  { name: 'model', description: 'Change AI model' },
] as const;

/**
 * Available at commands (agents)
 */
export const AT_COMMANDS = [
  { name: '@code', description: 'Code generation and refactoring' },
  { name: '@test', description: 'Test generation' },
  { name: '@doc', description: 'Documentation generation' },
  { name: '@review', description: 'Code review' },
  { name: '@debug', description: 'Debugging assistance' },
] as const;

/**
 * Get command suggestions based on partial input
 */
export const getCommandSuggestions = (query: string): Array<{name: string, description: string}> => {
  const trimmed = query.trim().toLowerCase();
  
  if (isSlashCommand(trimmed)) {
    const commandPart = trimmed.slice(1);
    return SLASH_COMMANDS
      .filter(cmd => cmd.name.startsWith(commandPart))
      .map(cmd => ({ name: `/${cmd.name}`, description: cmd.description }));
  }
  
  if (isAtCommand(trimmed)) {
    const commandPart = trimmed.slice(trimmed.lastIndexOf('@') + 1);
    return AT_COMMANDS
      .filter(cmd => cmd.name.slice(1).startsWith(commandPart))
      .map(cmd => ({ name: cmd.name, description: cmd.description }));
  }
  
  return [];
};
