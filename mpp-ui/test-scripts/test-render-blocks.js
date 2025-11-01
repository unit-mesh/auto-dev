#!/usr/bin/env node

/**
 * Test script for block rendering
 * Tests the parseCodeBlocksSync function
 */

import { parseCodeBlocksSync } from '../dist/utils/renderUtils.js';

console.log('🧪 Testing Block Parsing\n');

// Test case 1: Simple text
console.log('Test 1: Simple text');
const test1 = "This is a simple text paragraph.";
const result1 = parseCodeBlocksSync(test1);
console.log('Input:', test1);
console.log('Blocks:', result1.length);
result1.forEach((block, i) => {
  console.log(`  Block ${i}: lang="${block.languageId}", text="${block.text.substring(0, 30)}..."`);
});
console.log('');

// Test case 2: Text with code block
console.log('Test 2: Text + Code block');
const test2 = `Here's a Java program:

\`\`\`java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
\`\`\`

This is the output.`;

const result2 = parseCodeBlocksSync(test2);
console.log('Input lines:', test2.split('\n').length);
console.log('Blocks:', result2.length);
result2.forEach((block, i) => {
  console.log(`  Block ${i}:`);
  console.log(`    Language: "${block.languageId}"`);
  console.log(`    Text length: ${block.text.length}`);
  console.log(`    Text preview: "${block.text.substring(0, 50)}..."`);
  console.log(`    Complete: ${block.isComplete}`);
});
console.log('');

// Test case 3: Multiple code blocks
console.log('Test 3: Multiple code blocks');
const test3 = `Introduction text.

\`\`\`java
class Example {}
\`\`\`

Middle text.

\`\`\`bash
echo "Hello"
\`\`\`

Final text.`;

const result3 = parseCodeBlocksSync(test3);
console.log('Blocks:', result3.length);
result3.forEach((block, i) => {
  console.log(`  Block ${i}: lang="${block.languageId}", lines=${block.text.split('\n').length}`);
});
console.log('');

// Test case 4: Unclosed code block (streaming)
console.log('Test 4: Unclosed code block (streaming)');
const test4 = `Here's some code:

\`\`\`java
public class Test {
    // Still typing...`;

const result4 = parseCodeBlocksSync(test4);
console.log('Blocks:', result4.length);
result4.forEach((block, i) => {
  console.log(`  Block ${i}:`);
  console.log(`    Language: "${block.languageId}"`);
  console.log(`    Complete: ${block.isComplete}`);
  console.log(`    Text: "${block.text.substring(0, 50)}..."`);
});
console.log('');

// Test case 5: Real world example
console.log('Test 5: Real world AI response');
const test5 = `Here's a Java "Hello World" program:

## Basic version

\`\`\`java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
\`\`\`

## How to run:

1. Save as \`HelloWorld.java\`
2. Compile: \`javac HelloWorld.java\`
3. Run: \`java HelloWorld\`

This is the traditional starting point!`;

const result5 = parseCodeBlocksSync(test5);
console.log('Blocks:', result5.length);
result5.forEach((block, i) => {
  console.log(`\n  Block ${i}:`);
  console.log(`    Language: "${block.languageId}"`);
  console.log(`    Lines: ${block.text.split('\n').length}`);
  console.log(`    Preview: "${block.text.substring(0, 60)}..."`);
});

console.log('\n✅ Test completed\n');
console.log('Expected behavior:');
console.log('- Text blocks should have empty languageId');
console.log('- Code blocks should have the language (java, bash, etc)');
console.log('- All content should be captured (no missing text)');
console.log('- Blocks should be in correct order');

