/**
 * Content rendering utilities
 * Uses Kotlin's CodeFence.parseAll to parse different content blocks
 */

interface CodeBlock {
  languageId: string;
  text: string;
  isComplete: boolean;
  extension: string | null;
}

let codeFenceParser: any = null;

async function getCodeFenceParser() {
  if (!codeFenceParser) {
    const mppCore = await import('@autodev/mpp-core/autodev-mpp-core.js');
    const exports = mppCore['module.exports'] || mppCore.default || mppCore;
    if (exports?.cc?.unitmesh?.devins?.parser?.CodeFence) {
      codeFenceParser = exports.cc.unitmesh.devins.parser.CodeFence;
    }
  }
  return codeFenceParser;
}

export async function parseContentBlocks(content: string): Promise<CodeBlock[]> {
  try {
    const parser = await getCodeFenceParser();
    if (!parser || !parser.Companion || !parser.Companion.parseAll) {
      // Fallback: treat as single text block
      return [{
        languageId: '',
        text: content,
        isComplete: true,
        extension: null
      }];
    }

    // Call Kotlin's parseAll method
    const kotlinBlocks = parser.Companion.parseAll(content);
    
    // Convert Kotlin objects to JavaScript objects
    const blocks: CodeBlock[] = [];
    for (let i = 0; i < kotlinBlocks.size; i++) {
      const block = kotlinBlocks.get(i);
      blocks.push({
        languageId: block.languageId || '',
        text: block.text || '',
        isComplete: block.isComplete || false,
        extension: block.extension || null
      });
    }
    
    return blocks;
  } catch (error) {
    console.error('Error parsing content blocks:', error);
    // Fallback: treat as single text block
    return [{
      languageId: '',
      text: content,
      isComplete: true,
      extension: null
    }];
  }
}

/**
 * Simple synchronous parser for code blocks (fallback)
 */
export function parseCodeBlocksSync(content: string): CodeBlock[] {
  const blocks: CodeBlock[] = [];
  const lines = content.split('\n');
  
  let currentBlock: { lang: string; lines: string[] } | null = null;
  let textLines: string[] = [];
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();
    
    if (trimmed.startsWith('```')) {
      // Code fence start or end
      if (!currentBlock) {
        // Start of code block
        if (textLines.length > 0) {
          // Save accumulated text
          blocks.push({
            languageId: '',
            text: textLines.join('\n'),
            isComplete: true,
            extension: null
          });
          textLines = [];
        }
        
        const lang = trimmed.substring(3).trim();
        currentBlock = { lang, lines: [] };
      } else {
        // End of code block
        blocks.push({
          languageId: currentBlock.lang,
          text: currentBlock.lines.join('\n'),
          isComplete: true,
          extension: null
        });
        currentBlock = null;
      }
    } else {
      // Regular line
      if (currentBlock) {
        currentBlock.lines.push(line);
      } else {
        textLines.push(line);
      }
    }
  }
  
  // Add remaining text
  if (textLines.length > 0) {
    blocks.push({
      languageId: '',
      text: textLines.join('\n'),
      isComplete: true,
      extension: null
    });
  }
  
  // Add unclosed code block
  if (currentBlock) {
    blocks.push({
      languageId: currentBlock.lang,
      text: currentBlock.lines.join('\n'),
      isComplete: false,
      extension: null
    });
  }
  
  return blocks.length > 0 ? blocks : [{
    languageId: '',
    text: content,
    isComplete: true,
    extension: null
  }];
}

