/**
 * Banner - Display ASCII art logo
 */

import React from 'react';
import { Box, Text } from 'ink';
import { AUTODEV_LOGO, AUTODEV_TAGLINE } from '../constants/asciiArt.js';

export const Banner: React.FC = () => {
  return (
    <Box flexDirection="column" alignItems="center" paddingY={1}>
      <Text color="cyan" bold>
        {AUTODEV_LOGO}
      </Text>
      <Text color="gray" dimColor>
        {AUTODEV_TAGLINE}
      </Text>
    </Box>
  );
};
