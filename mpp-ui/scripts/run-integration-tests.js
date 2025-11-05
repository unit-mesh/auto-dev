#!/usr/bin/env node

/**
 * Integration Test Runner for CodingAgent
 *
 * This script provides a convenient way to run integration tests with
 * proper setup, configuration, and reporting.
 */

import { spawn } from 'child_process';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Test categories and their configurations
const TEST_CATEGORIES = {
  simple: {
    name: 'Simple Robustness Tests',
    file: 'simple-robustness.test.ts',
    timeout: 300000, // 5 minutes
    description: 'Basic tool usage and system prompt validation'
  },
  video: {
    name: 'Video Support Business Scenario',
    file: 'business-video-support.test.ts',
    timeout: 600000, // 10 minutes
    description: 'Adding video support to BlogPost entity'
  },
  jwt: {
    name: 'JWT Authentication Business Scenario',
    file: 'business-jwt-auth.test.ts',
    timeout: 900000, // 15 minutes
    description: 'Implementing Spring Security with JWT'
  },
  upgrade: {
    name: 'Spring Boot Upgrade Business Scenario',
    file: 'business-spring-upgrade.test.ts',
    timeout: 1200000, // 20 minutes
    description: 'Upgrading to Spring Boot 3.x with error handling'
  },
  graphql: {
    name: 'GraphQL API Business Scenario',
    file: 'business-graphql-api.test.ts',
    timeout: 900000, // 15 minutes
    description: 'Implementing GraphQL with subscriptions'
  }
};

// Parse command line arguments
const args = process.argv.slice(2);
const category = args[0];
const options = {
  verbose: args.includes('--verbose') || args.includes('-v'),
  keepProjects: args.includes('--keep-projects') || args.includes('-k'),
  quiet: args.includes('--quiet') || args.includes('-q'),
  coverage: args.includes('--coverage') || args.includes('-c'),
  help: args.includes('--help') || args.includes('-h')
};

function showHelp() {
  console.log(`
🧪 CodingAgent Integration Test Runner

Usage: node scripts/run-integration-tests.js [category] [options]

Categories:
  simple     ${TEST_CATEGORIES.simple.description}
  video      ${TEST_CATEGORIES.video.description}
  jwt        ${TEST_CATEGORIES.jwt.description}
  upgrade    ${TEST_CATEGORIES.upgrade.description}
  graphql    ${TEST_CATEGORIES.graphql.description}
  all        Run all integration tests
  business   Run all business scenario tests

Options:
  --verbose, -v      Show detailed output
  --keep-projects, -k Keep test projects after completion (for debugging)
  --quiet, -q        Minimize output
  --coverage, -c     Generate coverage report
  --help, -h         Show this help message

Examples:
  node scripts/run-integration-tests.js simple
  node scripts/run-integration-tests.js all --verbose
  node scripts/run-integration-tests.js jwt --keep-projects
  node scripts/run-integration-tests.js business --coverage
`);
}

function validateEnvironment() {
  console.log('🔍 Validating test environment...');
  
  // Check Node.js version
  const nodeVersion = process.version;
  const majorVersion = parseInt(nodeVersion.slice(1));
  if (majorVersion < 20) {
    console.error(`❌ Node.js version ${nodeVersion} is not supported. Please use Node.js 20 or higher.`);
    process.exit(1);
  }
  console.log(`✅ Node.js version: ${nodeVersion}`);
  
  // Check if we're in the right directory
  const packageJsonPath = path.join(process.cwd(), 'package.json');
  if (!fs.existsSync(packageJsonPath)) {
    console.error('❌ package.json not found. Please run this script from the mpp-ui directory.');
    process.exit(1);
  }
  
  const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
  if (packageJson.name !== '@autodev/cli') {
    console.error('❌ Not in the correct directory. Please run this script from the mpp-ui directory.');
    process.exit(1);
  }
  console.log(`✅ Working directory: ${process.cwd()}`);
  
  // Check if CodingAgent is built
  const distPath = path.join(process.cwd(), 'dist/index.js');
  if (!fs.existsSync(distPath)) {
    console.log('⚠️  CodingAgent not built. Building now...');
    return buildCodingAgent();
  }
  console.log(`✅ CodingAgent built: ${distPath}`);
  
  return Promise.resolve();
}

