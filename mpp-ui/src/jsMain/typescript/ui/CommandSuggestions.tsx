/**
 * CommandSuggestions - Display command auto-completion suggestions
 */

import React from 'react';
import { Box, Text } from 'ink';
import { semanticInk } from '../design-system/theme-helpers.js';

type CompletionItem = {
  text: string;
  displayText: string;
  description: string | null;
  icon: string | null;
  triggerType: string;
};

type CommandSuggestionsProps = {
  items: CompletionItem[];
  selectedIndex?: number;
};

// Maximum length for descriptions before truncation
const MAX_DESCRIPTION_LENGTH = 60;

/**
 * Truncate long text with ellipsis
 */
const truncateText = (text: string, maxLength: number): string => {
  if (text.length <= maxLength) {
    return text;
  }
  return text.substring(0, maxLength - 3) + '...';
};

/**
 * Display auto-completion suggestions using Kotlin CompletionManager
 */
export const CommandSuggestions: React.FC<CommandSuggestionsProps> = ({ 
  items, 
  selectedIndex = 0 
}) => {
  if (items.length === 0) {
    return null;
  }

  return (
    <Box flexDirection="column" marginTop={1} marginBottom={1}>
      <Box borderStyle="round" borderColor={semanticInk.accent} paddingX={1} paddingY={0}>
        <Box flexDirection="column" width="100%">
          {items.map((item, index) => {
            const isSelected = index === selectedIndex;
            const truncatedDescription = item.description 
              ? truncateText(item.description, MAX_DESCRIPTION_LENGTH)
              : null;
            
            return (
              <Box key={index} paddingY={0}>
                <Text color={isSelected ? semanticInk.accent : semanticInk.muted} bold={isSelected}>
                  {item.icon ? `${item.icon} ` : ''}
                  {item.displayText || item.text}
                  {truncatedDescription ? (
                    <Text color={semanticInk.muted} dimColor> - {truncatedDescription}</Text>
                  ) : null}
                </Text>
              </Box>
            );
          })}
        </Box>
      </Box>
      <Box marginTop={1}>
        <Text color={semanticInk.muted} dimColor>
          ↑/↓ Navigate • Tab/Enter Complete • Esc Cancel
        </Text>
      </Box>
    </Box>
  );
};

