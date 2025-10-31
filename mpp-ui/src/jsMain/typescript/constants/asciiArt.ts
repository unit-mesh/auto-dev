/**
 * ASCII Art and branding constants for AutoDev CLI
 */

export const AUTODEV_LOGO = `
   ╔═╗╦ ╦╔╦╗╔═╗╔╦╗╔═╗╦  ╦
   ╠═╣║ ║ ║ ║ ║ ║║║╣ ╚╗╔╝
   ╩ ╩╚═╝ ╩ ╚═╝═╩╝╚═╝ ╚╝ 
`;

export const AUTODEV_TAGLINE = 'AI-Powered Development Assistant';

export const WELCOME_MESSAGE = `
${AUTODEV_LOGO}
${AUTODEV_TAGLINE}

🚀 Type your message to start coding
💡 Use / for commands or @ for agents
⌨️  Press Tab for auto-completion
📖 Type /help for more information
`;

export const HELP_TEXT = `
📚 AutoDev CLI Help

Commands:
  /help       - Show this help message
  /clear      - Clear chat history
  /exit       - Exit the application
  /config     - Show current configuration
  /model      - Change AI model

Agents (use @ to invoke):
  @code       - Code generation and refactoring
  @test       - Test generation
  @doc        - Documentation generation
  @review     - Code review
  @debug      - Debugging assistance

Shortcuts:
  Ctrl+C      - Exit
  Ctrl+L      - Clear screen
  Tab         - Auto-complete
  ↑/↓         - Navigate history
`;

export const GOODBYE_MESSAGE = `
👋 Thanks for using AutoDev!
💾 Your session has been saved.
`;
