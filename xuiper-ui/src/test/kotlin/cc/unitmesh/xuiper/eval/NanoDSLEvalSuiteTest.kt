package cc.unitmesh.xuiper.eval

import cc.unitmesh.xuiper.ast.NanoNode
import cc.unitmesh.xuiper.dsl.NanoDSL
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for each NanoDSL evaluation test case.
 * Converted from nanodsl-eval-suite.json to ensure each DSL file can be parsed correctly.
 */
class NanoDSLEvalSuiteTest {
    
    private fun loadExpectFile(filename: String): String {
        val file = File("testcases/expect/$filename")
        if (!file.exists()) {
            throw IllegalStateException("Test file not found: ${file.absolutePath}")
        }
        return file.readText()
    }

    // ==================== BASIC ====================
    
    @Test
    fun `01 - simple-card - should parse greeting card with title and message`() {
        val source = loadExpectFile("01-simple-card.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("GreetingCard", result.name)
        assertTrue(result.children.isNotEmpty(), "Component should have children")
        val card = result.children[0] as NanoNode.Card
        assertEquals("md", card.padding)
        assertTrue(card.children.isNotEmpty(), "Card should have children")
    }

    // ==================== COMPOSITE ====================
    
    @Test
    fun `02 - product-card - should parse product card with image, badge, and button`() {
        val source = loadExpectFile("02-product-card.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("ProductCard", result.name)
        assertTrue(result.params.isNotEmpty())
        assertTrue(result.params.any { it.name == "item" })
    }

    // ==================== STATE ====================
    
    @Test
    fun `03 - counter-card - should parse counter with state and bindings`() {
        val source = loadExpectFile("03-counter-card.nanodsl")
        val result = NanoDSL.parse(source)
        
        assertEquals("CounterCard", result.name)
        assertNotNull(result.state)
        val stateVars = result.state!!.variables
        assertTrue(stateVars.any { it.name == "count" })
        assertTrue(stateVars.any { it.name == "price" })
    }

    // ==================== INTERACTION ====================
    
    @Test
    fun `04 - login-form - should parse login form with inputs and navigation`() {
        val source = loadExpectFile("04-login-form.nanodsl")
        val result = NanoDSL.parse(source)
        
        assertEquals("LoginForm", result.name)
        assertNotNull(result.state)
        assertTrue(result.state!!.variables.any { it.name == "email" })
        assertTrue(result.state!!.variables.any { it.name == "password" })
    }

    // ==================== LIST ====================
    
    @Test
    fun `05 - task-list - should parse task list with for loop and checkbox`() {
        val source = loadExpectFile("05-task-list.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("TaskList", result.name)
        assertTrue(result.params.isNotEmpty())
        assertTrue(result.params.any { it.name == "tasks" })
    }

    @Test
    fun `06 - user-profile - should parse user profile with conditional badge`() {
        val source = loadExpectFile("06-user-profile.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("UserProfile", result.name)
        assertTrue(result.params.isNotEmpty())
        assertTrue(result.params.any { it.name == "user" })
    }

    // ==================== HTTP ====================
    
    @Test
    fun `07 - http-request-form - should parse contact form with fetch and callbacks`() {
        val source = loadExpectFile("07-http-request-form.nanodsl")
        val result = NanoDSL.parse(source)
        
        assertEquals("ContactForm", result.name)
        assertNotNull(result.state)
        assertTrue(result.state!!.variables.any { it.name == "name" })
        assertTrue(result.state!!.variables.any { it.name == "loading" })
        assertTrue(result.state!!.variables.any { it.name == "success" })
    }

    // ==================== CONDITIONAL ====================
    
    @Test
    fun `08 - nested-conditionals - should parse order status with multiple nested conditionals`() {
        val source = loadExpectFile("08-nested-conditionals.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("OrderStatusCard", result.name)
        assertTrue(result.params.isNotEmpty())
        assertTrue(result.params.any { it.name == "order" })
    }

    // ==================== NESTED LOOPS ====================
    
    @Test
    fun `09 - nested-loops - should parse category product list with nested for loops`() {
        val source = loadExpectFile("09-nested-loops.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("CategoryProductList", result.name)
        assertTrue(result.params.isNotEmpty())
        assertTrue(result.params.any { it.name == "categories" })
        assertNotNull(result.state)
        assertTrue(result.state!!.variables.any { it.name == "expanded_category" })
    }

    // ==================== COMPLEX STATE ====================

    @Test
    fun `10 - complex-state - should parse invoice calculator with computed state`() {
        val source = loadExpectFile("10-complex-state.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("InvoiceCalculator", result.name)
        assertNotNull(result.state)
        val stateVars = result.state!!.variables
        assertTrue(stateVars.any { it.name == "tax_rate" })
        assertTrue(stateVars.any { it.name == "discount_percent" })
        assertTrue(stateVars.any { it.name == "total" })
    }

    // ==================== MULTI-ACTION ====================

    @Test
    fun `11 - multi-action-sequence - should parse payment form with action sequences`() {
        val source = loadExpectFile("11-multi-action-sequence.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("PaymentForm", result.name)
        assertNotNull(result.state)
        val stateVars = result.state!!.variables
        assertTrue(stateVars.any { it.name == "card_number" })
        assertTrue(stateVars.any { it.name == "is_processing" })
        assertTrue(stateVars.any { it.name == "payment_success" })
    }

    // ==================== DASHBOARD ====================

    @Test
    fun `12 - dashboard-layout - should parse dashboard with tabs and stat cards`() {
        val source = loadExpectFile("12-dashboard-layout.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("Dashboard", result.name)
        assertTrue(result.params.isNotEmpty())
        assertTrue(result.params.any { it.name == "user" })
        assertTrue(result.params.any { it.name == "stats" })
        assertNotNull(result.state)
        assertTrue(result.state!!.variables.any { it.name == "active_tab" })
    }

    // ==================== FORM VALIDATION ====================

    @Test
    fun `13 - form-validation - should parse registration form with async validation`() {
        val source = loadExpectFile("13-form-validation.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("RegistrationForm", result.name)
        assertNotNull(result.state)
        val stateVars = result.state!!.variables
        assertTrue(stateVars.any { it.name == "username" })
        assertTrue(stateVars.any { it.name == "email" })
        assertTrue(stateVars.any { it.name == "password" })
        assertTrue(stateVars.any { it.name == "username_available" })
    }

    // ==================== PAGINATION ====================

    @Test
    fun `14 - pagination-list - should parse article list with pagination controls`() {
        val source = loadExpectFile("14-pagination-list.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("ArticleList", result.name)
        assertNotNull(result.state)
        val stateVars = result.state!!.variables
        assertTrue(stateVars.any { it.name == "articles" })
        assertTrue(stateVars.any { it.name == "current_page" })
        assertTrue(stateVars.any { it.name == "total_pages" })
        assertTrue(stateVars.any { it.name == "search_query" })
    }

    // ==================== SHOPPING CART ====================

    @Test
    fun `15 - shopping-cart - should parse shopping cart with full functionality`() {
        val source = loadExpectFile("15-shopping-cart.nanodsl")
        val result = NanoDSL.parse(source)

        assertEquals("ShoppingCart", result.name)
        assertNotNull(result.state)
        val stateVars = result.state!!.variables
        assertTrue(stateVars.any { it.name == "items" })
        assertTrue(stateVars.any { it.name == "coupon_code" })
        assertTrue(stateVars.any { it.name == "subtotal" })
        assertTrue(stateVars.any { it.name == "total" })
    }
}

