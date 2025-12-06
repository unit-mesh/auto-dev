package cc.unitmesh.xuiper.integration

import cc.unitmesh.xuiper.parser.ParseResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * Integration tests for NanoDSL generation.
 * 
 * Each test:
 * 1. Takes a user requirement
 * 2. Calls LLM to generate NanoDSL
 * 3. Verifies the generated DSL is compilable
 * 
 * Run with: ./gradlew :xuiper-ui:integrationTest
 */
class NanoDSLGenerationIntegrationTest : NanoDSLIntegrationTestBase() {

    // ==================== BASIC ====================

    @Test
    @DisplayName("01 - Simple Card: Generate greeting card with title and message")
    fun `01 simple card should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()
        
        val userPrompt = "Create a simple greeting card with a hello title and welcome message"
        val generatedDsl = generateDsl(userPrompt)
        println("Generated DSL: $generatedDsl")
        val result = verifyDslCompiles(generatedDsl, "01-simple-card")
        
        assertTrue(result is ParseResult.Success, 
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    @Test
    @DisplayName("02 - Product Card: Generate product display with image, title, price, badge")
    fun `02 product card should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()
        
        val userPrompt = """Create a product card component that takes a Product item and displays: 
            product image, title, 'New' badge if item is new, description (limited to 2 lines), 
            price, and an 'Add to Cart' button"""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "02-product-card")
        
        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    @Test
    @DisplayName("03 - Counter Card: Generate counter with state and price calculation")
    fun `03 counter card should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()
        
        val userPrompt = """Create a counter card component with count and price state. 
            Show the total (count * price), and include minus/plus buttons to modify count, 
            with an input field for direct count entry"""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "03-counter-card")
        
        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    // ==================== INTERACTION ====================

    @Test
    @DisplayName("04 - Login Form: Generate login form with inputs and navigation")
    fun `04 login form should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()
        
        val userPrompt = """Create a login form component with email and password inputs, 
            a Login button that calls /api/login, and a 'Sign Up' link that navigates to /signup"""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "04-login-form")
        
        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    @Test
    @DisplayName("05 - Task List: Generate todo list with add/delete functionality")
    fun `05 task list should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()
        
        val userPrompt = """Create a task list component that takes a list of Task items. 
            Include an input to add new tasks, and render each task with a checkbox and delete button. 
            Use a for loop to iterate over tasks"""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "05-task-list")
        
        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    @Test
    @DisplayName("06 - User Profile: Generate profile card with avatar and stats")
    fun `06 user profile should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()
        
        val userPrompt = """Create a user profile card that displays: avatar image, name, email, 
            verified badge (if verified), bio text, follower/following counts, and an Edit Profile button"""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "06-user-profile")
        
        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    // ==================== HTTP ====================

    @Test
    @DisplayName("07 - HTTP Request Form: Generate contact form with POST submission")
    fun `07 http request form should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()
        
        val userPrompt = """Create a contact form component with name, email, and message fields. 
            Include a Send button that submits a POST request to /api/contact with the form data as JSON. 
            Show loading state while submitting, display success message on success, 
            and show error message on failure. Use proper HTTP headers for JSON content type."""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "07-http-request-form")
        
        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    // ==================== CONDITIONAL ====================

    @Test
    @DisplayName("08 - Nested Conditionals: Generate order status with complex conditions")
    fun `08 nested conditionals should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create an OrderStatusCard component that takes an Order object.
            Display the order ID with a status badge that changes color based on status
            (pending=yellow, processing=blue, shipped=green, cancelled=red).
            If the order is urgent, show an URGENT badge with priority message.
            Show total, item count, and discount if applicable. If shipped, show tracking number and ETA.
            Show Cancel button only for pending orders, and View Details button for non-cancelled orders."""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "08-nested-conditionals")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    // ==================== LIST ====================

    @Test
    @DisplayName("09 - Nested Loops: Generate category product list with nested iterations")
    fun `09 nested loops should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create a CategoryProductList component that takes a list of Category objects.
            Each category has a name, product_count, and list of products.
            Show categories with expand/collapse functionality using state.
            When expanded, show products with thumbnail, name, price (with sale price if on_sale),
            stock status badge, and Add to Cart button.
            Include Load More button if category has more products."""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "09-nested-loops")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    // ==================== STATE ====================

    @Test
    @DisplayName("10 - Complex State: Generate invoice calculator with computed values")
    fun `10 complex state should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create an InvoiceCalculator component with state for items list, tax_rate,
            discount_percent, discount_code, and computed values (subtotal, tax_amount, discount_amount, total).
            Allow adding/removing items, adjusting quantities.
            Include a discount code input that validates via API call.
            Show breakdown of subtotal, discount (if applied), tax, and total with reactive bindings."""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "10-complex-state")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    @Test
    @DisplayName("11 - Multi Action Sequence: Generate payment form with complex actions")
    fun `11 multi action sequence should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create a PaymentForm component with card details (number, expiry, cvv, name)
            and a save_card checkbox. On Pay button click, perform validation,
            then make POST to /api/payment/process with all data.
            On success: clear cart, show toast, track event, navigate to confirmation.
            On error: show error message.
            Show success state with View Order button after payment completes."""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "11-multi-action-sequence")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    // ==================== COMPOSITE ====================

    @Test
    @DisplayName("12 - Dashboard Layout: Generate dashboard with stats and tabs")
    fun `12 dashboard layout should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create a Dashboard component that takes User and Stats objects.
            Show welcome message, date range selector, and export button.
            Display 4 stat cards (Revenue, Orders, Customers, Conversion) with values and
            percentage change badges (green if positive, red if negative).
            Add tab navigation (Overview, Orders, Products, Customers).
            In Overview tab, show Recent Orders list and Top Products list side by side."""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "12-dashboard-layout")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    @Test
    @DisplayName("13 - Form Validation: Generate registration form with inline validation")
    fun `13 form validation should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create a RegistrationForm component with fields:
            username (with async availability check on blur), email,
            password (with strength indicator showing 8+ chars),
            confirm_password (with match validation), phone (optional), country select,
            and terms checkbox. Show field-level error messages from state.errors.
            On submit, call /api/register and handle success (navigate to login)
            and error (show field errors)."""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "13-form-validation")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    @Test
    @DisplayName("14 - Pagination List: Generate article list with search, filter, sort")
    fun `14 pagination list should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create an ArticleList component with state for articles, current_page,
            total_pages, search_query, filter_category, and sort_by.
            Show search input, category filter, and sort dropdown.
            Display articles with thumbnail, category badge, featured badge if applicable,
            title, excerpt, author, date, and read time.
            Include full pagination controls (First, Prev, numbered pages, Next, Last)
            and page indicator."""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "14-pagination-list")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    @Test
    @DisplayName("15 - Shopping Cart: Generate full shopping cart with checkout flow")
    fun `15 shopping cart should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create a ShoppingCart component with items list, coupon functionality,
            and order summary. Show empty cart state with Continue Shopping button.
            For items: display image, name, variant, price, quantity controls (with API update),
            stock status, line total, and remove button.
            Include coupon input with apply functionality.
            Show shipping method select, then subtotal/shipping/tax/total breakdown.
            Include Proceed to Checkout button."""
        val generatedDsl = generateDsl(userPrompt)
        println(generatedDsl)
        val result = verifyDslCompiles(generatedDsl, "15-shopping-cart")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    // ==================== NAVIGATION ====================

    @Test
    @DisplayName("16 - Multi-Page Navigation: Generate app with multiple pages and navigation")
    fun `16 multi page navigation should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create a simple multi-page app with:
            a HomePage component with a welcome message and a button to navigate to /about,
            an AboutPage component with company info and a Back button to navigate to /,
            and a NavBar component at the top with links to both pages"""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "16-multi-page-navigation")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    @Test
    @DisplayName("17 - Parameterized Route: Generate navigation with route parameters")
    fun `17 parameterized route should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create a UserListItem component that displays user avatar, name, email,
            and a View Profile button that navigates to /user/{id} with the user's id as a param.
            Also create a UserProfile component that shows the user ID from state and has a
            Back to List button that navigates to /users"""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "17-parameterized-route")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    @Test
    @DisplayName("18 - Search with Query: Generate search page with query parameter navigation")
    fun `18 search with query should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create a SearchPage component with search input, category filter dropdown,
            and results list. The Search button should navigate to /search with query parameters
            for search term (q), category, and page number. Include Previous/Next pagination buttons
            that update the page query param"""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "18-search-with-query")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }

    @Test
    @DisplayName("19 - Conditional Navigation: Generate login form with conditional navigation")
    fun `19 conditional navigation should generate compilable dsl`() = runTest(timeout = 2.minutes) {
        assumeConfigured()

        val userPrompt = """Create a LoginForm with email/password inputs. On login button click,
            call /api/login API. On success, navigate to /dashboard with replace=true
            (so user can't go back to login page). On error, show error message in a badge.
            Include a Sign Up link that navigates to /signup and a Forgot Password link
            that navigates to /forgot-password with the email as a query param"""
        val generatedDsl = generateDsl(userPrompt)
        val result = verifyDslCompiles(generatedDsl, "19-conditional-navigation")

        assertTrue(result is ParseResult.Success,
            "Generated DSL should be parseable. DSL:\n$generatedDsl")
    }
}

