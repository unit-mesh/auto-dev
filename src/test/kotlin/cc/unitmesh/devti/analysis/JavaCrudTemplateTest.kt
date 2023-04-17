package cc.unitmesh.devti.analysis

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JavaCrudTemplateTest: BasePlatformTestCase() {
    fun testShould_create_a_new_controller() {
        val template = JavaCrudTemplate(project)
        val code = template.controller("HelloController", "", "com.example")
        assertEquals(code, """package com.example;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
class HelloController {

}""");
    }
}
