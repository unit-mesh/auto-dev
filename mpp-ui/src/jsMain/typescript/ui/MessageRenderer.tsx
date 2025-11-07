/**
 * Shared message rendering components
 * Extracted from ChatInterface for reuse in CLI and other contexts
 * Supports hierarchical tree-like message formatting
 */

import React from 'react';
import { Box, Text } from 'ink';
import Spinner from 'ink-spinner';
import { parseCodeBlocksSync } from '../utils/renderUtils.js';
import { t } from '../i18n/index.js';
import { semanticInk } from '../design-system/theme-helpers.js';

// Tree formatting symbols
const TREE_SYMBOLS = {
  bullet: '●',
  branch: '⎿',
  indent: '  ',
  space: ' ',
} as const;

export interface Message {
  role: 'user' | 'assistant' | 'system' | 'compiling';
  content: string;
  showPrefix?: boolean;
}

export interface TreeMessage {
  type: 'action' | 'detail' | 'info' | 'error' | 'success';
  content: string;
  children?: TreeMessage[];
  metadata?: {
    file?: string;
    operation?: string;
    status?: 'running' | 'completed' | 'failed';
  };
}

export interface TreeMessageBubbleProps {
  message: TreeMessage;
  level?: number;
  isLast?: boolean;
  isPending?: boolean;
}

interface MessageBubbleProps {
  message: Message;
  isPending?: boolean;
}

/**
 * Renders a tree-structured message with hierarchical formatting
 * Uses bullet points (●) for top-level items and branches (⎿) for children
 */
export const TreeMessageBubble: React.FC<TreeMessageBubbleProps> = ({
  message,
  level = 0,
  isLast = true,
  isPending = false
}) => {
  const getSymbol = () => {
    if (level === 0) {
      return TREE_SYMBOLS.bullet;
    }
    return TREE_SYMBOLS.branch;
  };

  const getIndent = () => {
    if (level === 0) return '';
    return TREE_SYMBOLS.indent.repeat(level - 1) + TREE_SYMBOLS.space;
  };

  const getColor = () => {
    switch (message.type) {
      case 'action':
        return semanticInk.primary;
      case 'success':
        return semanticInk.success;
      case 'error':
        return semanticInk.error;
      case 'detail':
        return semanticInk.accent;
      case 'info':
      default:
        return semanticInk.muted;
    }
  };

  const renderContent = () => {
    const symbol = getSymbol();
    const indent = getIndent();
    const color = getColor();

    return (
      <Box flexDirection="column">
        <Box>
          <Text color={color}>
            {indent}{symbol}{TREE_SYMBOLS.space}{message.content}
            {isPending && message.metadata?.status === 'running' && (
              <Text color={semanticInk.warning}> <Spinner type="dots" /></Text>
            )}
          </Text>
        </Box>

        {/* Render metadata if available */}
        {message.metadata && (
          <Box paddingLeft={indent.length + 2}>
            {message.metadata.file && (
              <Text dimColor>
                {TREE_SYMBOLS.branch} {message.metadata.file}
              </Text>
            )}
            {message.metadata.operation && (
              <Text dimColor>
                {TREE_SYMBOLS.indent}{message.metadata.operation}
              </Text>
            )}
          </Box>
        )}

        {/* Render children recursively */}
        {message.children && message.children.length > 0 && (
          <Box flexDirection="column">
            {message.children.map((child, idx) => (
              <TreeMessageBubble
                key={idx}
                message={child}
                level={level + 1}
                isLast={idx === message.children!.length - 1}
                isPending={isPending}
              />
            ))}
          </Box>
        )}
      </Box>
    );
  };

  return (
    <Box marginBottom={level === 0 ? 1 : 0}>
      {renderContent()}
    </Box>
  );
};

/**
 * Helper functions to create tree messages
 */
