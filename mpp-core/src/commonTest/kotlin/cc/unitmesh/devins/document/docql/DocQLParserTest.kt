package cc.unitmesh.devins.document.docql

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith

class DocQLParserTest {
    
    @Test
    fun `test parse simple property access`() {
        val query = parseDocQL("$.toc")
        
        assertEquals(2, query.nodes.size)
        assertIs<DocQLNode.Root>(query.nodes[0])
        assertIs<DocQLNode.Property>(query.nodes[1])
        assertEquals("toc", (query.nodes[1] as DocQLNode.Property).name)
    }
    
    @Test
    fun `test parse array all access`() {
        val query = parseDocQL("$.toc[*]")
        
        assertEquals(3, query.nodes.size)
        assertIs<DocQLNode.Root>(query.nodes[0])
        assertIs<DocQLNode.Property>(query.nodes[1])
        assertIs<DocQLNode.ArrayAccess.All>(query.nodes[2])
    }
    
    @Test
    fun `test parse array index access`() {
        val query = parseDocQL("$.toc[0]")
        
        assertEquals(3, query.nodes.size)
        val arrayAccess = query.nodes[2] as DocQLNode.ArrayAccess.Index
        assertEquals(0, arrayAccess.index)
    }
    
    @Test
    fun `test parse filter with equals`() {
        val query = parseDocQL("$.toc[?(@.level==1)]")
        
        assertEquals(3, query.nodes.size)
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.Equals
        assertEquals("level", condition.property)
        assertEquals("1", condition.value)
    }
    
    @Test
    fun `test parse filter with contains`() {
        val query = parseDocQL("""$.toc[?(@.title~="架构")]""")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.Contains
        assertEquals("title", condition.property)
        assertEquals("架构", condition.value)
    }
    
