// Webpack configuration to handle Node.js modules in browser environment
// This file provides fallbacks for Node.js core modules when running tests in browser
const CopyWebpackPlugin = require('copy-webpack-plugin');

config.resolve = config.resolve || {};
config.resolve.fallback = config.resolve.fallback || {};

// Provide empty modules for Node.js core modules that are not available in browser
config.resolve.fallback.os = false;
config.resolve.fallback.fs = false;
config.resolve.fallback.path = false;
config.resolve.fallback.child_process = false;

// Also handle other common Node.js modules that might be used
config.resolve.fallback.crypto = false;
config.resolve.fallback.stream = false;
config.resolve.fallback.util = false;
config.resolve.fallback.buffer = false;
config.resolve.fallback.process = false;

config.plugins = config.plugins || [];
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            {
                from: '../../node_modules/wasm-git/lg2.wasm',
                to: 'lg2.wasm'
            }
        ]
    })
);

console.log('Applied Node.js polyfills for browser tests');
