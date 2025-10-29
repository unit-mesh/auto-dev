/**
 * DevIn Language Parser Implementation using Chevrotain
 * 
 * This file implements the DevIn language parser using the Chevrotain library.
 * It defines the grammar rules and builds an Abstract Syntax Tree (AST).
 */

// Import Chevrotain (assuming it's available globally or via module system)
// const chevrotain = require('chevrotain'); // For Node.js
// For browser, assume chevrotain is available globally

/**
 * DevIn Parser implementation using Chevrotain
 */
class DevInParserJS {
    constructor() {
        this.lexer = new DevInLexerJS();
        this.initializeParser();
    }
    
    initializeParser() {
        // Create token vocabulary from lexer
        const allTokens = this.createTokenVocabulary();
        
        // Create parser class
        class DevInParser extends chevrotain.CstParser {
            constructor() {
                super(allTokens);
                this.performSelfAnalysis();
            }
            
            // Main rule: DevIn file
            devInFile() {
                return this.RULE("devInFile", () => {
                    this.OPTION(() => {
                        this.SUBRULE(this.frontMatterHeader);
                    });
                    
                    this.MANY(() => {
                        this.OR([
                            { ALT: () => this.SUBRULE(this.used) },
                            { ALT: () => this.SUBRULE(this.code) },
                            { ALT: () => this.SUBRULE(this.velocityExpr) },
                            { ALT: () => this.SUBRULE(this.markdownHeader) },
                            { ALT: () => this.CONSUME(this.textSegment) },
                            { ALT: () => this.CONSUME(this.newline) },
                            { ALT: () => this.CONSUME(this.contentComment) }
                        ]);
                    });
                });
            }
            
            // Front matter header
            frontMatterHeader() {
                return this.RULE("frontMatterHeader", () => {
                    this.CONSUME(this.frontMatterStart);
                    this.CONSUME(this.newline);
                    this.SUBRULE(this.frontMatterEntries);
                    this.CONSUME(this.frontMatterEnd);
                });
            }
            
            // Front matter entries
            frontMatterEntries() {
                return this.RULE("frontMatterEntries", () => {
                    this.MANY(() => {
                        this.SUBRULE(this.frontMatterEntry);
                    });
                });
            }
            
            // Front matter entry
            frontMatterEntry() {
                return this.RULE("frontMatterEntry", () => {
                    this.OR([
                        {
                            ALT: () => {
                                this.SUBRULE(this.lifecycleId);
                                this.CONSUME(this.colon);
                                this.OR2([
                                    { ALT: () => this.SUBRULE(this.functionStatement) },
                                    { ALT: () => this.SUBRULE(this.conditionExpr) }
                                ]);
                                this.OPTION(() => this.CONSUME(this.comment));
                                this.OPTION2(() => this.CONSUME(this.newline));
                            }
                        },
                        {
                            ALT: () => {
                                this.SUBRULE(this.frontMatterKey);
                                this.CONSUME2(this.colon);
                                this.OR3([
                                    { ALT: () => this.SUBRULE(this.foreignFunction) },
                                    { ALT: () => this.SUBRULE(this.frontMatterValue) },
                                    { ALT: () => this.SUBRULE(this.patternAction) },
                                    { ALT: () => this.SUBRULE2(this.functionStatement) }
                                ]);
                                this.OPTION3(() => this.CONSUME2(this.comment));
                                this.OPTION4(() => this.CONSUME2(this.newline));
                            }
                        },
                        {
                            ALT: () => {
                                this.CONSUME3(this.comment);
                                this.CONSUME3(this.newline);
                            }
                        }
                    ]);
                });
            }
            
            // Lifecycle identifiers
            lifecycleId() {
                return this.RULE("lifecycleId", () => {
                    this.OR([
                        { ALT: () => this.CONSUME(this.when) },
                        { ALT: () => this.CONSUME(this.onStreaming) },
                        { ALT: () => this.CONSUME(this.beforeStreaming) },
                        { ALT: () => this.CONSUME(this.onStreamingEnd) },
                        { ALT: () => this.CONSUME(this.afterStreaming) }
                    ]);
                });
            }
            
            // Used blocks (agent, command, variable)
            used() {
                return this.RULE("used", () => {
                    this.OR([
                        {
                            ALT: () => {
                                this.SUBRULE(this.agentStart);
                                this.SUBRULE(this.agentId);
                            }
                        },
                        {
                            ALT: () => {
                                this.SUBRULE(this.commandStart);
                                this.SUBRULE(this.commandId);
                                this.OPTION(() => {
                                    this.CONSUME(this.colon);
                                    this.CONSUME(this.commandProp);
                                    this.OPTION2(() => {
                                        this.CONSUME(this.sharp);
                                        this.CONSUME(this.lineInfo);
                                    });
                                });
                            }
                        },
                        {
                            ALT: () => {
                                this.SUBRULE(this.variableStart);
                                this.OR2([
                                    { ALT: () => this.SUBRULE(this.variableId) },
                                    { ALT: () => this.SUBRULE(this.varAccess) }
                                ]);
                            }
                        },
                        {
                            ALT: () => {
                                this.CONSUME2(this.sharp);
                                this.SUBRULE2(this.variableStart);
                                this.SUBRULE(this.expr);
                            }
                        }
                    ]);
                });
            }
            
            // Code blocks
            code() {
                return this.RULE("code", () => {
                    this.CONSUME(this.codeBlockStart);
                    this.OPTION(() => {
                        this.OR([
                            { ALT: () => this.CONSUME(this.languageId) },
                            {
                                ALT: () => {
                                    this.SUBRULE(this.variableStart);
                                    this.SUBRULE(this.expr);
                                }
                            }
                        ]);
                    });
                    this.OPTION2(() => this.CONSUME(this.newline));
                    this.OPTION3(() => this.SUBRULE(this.codeContents));
                    this.OPTION4(() => this.CONSUME(this.codeBlockEnd));
                });
            }
            
            // Code contents
            codeContents() {
                return this.RULE("codeContents", () => {
                    this.MANY(() => {
                        this.OR([
                            { ALT: () => this.CONSUME(this.newline) },
                            { ALT: () => this.CONSUME(this.codeContent) }
                        ]);
                    });
                });
            }
            
            // Expressions
            expr() {
                return this.RULE("expr", () => {
                    this.OR([
                        { ALT: () => this.SUBRULE(this.logicalOrExpr) },
                        { ALT: () => this.SUBRULE(this.logicalAndExpr) },
                        { ALT: () => this.SUBRULE(this.eqComparisonExpr) },
                        { ALT: () => this.SUBRULE(this.ineqComparisonExpr) },
                        { ALT: () => this.SUBRULE(this.callExpr) },
                        { ALT: () => this.SUBRULE(this.qualRefExpr) },
                        { ALT: () => this.SUBRULE(this.simpleRefExpr) },
                        { ALT: () => this.SUBRULE(this.literalExpr) },
                        { ALT: () => this.SUBRULE(this.parenExpr) },
                        { ALT: () => this.SUBRULE(this.variableExpr) }
                    ]);
                });
            }
            
            // Logical OR expression
            logicalOrExpr() {
                return this.RULE("logicalOrExpr", () => {
                    this.SUBRULE(this.expr, { LABEL: "lhs" });
                    this.CONSUME(this.oror);
                    this.SUBRULE2(this.expr, { LABEL: "rhs" });
                });
            }
            
            // Logical AND expression
            logicalAndExpr() {
                return this.RULE("logicalAndExpr", () => {
                    this.SUBRULE(this.expr, { LABEL: "lhs" });
                    this.OR([
                        { ALT: () => this.CONSUME(this.andand) },
                        { ALT: () => this.CONSUME(this.and) }
                    ]);
                    this.SUBRULE2(this.expr, { LABEL: "rhs" });
                });
            }
            
            // Equality comparison expression
            eqComparisonExpr() {
                return this.RULE("eqComparisonExpr", () => {
                    this.SUBRULE(this.expr, { LABEL: "lhs" });
                    this.SUBRULE(this.eqComparisonOp);
                    this.SUBRULE2(this.expr, { LABEL: "rhs" });
                });
            }
            
            // Inequality comparison expression
            ineqComparisonExpr() {
                return this.RULE("ineqComparisonExpr", () => {
                    this.SUBRULE(this.expr, { LABEL: "lhs" });
                    this.SUBRULE(this.ineqComparisonOp);
                    this.SUBRULE2(this.expr, { LABEL: "rhs" });
                });
            }
            
            // Call expression
            callExpr() {
                return this.RULE("callExpr", () => {
                    this.SUBRULE(this.refExpr);
                    this.CONSUME(this.lparen);
                    this.OPTION(() => this.SUBRULE(this.expressionList));
                    this.CONSUME(this.rparen);
                });
            }
            
            // Reference expressions
            simpleRefExpr() {
                return this.RULE("simpleRefExpr", () => {
                    this.CONSUME(this.identifier);
                });
            }
            
            qualRefExpr() {
                return this.RULE("qualRefExpr", () => {
                    this.SUBRULE(this.expr);
                    this.CONSUME(this.dot);
                    this.CONSUME(this.identifier);
                });
            }
            
            // Literal expression
            literalExpr() {
                return this.RULE("literalExpr", () => {
                    this.SUBRULE(this.literal);
                });
            }
            
            // Parenthesized expression
            parenExpr() {
                return this.RULE("parenExpr", () => {
                    this.CONSUME(this.lparen);
                    this.SUBRULE(this.expr);
                    this.CONSUME(this.rparen);
                });
            }
            
            // Variable expression
            variableExpr() {
                return this.RULE("variableExpr", () => {
                    this.CONSUME(this.openBrace);
                    this.SUBRULE(this.expr);
                    this.CONSUME(this.closeBrace);
                });
            }
            
            // Literals
            literal() {
                return this.RULE("literal", () => {
                    this.OR([
                        { ALT: () => this.CONSUME(this.number) },
                        { ALT: () => this.CONSUME(this.boolean) },
                        { ALT: () => this.CONSUME(this.string) },
                        { ALT: () => this.CONSUME(this.identifier) },
                        {
                            ALT: () => {
                                this.CONSUME(this.variableStart);
                                this.CONSUME2(this.identifier);
                            }
                        }
                    ]);
                });
            }
            
            // Comparison operators
            eqComparisonOp() {
                return this.RULE("eqComparisonOp", () => {
                    this.OR([
                        { ALT: () => this.CONSUME(this.eqeq) },
                        { ALT: () => this.CONSUME(this.and) }
                    ]);
                });
            }
            
            ineqComparisonOp() {
                return this.RULE("ineqComparisonOp", () => {
                    this.OR([
                        { ALT: () => this.CONSUME(this.lte) },
                        { ALT: () => this.CONSUME(this.gte) },
                        { ALT: () => this.CONSUME(this.lt) },
                        { ALT: () => this.CONSUME(this.gt) },
                        { ALT: () => this.CONSUME(this.neq) }
                    ]);
                });
            }
            
            // Expression list
            expressionList() {
                return this.RULE("expressionList", () => {
                    this.SUBRULE(this.expr);
                    this.MANY(() => {
                        this.CONSUME(this.comma);
                        this.SUBRULE2(this.expr);
                    });
                });
            }
            
            // Markdown header
            markdownHeader() {
                return this.RULE("markdownHeader", () => {
                    this.CONSUME(this.sharp);
                    this.MANY(() => this.CONSUME2(this.sharp));
                    this.CONSUME(this.textSegment);
                });
            }
            
            // Velocity expressions (template syntax)
            velocityExpr() {
                return this.RULE("velocityExpr", () => {
                    this.SUBRULE(this.ifExpr);
                    this.MANY(() => this.CONSUME(this.newline));
                });
            }
            
            // If expression
            ifExpr() {
                return this.RULE("ifExpr", () => {
                    this.SUBRULE(this.ifClause);
                    this.MANY(() => this.SUBRULE(this.elseifClause));
                    this.OPTION(() => this.SUBRULE(this.elseClause));
                    this.CONSUME(this.sharp);
                    this.CONSUME(this.end);
                });
            }
            
            // If clause
            ifClause() {
                return this.RULE("ifClause", () => {
                    this.CONSUME(this.sharp);
                    this.CONSUME(this.if);
                    this.CONSUME(this.lparen);
                    this.SUBRULE(this.expr);
                    this.CONSUME(this.rparen);
                    this.SUBRULE(this.velocityBlock);
                });
            }
            
            // Else if clause
            elseifClause() {
                return this.RULE("elseifClause", () => {
                    this.CONSUME(this.sharp);
                    this.CONSUME(this.elseif);
                    this.CONSUME(this.lparen);
                    this.SUBRULE(this.expr);
                    this.CONSUME(this.rparen);
                    this.SUBRULE(this.velocityBlock);
                });
            }
            
            // Else clause
            elseClause() {
                return this.RULE("elseClause", () => {
                    this.CONSUME(this.sharp);
                    this.CONSUME(this.else);
                    this.SUBRULE(this.velocityBlock);
                });
            }
            
            // Velocity block
            velocityBlock() {
                return this.RULE("velocityBlock", () => {
                    this.MANY(() => {
                        this.OR([
                            { ALT: () => this.SUBRULE(this.used) },
                            { ALT: () => this.SUBRULE(this.code) },
                            { ALT: () => this.SUBRULE(this.velocityExpr) },
                            { ALT: () => this.SUBRULE(this.markdownHeader) },
                            { ALT: () => this.CONSUME(this.textSegment) },
                            { ALT: () => this.CONSUME(this.newline) },
                            { ALT: () => this.CONSUME(this.contentComment) }
                        ]);
                    });
                });
            }
        }
        
        this.parser = new DevInParser();
    }
    
