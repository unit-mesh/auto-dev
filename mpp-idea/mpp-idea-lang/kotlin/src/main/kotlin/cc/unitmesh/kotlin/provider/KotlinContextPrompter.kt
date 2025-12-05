package cc.unitmesh.kotlin.provider

import cc.unitmesh.idea.prompting.JavaContextPrompter

class KotlinContextPrompter: JavaContextPrompter() {
    override val psiElementDataBuilder = KotlinPsiElementDataBuilder()
}