export const createTreeMessage = {
  action: (content: string, children?: TreeMessage[], metadata?: TreeMessage['metadata']): TreeMessage => ({
    type: 'action',
    content,
    children,
    metadata,
  }),

  detail: (content: string, children?: TreeMessage[], metadata?: TreeMessage['metadata']): TreeMessage => ({
    type: 'detail',
    content,
    children,
    metadata,
  }),

  success: (content: string, children?: TreeMessage[], metadata?: TreeMessage['metadata']): TreeMessage => ({
    type: 'success',
    content,
    children,
    metadata,
  }),

  error: (content: string, children?: TreeMessage[], metadata?: TreeMessage['metadata']): TreeMessage => ({
    type: 'error',
    content,
    children,
    metadata,
  }),

  info: (content: string, children?: TreeMessage[], metadata?: TreeMessage['metadata']): TreeMessage => ({
    type: 'info',
    content,
    children,
    metadata,
  }),

  fileOperation: (operation: string, file: string, details?: string[], status: 'running' | 'completed' | 'failed' = 'completed'): TreeMessage => ({
    type: status === 'failed' ? 'error' : status === 'running' ? 'action' : 'success',
    content: operation,
    metadata: { file, operation, status },
    children: details ? details.map(detail => createTreeMessage.detail(detail)) : undefined,
  }),
};

/**
 * Renders a single message bubble with appropriate styling
 * Enhanced to support tree-like formatting when content contains structured data
 */
export const MessageBubble: React.FC<MessageBubbleProps> = ({ message, isPending = false }) => {
  // Handle different message roles
  if (message.role === 'compiling') {
    return (
      <Box marginBottom={1}>
        <Text color={semanticInk.warning}>
          <Spinner type="dots" /> {message.content}
        </Text>
      </Box>
    );
  }

  if (message.role === 'system') {
    return (
      <Box flexDirection="column" marginBottom={1}>
        <Box>
          <Text bold color={semanticInk.primary}>
            {t('chat.prefixes.system')}:
          </Text>
        </Box>
        <Box paddingLeft={2}>
          <Text dimColor>{message.content}</Text>
        </Box>
      </Box>
    );
  }

  const isUser = message.role === 'user';
  const color = isUser ? semanticInk.success : semanticInk.accent;
  const prefix = isUser ? t('chat.prefixes.you') : t('chat.prefixes.ai');

  // Check if we should show prefix (defaults to true for backward compatibility)
  const showPrefix = message.showPrefix !== false;

  // Try to parse content as tree structure if it contains bullet points
  const isTreeStructured = message.content.includes('●') || message.content.includes('⎿');

  return (
    <Box flexDirection="column" marginBottom={1}>
      {/* Only show prefix if showPrefix is true */}
      {showPrefix && (
        <Box>
          <Text bold color={color}>
            {prefix}:
            {isPending && !isUser && (
              <Text color={semanticInk.accent}> <Spinner type="dots" /></Text>
            )}
          </Text>
        </Box>
      )}
      <Box paddingLeft={showPrefix ? 2 : 0}>
        {isTreeStructured ? (
          <TreeStructuredContent content={message.content || ''} isPending={isPending} />
        ) : (
          <ContentBlocks content={message.content || ''} isPending={isPending} />
        )}
      </Box>
    </Box>
  );
};

interface ContentBlocksProps {
  content: string;
  isPending: boolean;
}

interface TreeStructuredContentProps {
  content: string;
  isPending: boolean;
}

/**
 * Renders tree-structured content by parsing bullet points and branches
 */