    createTokenVocabulary() {
        // Create Chevrotain tokens that match our Moo lexer tokens
        const createToken = chevrotain.createToken;

        // Define all token types
        const tokens = {};

        // Basic tokens
        tokens.TextSegment = createToken({ name: "TextSegment", pattern: /[^@\/$#\n`]+/ });
        tokens.Newline = createToken({ name: "Newline", pattern: /\r?\n/, line_breaks: true });
        tokens.Whitespace = createToken({ name: "Whitespace", pattern: /[ \t]+/, group: chevrotain.Lexer.SKIPPED });

        // Special markers
        tokens.AgentStart = createToken({ name: "AgentStart", pattern: /@/ });
        tokens.CommandStart = createToken({ name: "CommandStart", pattern: /\// });
        tokens.VariableStart = createToken({ name: "VariableStart", pattern: /\$/ });
        tokens.Sharp = createToken({ name: "Sharp", pattern: /#/ });

        // Front matter
        tokens.FrontMatterStart = createToken({ name: "FrontMatterStart", pattern: /^---$/ });
        tokens.FrontMatterEnd = createToken({ name: "FrontMatterEnd", pattern: /^---$/ });

        // Code blocks
        tokens.CodeBlockStart = createToken({ name: "CodeBlockStart", pattern: /```/ });
        tokens.CodeBlockEnd = createToken({ name: "CodeBlockEnd", pattern: /```/ });
        tokens.CodeContent = createToken({ name: "CodeContent", pattern: /[^\n`]+/ });
        tokens.LanguageId = createToken({ name: "LanguageId", pattern: /[a-zA-Z0-9_\-+.]+/ });

        // Identifiers and literals
        tokens.Identifier = createToken({ name: "Identifier", pattern: /[a-zA-Z0-9][_\-a-zA-Z0-9]*/ });
        tokens.Number = createToken({ name: "Number", pattern: /[0-9]+(?:\.[0-9]+)?/ });
        tokens.Boolean = createToken({ name: "Boolean", pattern: /true|false|TRUE|FALSE/ });
        tokens.String = createToken({ name: "String", pattern: /"(?:[^"\\]|\\.)*"/ });
        tokens.SingleString = createToken({ name: "SingleString", pattern: /'(?:[^'\\]|\\.)*'/ });

        // Punctuation
        tokens.Colon = createToken({ name: "Colon", pattern: /:/ });
        tokens.Comma = createToken({ name: "Comma", pattern: /,/ });
        tokens.LParen = createToken({ name: "LParen", pattern: /\(/ });
        tokens.RParen = createToken({ name: "RParen", pattern: /\)/ });
        tokens.LBracket = createToken({ name: "LBracket", pattern: /\[/ });
        tokens.RBracket = createToken({ name: "RBracket", pattern: /\]/ });
        tokens.OpenBrace = createToken({ name: "OpenBrace", pattern: /\{/ });
        tokens.CloseBrace = createToken({ name: "CloseBrace", pattern: /\}/ });
        tokens.Pipe = createToken({ name: "Pipe", pattern: /\|/ });
        tokens.Dot = createToken({ name: "Dot", pattern: /\./ });

        // Operators
        tokens.Arrow = createToken({ name: "Arrow", pattern: /=>/ });
        tokens.Access = createToken({ name: "Access", pattern: /::/ });
        tokens.Process = createToken({ name: "Process", pattern: /->/ });
        tokens.EqEq = createToken({ name: "EqEq", pattern: /==/ });
        tokens.Neq = createToken({ name: "Neq", pattern: /!=/ });
        tokens.Lte = createToken({ name: "Lte", pattern: /<=/ });
        tokens.Gte = createToken({ name: "Gte", pattern: />=/ });
        tokens.Lt = createToken({ name: "Lt", pattern: /</ });
        tokens.Gt = createToken({ name: "Gt", pattern: />/ });
        tokens.AndAnd = createToken({ name: "AndAnd", pattern: /&&/ });
        tokens.OrOr = createToken({ name: "OrOr", pattern: /\|\|/ });
        tokens.Not = createToken({ name: "Not", pattern: /!/ });

        // Keywords
        tokens.If = createToken({ name: "If", pattern: /if/ });
        tokens.Else = createToken({ name: "Else", pattern: /else/ });
        tokens.ElseIf = createToken({ name: "ElseIf", pattern: /elseif/ });
        tokens.End = createToken({ name: "End", pattern: /end/ });
        tokens.EndIf = createToken({ name: "EndIf", pattern: /endif/ });
        tokens.Case = createToken({ name: "Case", pattern: /case/ });
        tokens.Default = createToken({ name: "Default", pattern: /default/ });
        tokens.When = createToken({ name: "When", pattern: /when/ });
        tokens.From = createToken({ name: "From", pattern: /from/ });
        tokens.Where = createToken({ name: "Where", pattern: /where/ });
        tokens.Select = createToken({ name: "Select", pattern: /select/ });
        tokens.Condition = createToken({ name: "Condition", pattern: /condition/ });
        tokens.Functions = createToken({ name: "Functions", pattern: /functions/ });
        tokens.And = createToken({ name: "And", pattern: /and/ });

        // Lifecycle keywords
        tokens.OnStreaming = createToken({ name: "OnStreaming", pattern: /onStreaming/ });
        tokens.BeforeStreaming = createToken({ name: "BeforeStreaming", pattern: /beforeStreaming/ });
        tokens.OnStreamingEnd = createToken({ name: "OnStreamingEnd", pattern: /onStreamingEnd/ });
        tokens.AfterStreaming = createToken({ name: "AfterStreaming", pattern: /afterStreaming/ });

        // Comments
        tokens.LineComment = createToken({ name: "LineComment", pattern: /\/\/[^\r\n]*/, group: chevrotain.Lexer.SKIPPED });
        tokens.BlockComment = createToken({ name: "BlockComment", pattern: /\/\*[^]*?\*\//, group: chevrotain.Lexer.SKIPPED });
        tokens.ContentComment = createToken({ name: "ContentComment", pattern: /\[[^\]]*\][^\t\r\n]*/ });

        // Command specific
        tokens.CommandProp = createToken({ name: "CommandProp", pattern: /[^\s\t\r\n#]+/ });
        tokens.LineInfo = createToken({ name: "LineInfo", pattern: /L[0-9]+(?:C[0-9]+)?(?:-L[0-9]+(?:C[0-9]+)?)?/ });

        // Pattern expressions
        tokens.PatternExpr = createToken({ name: "PatternExpr", pattern: /\/[^\r\n\\\/]+\/[gimuy]*/ });

        // Convert to array for Chevrotain
        return Object.values(tokens);
    }
    
    /**
     * Parse DevIn source code
     */
    parse(input) {
        try {
            const tokens = this.lexer.tokenize(input);
            this.parser.input = tokens;
            const cst = this.parser.devInFile();
            
            if (this.parser.errors.length > 0) {
                return {
                    ast: null,
                    errors: this.parser.errors.map(error => ({
                        message: error.message,
                        line: error.token?.startLine || 1,
                        column: error.token?.startColumn || 1,
                        offset: error.token?.startOffset || 0,
                        token: error.token
                    }))
                };
            }
            
            // Convert CST to AST
            const ast = this.cstToAst(cst);
            
            return {
                ast: ast,
                errors: []
            };
        } catch (error) {
            return {
                ast: null,
                errors: [{
                    message: error.message,
                    line: 1,
                    column: 1,
                    offset: 0,
                    token: null
                }]
            };
        }
    }
    
    /**
     * Parse from token stream
     */
    parseTokens(tokens) {
        try {
            this.parser.input = tokens;
            const cst = this.parser.devInFile();
            
            if (this.parser.errors.length > 0) {
                return {
                    ast: null,
                    errors: this.parser.errors
                };
            }
            
            const ast = this.cstToAst(cst);
            
            return {
                ast: ast,
                errors: []
            };
        } catch (error) {
            return {
                ast: null,
                errors: [{ message: error.message, line: 1, column: 1, offset: 0, token: null }]
            };
        }
    }
    
    /**
     * Parse a specific rule
     */
    parseRule(ruleName, input) {
        try {
            const tokens = this.lexer.tokenize(input);
            this.parser.input = tokens;
            
            if (this.parser[ruleName]) {
                const cst = this.parser[ruleName]();
                const ast = this.cstToAst(cst);
                
                return {
                    ast: ast,
                    errors: this.parser.errors
                };
            } else {
                throw new Error(`Rule '${ruleName}' not found`);
            }
        } catch (error) {
            return {
                ast: null,
                errors: [{ message: error.message, line: 1, column: 1, offset: 0, token: null }]
            };
        }
    }
    
    /**
     * Convert CST to AST
     */
    cstToAst(cst) {
        // Simplified CST to AST conversion
        // In a real implementation, this would be more sophisticated
        return {
            type: 'FILE',
            children: [],
            startOffset: 0,
            endOffset: 0,
            line: 1,
            column: 1
        };
    }
}

// Export for use in Kotlin/JS
if (typeof module !== 'undefined' && module.exports) {
    module.exports = DevInParserJS;
} else if (typeof window !== 'undefined') {
    window.DevInParserJS = DevInParserJS;
}
