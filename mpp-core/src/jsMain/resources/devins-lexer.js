/**
 * DevIn Language Lexer Implementation using Moo.js
 * 
 * This file implements the DevIn language lexer using the Moo.js library.
 * It defines all the token types and lexer states needed to tokenize DevIn source code.
 */

// Import Moo.js (assuming it's available globally or via module system)
// const moo = require('moo'); // For Node.js
// For browser, assume moo is available globally

/**
 * DevIn Lexer implementation using Moo.js
 */
class DevInLexerJS {
    constructor() {
        this.lexer = null;
        this.initializeLexer();
    }
    
    initializeLexer() {
        // Define the main lexer with states
        this.lexer = moo.states({
            // Initial state - main content parsing
            main: {
                // Front matter start (must be at beginning of line)
                frontMatterStart: {
                    match: /^---(?=\s*$)/,
                    lineBreaks: false,
                    push: 'frontMatter'
                },

                // Code block start with optional language
                codeBlockStart: {
                    match: /```([a-zA-Z0-9_\-+.]*)?/,
                    push: 'codeBlock',
                    value: (s) => s.slice(3) // Remove ``` prefix
                },

                // Special syntax markers
                agentStart: { match: '@', push: 'agent' },
                commandStart: { match: '/', push: 'command' },
                variableStart: { match: '$', push: 'variable' },
                sharp: { match: '#', push: 'expression' },

                // Comments
                lineComment: /\/\/[^\r\n]*/,
                blockComment: /\/\*[^]*?\*\//,
                contentComment: /\[[^\]]*\][^\t\r\n]*/,

                // Text content (everything else that's not special)
                textSegment: {
                    match: /[^@\/$#\n`]+/,
                    lineBreaks: false
                },

                // Newlines and whitespace
                newline: { match: /\r?\n/, lineBreaks: true },
                whitespace: /[ \t]+/,

                // Single backticks (not code blocks)
                backtick: '`',
            },
            
            // Front matter state (YAML-like)
            frontMatter: {
                frontMatterEnd: {
                    match: /^---(?=\s*$)/,
                    lineBreaks: false,
                    pop: 1
                },

                // Lifecycle keywords
                when: 'when',
                onStreaming: 'onStreaming',
                beforeStreaming: 'beforeStreaming',
                onStreamingEnd: 'onStreamingEnd',
                afterStreaming: 'afterStreaming',
                functions: 'functions',

                // Front matter keys and values
                frontMatterKey: /[a-zA-Z0-9][_\-a-zA-Z0-9]*/,
                colon: ':',
                string: /"(?:[^"\\]|\\.)*"/,
                singleString: /'(?:[^'\\]|\\.)*'/,
                number: /[0-9]+(?:\.[0-9]+)?/,
                boolean: /true|false|TRUE|FALSE/,
                date: /[0-9]{4}-[0-9]{2}-[0-9]{2}/,

                // Brackets and braces
                lbracket: '[',
                rbracket: ']',
                comma: ',',
                openBrace: '{',
                closeBrace: '}',

                // Operators
                arrow: '=>',
                access: '::',
                process: '->',

                // Pattern expressions
                patternExpr: /\/[^\r\n\\\/]+\/[gimuy]*/,

                // Whitespace and structure
                newline: { match: /\r?\n/, lineBreaks: true },
                whitespace: /[ \t]+/,
                indent: /^[ \t]+/,
                comment: /\/\/[^\r\n]*/,

