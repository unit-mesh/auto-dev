package cc.unitmesh.diagram.parser

import cc.unitmesh.diagram.model.GraphGraphType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MermaidClassDiagramParserTest {
    
    private val parser = MermaidClassDiagramParser()
    
    @Test
    fun `should parse simple class diagram`() {
        val mermaidContent = """
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
        
        val result = parser.parse(mermaidContent)
        
        assertNotNull(result)
        assertEquals(GraphGraphType.DIGRAPH, result.graphType)
        assertEquals("mermaid_class_diagram", result.getGraphAttribute("type"))
        
        // Check entities
        assertEquals(2, result.entities.size)
        
        val animal = result.getEntityByName("Animal")
        assertNotNull(animal)
        assertEquals(3, animal!!.getFields().size)
        
        val duck = result.getEntityByName("Duck")
        assertNotNull(duck)
        assertEquals(2, duck!!.getFields().size)
        
        // Check inheritance edge
        assertEquals(1, result.edges.size)
        val edge = result.edges.first()
        assertEquals("Duck", edge.sourceNodeId)
        assertEquals("Animal", edge.targetNodeId)
        assertEquals("extends", edge.label)
    }
    
    @Test
    fun `should parse complex class diagram with multiple classes`() {
        val mermaidContent = """
            ---
            title: Animal example
            ---
            classDiagram
                note "From Duck till Zebra"
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
        
        val result = parser.parse(mermaidContent)
        
        assertNotNull(result)
        assertEquals(4, result.entities.size)
        assertEquals(3, result.edges.size)
        
        // Check all classes exist
        assertNotNull(result.getEntityByName("Animal"))
        assertNotNull(result.getEntityByName("Duck"))
        assertNotNull(result.getEntityByName("Fish"))
        assertNotNull(result.getEntityByName("Zebra"))
        
        // Check Animal has correct fields
        val animal = result.getEntityByName("Animal")!!
        assertEquals(4, animal.getFields().size)
        
        // Check Duck has correct fields
        val duck = result.getEntityByName("Duck")!!
        assertEquals(3, duck.getFields().size)
        
        // Check Fish has correct fields
        val fish = result.getEntityByName("Fish")!!
        assertEquals(2, fish.getFields().size)
        
        // Check Zebra has correct fields
        val zebra = result.getEntityByName("Zebra")!!
        assertEquals(2, zebra.getFields().size)
    }
    
    @Test
    fun `should return empty data for invalid content`() {
        val invalidContent = "This is not a valid mermaid diagram"
        
        val result = parser.parse(invalidContent)
        
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `should handle empty class diagram`() {
        val emptyContent = """
            classDiagram
        """.trimIndent()

        val result = parser.parse(emptyContent)

        assertNotNull(result)
        assertEquals(0, result.entities.size)
        assertEquals(0, result.edges.size)
    }

    @Test
    fun `should parse class annotation statements`() {
        val mermaidContent = """
            classDiagram
                class User {
                    +name
                    +email
                    +getName()
                    +setName(String)
                }
                User : Added
        """.trimIndent()

        val result = parser.parse(mermaidContent)

        assertNotNull(result)
        assertEquals(1, result.entities.size)

        val userEntity = result.getEntityByName("User")
        assertNotNull(userEntity)
        assertEquals(4, userEntity!!.getFields().size)

        // Check that change status is captured in graph attributes
        assertTrue(result.graphAttributes.containsKey("User_change"))
        assertEquals("Added", result.graphAttributes["User_change"])
    }
}
