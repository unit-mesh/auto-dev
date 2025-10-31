/**
 * CommandSuggestions - Display command auto-completion suggestions
 */

import React from 'react';
import { Box, Text } from 'ink';

interface CommandSuggestion {
  name: string;
  description: string;
}

interface Props {
  suggestions: CommandSuggestion[];
  selectedIndex?: number;
}

export const CommandSuggestions: React.FC<Props> = ({ suggestions, selectedIndex = 0 }) => {
  if (suggestions.length === 0) {
    return null;
  }

  return (
    <Box flexDirection="column" marginTop={1} paddingX={2}>
      <Text dimColor>─── Suggestions ───</Text>
      {suggestions.map((suggestion, index) => (
        <Box key={suggestion.name} paddingLeft={2}>
          <Text color={index === selectedIndex ? 'cyan' : 'gray'}>
            {index === selectedIndex ? '▶ ' : '  '}
            <Text bold={index === selectedIndex}>{suggestion.name}</Text>
            <Text dimColor> - {suggestion.description}</Text>
          </Text>
        </Box>
      ))}
      <Box marginTop={1}>
        <Text dimColor>
          ↑/↓ to navigate  •  Tab to complete  •  Enter to select
        </Text>
      </Box>
    </Box>
  );
};
