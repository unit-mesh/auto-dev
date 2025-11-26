package cc.unitmesh.devins.document.docql

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith

class DocQLLexerTest {
    
    @Test
    fun `test simple root query`() {
        val lexer = DocQLLexer("$")
        val tokens = lexer.tokenize()
        
        assertEquals(2, tokens.size)
        assertIs<DocQLToken.Root>(tokens[0])
        assertIs<DocQLToken.EOF>(tokens[1])
    }
    
    @Test
    fun `test property access`() {
        val lexer = DocQLLexer("$.toc")
        val tokens = lexer.tokenize()
        
        assertEquals(4, tokens.size)
        assertIs<DocQLToken.Root>(tokens[0])
        assertIs<DocQLToken.Dot>(tokens[1])
        assertEquals("toc", (tokens[2] as DocQLToken.Identifier).value)
        assertIs<DocQLToken.EOF>(tokens[3])
    }
    
    @Test
    fun `test array access with star`() {
        val lexer = DocQLLexer("$.toc[*]")
        val tokens = lexer.tokenize()
        
        assertEquals(7, tokens.size)
        assertIs<DocQLToken.Root>(tokens[0])
        assertIs<DocQLToken.Dot>(tokens[1])
        assertIs<DocQLToken.Identifier>(tokens[2])
        assertIs<DocQLToken.LeftBracket>(tokens[3])
        assertIs<DocQLToken.Star>(tokens[4])
        assertIs<DocQLToken.RightBracket>(tokens[5])
        assertIs<DocQLToken.EOF>(tokens[6])
    }
    
    @Test
    fun `test array access with index`() {
        val lexer = DocQLLexer("$.toc[0]")
        val tokens = lexer.tokenize()
        
        assertEquals(7, tokens.size)
        assertIs<DocQLToken.LeftBracket>(tokens[3])
        assertEquals(0, (tokens[4] as DocQLToken.Number).value)
        assertIs<DocQLToken.RightBracket>(tokens[5])
    }
    
    @Test
    fun `test filter with equals`() {
        val lexer = DocQLLexer("$.toc[?(@.level==1)]")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.Question>(tokens[4])
        assertIs<DocQLToken.LeftParen>(tokens[5])
        assertIs<DocQLToken.At>(tokens[6])
        assertIs<DocQLToken.Dot>(tokens[7])
        assertEquals("level", (tokens[8] as DocQLToken.Identifier).value)
        assertIs<DocQLToken.Equals>(tokens[9])
        assertEquals(1, (tokens[10] as DocQLToken.Number).value)
        assertIs<DocQLToken.RightParen>(tokens[11])
    }
    
    @Test
    fun `test filter with contains`() {
        val lexer = DocQLLexer("""$.toc[?(@.title~="架构")]""")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.Question>(tokens[4])
        assertIs<DocQLToken.At>(tokens[6])
        assertIs<DocQLToken.Dot>(tokens[7])
        assertEquals("title", (tokens[8] as DocQLToken.Identifier).value)
        assertIs<DocQLToken.Contains>(tokens[9])
        assertEquals("架构", (tokens[10] as DocQLToken.StringLiteral).value)
    }
    
    @Test
    fun `test function call`() {
        val lexer = DocQLLexer("""$.content.heading("architecture")""")
        val tokens = lexer.tokenize()
        
        assertEquals(9, tokens.size)
        assertIs<DocQLToken.Root>(tokens[0])
        assertIs<DocQLToken.Dot>(tokens[1])
        assertEquals("content", (tokens[2] as DocQLToken.Identifier).value)
        assertIs<DocQLToken.Dot>(tokens[3])
        assertEquals("heading", (tokens[4] as DocQLToken.Identifier).value)
        assertIs<DocQLToken.LeftParen>(tokens[5])
        assertEquals("architecture", (tokens[6] as DocQLToken.StringLiteral).value)
        assertIs<DocQLToken.RightParen>(tokens[7])
    }
    
    @Test
    fun `test string literal with spaces`() {
        val lexer = DocQLLexer("""$.content.heading("system architecture")""")
        val tokens = lexer.tokenize()
        
        val stringToken = tokens[6] as DocQLToken.StringLiteral
        assertEquals("system architecture", stringToken.value)
    }
    
