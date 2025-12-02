/**
 * SketchRenderer - Main content renderer
 * 
 * Parses LLM response content and dispatches to appropriate sub-renderers:
 * - Markdown/Text -> MarkdownRenderer
 * - Code -> CodeBlockRenderer
 * - Diff -> DiffRenderer
 * - Thinking -> ThinkingRenderer
 * - Terminal -> TerminalRenderer
 * - Mermaid -> MermaidRenderer
 * - DevIn -> DevInRenderer
 */

import React from 'react';
import { parseCodeBlocks, CodeBlock } from '../../utils/codeFence';
import { CodeBlockRenderer } from './CodeBlockRenderer';
import { DiffRenderer } from './DiffRenderer';
import { ThinkingRenderer } from './ThinkingRenderer';
import { TerminalRenderer } from './TerminalRenderer';
import { MarkdownRenderer } from './MarkdownRenderer';
import './SketchRenderer.css';

interface SketchRendererProps {
  content: string;
  isComplete?: boolean;
  onAction?: (action: string, data: any) => void;
}

export const SketchRenderer: React.FC<SketchRendererProps> = ({
  content,
  isComplete = false,
  onAction
}) => {
  const blocks = parseCodeBlocks(content);

  return (
    <div className="sketch-renderer">
      {blocks.map((block, index) => {
        const isLastBlock = index === blocks.length - 1;
        const blockIsComplete = block.isComplete && (isComplete || !isLastBlock);

        return (
          <div key={index} className="sketch-block">
            {renderBlock(block, blockIsComplete, onAction)}
          </div>
        );
      })}
      
      {!isComplete && content.length > 0 && (
        <div className="sketch-loading">
          <div className="loading-indicator">
            <span className="dot"></span>
            <span className="dot"></span>
            <span className="dot"></span>
          </div>
        </div>
      )}
    </div>
  );
};

function renderBlock(
  block: CodeBlock,
  isComplete: boolean,
  onAction?: (action: string, data: any) => void
): React.ReactNode {
  const languageId = block.languageId.toLowerCase();

  switch (languageId) {
    case 'markdown':
    case 'md':
    case '':
      // Plain text or markdown
      if (block.text.trim()) {
        return <MarkdownRenderer content={block.text} />;
      }
      return null;

    case 'diff':
    case 'patch':
      return (
        <DiffRenderer
          diffContent={block.text}
          onAction={onAction}
        />
      );

    case 'thinking':
      return (
        <ThinkingRenderer
          content={block.text}
          isComplete={isComplete}
        />
      );

    case 'bash':
    case 'shell':
    case 'sh':
    case 'zsh':
      return (
        <TerminalRenderer
          command={block.text}
          isComplete={isComplete}
          onAction={onAction}
        />
      );

    case 'mermaid':
    case 'mmd':
      // TODO: Implement MermaidRenderer
      return (
        <CodeBlockRenderer
          code={block.text}
          language="mermaid"
          isComplete={isComplete}
        />
      );

    case 'devin':
      // TODO: Implement DevInRenderer
      return (
        <CodeBlockRenderer
          code={block.text}
          language="devin"
          isComplete={isComplete}
        />
      );

    case 'plan':
      // TODO: Implement PlanRenderer
      return (
        <CodeBlockRenderer
          code={block.text}
          language="plan"
          isComplete={isComplete}
        />
      );

    default:
      // Regular code block
      return (
        <CodeBlockRenderer
          code={block.text}
          language={block.languageId}
          isComplete={isComplete}
          onAction={onAction}
        />
      );
  }
}

