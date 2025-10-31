#!/usr/bin/env node

/**
 * Post-install script to verify mpp-core is built
 */

import { existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { execSync } from 'child_process';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const rootDir = resolve(__dirname, '..');
const mppCorePath = resolve(rootDir, '../mpp-core/build/packages/js/autodev-mpp-core.js');
const packageJsonPath = resolve(rootDir, '../mpp-core/build/packages/js/package.json');
const nodeModulesPath = resolve(rootDir, '../mpp-core/build/packages/js/node_modules');

if (!existsSync(mppCorePath) || !existsSync(packageJsonPath)) {
  console.error('❌ Error: mpp-core build not found!');
  console.error('');
  console.error('Expected files:');
  console.error('  ', mppCorePath);
  console.error('  ', packageJsonPath);
  console.error('');
  console.error('Please build mpp-core first:');
  console.error('  cd', resolve(rootDir, '..'));
  console.error('  ./gradlew :mpp-core:assembleJsPackage');
  console.error('');
  console.error('Or run from mpp-ui:');
  console.error('  npm run build:kotlin');
  process.exit(1);
}

// Check if dependencies are installed
if (!existsSync(nodeModulesPath)) {
  console.log('📦 Installing mpp-core dependencies...');
  try {
    execSync('npm install', { 
      cwd: resolve(rootDir, '../mpp-core/build/packages/js'),
      stdio: 'inherit'
    });
    console.log('✅ mpp-core dependencies installed');
  } catch (error) {
    console.error('❌ Failed to install mpp-core dependencies');
    process.exit(1);
  }
}

console.log('✅ mpp-core build found');
