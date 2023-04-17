package cc.unitmesh.devti.analysis

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JavaCrudTemplateTest : BasePlatformTestCase() {
    fun testShould_create_a_new_controller() {
        val template = JavaCrudTemplate(project)
        val code = template.controller("HelloController", "", "com.example")
        assertEquals(
            code, """package com.example;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
class HelloController {

}
"""
        )
    }

}