function buildCodingAgent() {
  return new Promise((resolve, reject) => {
    console.log('🔨 Building CodingAgent...');
    
    const buildProcess = spawn('npm', ['run', 'build'], {
      stdio: options.verbose ? 'inherit' : 'pipe',
      cwd: process.cwd()
    });
    
    buildProcess.on('close', (code) => {
      if (code === 0) {
        console.log('✅ CodingAgent built successfully');
        resolve();
      } else {
        console.error('❌ Failed to build CodingAgent');
        reject(new Error(`Build failed with exit code ${code}`));
      }
    });
    
    buildProcess.on('error', (error) => {
      console.error('❌ Build process error:', error.message);
      reject(error);
    });
  });
}

function runTests(testFiles) {
  return new Promise((resolve, reject) => {
    const vitestArgs = ['run'];
    
    // Add test files
    testFiles.forEach(file => {
      vitestArgs.push(`src/test/integration/${file}`);
    });
    
    // Add options
    if (options.verbose) {
      vitestArgs.push('--reporter=verbose');
    }
    
    if (options.coverage) {
      vitestArgs.push('--coverage');
    }
    
    // Set environment variables
    const env = { ...process.env };
    if (options.keepProjects) {
      env.KEEP_TEST_PROJECTS = 'true';
    }
    if (options.quiet) {
      env.QUIET = 'true';
    }
    
    console.log(`🚀 Running tests: ${testFiles.join(', ')}`);
    console.log(`📝 Command: npx vitest ${vitestArgs.join(' ')}`);
    
    const testProcess = spawn('npx', ['vitest', ...vitestArgs], {
      stdio: 'inherit',
      cwd: process.cwd(),
      env
    });
    
    testProcess.on('close', (code) => {
      if (code === 0) {
        console.log('🎉 All tests completed successfully!');
        resolve();
      } else {
        console.error(`❌ Tests failed with exit code ${code}`);
        reject(new Error(`Tests failed with exit code ${code}`));
      }
    });
    
    testProcess.on('error', (error) => {
      console.error('❌ Test process error:', error.message);
      reject(error);
    });
  });
}

async function main() {
  if (options.help) {
    showHelp();
    return;
  }
  
  try {
    // Validate environment and build if necessary
    await validateEnvironment();
    
    // Determine which tests to run
    let testFiles = [];
    
    if (!category || category === 'all') {
      testFiles = Object.values(TEST_CATEGORIES).map(cat => cat.file);
      console.log('📋 Running all integration tests');
    } else if (category === 'business') {
      testFiles = [
        TEST_CATEGORIES.video.file,
        TEST_CATEGORIES.jwt.file,
        TEST_CATEGORIES.upgrade.file,
        TEST_CATEGORIES.graphql.file
      ];
      console.log('📋 Running business scenario tests');
    } else if (TEST_CATEGORIES[category]) {
      testFiles = [TEST_CATEGORIES[category].file];
      console.log(`📋 Running ${TEST_CATEGORIES[category].name}`);
    } else {
      console.error(`❌ Unknown test category: ${category}`);
      console.log('Available categories: simple, video, jwt, upgrade, graphql, business, all');
      process.exit(1);
    }
    
    // Run the tests
    await runTests(testFiles);
    
  } catch (error) {
    console.error('❌ Test execution failed:', error.message);
    process.exit(1);
  }
}

// Handle process signals
process.on('SIGINT', () => {
  console.log('\n⚠️  Test execution interrupted by user');
  process.exit(130);
});

process.on('SIGTERM', () => {
  console.log('\n⚠️  Test execution terminated');
  process.exit(143);
});

// Run the main function
main().catch(error => {
  console.error('❌ Unexpected error:', error);
  process.exit(1);
});
