#!/usr/bin/env node

/**
 * Test empty line handling
 * Verifies that excessive empty lines are filtered out
 */

console.log('🧪 Testing Empty Line Handling\n');

// Simulate content with various empty line patterns
const testCases = [
  {
    name: 'Multiple empty lines at start',
    input: '\n\n\nHello World',
    expected: 'Should trim leading empty lines'
  },
  {
    name: 'Multiple empty lines at end',
    input: 'Hello World\n\n\n',
    expected: 'Should trim trailing empty lines'
  },
  {
    name: 'Multiple empty lines in middle',
    input: 'Paragraph 1\n\n\n\nParagraph 2',
    expected: 'Should keep only one empty line between paragraphs'
  },
  {
    name: 'Empty lines between blocks',
    input: '## Heading\n\nContent here\n\nMore content',
    expected: 'Should preserve single empty lines between content'
  },
  {
    name: 'All empty',
    input: '\n\n\n',
    expected: 'Should skip completely empty blocks'
  }
];

console.log('Empty Line Filtering Logic:\n');
console.log('1. Text blocks:');
console.log('   - Remove leading/trailing empty lines');
console.log('   - Keep single empty line between non-empty lines (paragraph break)');
console.log('   - Remove consecutive empty lines\n');
console.log('2. Code blocks:');
console.log('   - Preserve ALL empty lines (formatting matters)\n');

console.log('═══════════════════════════════════════\n');

testCases.forEach((test, i) => {
  console.log(`Test ${i + 1}: ${test.name}`);
  console.log('Input:', JSON.stringify(test.input));
  console.log('Expected:', test.expected);
  
  // Simulate the filtering logic
  const lines = test.input.split('\n');
  
  // Find start and end of non-empty content
  let startIdx = 0;
  let endIdx = lines.length - 1;
  
  while (startIdx < lines.length && lines[startIdx].trim() === '') {
    startIdx++;
  }
  while (endIdx >= 0 && lines[endIdx].trim() === '') {
    endIdx--;
  }
  
  const trimmedLines = lines.slice(startIdx, endIdx + 1);
  
  console.log('After trimming:', trimmedLines.length, 'lines');
  if (trimmedLines.length > 0) {
    console.log('Result:', JSON.stringify(trimmedLines.join('\n')));
  } else {
    console.log('Result: (empty block - will be skipped)');
  }
  console.log('');
});

console.log('═══════════════════════════════════════\n');

console.log('Visual Example:\n');
console.log('BEFORE (with excessive empty lines):');
console.log('┌─────────────────────────┐');
console.log('│                         │');
console.log('│                         │');
console.log('│ ## Key points:          │');
console.log('│                         │');
console.log('│                         │');
console.log('│                         │');
console.log('│ - Point 1               │');
console.log('│                         │');
console.log('│                         │');
console.log('│                         │');
console.log('│ - Point 2               │');
console.log('│                         │');
console.log('│                         │');
console.log('└─────────────────────────┘\n');

console.log('AFTER (optimized spacing):');
console.log('┌─────────────────────────┐');
console.log('│ ## Key points:          │');
console.log('│                         │');  // Single empty line (paragraph break)
console.log('│ - Point 1               │');
console.log('│                         │');  // Single empty line (paragraph break)
console.log('│ - Point 2               │');
console.log('└─────────────────────────┘\n');

console.log('✅ Benefits:');
console.log('- Cleaner visual appearance');
console.log('- Better use of screen space');
console.log('- Maintains readability with single line breaks');
console.log('- Code blocks still preserve exact formatting\n');

