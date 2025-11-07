/**
 * Tests for MessageRenderer components
 */

import React from 'react';
import { render } from 'ink-testing-library';
import { 
  MessageBubble, 
  TreeMessageBubble, 
  createTreeMessage,
  type Message,
  type TreeMessage 
} from '../MessageRenderer.js';

describe('MessageRenderer', () => {
  describe('MessageBubble', () => {
    it('renders user message correctly', () => {
      const message: Message = {
        role: 'user',
        content: 'Hello, world!'
      };

      const { lastFrame } = render(<MessageBubble message={message} />);
      expect(lastFrame()).toContain('Hello, world!');
    });

    it('renders assistant message correctly', () => {
      const message: Message = {
        role: 'assistant',
        content: 'Hello back!'
      };

      const { lastFrame } = render(<MessageBubble message={message} />);
      expect(lastFrame()).toContain('Hello back!');
    });

    it('renders tree-structured content', () => {
      const message: Message = {
        role: 'assistant',
        content: '● Main action\n  ⎿ Detail information'
      };

      const { lastFrame } = render(<MessageBubble message={message} />);
      const output = lastFrame();
      expect(output).toContain('●');
      expect(output).toContain('⎿');
    });

    it('renders compiling message with spinner', () => {
      const message: Message = {
        role: 'compiling',
        content: 'Building project...'
      };

      const { lastFrame } = render(<MessageBubble message={message} />);
      expect(lastFrame()).toContain('Building project...');
    });
  });

  describe('TreeMessageBubble', () => {
    it('renders simple tree message', () => {
      const message: TreeMessage = createTreeMessage.action('Test action');

      const { lastFrame } = render(<TreeMessageBubble message={message} />);
      const output = lastFrame();
      expect(output).toContain('●');
      expect(output).toContain('Test action');
    });

    it('renders nested tree structure', () => {
      const message: TreeMessage = createTreeMessage.action(
        'Parent action',
        [
          createTreeMessage.detail('Child detail'),
          createTreeMessage.success('Child success')
        ]
      );

      const { lastFrame } = render(<TreeMessageBubble message={message} />);
      const output = lastFrame();
      expect(output).toContain('●');
      expect(output).toContain('⎿');
      expect(output).toContain('Parent action');
      expect(output).toContain('Child detail');
      expect(output).toContain('Child success');
    });

    it('renders file operation message', () => {
      const message: TreeMessage = createTreeMessage.fileOperation(
        'edit file',
        'test.ts',
        ['Added 1 line', 'Removed 2 lines']
      );

      const { lastFrame } = render(<TreeMessageBubble message={message} />);
      const output = lastFrame();
      expect(output).toContain('edit file');
      expect(output).toContain('test.ts');
      expect(output).toContain('Added 1 line');
      expect(output).toContain('Removed 2 lines');
    });
  });

  describe('createTreeMessage helpers', () => {
    it('creates action message correctly', () => {
      const message = createTreeMessage.action('Test action');
      expect(message.type).toBe('action');
      expect(message.content).toBe('Test action');
    });

    it('creates file operation message correctly', () => {
      const message = createTreeMessage.fileOperation('edit', 'file.ts', ['detail']);
      expect(message.type).toBe('success');
      expect(message.content).toBe('edit');
      expect(message.metadata?.file).toBe('file.ts');
      expect(message.children).toHaveLength(1);
    });
  });
});
