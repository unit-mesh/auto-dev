package cc.unitmesh.devti.kanban.impl

import cc.unitmesh.devti.kanban.Kanban
import cc.unitmesh.devti.kanban.SimpleProjectInfo
import cc.unitmesh.devti.kanban.SimpleStory
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

class GitHubIssue(val repoUrl: String, val token: String) : Kanban {
    private val gitHub: GitHub

    init {
        try {
            gitHub = GitHubBuilder()
                .withOAuthToken(token)
                .build()
        } catch (e: Exception) {
            throw e
        }
    }

    private fun fetchIssueById(id: String): String {
        return gitHub.getRepository(repoUrl).getIssue(Integer.parseInt(id)).body
    }

    override fun isValidStory(id: String): Boolean {
        return super.isValidStory(fetchIssueById(id))
    }

    override fun getProjectInfo(): SimpleProjectInfo {
        val repo = gitHub.getRepository(repoUrl)
        return SimpleProjectInfo(repo.nodeId, repo.name, repo.description)
    }

    override fun getStories(): List<SimpleStory> {
        TODO("Not yet implemented")
    }

    override fun getStoryById(storyId: String): SimpleStory {
        TODO("Not yet implemented")
    }

    override fun updateStoryDetail(simpleStory: SimpleStory) {
        TODO("Not yet implemented")
    }
}
