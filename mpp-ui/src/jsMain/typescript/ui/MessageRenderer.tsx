/**
 * Shared message rendering components
 * Extracted from ChatInterface for reuse in CLI and other contexts
 */

import React from 'react';
import { Box, Text } from 'ink';
import Spinner from 'ink-spinner';
import { parseCodeBlocksSync } from '../utils/renderUtils.js';
import { t } from '../i18n/index.js';

export interface Message {
  role: 'user' | 'assistant' | 'system' | 'compiling';
  content: string;
  showPrefix?: boolean;
}

interface MessageBubbleProps {
  message: Message;
  isPending?: boolean;
}

/**
 * Renders a single message bubble with appropriate styling
 */
export const MessageBubble: React.FC<MessageBubbleProps> = ({ message, isPending = false }) => {
  // Handle different message roles
  if (message.role === 'compiling') {
    return (
      <Box marginBottom={1}>
        <Text color="yellow">
          <Spinner type="dots" /> {message.content}
        </Text>
      </Box>
    );
  }

  if (message.role === 'system') {
    return (
      <Box flexDirection="column" marginBottom={1}>
        <Box>
          <Text bold color="blue">
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
  const color = isUser ? 'green' : 'cyan';
  const prefix = isUser ? t('chat.prefixes.you') : t('chat.prefixes.ai');
  
  // Check if we should show prefix (defaults to true for backward compatibility)
  const showPrefix = message.showPrefix !== false;

  return (
    <Box flexDirection="column" marginBottom={1}>
      {/* Only show prefix if showPrefix is true */}
      {showPrefix && (
        <Box>
          <Text bold color={color}>
            {prefix}:
            {isPending && !isUser && (
              <Text color="cyan"> <Spinner type="dots" /></Text>
            )}
          </Text>
        </Box>
      )}
      <Box paddingLeft={showPrefix ? 2 : 0}>
        <ContentBlocks content={message.content || ''} isPending={isPending} />
      </Box>
    </Box>
  );
};

interface ContentBlocksProps {
  content: string;
  isPending: boolean;
}

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
              <Box borderStyle="round" borderColor="gray" paddingX={1} flexDirection="column">
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

