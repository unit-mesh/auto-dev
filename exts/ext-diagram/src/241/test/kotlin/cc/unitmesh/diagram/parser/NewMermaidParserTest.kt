package cc.unitmesh.diagram.parser

import cc.unitmesh.diagram.parser.mermaid.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class NewMermaidParserTest {
    
    @Test
    fun `should tokenize simple class diagram`() {
        val content = """
            classDiagram
                class Animal {
                    +String name
                    +int age
                    +getName()
                }
        """.trimIndent()
        
        val lexer = MermaidLexer(content)
        val tokens = lexer.tokenize()
        
        assertTrue(tokens.isNotEmpty())
        assertEquals(TokenType.CLASS_DIAGRAM, tokens.first().type)
        assertTrue(tokens.any { it.type == TokenType.CLASS })
        assertTrue(tokens.any { it.type == TokenType.STRUCT_START })
        assertTrue(tokens.any { it.type == TokenType.STRUCT_STOP })
        assertTrue(tokens.any { it.type == TokenType.EOF })
    }
    
    @Test
    fun `should parse simple class diagram`() {
        val content = """
            classDiagram
                class Animal {
                    +String name
                    +int age
                    +getName()
                }
        """.trimIndent()
        
        val lexer = MermaidLexer(content)
        val tokens = lexer.tokenize()
        
        val parser = MermaidParser(tokens)
        val result = parser.parse()
        
        assertTrue(result is ParseResult.Success)
        val ast = (result as ParseResult.Success).ast
        
        assertEquals(1, ast.statements.size)
        assertTrue(ast.statements.first() is ClassStatementNode)
        
        val classStatement = ast.statements.first() as ClassStatementNode
        assertEquals("Animal", classStatement.className)
        assertEquals(3, classStatement.members.size)
    }
    
    @Test
    fun `should parse inheritance relationship`() {
        val content = """
            classDiagram
                Animal <|-- Dog
                class Animal {
                    +String name
                }
                class Dog {
                    +String breed
                }
        """.trimIndent()
        
        val lexer = MermaidLexer(content)
        val tokens = lexer.tokenize()
        
        val parser = MermaidParser(tokens)
        val result = parser.parse()
        
        assertTrue(result is ParseResult.Success)
        val ast = (result as ParseResult.Success).ast
        
        // Should have 3 statements: 1 relation + 2 classes
        assertEquals(3, ast.statements.size)
        
        val relationStatement = ast.statements.find { it is RelationStatementNode } as? RelationStatementNode
        assertNotNull(relationStatement)
        assertEquals("Animal", relationStatement!!.sourceClass)
        assertEquals("Dog", relationStatement.targetClass)
        assertEquals(RelationType.EXTENSION, relationStatement.relation.type1)
    }
    
    @Test
    fun `should parse member statements`() {
        val content = """
            classDiagram
                Animal : +String name
                Animal : +int age
                Animal : +getName()
        """.trimIndent()
        
        val lexer = MermaidLexer(content)
        val tokens = lexer.tokenize()
        
        val parser = MermaidParser(tokens)
        val result = parser.parse()
        
        assertTrue(result is ParseResult.Success)
        val ast = (result as ParseResult.Success).ast
        
        assertEquals(3, ast.statements.size)
        assertTrue(ast.statements.all { it is MemberStatementNode })
        
        val memberStatements = ast.statements.filterIsInstance<MemberStatementNode>()
        assertTrue(memberStatements.all { it.className == "Animal" })
    }
    
    @Test
    fun `should integrate with MermaidClassDiagramParser`() {
        val content = """
            classDiagram
                Animal <|-- Duck
                Animal : +int age
                Animal : +String gender
                Animal: +isMammal()
                class Duck{
                    +String beakColor
                    +swim()
                }
        """.trimIndent()
        
        val parser = MermaidClassDiagramParser()
        val result = parser.parse(content)
        
        assertNotNull(result)
        assertFalse(result.isEmpty())
        
        // Should have 2 entities: Animal and Duck
        assertEquals(2, result.entities.size)
        
        val animal = result.getEntityByName("Animal")
        assertNotNull(animal)
        assertEquals(3, animal!!.getFields().size)
        
        val duck = result.getEntityByName("Duck")
        assertNotNull(duck)
        assertEquals(2, duck!!.getFields().size)
        
        // Should have 1 inheritance edge
        assertEquals(1, result.edges.size)
        val edge = result.edges.first()
        assertEquals("Duck", edge.sourceNodeId)
        assertEquals("Animal", edge.targetNodeId)
    }
    
    @Test
    fun `should handle complex class diagram with multiple relationships`() {
        val content = """
            classDiagram
                Animal <|-- Duck
                Animal <|-- Fish
                Animal <|-- Zebra
                Animal : +int age
                Animal : +String gender
                Animal: +isMammal()
                Animal: +mate()
                class Duck{
                    +String beakColor
                    +swim()
                    +quack()
                }
                class Fish{
                    -int sizeInFeet
                    -canEat()
                }
                class Zebra{
                    +bool is_wild
                    +run()
                }
        """.trimIndent()
        
        val parser = MermaidClassDiagramParser()
        val result = parser.parse(content)
        
        assertNotNull(result)
        assertFalse(result.isEmpty())
        
        // Should have 4 entities
        assertEquals(4, result.entities.size)
        
        // Should have 3 inheritance edges
        assertEquals(3, result.edges.size)
        
        // Check all classes exist
        assertNotNull(result.getEntityByName("Animal"))
        assertNotNull(result.getEntityByName("Duck"))
        assertNotNull(result.getEntityByName("Fish"))
        assertNotNull(result.getEntityByName("Zebra"))
        
        // Check field counts
        assertEquals(4, result.getEntityByName("Animal")!!.getFields().size)
        assertEquals(3, result.getEntityByName("Duck")!!.getFields().size)
        assertEquals(2, result.getEntityByName("Fish")!!.getFields().size)
        assertEquals(2, result.getEntityByName("Zebra")!!.getFields().size)
    }
    
    @Test
    fun `should handle error cases gracefully`() {
        val invalidContent = "This is not a valid mermaid diagram"
        
        val parser = MermaidClassDiagramParser()
        val result = parser.parse(invalidContent)
        
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `should handle empty class diagram`() {
        val emptyContent = """
            classDiagram
        """.trimIndent()
        
        val parser = MermaidClassDiagramParser()
        val result = parser.parse(emptyContent)
        
        assertNotNull(result)
        assertEquals(0, result.entities.size)
        assertEquals(0, result.edges.size)
    }
}
