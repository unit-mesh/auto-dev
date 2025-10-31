/**
 * CommandSuggestions - Display command auto-completion suggestions
 */

import React from 'react';
import { Box, Text } from 'ink';

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
      <Box borderStyle="round" borderColor="cyan" paddingX={1} paddingY={0}>
        <Box flexDirection="column" width="100%">
          {items.map((item, index) => (
            <Box key={index} paddingY={0}>
              <Text color={index === selectedIndex ? 'cyan' : 'gray'} bold={index === selectedIndex}>
                {item.icon ? `${item.icon} ` : ''}
                {item.displayText || item.text}
                {item.description ? (
                  <Text color="gray" dimColor> - {item.description}</Text>
                ) : null}
              </Text>
            </Box>
          ))}
        </Box>
      </Box>
      <Box marginTop={1}>
        <Text color="gray" dimColor>
          ↑/↓ Navigate • Tab/Enter Complete • Esc Cancel
        </Text>
      </Box>
    </Box>
  );
};

