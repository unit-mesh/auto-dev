#!/usr/bin/env node
/**
 * Copy WASM files from @unit-mesh/treesitter-artifacts to dist/wasm
 */

const fs = require('fs');
const path = require('path');

const sourceDir = path.join(__dirname, '../node_modules/@unit-mesh/treesitter-artifacts/wasm');
const targetDir = path.join(__dirname, '../dist/wasm');

// Create target directory if it doesn't exist
if (!fs.existsSync(targetDir)) {
  fs.mkdirSync(targetDir, { recursive: true });
}

// List of required WASM files
const wasmFiles = [
  'tree-sitter-typescript.wasm',
  'tree-sitter-tsx.wasm',
  'tree-sitter-javascript.wasm',
  'tree-sitter-python.wasm',
  'tree-sitter-java.wasm',
  'tree-sitter-kotlin.wasm',
  'tree-sitter-go.wasm',
  'tree-sitter-rust.wasm'
];

// Copy each WASM file
let copiedCount = 0;
for (const file of wasmFiles) {
  const sourcePath = path.join(sourceDir, file);
  const targetPath = path.join(targetDir, file);
  
  try {
    if (fs.existsSync(sourcePath)) {
      fs.copyFileSync(sourcePath, targetPath);
      console.log(`✓ Copied ${file}`);
      copiedCount++;
    } else {
      console.warn(`⚠ Warning: ${file} not found in source directory`);
    }
  } catch (error) {
    console.error(`✗ Failed to copy ${file}: ${error.message}`);
  }
}

console.log(`\nCopied ${copiedCount}/${wasmFiles.length} WASM files to dist/wasm/`);

