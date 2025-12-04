/**
 * MarkdownRenderer - Renders markdown content
 * Uses react-markdown with GFM support
 * 
 * Mirrors mpp-idea's JewelMarkdownRenderer and mpp-ui's ContentBlocks
 */

import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import './MarkdownRenderer.css';

interface MarkdownRendererProps {
  content: string;
}

/**
 * Filter out <devin>...</devin> tags and their content from markdown
 * These are tool call blocks that should not be displayed as text
 */
function filterDevinTags(content: string): string {
  // Remove <devin>...</devin> blocks (including multiline)
  let filtered = content.replace(/<devin>[\s\S]*?<\/devin>/gi, '');
  // Remove unclosed <devin> tags at the end (streaming)
  filtered = filtered.replace(/<devin>[\s\S]*$/gi, '');
  // Remove standalone </devin> tags
  filtered = filtered.replace(/<\/devin>/gi, '');
  return filtered.trim();
}

export const MarkdownRenderer: React.FC<MarkdownRendererProps> = ({ content }) => {
  const filteredContent = filterDevinTags(content);

  if (!filteredContent.trim()) {
    return null;
  }

  return (
    <div className="markdown-renderer">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          // Custom heading renderers
          h1: ({ children }) => <h1 className="md-heading md-h1">{children}</h1>,
          h2: ({ children }) => <h2 className="md-heading md-h2">{children}</h2>,
          h3: ({ children }) => <h3 className="md-heading md-h3">{children}</h3>,
          h4: ({ children }) => <h4 className="md-heading md-h4">{children}</h4>,
          h5: ({ children }) => <h5 className="md-heading md-h5">{children}</h5>,
          h6: ({ children }) => <h6 className="md-heading md-h6">{children}</h6>,
          
          // Paragraph
          p: ({ children }) => <p className="md-paragraph">{children}</p>,
          
          // Lists
          ul: ({ children }) => <ul className="md-list md-ul">{children}</ul>,
          ol: ({ children }) => <ol className="md-list md-ol">{children}</ol>,
          li: ({ children }) => <li className="md-list-item">{children}</li>,
          
          // Task list items (GFM)
          input: ({ checked, ...props }) => (
            <input
              type="checkbox"
              checked={checked}
              readOnly
              className="md-checkbox"
              {...props}
            />
          ),
          
          // Links
          a: ({ href, children }) => (
            <a href={href} className="md-link" target="_blank" rel="noopener noreferrer">
              {children}
            </a>
          ),
          
          // Inline code
          code: ({ className, children, ...props }) => {
            // Check if it's a code block (has language class)
            const isBlock = className?.startsWith('language-');
            if (isBlock) {
              return (
                <code className={`md-code-block ${className}`} {...props}>
                  {children}
                </code>
              );
            }
            return <code className="md-inline-code" {...props}>{children}</code>;
          },
          
          // Block code
          pre: ({ children }) => <pre className="md-pre">{children}</pre>,
          
          // Blockquote
          blockquote: ({ children }) => (
            <blockquote className="md-blockquote">{children}</blockquote>
          ),
          
          // Table (GFM)
          table: ({ children }) => (
            <div className="md-table-wrapper">
              <table className="md-table">{children}</table>
            </div>
          ),
          thead: ({ children }) => <thead className="md-thead">{children}</thead>,
          tbody: ({ children }) => <tbody className="md-tbody">{children}</tbody>,
          tr: ({ children }) => <tr className="md-tr">{children}</tr>,
          th: ({ children }) => <th className="md-th">{children}</th>,
          td: ({ children }) => <td className="md-td">{children}</td>,
          
          // Horizontal rule
          hr: () => <hr className="md-hr" />,
          
          // Strong and emphasis
          strong: ({ children }) => <strong className="md-strong">{children}</strong>,
          em: ({ children }) => <em className="md-em">{children}</em>,
          
          // Strikethrough (GFM)
          del: ({ children }) => <del className="md-del">{children}</del>,
          
          // Images
          img: ({ src, alt }) => (
            <img src={src} alt={alt} className="md-image" loading="lazy" />
          ),
        }}
      >
        {filteredContent}
      </ReactMarkdown>
    </div>
  );
};

