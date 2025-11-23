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
        assert(str.contains("$"))
        assert(str.contains("toc"))
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
        assertFailsWith<DocQLException> {
            parseDocQL("$.content.heading()")
        }
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
}

