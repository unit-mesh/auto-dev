package cc.unitmesh.devti.flow.model

/**
 * for GPT to generate stories, like:
 * 网站信息
 *
 * """
 * phodal.com (A Growth Engineering Blog)
 * """
 */
class SimpleProjectInfo(
    val id: String,
    val name: String,
    val description: String
) {
}