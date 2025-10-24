package cc.unitmesh.kotlin.indexer.provider

import cc.unitmesh.devti.indexer.model.ElementType
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.runBlocking

class KotlinLangDictProviderTest : LightPlatformTestCase() {

    fun testShouldCollectFileNames() = runBlocking {
        // given
        val provider = KotlinLangDictProvider()

        // when
        val fileNames = provider.collectFileNames(project, 1000)

        // then - may be empty in test project, just verify it doesn't crash
        assertNotNull("Should return a list", fileNames)
    }

    fun testShouldCollectSemanticNames() = runBlocking {
        // given
        val provider = KotlinLangDictProvider()

        // when
        val domainDict = provider.collectSemanticNames(project, 2000)

        // then - verify structure even if empty
        assertNotNull("Should return domain dictionary", domainDict)
        assertTrue("Should have metadata", domainDict.metadata.isNotEmpty())
        assertEquals("Should have level1_count", true, domainDict.metadata.containsKey("level1_count"))
        assertEquals("Should have level2_count", true, domainDict.metadata.containsKey("level2_count"))
    }

    fun testShouldFilterTestFiles() {
        // given
        val provider = KotlinLangDictProvider()
        
        // when & then
        assertFalse(provider.shouldIncludeFile("UserTest.kt", "src/test/kotlin/UserTest.kt"))
        assertFalse(provider.shouldIncludeFile("UserTests.kt", "src/test/kotlin/UserTests.kt"))
        assertFalse(provider.shouldIncludeFile("UserSpec.kt", "src/test/kotlin/UserSpec.kt"))
        assertTrue(provider.shouldIncludeFile("User.kt", "src/main/kotlin/User.kt"))
    }

    fun testShouldFilterGeneratedFiles() {
        // given
        val provider = KotlinLangDictProvider()

        // when & then
        assertFalse("Should filter .gradle files",
            provider.shouldIncludeFile("Generated.kt", "/project/.gradle/Generated.kt"))
        assertFalse("Should filter build/generated files",
            provider.shouldIncludeFile("Generated.kt", "/project/build/generated/Generated.kt"))
        assertTrue("Should include main source files",
            provider.shouldIncludeFile("User.kt", "/project/src/main/kotlin/User.kt"))
    }

    fun testShouldSkipDataClassMethods() {
        // given
        val provider = KotlinLangDictProvider()
        
        // when & then
        assertTrue(provider.shouldSkipFunction("component1"))
        assertTrue(provider.shouldSkipFunction("component2"))
        assertTrue(provider.shouldSkipFunction("copy"))
        assertTrue(provider.shouldSkipFunction("equals"))
        assertTrue(provider.shouldSkipFunction("hashCode"))
        assertTrue(provider.shouldSkipFunction("toString"))
        assertFalse(provider.shouldSkipFunction("createUser"))
    }

    fun testShouldSkipGetterSetterMethods() {
        // given
        val provider = KotlinLangDictProvider()
        
        // when & then
        assertTrue(provider.shouldSkipFunction("getName"))
        assertTrue(provider.shouldSkipFunction("setName"))
        assertTrue(provider.shouldSkipFunction("getAge"))
        assertFalse(provider.shouldSkipFunction("createUser"))
    }

    fun testShouldSkipTestMethods() {
        // given
        val provider = KotlinLangDictProvider()
        
        // when & then
        assertTrue(provider.shouldSkipFunction("testCreateUser"))
        assertTrue(provider.shouldSkipFunction("testUpdateUser"))
        assertFalse(provider.shouldSkipFunction("createUser"))
    }

    fun testShouldCollectLevel1WithCorrectType() = runBlocking {
        // given
        val provider = KotlinLangDictProvider()

        // when
        val domainDict = provider.collectSemanticNames(project, 2000)

        // then
        val level1 = domainDict.level1
        // If there are any elements, they should be FILE type
        if (level1.isNotEmpty()) {
            assertTrue("Level 1 should contain FILE type elements",
                level1.all { it.type == ElementType.FILE })
        }
    }

    fun testShouldHaveWeightInformation() = runBlocking {
        // given
        val provider = KotlinLangDictProvider()
        
        // when
        val domainDict = provider.collectSemanticNames(project, 2000)
        
        // then
        val level1 = domainDict.level1
        if (level1.isNotEmpty()) {
            val firstElement = level1.first()
            assertNotNull("Should have weight", firstElement.weight)
            assertNotNull("Should have weight category", firstElement.weightCategory)
        }
    }

    fun testShouldRespectTokenBudget() = runBlocking {
        // given
        val provider = KotlinLangDictProvider()
        val smallBudget = 100
        
        // when
        val domainDict = provider.collectSemanticNames(project, smallBudget)
        
        // then
        val totalTokens = domainDict.getTotalTokens()
        assertTrue("Total tokens should not exceed budget significantly", 
            totalTokens <= smallBudget * 1.1) // Allow 10% overflow for edge cases
    }
}