    @Test
    fun `test string literal with escape`() {
        val lexer = DocQLLexer("""$.content.heading("quote \"test\"")""")
        val tokens = lexer.tokenize()
        
        val stringToken = tokens[6] as DocQLToken.StringLiteral
        assertEquals("""quote \"test\"""", stringToken.value)
    }
    
    @Test
    fun `test query with whitespace`() {
        val lexer = DocQLLexer("$  .  toc  [  *  ]")
        val tokens = lexer.tokenize()
        
        assertEquals(7, tokens.size)
        assertIs<DocQLToken.Root>(tokens[0])
        assertIs<DocQLToken.Dot>(tokens[1])
        assertIs<DocQLToken.Identifier>(tokens[2])
        assertIs<DocQLToken.LeftBracket>(tokens[3])
        assertIs<DocQLToken.Star>(tokens[4])
    }
    
    @Test
    fun `test unterminated string error`() {
        val lexer = DocQLLexer("""$.content.heading("unterminated""")
        
        assertFailsWith<DocQLException> {
            lexer.tokenize()
        }
    }
    
    @Test
    fun `test single equals is valid for robustness`() {
        // Single '=' is now treated as '==' for robustness (common user typo)
        val lexer = DocQLLexer("$.toc[?(@.level=1)]")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.Equals>(tokens[9])
    }
    
    @Test
    fun `test complex query`() {
        val lexer = DocQLLexer("""$.entities[?(@.type=="API")]""")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.Root>(tokens[0])
        assertIs<DocQLToken.Dot>(tokens[1])
        assertEquals("entities", (tokens[2] as DocQLToken.Identifier).value)
        assertIs<DocQLToken.LeftBracket>(tokens[3])
        assertIs<DocQLToken.Question>(tokens[4])
        assertIs<DocQLToken.LeftParen>(tokens[5])
        assertIs<DocQLToken.At>(tokens[6])
        assertIs<DocQLToken.Dot>(tokens[7])
        assertEquals("type", (tokens[8] as DocQLToken.Identifier).value)
        assertIs<DocQLToken.Equals>(tokens[9])
        assertEquals("API", (tokens[10] as DocQLToken.StringLiteral).value)
    }
    
    @Test
    fun `test heading level query`() {
        val lexer = DocQLLexer("""$.content.h1("Introduction")""")
        val tokens = lexer.tokenize()
        
        assertEquals(9, tokens.size)
        assertEquals("content", (tokens[2] as DocQLToken.Identifier).value)
        assertEquals("h1", (tokens[4] as DocQLToken.Identifier).value)
        assertEquals("Introduction", (tokens[6] as DocQLToken.StringLiteral).value)
    }
    
    @Test
    fun `test grep query`() {
        val lexer = DocQLLexer("""$.content.grep("keyword")""")
        val tokens = lexer.tokenize()
        
        assertEquals("grep", (tokens[4] as DocQLToken.Identifier).value)
        assertEquals("keyword", (tokens[6] as DocQLToken.StringLiteral).value)
    }
    
    // ============ Regex Match Lexer Tests ============
    
    @Test
    fun `test regex match operator`() {
        val lexer = DocQLLexer("$.toc[?(@.title =~ /MCP/)]")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.RegexMatch>(tokens[9])
        val regexToken = tokens[10] as DocQLToken.RegexLiteral
        assertEquals("MCP", regexToken.pattern)
        assertEquals("", regexToken.flags)
    }
    
    @Test
    fun `test regex literal with flags`() {
        val lexer = DocQLLexer("$.toc[?(@.title =~ /pattern/i)]")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.RegexMatch>(tokens[9])
        val regexToken = tokens[10] as DocQLToken.RegexLiteral
        assertEquals("pattern", regexToken.pattern)
        assertEquals("i", regexToken.flags)
    }
    
    @Test
    fun `test regex literal with multiple flags`() {
        val lexer = DocQLLexer("$.toc[?(@.title =~ /test/img)]")
        val tokens = lexer.tokenize()
        
        val regexToken = tokens[10] as DocQLToken.RegexLiteral
        assertEquals("test", regexToken.pattern)
        assertEquals("img", regexToken.flags)
    }
    
