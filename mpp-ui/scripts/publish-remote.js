#!/usr/bin/env node

/**
 * Remote publish script
 * This publishes both mpp-core and mpp-ui to npm registry
 * 
 * Steps:
 * 1. Build and publish @autodev/mpp-core to npm
 * 2. Update package.json to use published version
 * 3. Build and publish @autodev/cli
 * 4. Restore package.json to use local file: dependency
 */

import { readFileSync, writeFileSync, existsSync, copyFileSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { execSync } from 'child_process';
import readline from 'readline';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const rootDir = resolve(__dirname, '..');
const packageJsonPath = resolve(rootDir, 'package.json');
const packageJsonBackupPath = resolve(rootDir, 'package.json.backup');
const mppCoreDir = resolve(rootDir, '../mpp-core');
const mppCorePackageDir = resolve(mppCoreDir, 'build/packages/js');

// Utility to ask user confirmation
function askConfirmation(question) {
  return new Promise((resolve) => {
    const rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout
    });
    
    rl.question(question + ' (y/N): ', (answer) => {
      rl.close();
      resolve(answer.toLowerCase() === 'y' || answer.toLowerCase() === 'yes');
    });
  });
}

async function main() {
  console.log('🚀 Remote Publish Script for @autodev/cli\n');
  
  // Read current package.json
  const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf-8'));
  const currentVersion = packageJson.version;
  
  console.log('📦 Current package info:');
  console.log('   Name:', packageJson.name);
  console.log('   Version:', currentVersion);
  console.log('   mpp-core dependency:', packageJson.dependencies['@autodev/mpp-core']);
  console.log();
  
  // Confirm publish
  const shouldContinue = await askConfirmation('Do you want to continue with remote publish?');
  if (!shouldContinue) {
    console.log('❌ Publish cancelled');
    process.exit(0);
  }
  
  // Step 1: Build mpp-core
  console.log('\n1️⃣  Building mpp-core...');
  try {
    execSync('npm run build:kotlin', { cwd: rootDir, stdio: 'inherit' });
    console.log('✅ mpp-core build complete\n');
  } catch (error) {
    console.error('❌ mpp-core build failed');
    process.exit(1);
  }
  
  // Step 2: Check if mpp-core package exists
  console.log('2️⃣  Checking mpp-core package...');
  const mppCorePackageJsonPath = resolve(mppCorePackageDir, 'package.json');
  if (!existsSync(mppCorePackageJsonPath)) {
    console.error('❌ mpp-core package.json not found at:', mppCorePackageJsonPath);
    process.exit(1);
  }
  
  const mppCorePackageJson = JSON.parse(readFileSync(mppCorePackageJsonPath, 'utf-8'));
  const mppCoreVersion = mppCorePackageJson.version;
  console.log('✅ mpp-core package found (v' + mppCoreVersion + ')\n');
  
  // Step 3: Publish mpp-core to npm
  console.log('3️⃣  Publishing @autodev/mpp-core...');
  const shouldPublishCore = await askConfirmation('Publish @autodev/mpp-core v' + mppCoreVersion + ' to npm?');
  if (!shouldPublishCore) {
    console.log('⏭️  Skipping mpp-core publish (using existing version)\n');
  } else {
    try {
      // Check if user is logged in
      execSync('npm whoami', { cwd: mppCorePackageDir, stdio: 'pipe' });
      
      // Publish
      execSync('npm publish --access public', { cwd: mppCorePackageDir, stdio: 'inherit' });
      console.log('✅ mpp-core published successfully\n');
      
      // Wait a bit for npm registry to update
      console.log('⏳ Waiting for npm registry to update (5 seconds)...');
      await new Promise(resolve => setTimeout(resolve, 5000));
      console.log();
    } catch (error) {
      console.error('❌ mpp-core publish failed');
      console.error('   Make sure you are logged in: npm login');
      process.exit(1);
    }
  }
  
  // Step 4: Backup and update package.json
  console.log('4️⃣  Updating package.json for remote dependency...');
  copyFileSync(packageJsonPath, packageJsonBackupPath);
  
  packageJson.dependencies['@autodev/mpp-core'] = '^' + mppCoreVersion;
  writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2) + '\n');
  console.log('✅ Updated to use @autodev/mpp-core@^' + mppCoreVersion + '\n');
  
  // Step 5: Install dependencies with remote version
  console.log('5️⃣  Installing dependencies...');
  try {
    execSync('npm install', { cwd: rootDir, stdio: 'inherit' });
    console.log('✅ Dependencies installed\n');
  } catch (error) {
    console.error('❌ npm install failed');
    console.log('🔄 Restoring package.json...');
    copyFileSync(packageJsonBackupPath, packageJsonPath);
    process.exit(1);
  }
  
  // Step 6: Build TypeScript
  console.log('6️⃣  Building TypeScript...');
  try {
    execSync('npm run build:ts', { cwd: rootDir, stdio: 'inherit' });
    console.log('✅ TypeScript build complete\n');
  } catch (error) {
    console.error('❌ TypeScript build failed');
    console.log('🔄 Restoring package.json...');
    copyFileSync(packageJsonBackupPath, packageJsonPath);
    process.exit(1);
  }
  
  // Step 7: Publish @autodev/cli
  console.log('7️⃣  Publishing @autodev/cli...');
  const shouldPublishCli = await askConfirmation('Publish @autodev/cli v' + currentVersion + ' to npm?');
  if (!shouldPublishCli) {
    console.log('❌ Publish cancelled');
    console.log('🔄 Restoring package.json...');
    copyFileSync(packageJsonBackupPath, packageJsonPath);
    process.exit(0);
  }
  
  try {
    execSync('npm publish --access public', { cwd: rootDir, stdio: 'inherit' });
    console.log('✅ @autodev/cli published successfully\n');
  } catch (error) {
    console.error('❌ @autodev/cli publish failed');
    console.log('🔄 Restoring package.json...');
    copyFileSync(packageJsonBackupPath, packageJsonPath);
    process.exit(1);
  }
  
  // Step 8: Restore package.json for local development
  console.log('8️⃣  Restoring package.json for local development...');
  copyFileSync(packageJsonBackupPath, packageJsonPath);
  console.log('✅ package.json restored\n');
  
  // Step 9: Reinstall with local file: dependency
  console.log('9️⃣  Reinstalling local dependencies...');
  try {
    execSync('npm install', { cwd: rootDir, stdio: 'inherit' });
    console.log('✅ Local dependencies restored\n');
  } catch (error) {
    console.warn('⚠️  npm install failed, you may need to run it manually\n');
  }
  
  console.log('🎉 Remote publish complete!\n');
  console.log('📦 Published packages:');
  console.log('   @autodev/mpp-core@' + mppCoreVersion);
  console.log('   @autodev/cli@' + currentVersion);
  console.log('\n💡 To install globally:');
  console.log('   npm install -g @autodev/cli');
  console.log('\n💡 To use:');
  console.log('   autodev');
}

main().catch(error => {
  console.error('❌ Unexpected error:', error);
  
  // Try to restore package.json
  if (existsSync(packageJsonBackupPath)) {
    console.log('🔄 Restoring package.json...');
    copyFileSync(packageJsonBackupPath, packageJsonPath);
  }
  
  process.exit(1);
});
