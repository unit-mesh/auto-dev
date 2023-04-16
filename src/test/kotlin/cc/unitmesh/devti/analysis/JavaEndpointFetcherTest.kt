package cc.unitmesh.devti.analysis

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.io.path.Path


class JavaEndpointFetcherTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = Path("resources").resolve("java")
        .resolve("endpoint")
        .toString()
//
//    fun testShould_success_filter_all_controller() {
//        myFixture.testDataPath = testDataPath
//
//        val javaEndpointFetcher = JavaEndpointFetcher(myFixture.project)
//        val controllerFiles = javaEndpointFetcher.getAllControllerFiles()
//        assertEquals(controllerFiles.size, 1)
//        assertEquals(controllerFiles[0].name, "Controller1.java")
//    }
}