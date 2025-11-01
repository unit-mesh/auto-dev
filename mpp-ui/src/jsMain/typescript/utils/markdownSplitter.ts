/**
 * Markdown-aware message splitter for streaming performance optimization.
 * 
 * This utility helps prevent flickering during streaming by intelligently
 * splitting long messages at safe boundaries (paragraph breaks, code block edges).
 * Completed portions can be moved to Static rendering while only the latest
 * fragment continues to update.
 */

/**
 * Checks if a given character index is inside a fenced code block.
 */
const isIndexInsideCodeBlock = (
  content: string,
  indexToTest: number,
): boolean => {
  let fenceCount = 0;
  let searchPos = 0;
  while (searchPos < content.length) {
    const nextFence = content.indexOf('```', searchPos);
    if (nextFence === -1 || nextFence >= indexToTest) {
      break;
    }
    fenceCount++;
    searchPos = nextFence + 3;
  }
  return fenceCount % 2 === 1;
};

/**
 * Finds the starting index of the code block that encloses the given index.
 * Returns -1 if the index is not inside a code block.
 */
const findEnclosingCodeBlockStart = (
  content: string,
  index: number,
): number => {
  if (!isIndexInsideCodeBlock(content, index)) {
    return -1;
  }
  let currentSearchPos = 0;
  while (currentSearchPos < index) {
    const blockStartIndex = content.indexOf('```', currentSearchPos);
    if (blockStartIndex === -1 || blockStartIndex >= index) {
      break;
    }
    const blockEndIndex = content.indexOf('```', blockStartIndex + 3);
    if (blockStartIndex < index) {
      if (blockEndIndex === -1 || index < blockEndIndex + 3) {
        return blockStartIndex;
      }
    }
    if (blockEndIndex === -1) break;
    currentSearchPos = blockEndIndex + 3;
  }
  return -1;
};

/**
 * Finds the last safe split point in the content.
 * 
 * This function looks for natural boundaries (double newlines, code block edges)
 * where a message can be safely split without breaking Markdown formatting.
 * 
 * If a safe split point is found, content can be divided into:
 * - Before split: Move to history (Static rendering)
 * - After split: Keep in pending (dynamic rendering)
 * 
 * @param content The markdown content to analyze
 * @returns Index where content can be safely split, or content.length if no split needed
 */
export const findLastSafeSplitPoint = (content: string): number => {
  // Check if the end of content is inside a code block
  const enclosingBlockStart = findEnclosingCodeBlockStart(
    content,
    content.length,
  );
  if (enclosingBlockStart !== -1) {
    // The end is in a code block, split right before it
    return enclosingBlockStart;
  }

  // Search for the last double newline (\n\n) not in a code block
  let searchStartIndex = content.length;
  while (searchStartIndex >= 0) {
    const dnlIndex = content.lastIndexOf('\n\n', searchStartIndex);
    if (dnlIndex === -1) {
      // No more double newlines found
      break;
    }

    const potentialSplitPoint = dnlIndex + 2;
    if (!isIndexInsideCodeBlock(content, potentialSplitPoint)) {
      return potentialSplitPoint;
    }

    // If potentialSplitPoint was inside a code block,
    // search before this \n\n
    searchStartIndex = dnlIndex - 1;
  }

  // No safe split point found, keep entire content as one piece
  return content.length;
};

