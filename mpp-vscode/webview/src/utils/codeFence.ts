/**
 * CodeFence parser - mirrors mpp-core's CodeFence.parseAll()
 * Parses markdown content into code blocks and text blocks
 */

export interface CodeBlock {
  languageId: string;
  text: string;
  isComplete: boolean;
  extension?: string;
}

/**
 * Parse content into code blocks and text blocks
 * Handles markdown code fences (```) and special block types
 */
export function parseCodeBlocks(content: string): CodeBlock[] {
  const blocks: CodeBlock[] = [];
  const lines = content.split('\n');
  
  let currentBlock: CodeBlock | null = null;
  let textBuffer: string[] = [];
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmedLine = line.trim();
    
    // Check for code fence start
    if (trimmedLine.startsWith('```')) {
      // Flush text buffer
      if (textBuffer.length > 0) {
        const text = textBuffer.join('\n').trim();
        if (text) {
          blocks.push({
            languageId: '',
            text,
            isComplete: true
          });
        }
        textBuffer = [];
      }
      
      if (currentBlock) {
        // End of code block
        currentBlock.isComplete = true;
        blocks.push(currentBlock);
        currentBlock = null;
      } else {
        // Start of code block
        const langMatch = trimmedLine.match(/^```(\w+)?/);
        const languageId = langMatch?.[1] || '';
        currentBlock = {
          languageId,
          text: '',
          isComplete: false,
          extension: getExtensionForLanguage(languageId)
        };
      }
      continue;
    }
    
    if (currentBlock) {
      // Inside code block
      currentBlock.text += (currentBlock.text ? '\n' : '') + line;
    } else {
      // Regular text
      textBuffer.push(line);
    }
  }
  
  // Handle remaining content
  if (currentBlock) {
    // Unclosed code block
    blocks.push(currentBlock);
  } else if (textBuffer.length > 0) {
    const text = textBuffer.join('\n').trim();
    if (text) {
      blocks.push({
        languageId: '',
        text,
        isComplete: true
      });
    }
  }
  
  return blocks;
}

/**
 * Get file extension for a language ID
 */
function getExtensionForLanguage(languageId: string): string {
  const extensionMap: Record<string, string> = {
    javascript: 'js',
    typescript: 'ts',
    python: 'py',
    java: 'java',
    kotlin: 'kt',
    rust: 'rs',
    go: 'go',
    cpp: 'cpp',
    c: 'c',
    csharp: 'cs',
    ruby: 'rb',
    php: 'php',
    swift: 'swift',
    shell: 'sh',
    bash: 'sh',
    zsh: 'sh',
    json: 'json',
    yaml: 'yaml',
    yml: 'yml',
    xml: 'xml',
    html: 'html',
    css: 'css',
    scss: 'scss',
    markdown: 'md',
    md: 'md',
    sql: 'sql',
    diff: 'diff',
    patch: 'patch'
  };
  
  return extensionMap[languageId.toLowerCase()] || languageId;
}

/**
 * Get display name for a language
 */
export function getDisplayName(languageId: string): string {
  const displayNames: Record<string, string> = {
    javascript: 'JavaScript',
    typescript: 'TypeScript',
    python: 'Python',
    java: 'Java',
    kotlin: 'Kotlin',
    rust: 'Rust',
    go: 'Go',
    cpp: 'C++',
    c: 'C',
    csharp: 'C#',
    ruby: 'Ruby',
    php: 'PHP',
    swift: 'Swift',
    shell: 'Shell',
    bash: 'Bash',
    json: 'JSON',
    yaml: 'YAML',
    xml: 'XML',
    html: 'HTML',
    css: 'CSS',
    markdown: 'Markdown',
    md: 'Markdown',
    sql: 'SQL',
    diff: 'Diff',
    patch: 'Patch',
    thinking: 'Thinking',
    walkthrough: 'Walkthrough',
    mermaid: 'Mermaid',
    devin: 'DevIn',
    plan: 'Plan'
  };
  
  return displayNames[languageId.toLowerCase()] || languageId || 'Text';
}

