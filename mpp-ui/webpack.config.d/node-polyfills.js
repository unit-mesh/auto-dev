// Webpack configuration to handle Node.js modules in browser environment for wasmJs target
// This file provides fallbacks for Node.js core modules when running tests in browser
const CopyWebpackPlugin = require('copy-webpack-plugin');

config.resolve = config.resolve || {};
config.resolve.fallback = config.resolve.fallback || {};

// Provide empty modules for Node.js core modules that are not available in browser
// These are required by web-tree-sitter and wasm-git when running in browser/wasm environment
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

// Configure module resolution for wasm-git
config.resolve.alias = config.resolve.alias || {};
config.resolve.alias['module'] = false;

// Ignore dynamic requires in wasm-git that can't be resolved
config.module = config.module || {};
config.module.unknownContextCritical = false;
config.module.exprContextCritical = false;

// Add webpack plugin to ignore wasm-git's dynamic requires
const webpack = require('webpack');
config.plugins.push(
    new webpack.ContextReplacementPlugin(
        /wasm-git/,
        (data) => {
            // Ignore all dynamic requires in wasm-git
            delete data.dependencies[0].critical;
            return data;
        }
    )
);

// Ignore specific problematic requires in wasm-git
config.plugins.push(
    new webpack.IgnorePlugin({
        resourceRegExp: /^\.\/$/,
        contextRegExp: /wasm-git/
    })
);

// Copy tree-sitter.wasm and language WASM files to the output directory
config.plugins = config.plugins || [];
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            {
                from: '../../node_modules/wasm-git/lg2_async.wasm',
                to: 'lg2_async.wasm'
            },
            {
                from: '../../node_modules/web-tree-sitter/tree-sitter.wasm',
                to: 'tree-sitter.wasm'
            },
            {
                from: '../../node_modules/@unit-mesh/treesitter-artifacts/wasm/*.wasm',
                to: 'wasm/[name][ext]'
            },
            {
                from: '../../node_modules/sql.js/dist/sql-wasm.wasm',
                to: 'sql-wasm.wasm'
            }
        ]
    })
);

console.log('Applied Node.js polyfills for mpp-codegraph wasmJs browser tests');
