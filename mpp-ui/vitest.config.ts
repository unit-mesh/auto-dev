import { defineConfig } from 'vitest/config';

export default defineConfig({
  esbuild: {
    jsx: 'transform',
    jsxFactory: 'React.createElement',
    jsxFragment: 'React.Fragment',
  },
  test: {
    globals: true,
    environment: 'node',
    include: [
      'src/jsMain/typescript/**/*.test.ts',
      'src/jsMain/typescript/**/*.test.tsx',
      'src/test/integration/**/*.test.ts',
      'src/test/integration/**/*.test.js',
      'src/test/integration-v2/**/*.test.ts'
    ],
    exclude: [
      'node_modules/**',
      'dist/**',
      'build/**'
    ],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
    },
    testTimeout: 300000, // 5 minutes for integration tests
    hookTimeout: 60000,  // 1 minute for setup/teardown
  },
});

