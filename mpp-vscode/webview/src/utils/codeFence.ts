/**
 * CodeFence parser - mirrors mpp-core's CodeFence.parseAll()
 * Parses markdown content into code blocks and text blocks
 *
 * Handles:
 * - Standard markdown code fences (```)
 * - <devin> tags (converted from ```devin blocks)
 * - <thinking> tags
 * - <!-- walkthrough_start --> comments
 */

export interface CodeBlock {
  languageId: string;
  text: string;
  isComplete: boolean;
  extension?: string;
}

// Regex patterns matching mpp-core's CodeFence
const devinStartRegex = /<devin>/;
const devinEndRegex = /<\/devin>/;
const thinkingStartRegex = /<thinking>/;
const thinkingEndRegex = /<\/thinking>/;
const walkthroughStartRegex = /<!--\s*walkthrough_start\s*-->/;
const walkthroughEndRegex = /<!--\s*walkthrough_end\s*-->/;
const normalCodeBlockRegex = /\s*```([\w#+ ]*)\n/;
const languageRegex = /\s*```([\w#+ ]*)/;

/**
 * Pre-process ```devin blocks to <devin> tags
 * Matches mpp-core's preProcessDevinBlock
 */
function preProcessDevinBlock(content: string): string {
  let currentContent = content;

  // Find all ```devin blocks
  const devinMatches = [...content.matchAll(/(?:^|\n)```devin\n([\s\S]*?)\n```(?:\n|$)/g)];

  for (const match of devinMatches) {
    let devinContent = match[1] || '';

    // Check if there's an unclosed code block inside
    if (normalCodeBlockRegex.test(devinContent)) {
      if (!devinContent.trim().endsWith('```')) {
        devinContent += '\n```';
      }
    }

    const replacement = `\n<devin>\n${devinContent}\n</devin>`;
    currentContent = currentContent.replace(match[0], replacement);
  }

  return currentContent;
}

/**
 * Parse content into code blocks and text blocks
 * Handles markdown code fences (```) and special block types
 */
export function parseCodeBlocks(content: string): CodeBlock[] {
  const blocks: CodeBlock[] = [];
  let currentIndex = 0;

  // Pre-process ```devin blocks to <devin> tags
  let processedContent = content;
  if (content.includes('```devin\n')) {
    processedContent = preProcessDevinBlock(content);
  }

  // Find all special tag matches
  interface TagMatch {
    type: string;
    match: RegExpExecArray;
  }
  const tagMatches: TagMatch[] = [];

  // Find <devin> tags
  let match: RegExpExecArray | null;
  const devinRegex = new RegExp(devinStartRegex.source, 'g');
  while ((match = devinRegex.exec(processedContent)) !== null) {
    tagMatches.push({ type: 'devin', match });
  }

  // Find <thinking> tags
  const thinkingRegex = new RegExp(thinkingStartRegex.source, 'g');
  while ((match = thinkingRegex.exec(processedContent)) !== null) {
    tagMatches.push({ type: 'thinking', match });
  }

  // Find <!-- walkthrough_start --> tags
  const walkthroughRegex = new RegExp(walkthroughStartRegex.source, 'g');
  while ((match = walkthroughRegex.exec(processedContent)) !== null) {
    tagMatches.push({ type: 'walkthrough', match });
  }

  // Sort by position
  tagMatches.sort((a, b) => a.match.index - b.match.index);

  // Process each tag match
  for (const { type, match: startMatch } of tagMatches) {
    if (startMatch.index >= currentIndex) {
      // Parse content before this tag
      if (startMatch.index > currentIndex) {
        const beforeText = processedContent.substring(currentIndex, startMatch.index);
        if (beforeText.trim()) {
          parseMarkdownContent(beforeText, blocks);
        }
      }

      // Find the corresponding end tag
      const endRegex = type === 'devin' ? devinEndRegex :
                       type === 'thinking' ? thinkingEndRegex :
                       walkthroughEndRegex;

      const endMatch = endRegex.exec(processedContent.substring(startMatch.index + startMatch[0].length));
      const isComplete = endMatch !== null;

      const tagContent = isComplete
        ? processedContent.substring(
            startMatch.index + startMatch[0].length,
            startMatch.index + startMatch[0].length + endMatch!.index
          ).trim()
        : processedContent.substring(startMatch.index + startMatch[0].length).trim();

      blocks.push({
        languageId: type,
        text: tagContent,
        isComplete,
        extension: type
      });

      currentIndex = isComplete
        ? startMatch.index + startMatch[0].length + endMatch!.index + endMatch![0].length
        : processedContent.length;
    }
  }

  // Parse remaining content
  if (currentIndex < processedContent.length) {
    const remainingContent = processedContent.substring(currentIndex);
    if (remainingContent.trim()) {
      parseMarkdownContent(remainingContent, blocks);
    }
  }

  // Filter out empty blocks (except special types)
  return blocks.filter(block => {
    if (block.languageId === 'devin' || block.languageId === 'thinking' || block.languageId === 'walkthrough') {
      return true;
    }
    return block.text.trim().length > 0;
  });
}

/**
 * Parse markdown content (text and code blocks)
 */
function parseMarkdownContent(content: string, blocks: CodeBlock[]): void {
  const lines = content.split('\n');

  let codeStarted = false;
  let languageId: string | null = null;
  const codeBuilder: string[] = [];
  const textBuilder: string[] = [];

  for (const line of lines) {
    if (!codeStarted) {
      const trimmedLine = line.trimStart();
      const matchResult = languageRegex.exec(trimmedLine);

      if (matchResult) {
        // Save accumulated text
        if (textBuilder.length > 0) {
          const text = textBuilder.join('\n').trim();
          if (text) {
            blocks.push({
              languageId: 'markdown',
              text,
              isComplete: true,
              extension: 'md'
            });
          }
          textBuilder.length = 0;
        }

        languageId = matchResult[1]?.trim() || null;
        codeStarted = true;
      } else {
        textBuilder.push(line);
      }
    } else {
      const trimmedLine = line.trimStart();

      if (trimmedLine === '```') {
        // End of code block
        const codeContent = codeBuilder.join('\n').trim();
        blocks.push({
          languageId: languageId || 'markdown',
          text: codeContent,
          isComplete: true,
          extension: getExtensionForLanguage(languageId || 'md')
        });

        codeBuilder.length = 0;
        codeStarted = false;
        languageId = null;
      } else {
        codeBuilder.push(line);
      }
    }
  }

  // Add remaining text
  if (textBuilder.length > 0) {
    const text = textBuilder.join('\n').trim();
    if (text) {
      blocks.push({
        languageId: 'markdown',
        text,
        isComplete: true,
        extension: 'md'
      });
    }
  }

  // Add unclosed code block
  if (codeStarted && codeBuilder.length > 0) {
    const code = codeBuilder.join('\n').trim();
    blocks.push({
      languageId: languageId || 'markdown',
      text: code,
      isComplete: false,
      extension: getExtensionForLanguage(languageId || 'md')
    });
  }
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