    @Test
    fun `test regex literal with escaped slash`() {
        val lexer = DocQLLexer("""$.toc[?(@.path =~ /\/api\/v1/)]""")
        val tokens = lexer.tokenize()
        
        val regexToken = tokens[10] as DocQLToken.RegexLiteral
        // In raw string """\/""" is backslash + slash, so the pattern is: \/api\/v1
        assertEquals("\\/api\\/v1", regexToken.pattern)
        assertEquals("", regexToken.flags)
    }
    
    @Test
    fun `test regex literal with complex pattern`() {
        val lexer = DocQLLexer("""$.toc[?(@.title =~ /^Chapter\s+\d+/i)]""")
        val tokens = lexer.tokenize()
        
        val regexToken = tokens[10] as DocQLToken.RegexLiteral
        assertEquals("""^Chapter\s+\d+""", regexToken.pattern)
        assertEquals("i", regexToken.flags)
    }
    
    @Test
    fun `test regex literal with chinese characters`() {
        val lexer = DocQLLexer("$.toc[?(@.title =~ /.*架构.*/)]")
        val tokens = lexer.tokenize()
        
        val regexToken = tokens[10] as DocQLToken.RegexLiteral
        assertEquals(".*架构.*", regexToken.pattern)
    }
    
    @Test
    fun `test unterminated regex error`() {
        val lexer = DocQLLexer("$.toc[?(@.title =~ /unterminated")
        
        assertFailsWith<DocQLException> {
            lexer.tokenize()
        }
    }
    
    // ============ Additional Operator Tests ============
    
    @Test
    fun `test not equals operator`() {
        val lexer = DocQLLexer("$.toc[?(@.level!=2)]")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.NotEquals>(tokens[9])
    }
    
    @Test
    fun `test greater than or equals operator`() {
        val lexer = DocQLLexer("$.toc[?(@.level>=2)]")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.GreaterThanOrEquals>(tokens[9])
    }
    
    @Test
    fun `test less than or equals operator`() {
        val lexer = DocQLLexer("$.toc[?(@.level<=10)]")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.LessThanOrEquals>(tokens[9])
    }
    
    @Test
    fun `test single quote string literal`() {
        val lexer = DocQLLexer("$.entities[?(@.type=='API')]")
        val tokens = lexer.tokenize()
        
        val stringToken = tokens[10] as DocQLToken.StringLiteral
        assertEquals("API", stringToken.value)
    }
    
    @Test
    fun `test single equals treated as double equals`() {
        val lexer = DocQLLexer("$.toc[?(@.level=1)]")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.Equals>(tokens[9])
    }
    
    @Test
    fun `test startsWith keyword`() {
        val lexer = DocQLLexer("""$.toc[?(@.title startsWith "Ch")]""")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.StartsWith>(tokens[9])
    }
    
    @Test
    fun `test endsWith keyword`() {
        val lexer = DocQLLexer("""$.toc[?(@.title endsWith "er")]""")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.EndsWith>(tokens[9])
    }
    
    @Test
    fun `test starts with two words`() {
        val lexer = DocQLLexer("""$.toc[?(@.title starts with "Ch")]""")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.StartsWith>(tokens[9])
    }
    
    @Test
    fun `test ends with two words`() {
        val lexer = DocQLLexer("""$.toc[?(@.title ends with "er")]""")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.EndsWith>(tokens[9])
    }
    
    @Test
    fun `test single quote with escape`() {
        val lexer = DocQLLexer("""$.entities[?(@.name=='it\'s')]""")
        val tokens = lexer.tokenize()
        
        val stringToken = tokens[10] as DocQLToken.StringLiteral
        assertEquals("it\\'s", stringToken.value)
    }
    
    @Test
    fun `test mixed operators in complex query`() {
        val lexer = DocQLLexer("$.toc[?(@.level>=1)]")
        val tokens = lexer.tokenize()
        
        assertIs<DocQLToken.Root>(tokens[0])
        assertIs<DocQLToken.Dot>(tokens[1])
        assertIs<DocQLToken.Identifier>(tokens[2])
        assertIs<DocQLToken.LeftBracket>(tokens[3])
        assertIs<DocQLToken.Question>(tokens[4])
        assertIs<DocQLToken.LeftParen>(tokens[5])
        assertIs<DocQLToken.At>(tokens[6])
        assertIs<DocQLToken.Dot>(tokens[7])
        assertIs<DocQLToken.Identifier>(tokens[8])
        assertIs<DocQLToken.GreaterThanOrEquals>(tokens[9])
    }
}
