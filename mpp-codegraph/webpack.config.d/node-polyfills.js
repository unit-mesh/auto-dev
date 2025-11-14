// Webpack configuration to handle Node.js modules in browser environment for wasmJs target
// This file provides fallbacks for Node.js core modules when running tests in browser
// Specifically needed for web-tree-sitter which may try to use Node.js APIs

const CopyWebpackPlugin = require('copy-webpack-plugin');

config.resolve = config.resolve || {};
config.resolve.fallback = config.resolve.fallback || {};

// Provide empty modules for Node.js core modules that are not available in browser
// These are required by web-tree-sitter when running in browser/wasm environment
config.resolve.fallback.fs = false;
config.resolve.fallback.path = false;
config.resolve.fallback.os = false;
config.resolve.fallback.child_process = false;

// Also handle other common Node.js modules that might be used
config.resolve.fallback.crypto = false;
config.resolve.fallback.stream = false;
config.resolve.fallback.util = false;
config.resolve.fallback.buffer = false;
config.resolve.fallback.process = false;

// Copy tree-sitter.wasm and language WASM files to the output directory
config.plugins = config.plugins || [];
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            {
                from: '../../node_modules/web-tree-sitter/tree-sitter.wasm',
                to: 'tree-sitter.wasm'
            },
            // Copy tree-sitter language WASM files from @unit-mesh/treesitter-artifacts
            {
                from: '../../node_modules/@unit-mesh/treesitter-artifacts/wasm/*.wasm',
                to: 'wasm/[name][ext]'
            }
        ]
    })
);

console.log('Applied Node.js polyfills for mpp-codegraph wasmJs browser tests');
