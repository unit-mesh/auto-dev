package cc.unitmesh.kotlin.provider;

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory


class KotlinTestContextProviderTest : LightPlatformTestCase() {

    fun testShouldReturnLangFileSuffix() {
        val code = """
            package com.example.blog

            import org.springframework.http.HttpStatus.*
            import org.springframework.stereotype.Controller
            import org.springframework.ui.Model
            import org.springframework.ui.set
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.server.ResponseStatusException

            @Controller
            class HtmlController(private val repository: ArticleRepository,
            					 private val properties: BlogProperties) {

            	@GetMapping("/")
            	fun blog(model: Model): String {
            		model["title"] = properties.title
            		model["banner"] = properties.banner
            		model["articles"] = repository.findAllByOrderByAddedAtDesc().map { it.render() }
            		return "blog"
            	}

            	@GetMapping("/article/{slug}")
            	fun article(@PathVariable slug: String, model: Model): String {
            		val article = repository
            				.findBySlug(slug)
            				?.render()
            				?: throw ResponseStatusException(NOT_FOUND, "This article does not exist")
            		model["title"] = article.title
            		model["article"] = article
            		return "article"
            	}

            	fun Article.render() = RenderedArticle(
            			slug,
            			title,
            			headline,
            			content,
            			author,
            			addedAt.format()
            	)

            	data class RenderedArticle(
            			val slug: String,
            			val title: String,
            			val headline: String,
            			val content: String,
            			val author: User,
            			val addedAt: String)

            }
            """.trimIndent()
        val createFile = KtPsiFactory(project).createFile("HtmlController.kt", code)
        val provider = KotlinTestContextProvider()

        TestCase.assertEquals(provider.langFileSuffix(), "kt")

        val firstFunction = PsiTreeUtil.findChildOfType(createFile, KtNamedFunction::class.java)!!
        TestCase.assertTrue(provider.isSpringRelated(firstFunction))
    }

}
