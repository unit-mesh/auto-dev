package cc.unitmesh.kotlin.provider

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory


class KotlinTestDataBuilderTest : LightPlatformTestCase() {
    private val code = """
            package cc.unitmesh.untitled.demo.controller

            import org.springframework.web.bind.annotation.*
            
            @RestController
            @RequestMapping("/user")
            class UserController() {
                @GetMapping
                fun getUsers(): UserDTO {
                    return UserDTO(1L, "username", "email", "name", "surname")
                }
            }
            
            data class UserDTO(
                val id: Long? = null,
                val username: String,
                val email: String,
                val name: String,
                val surname: String? = null,
            )
            """.trimIndent()

    fun testShouldPass() {
        assertTrue(true)
    }

    fun testShouldParseBaseRoute() {
        val createFile = KtPsiFactory(project).createFile("UserController.kt", code)
        val builder = KotlinPsiElementDataBuilder()

        val firstFunction = PsiTreeUtil.findChildOfType(createFile, KtNamedFunction::class.java)!!

        assertEquals(builder.baseRoute(firstFunction), "/user")
    }

    fun testShouldHandleForList() {
        if(!isLatest()) {
            println("skip testShouldReturnLangFileSuffix")
            return
        }

        val code = """
            package cc.unitmesh.untitled.demo.controller

            import org.springframework.web.bind.annotation.*
            
            @RestController
            @RequestMapping("/user")
            class UserController() {
                @GetMapping
                fun getUsers(): List<UserDTO> {
                    return listOf(UserDTO(1L, "username", "email", "name", "surname"))
                }
            }
            
            data class UserDTO(
                val id: Long? = null,
                val username: String,
                val email: String,
                val name: String,
                val surname: String? = null,
            )
            """.trimIndent()

        val createFile = KtPsiFactory(project).createFile("UserController.kt", code)
        val builder = KotlinPsiElementDataBuilder()

        val firstFunction = PsiTreeUtil.findChildOfType(createFile, KtNamedFunction::class.java)!!
        val outboundData = builder.outboundData(firstFunction)

        assertEquals(outboundData.size, 1)
        assertEquals(
            outboundData["cc.unitmesh.untitled.demo.controller.UserDTO"],
            """
            'package: cc.unitmesh.untitled.demo.controller.UserDTO
            class UserDTO {
              val id: Long? = null
              val username: String
              val email: String
              val name: String
              val surname: String? = null
              
            }
            """.trimIndent()
        )
    }

    private fun isLatest() = ApplicationInfo.getInstance().build.baselineVersion >= 232

    // test will fail if 222
    fun testShouldReturnLangFileSuffix() {
        if(!isLatest()) {
            println("skip testShouldReturnLangFileSuffix")
            return
        }

        val createFile = KtPsiFactory(project).createFile("UserController.kt", code)
        val builder = KotlinPsiElementDataBuilder()

        val firstFunction = PsiTreeUtil.findChildOfType(createFile, KtNamedFunction::class.java)!!
        val outboundData = builder.outboundData(firstFunction)

        assertEquals(outboundData.size, 1)
        assertEquals(
            outboundData["cc.unitmesh.untitled.demo.controller.UserDTO"],
            """
                'package: cc.unitmesh.untitled.demo.controller.UserDTO
                class UserDTO {
                  val id: Long? = null
                  val username: String
                  val email: String
                  val name: String
                  val surname: String? = null
                  
                }
                """.trimIndent()
        )
    }

    fun testShouldHandleWithResponse() {
        if(!isLatest()) {
            println("skip testShouldReturnLangFileSuffix")
            return
        }

        val code = """
            package com.thoughtworks.archguard.code.method.controller

            import org.springframework.http.ResponseEntity
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RestController
            
            @Serializable
            data class JMethod(
                val id: String,
                val name: String,
                val clazz: String,
                val module: String?,
                val returnType: String,
                val argumentTypes: List<String>
            )
            
            @RestController
            @RequestMapping("/api/systems/{systemId}/methods")
            class MethodController(val methodService: MethodService) {
                @GetMapping("/callees")
                fun getMethodCallees(
                    @PathVariable("systemId") systemId: Long,
                    @RequestParam("name") methodName: String,
                    @RequestParam(value = "clazz") clazzName: String,
                    @RequestParam(value = "deep", required = false, defaultValue = "3") deep: Int,
                    @RequestParam(value = "needIncludeImpl", required = false, defaultValue = "true") needIncludeImpl: Boolean,
                    @RequestParam(value = "module") moduleName: String
                ): ResponseEntity<List<JMethod>> {
                    val jMethod = listOf(JMethod("1", "methodName", "TestClass", null, "void", listOf("String")))
                    return ResponseEntity.ok(jMethod)
                }
            }
            
        """.trimIndent()

        val createFile = KtPsiFactory(project).createFile("MethodController.kt", code)
        val builder = KotlinPsiElementDataBuilder()

        val firstFunction = PsiTreeUtil.findChildOfType(createFile, KtNamedFunction::class.java)!!
        val outboundData = builder.outboundData(firstFunction)
        println(outboundData)
    }
}
