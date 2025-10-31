#!/usr/bin/env node

/**
 * AutoDev CLI - Main entry point
 * 
 * This file serves as the entry point for the @autodev/cli application.
 * It initializes the Ink UI and connects to the Kotlin/JS backend.
 */

import React from 'react';
import { render } from 'ink';
import { App } from './ui/App.js';

// Main entry point
async function main() {
  try {
    // Render the Ink application
    const { waitUntilExit } = render(<App />);
    
    // Wait for the app to exit
    await waitUntilExit();
    
  } catch (error) {
    console.error('Fatal error:', error);
    process.exit(1);
  }
}

// Run the application
main().catch((error) => {
  console.error('Unhandled error:', error);
  process.exit(1);
});

