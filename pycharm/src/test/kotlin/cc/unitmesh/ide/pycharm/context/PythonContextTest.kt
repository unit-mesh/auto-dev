package cc.unitmesh.ide.pycharm.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.PyFile

class PythonContextTest  : LightPlatformTestCase() {
    private val fileFactory: PsiFileFactory get() = PsiFileFactory.getInstance(project)
    private val classCode = """class Person:
  def __init__(self, name, age):
    self.name = name
    self.age = age

p1 = Person("John", 36)

print(p1.name)
print(p1.age) """

    fun testShould_convert_class_to_string() {
        val psiFile = fileFactory.createFileFromText(PythonLanguage.INSTANCE, classCode)
        val psiElement = (psiFile as PyFile).topLevelClasses[0]
        val classContext: ClassContext = ClassContextProvider(false).from(psiElement)

        assertEquals(classContext.toQuery(), """class name: _
class fields: 
class methods: 
super classes: null
""");
    }
}