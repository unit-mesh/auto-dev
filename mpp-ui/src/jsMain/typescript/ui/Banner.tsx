/**
 * Banner - Display ASCII art logo
 */

import React from 'react';
import { Box, Text } from 'ink';
import { AUTODEV_LOGO, AUTODEV_TAGLINE } from '../constants/asciiArt.js';
import { semanticInk } from '../design-system/theme-helpers.js';

export const Banner: React.FC = () => {
  return (
    <Box flexDirection="column" alignItems="center" paddingY={1}>
      <Text color={semanticInk.accent} bold>
        {AUTODEV_LOGO}
      </Text>
      <Text color={semanticInk.muted} dimColor>
        {AUTODEV_TAGLINE}
      </Text>
    </Box>
  );
};
