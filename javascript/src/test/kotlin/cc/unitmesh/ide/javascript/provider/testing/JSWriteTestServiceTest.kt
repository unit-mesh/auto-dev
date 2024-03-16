package cc.unitmesh.ide.javascript.provider.testing;

import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase
import java.io.File

class JSWriteTestServiceTest : LightPlatformTestCase() {
    fun testShouldReturnNullWhenFilePathEmpty() {
        // given
        val code = """
            export class Foo {
                constructor() {
                }
            }
        """.trimIndent()

        val file = PsiFileFactory.getInstance(project).createFileFromText(JavascriptLanguage.INSTANCE, code)

        // when
        val result = JSAutoTestService.Util.getTestFilePath(file)

        // then
        assertEquals("null", result.toString())
    }

    fun testShouldReturnCorrectPath() {
        // given
        val code = """
            export class Foo {
                constructor() {
                }
            }
        """.trimIndent()

        val fileType = JavaScriptFileType.INSTANCE
        val file = PsiFileFactory.getInstance(project).createFileFromText(
            "parent" + File.separator + "Foo." + fileType.defaultExtension,
            fileType,
            code,
        )

        // when
        val result = JSAutoTestService.Util.getTestFilePath(file)

        // then
        assertEquals("null", result.toString())
    }

    fun testShouldHandleForFunction() {
        // given
        val code = """
            interface InputData {
              name: string;
              age: number;
            }
            
            interface OutputData {
              greeting: string;
              message: string;
            }
            
            class GreetingService {
              static greet(input: InputData): OutputData {
                const greeting = `Hello, ${'$'}{input.name}!`;
                const message = `You are ${'$'}{input.age} years old.`;
            
                return { greeting, message };
              }
            }
            """.trimIndent()

        val fileType = TypeScriptFileType.INSTANCE
        val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
            "Foo." + fileType.defaultExtension,
            fileType,
            code,
        )
        val clazz = PsiTreeUtil.findChildrenOfAnyType(psiFile, JSClass::class.java).toList()[2]
        val function = PsiTreeUtil.findChildOfType(clazz, JSFunction::class.java)!!

        val relevantClass = JSAutoTestService().lookupRelevantClass(project, function)

        TestCase.assertEquals(relevantClass.size, 2)
        println(relevantClass.map { it.format() })
    }

    fun testShouldHandleReactPropsAsRelevant() {
        val code = """
            import React, { useCallback, useState } from "react";
            
            interface CoreEditorProps {
              value: string;
              onChange: (value: string) => any;
              onSave: (value: string) => any;
            }

            function CoreEditor(props: CoreEditorProps) {
               return (<div />); 
            }
            
            export default CoreEditor;
            """.trimIndent()

        val fileType = TypeScriptFileType.INSTANCE
        val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
            "Foo." + fileType.defaultExtension,
            fileType,
            code,
        )

        val function = PsiTreeUtil.findChildrenOfAnyType(psiFile, JSFunction::class.java).toList()[2]

        val relevantClass = JSAutoTestService().lookupRelevantClass(project, function)

        TestCase.assertEquals(relevantClass.size, 1)

        println(relevantClass.map { it.format() })
    }
}
