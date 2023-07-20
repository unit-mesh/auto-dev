package cc.unitmesh.ide.pycharm.provider

import cc.unitmesh.devti.provider.TechStackProvider
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase
import org.junit.Ignore


@Ignore
class PythonTechStackServiceTest : LightPlatformTestCase() {
    fun testShould_get_python_tech_provider() {
        val stack = TechStackProvider.stack("Python")
        TestCase.assertNotNull(stack)
    }
}