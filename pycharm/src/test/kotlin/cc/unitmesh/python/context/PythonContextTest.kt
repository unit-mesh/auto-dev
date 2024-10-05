package cc.unitmesh.python.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.PyFile
import org.intellij.lang.annotations.Language

class PythonContextTest  : BasePlatformTestCase() {
    private val fileFactory: PsiFileFactory get() = PsiFileFactory.getInstance(project)
    @Language("Python")
    private val classCode = """class Person:
  def __init__(self, name, age):
    self.name = name
    self.age = age

  def myfunc(self):
    print("Hello my name is " + self.name)  

p1 = Person("John", 36)

print(p1.name)
print(p1.age) """

    fun testShould_convert_class_to_string() {
//        val psiFile = fileFactory.createFileFromText(PythonLanguage.INSTANCE, classCode)
//        val psiElement = (psiFile as PyFile).topLevelClasses[0]
//        val classContext: ClassContext = ClassContextProvider(false).from(psiElement)
//
//        assertEquals(classContext.format(), """'package: /foo.bar
//class Person {
//
//  + __init__
//  + myfunc
//}""")
    }
}