                // Foreign function syntax
                lparen: '(',
                rparen: ')',
                pipe: '|',
            },
            
            // Agent block state
            agent: {
                identifier: {
                    match: /[a-zA-Z0-9][_\-a-zA-Z0-9]*/,
                    pop: 1
                },
                string: {
                    match: /"(?:[^"\\]|\\.)*"/,
                    pop: 1,
                    value: (s) => s.slice(1, -1) // Remove quotes
                },
                whitespace: /[ \t]+/,
                newline: { match: /\r?\n/, lineBreaks: true, pop: 1 },
                fallback: { match: /./, pop: 1 },
            },
            
            // Command block state
            command: {
                identifier: {
                    match: /[a-zA-Z0-9][_\-a-zA-Z0-9.]*/,
                    next: 'commandAfterName'
                },
                whitespace: /[ \t]+/,
                newline: { match: /\r?\n/, lineBreaks: true, pop: 1 },
                fallback: { match: /./, pop: 1 },
            },

            // After command name
            commandAfterName: {
                colon: { match: ':', next: 'commandValue' },
                whitespace: /[ \t]+/,
                newline: { match: /\r?\n/, lineBreaks: true, pop: 1 },
                fallback: { match: /./, pop: 1 },
            },

            // Command value state
            commandValue: {
                commandProp: {
                    match: /[^\s\t\r\n#]+/,
                    next: 'commandAfterProp'
                },
                sharp: { match: '#', next: 'lineInfo' },
                whitespace: /[ \t]+/,
                newline: { match: /\r?\n/, lineBreaks: true, pop: 1 },
                fallback: { match: /./, pop: 1 },
            },

            // After command property
            commandAfterProp: {
                sharp: { match: '#', next: 'lineInfo' },
                whitespace: /[ \t]+/,
                newline: { match: /\r?\n/, lineBreaks: true, pop: 1 },
                fallback: { match: /./, pop: 1 },
            },

            // Line info state (for #L1C1-L2C2 format)
            lineInfo: {
                lineInfo: {
                    match: /L[0-9]+(?:C[0-9]+)?(?:-L[0-9]+(?:C[0-9]+)?)?/,
                    pop: 1
                },
                whitespace: /[ \t]+/,
                newline: { match: /\r?\n/, lineBreaks: true, pop: 1 },
                fallback: { match: /./, pop: 1 },
            },
            
            // Variable block state
            variable: {
                identifier: /[a-zA-Z0-9][_\-a-zA-Z0-9]*/,
                openBrace: { match: '{', next: 'variableAccess' },
                whitespace: /[ \t]+/,
                newline: { match: /\r?\n/, lineBreaks: true, pop: 1 },
                fallback: { match: /./, pop: 1 },
            },
            
            // Variable access state (for ${var.prop} format)
            variableAccess: {
                identifier: /[a-zA-Z0-9][_\-a-zA-Z0-9]*/,
                dot: '.',
                closeBrace: { match: '}', pop: 1 },
                whitespace: /[ \t]+/,
            },
            
            // Expression block state
            expression: {
                // Keywords
                if: 'if',
                else: 'else',
                elseif: 'elseif',
                end: 'end',
                endif: 'endif',
                case: 'case',
                default: 'default',
                when: 'when',
                from: 'from',
                where: 'where',
                select: 'select',
                condition: 'condition',
                
                // Lifecycle keywords
                onStreaming: 'onStreaming',
                beforeStreaming: 'beforeStreaming',
                onStreamingEnd: 'onStreamingEnd',
                afterStreaming: 'afterStreaming',
                functions: 'functions',
                
                // Operators
                eqeq: '==',
                neq: '!=',
                lte: '<=',
                gte: '>=',
                lt: '<',
                gt: '>',
                andand: '&&',
                and: 'and',
                oror: '||',
                not: '!',
                
                // Punctuation
                lparen: '(',
                rparen: ')',
                openBrace: '{',
                closeBrace: '}',
                comma: ',',
                pipe: '|',
                dot: '.',
                colon: ':',
                access: '::',
                process: '->',
                
                // Literals
                number: /[0-9]+(?:\.[0-9]+)?/,
                boolean: /true|false/,
                string: /"(?:[^"\\]|\\.)*"/,
                singleString: /'(?:[^'\\]|\\.)*'/,
                identifier: /[a-zA-Z0-9][_\-a-zA-Z0-9]*/,
                
                // Variable reference
                variableStart: '$',
                
                // Whitespace and newlines
                whitespace: /[ \t]+/,
                newline: { match: /\r?\n/, lineBreaks: true, pop: 1 },
                
                // Back to main for markdown headers
                sharp: { match: '#', pop: 1 },
                
                // Fallback
                fallback: { match: /./, pop: 1 },
            },
            
            // Code block state
            codeBlock: {
                codeBlockEnd: { match: /```/, pop: 1 },
                codeContent: { match: /[^\n`]+/, lineBreaks: false },
                backtick: '`',
                newline: { match: /\r?\n/, lineBreaks: true },
            },
        });
    }
    
    /**
     * Reset the lexer with new input
     */
    reset(input) {
        this.lexer.reset(input);
    }
    
    /**
     * Get the next token
     */
    next() {
        try {
            return this.lexer.next();
        } catch (error) {
            // Return error token
            return {
                type: 'ERROR',
                value: error.message,
                text: error.message,
                offset: this.lexer.index || 0,
                lineBreaks: 0,
                line: this.lexer.line || 1,
                col: this.lexer.col || 1
            };
        }
    }
    
    /**
     * Check if there are more tokens
     */
    hasNext() {
        return this.lexer.index < this.lexer.buffer.length;
    }
    
    /**
     * Get current lexer state
     */
    getCurrentState() {
        return this.lexer.state;
    }
    
    /**
     * Set lexer state
     */
    setState(state) {
        this.lexer.setState(state);
    }
    
    /**
     * Tokenize input and return all tokens
     */
    tokenize(input) {
        this.reset(input);
        const tokens = [];
        let token;
        
        while ((token = this.next()) !== undefined) {
            tokens.push(token);
        }
        
        return tokens;
    }
}

// Export for use in Kotlin/JS
if (typeof module !== 'undefined' && module.exports) {
    module.exports = DevInLexerJS;
} else if (typeof window !== 'undefined') {
    window.DevInLexerJS = DevInLexerJS;
}