export const TreeStructuredContent: React.FC<TreeStructuredContentProps> = ({ content, isPending }) => {
  if (!content && isPending) {
    return <Text dimColor>...</Text>;
  }

  if (!content) {
    return null;
  }

  const lines = content.split('\n');

  return (
    <Box flexDirection="column">
      {lines.map((line, idx) => {
        const trimmedLine = line.trim();

        // Skip empty lines
        if (!trimmedLine) {
          return null;
        }

        // Detect tree structure symbols
        const isBulletPoint = trimmedLine.startsWith('●');
        const isBranch = trimmedLine.startsWith('⎿');
        const isIndented = line.startsWith('  ') || line.startsWith('   ') || line.startsWith('    ');

        let color: string = semanticInk.primary;
        let content = trimmedLine;

        if (isBulletPoint) {
          color = semanticInk.primary;
          content = trimmedLine.substring(1).trim(); // Remove bullet
        } else if (isBranch) {
          color = semanticInk.accent;
          content = trimmedLine.substring(1).trim(); // Remove branch
        } else if (isIndented) {
          color = semanticInk.muted;
        }

        // Calculate indentation level
        const indentLevel = Math.floor((line.length - line.trimStart().length) / 2);
        const indentString = TREE_SYMBOLS.indent.repeat(Math.max(0, indentLevel));

        return (
          <Box key={idx}>
            <Text color={color}>
              {isBulletPoint && `${TREE_SYMBOLS.bullet} `}
              {isBranch && `${indentString}${TREE_SYMBOLS.branch} `}
              {!isBulletPoint && !isBranch && indentString}
              {content}
            </Text>
          </Box>
        );
      })}
    </Box>
  );
};

/**
 * Renders content blocks with code highlighting
 */
export const ContentBlocks: React.FC<ContentBlocksProps> = ({ content, isPending }) => {
  if (!content && isPending) {
    return <Text dimColor>...</Text>;
  }

  if (!content) {
    return null;
  }

  // Parse content into blocks
  const blocks = parseCodeBlocksSync(content);

  return (
    <Box flexDirection="column">
      {blocks.map((block, idx) => {
        if (!block.languageId) {
          // Text block - filter empty lines and render with proper spacing
          const lines = block.text.split('\n');
          
          // Filter out leading and trailing empty lines
          let startIdx = 0;
          let endIdx = lines.length - 1;
          
          while (startIdx < lines.length && lines[startIdx].trim() === '') {
            startIdx++;
          }
          while (endIdx >= 0 && lines[endIdx].trim() === '') {
            endIdx--;
          }
          
          const trimmedLines = lines.slice(startIdx, endIdx + 1);
          
          // If block is empty after trimming, skip it
          if (trimmedLines.length === 0) {
            return null;
          }
          
          return (
            <Box key={idx} flexDirection="column" marginBottom={idx < blocks.length - 1 ? 1 : 0}>
              {trimmedLines.map((line, lineIdx) => {
                // For empty lines in the middle, only render if surrounded by non-empty lines
                if (line.trim() === '') {
                  // Check if this is a paragraph break (should show as spacing)
                  const prevLine = lineIdx > 0 ? trimmedLines[lineIdx - 1] : '';
                  const nextLine = lineIdx < trimmedLines.length - 1 ? trimmedLines[lineIdx + 1] : '';
                  
                  // Only render empty line if it's between two non-empty lines
                  if (prevLine.trim() && nextLine.trim()) {
                    return <Text key={lineIdx}> </Text>;
                  }
                  return null;
                }
                return <Text key={lineIdx}>{line}</Text>;
              })}
            </Box>
          );
        } else {
          // Code block - preserve all lines including empty ones (formatting matters)
          const codeLines = block.text.split('\n');
          return (
            <Box key={idx} flexDirection="column" marginBottom={idx < blocks.length - 1 ? 1 : 0}>
              {/* Language label */}
              {block.languageId && (
                <Text dimColor>{`[${block.languageId}]`}</Text>
              )}
              {/* Code content with border */}
              <Box borderStyle="round" borderColor={semanticInk.muted} paddingX={1} flexDirection="column">
                {codeLines.map((line, lineIdx) => (
                  <Text key={lineIdx}>{line || ' '}</Text>
                ))}
              </Box>
            </Box>
          );
        }
      })}
    </Box>
  );
};