    @Test
    fun `test parse filter with greater than`() {
        val query = parseDocQL("$.toc[?(@.level>2)]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.GreaterThan
        assertEquals("level", condition.property)
        assertEquals(2, condition.value)
    }
    
    @Test
    fun `test parse filter with less than`() {
        val query = parseDocQL("$.toc[?(@.page<10)]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.LessThan
        assertEquals("page", condition.property)
        assertEquals(10, condition.value)
    }
    
    @Test
    fun `test parse function call`() {
        val query = parseDocQL("""$.content.heading("architecture")""")
        
        assertEquals(3, query.nodes.size)
        assertIs<DocQLNode.Root>(query.nodes[0])
        assertIs<DocQLNode.Property>(query.nodes[1])
        assertEquals("content", (query.nodes[1] as DocQLNode.Property).name)
        
        val functionCall = query.nodes[2] as DocQLNode.FunctionCall
        assertEquals("heading", functionCall.name)
        assertEquals("architecture", functionCall.argument)
    }
    
    @Test
    fun `test parse h1 query`() {
        val query = parseDocQL("""$.content.h1("Introduction")""")
        
        val functionCall = query.nodes[2] as DocQLNode.FunctionCall
        assertEquals("h1", functionCall.name)
        assertEquals("Introduction", functionCall.argument)
    }
    
    @Test
    fun `test parse grep query`() {
        val query = parseDocQL("""$.content.grep("keyword")""")
        
        val functionCall = query.nodes[2] as DocQLNode.FunctionCall
        assertEquals("grep", functionCall.name)
        assertEquals("keyword", functionCall.argument)
    }
    
    @Test
    fun `test parse chapter query`() {
        val query = parseDocQL("""$.content.chapter("1.2")""")
        
        val functionCall = query.nodes[2] as DocQLNode.FunctionCall
        assertEquals("chapter", functionCall.name)
        assertEquals("1.2", functionCall.argument)
    }
    
    @Test
    fun `test parse entities filter`() {
        val query = parseDocQL("""$.entities[?(@.type=="API")]""")
        
        assertEquals(3, query.nodes.size)
        assertEquals("entities", (query.nodes[1] as DocQLNode.Property).name)
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.Equals
        assertEquals("type", condition.property)
        assertEquals("API", condition.value)
    }
    
    @Test
    fun `test parse code blocks query`() {
        val query = parseDocQL("$.content.code[*]")
        
        assertEquals(4, query.nodes.size)
        assertEquals("content", (query.nodes[1] as DocQLNode.Property).name)
        assertEquals("code", (query.nodes[2] as DocQLNode.Property).name)
        assertIs<DocQLNode.ArrayAccess.All>(query.nodes[3])
    }
    
    @Test
    fun `test parse table query`() {
        val query = parseDocQL("$.content.table[0]")
        
        assertEquals(4, query.nodes.size)
        assertEquals("content", (query.nodes[1] as DocQLNode.Property).name)
        assertEquals("table", (query.nodes[2] as DocQLNode.Property).name)
        assertEquals(0, (query.nodes[3] as DocQLNode.ArrayAccess.Index).index)
    }
    
    @Test
    fun `test query toString`() {
        val query = parseDocQL("""$.toc[?(@.level==1)]""")
        val str = query.toString()
        
        // Check it contains key parts (exact format may vary)
        kotlin.test.assertTrue(str.contains("$"))
        kotlin.test.assertTrue(str.contains("toc"))
    }
    
    @Test
    fun `test parse error - missing root`() {
        assertFailsWith<DocQLException> {
            parseDocQL("toc[*]")
        }
    }
    
    @Test
    fun `test parse error - invalid array access`() {
        assertFailsWith<DocQLException> {
            parseDocQL("$.toc[]")
        }
    }
    
    @Test
    fun `test parse error - missing function argument`() {
        // Empty function calls are now allowed (e.g., heading(), chunks())
        val query = parseDocQL("$.content.heading()")
        kotlin.test.assertTrue(query.nodes.any { it is DocQLNode.FunctionCall })
        val funcNode = query.nodes.first { it is DocQLNode.FunctionCall } as DocQLNode.FunctionCall
        kotlin.test.assertEquals("", funcNode.argument)
    }
    
    @Test
    fun `test parse error - missing closing bracket`() {
        assertFailsWith<DocQLException> {
            parseDocQL("$.toc[*")
        }
    }
    
    @Test
    fun `test parse error - missing closing paren`() {
        assertFailsWith<DocQLException> {
            parseDocQL("""$.content.heading("test"""")
        }
    }
    
    @Test
    fun `test complex nested query`() {
        val query = parseDocQL("""$.toc[?(@.title~="Design")]""")
        
        assertEquals(3, query.nodes.size)
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.Contains
        assertEquals("title", condition.property)
        assertEquals("Design", condition.value)
    }
    
    @Test
    fun `test query with Chinese characters`() {
        val query = parseDocQL("""$.content.heading("系统架构设计")""")
        
        val functionCall = query.nodes[2] as DocQLNode.FunctionCall
        assertEquals("heading", functionCall.name)
        assertEquals("系统架构设计", functionCall.argument)
    }
    
    @Test
    fun `test multiple properties`() {
        val query = parseDocQL("$.document.metadata")
        
        assertEquals(3, query.nodes.size)
        assertIs<DocQLNode.Root>(query.nodes[0])
        assertEquals("document", (query.nodes[1] as DocQLNode.Property).name)
        assertEquals("metadata", (query.nodes[2] as DocQLNode.Property).name)
    }
    
    // ============ Regex Match Tests (JSONPath-style =~ /pattern/flags) ============
    
    @Test
    fun `test parse filter with regex match simple`() {
        val query = parseDocQL("$.toc[?(@.title =~ /MCP/)]")
        
        assertEquals(3, query.nodes.size)
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.RegexMatch
        assertEquals("title", condition.property)
        assertEquals("MCP", condition.pattern)
        assertEquals("", condition.flags)
    }
    
    @Test
    fun `test parse filter with regex match case insensitive`() {
        val query = parseDocQL("$.toc[?(@.title =~ /MCP/i)]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.RegexMatch
        assertEquals("title", condition.property)
        assertEquals("MCP", condition.pattern)
        assertEquals("i", condition.flags)
    }
    
    @Test
    fun `test parse filter with regex match multiple flags`() {
        val query = parseDocQL("$.toc[?(@.content =~ /pattern/img)]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.RegexMatch
        assertEquals("content", condition.property)
        assertEquals("pattern", condition.pattern)
        assertEquals("img", condition.flags)
    }
    
    @Test
    fun `test parse filter with regex match complex pattern`() {
        val query = parseDocQL("""$.toc[?(@.title =~ /^Chapter\s+\d+/i)]""")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.RegexMatch
        assertEquals("title", condition.property)
        assertEquals("""^Chapter\s+\d+""", condition.pattern)
        assertEquals("i", condition.flags)
    }
    
    @Test
    fun `test parse filter with regex match chinese characters`() {
        val query = parseDocQL("$.entities[?(@.name =~ /.*架构.*/)]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.RegexMatch
        assertEquals("name", condition.property)
        assertEquals(".*架构.*", condition.pattern)
        assertEquals("", condition.flags)
    }
    
    @Test
    fun `test parse filter with regex match without parentheses`() {
        val query = parseDocQL("$.toc[?@.title =~ /API/i]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.RegexMatch
        assertEquals("title", condition.property)
        assertEquals("API", condition.pattern)
        assertEquals("i", condition.flags)
    }
    
    // ============ Additional JSONPath Operators Tests ============
    
    @Test
    fun `test parse filter with not equals`() {
        val query = parseDocQL("$.toc[?(@.level!=2)]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.NotEquals
        assertEquals("level", condition.property)
        assertEquals("2", condition.value)
    }
    
    @Test
    fun `test parse filter with not equals string`() {
        val query = parseDocQL("""$.entities[?(@.type!="Term")]""")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.NotEquals
        assertEquals("type", condition.property)
        assertEquals("Term", condition.value)
    }
    
    @Test
    fun `test parse filter with greater than or equals`() {
        val query = parseDocQL("$.toc[?(@.level>=2)]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.GreaterThanOrEquals
        assertEquals("level", condition.property)
        assertEquals(2, condition.value)
    }
    
    @Test
    fun `test parse filter with less than or equals`() {
        val query = parseDocQL("$.toc[?(@.page<=100)]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.LessThanOrEquals
        assertEquals("page", condition.property)
        assertEquals(100, condition.value)
    }
    
    @Test
    fun `test parse filter with single quote string`() {
        val query = parseDocQL("$.entities[?(@.type=='API')]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.Equals
        assertEquals("type", condition.property)
        assertEquals("API", condition.value)
    }
    
    @Test
    fun `test parse filter with startsWith`() {
        val query = parseDocQL("""$.toc[?(@.title startsWith "Chapter")]""")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.StartsWith
        assertEquals("title", condition.property)
        assertEquals("Chapter", condition.value)
    }
    
    @Test
    fun `test parse filter with endsWith`() {
        val query = parseDocQL("""$.toc[?(@.title endsWith "Introduction")]""")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.EndsWith
        assertEquals("title", condition.property)
        assertEquals("Introduction", condition.value)
    }
    
    @Test
    fun `test parse filter with starts with two words`() {
        val query = parseDocQL("""$.toc[?(@.title starts with "Ch")]""")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.StartsWith
        assertEquals("title", condition.property)
        assertEquals("Ch", condition.value)
    }
    
    @Test
    fun `test parse filter with ends with two words`() {
        val query = parseDocQL("""$.toc[?(@.title ends with "ion")]""")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.EndsWith
        assertEquals("title", condition.property)
        assertEquals("ion", condition.value)
    }
    
    @Test
    fun `test parse filter with single equals as robustness`() {
        // Single '=' should be treated as '==' for robustness
        val query = parseDocQL("$.toc[?(@.level=1)]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.Equals
        assertEquals("level", condition.property)
        assertEquals("1", condition.value)
    }
    
    @Test
    fun `test parse filter with mixed quote styles`() {
        // Double quotes
        val query1 = parseDocQL("""$.entities[?(@.name=="test")]""")
        val condition1 = (query1.nodes[2] as DocQLNode.ArrayAccess.Filter).condition as FilterCondition.Equals
        assertEquals("test", condition1.value)
        
        // Single quotes
        val query2 = parseDocQL("$.entities[?(@.name=='test')]")
        val condition2 = (query2.nodes[2] as DocQLNode.ArrayAccess.Filter).condition as FilterCondition.Equals
        assertEquals("test", condition2.value)
    }
    
    @Test
    fun `test parse filter with spaces around operators`() {
        val query = parseDocQL("$.toc[?(@.level >= 2)]")
        
        val filter = query.nodes[2] as DocQLNode.ArrayAccess.Filter
        val condition = filter.condition as FilterCondition.GreaterThanOrEquals
        assertEquals("level", condition.property)
        assertEquals(2, condition.value)
    }
}