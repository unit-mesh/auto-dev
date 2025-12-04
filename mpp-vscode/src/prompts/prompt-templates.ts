/**
 * Prompt Templates for CodeLens Actions
 * 
 * Based on autodev-vscode's Velocity templates, converted to TypeScript template strings.
 */

export interface AutoDocContext {
  language: string;
  code: string;
  startSymbol: string;
  endSymbol: string;
  originalComments?: string[];
  chatContext?: string;
}

export interface AutoTestContext {
  language: string;
  sourceCode: string;
  className?: string;
  methodName?: string;
  imports?: string;
  relatedClasses?: string;
  chatContext?: string;
  isNewFile?: boolean;
  testFramework?: string;
}

export interface AutoMethodContext {
  language: string;
  code: string;
  methodSignature: string;
  className?: string;
  chatContext?: string;
}

export const LANGUAGE_COMMENT_MAP: Record<string, { start: string; end: string }> = {
  typescript: { start: '/**', end: '*/' },
  javascript: { start: '/**', end: '*/' },
  typescriptreact: { start: '/**', end: '*/' },
  javascriptreact: { start: '/**', end: '*/' },
  java: { start: '/**', end: '*/' },
  kotlin: { start: '/**', end: '*/' },
  python: { start: '"""', end: '"""' },
  go: { start: '/*', end: '*/' },
  rust: { start: '///', end: '' },
  csharp: { start: '///', end: '' },
};

export function generateAutoDocPrompt(context: AutoDocContext): string {
  let prompt = `Write documentation for the following ${context.language} code.\n\n`;
  if (context.chatContext) prompt += `Additional context:\n${context.chatContext}\n\n`;
  prompt += `Requirements:
- Start your documentation with \`${context.startSymbol}\` and end with \`${context.endSymbol}\`
- Include a brief description of what the code does
- Document parameters and return values if applicable
- Use proper ${context.language} documentation format

Here is the code:
\`\`\`${context.language}
${context.code}
\`\`\`

Please write the documentation inside a Markdown code block.`;
  return prompt;
}

export function generateAutoTestPrompt(context: AutoTestContext): string {
  let prompt = `Write unit tests for the following ${context.language} code.\n\n`;
  if (context.chatContext) prompt += `Additional context:\n${context.chatContext}\n\n`;
  if (context.relatedClasses) prompt += `Related classes:\n${context.relatedClasses}\n\n`;
  if (context.imports) prompt += `Imports used:\n${context.imports}\n\n`;
  if (context.testFramework) prompt += `Use ${context.testFramework} testing framework.\n\n`;
  prompt += `Source code to test:
\`\`\`${context.language}
${context.sourceCode}
\`\`\`

Requirements:
- Write comprehensive unit tests covering main functionality
- Include edge cases and error handling tests
- Use descriptive test names
- Follow ${context.language} testing best practices
`;
  if (context.className) prompt += `Generate tests for class: ${context.className}\n`;
  if (context.methodName) prompt += `Focus on method: ${context.methodName}\n`;
  prompt += `\nStart the test code with a ${context.language} Markdown code block:`;
  return prompt;
}

export function generateAutoMethodPrompt(context: AutoMethodContext): string {
  let prompt = `Complete the implementation for the following ${context.language} method.\n\n`;
  if (context.chatContext) prompt += `Additional context:\n${context.chatContext}\n\n`;
  if (context.className) prompt += `Class: ${context.className}\n`;
  prompt += `Method signature:
\`\`\`${context.language}
${context.methodSignature}
\`\`\`

Current code context:
\`\`\`${context.language}
${context.code}
\`\`\`

Requirements:
- Implement the method body based on the signature and context
- Follow ${context.language} best practices
- Handle edge cases appropriately
- Add inline comments for complex logic

Please provide the complete method implementation inside a Markdown code block.`;
  return prompt;
}

export function parseCodeBlock(response: string, language?: string): string {
  const codeBlockRegex = /```(?:\w+)?\n([\s\S]*?)```/g;
  const matches = [...response.matchAll(codeBlockRegex)];
  if (matches.length > 0) return matches[0][1].trim();
  return response.trim();
}

export function getTestFramework(language: string): string {
  const frameworks: Record<string, string> = {
    typescript: 'Jest or Vitest',
    javascript: 'Jest or Vitest',
    typescriptreact: 'Jest with React Testing Library',
    javascriptreact: 'Jest with React Testing Library',
    java: 'JUnit 5',
    kotlin: 'JUnit 5 or Kotest',
    python: 'pytest',
    go: 'testing package',
    rust: 'built-in test framework',
    csharp: 'xUnit or NUnit',
  };
  return frameworks[language] || 'appropriate testing framework';
}

export function getTestFilePath(sourcePath: string, language: string): string {
  const pathParts = sourcePath.split('/');
  const fileName = pathParts.pop() || '';
  const dirPath = pathParts.join('/');
  const extIndex = fileName.lastIndexOf('.');
  const baseName = extIndex > 0 ? fileName.substring(0, extIndex) : fileName;
  const ext = extIndex > 0 ? fileName.substring(extIndex) : '';
  
  const testSuffixes: Record<string, string> = {
    typescript: '.test.ts',
    javascript: '.test.js',
    typescriptreact: '.test.tsx',
    javascriptreact: '.test.jsx',
    java: 'Test.java',
    kotlin: 'Test.kt',
    python: '.py',
    go: '_test.go',
    rust: '',
    csharp: 'Tests.cs',
  };
  
  const testSuffix = testSuffixes[language] || `.test${ext}`;
  
  if (language === 'java' || language === 'kotlin') {
    const testDir = dirPath.replace('/src/main/', '/src/test/');
    return `${testDir}/${baseName}${testSuffix}`;
  }
  // Python uses test_ prefix for pytest convention
  if (language === 'python') return `${dirPath}/test_${baseName}${testSuffix}`;
  return `${dirPath}/${baseName}${testSuffix}`;
}
