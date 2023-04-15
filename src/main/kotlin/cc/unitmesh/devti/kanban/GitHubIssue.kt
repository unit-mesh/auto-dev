package cc.unitmesh.devti.kanban

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

    override fun getProjectInfo(): SimpleProjectInfo {
        TODO("Not yet implemented")
    }

    override fun getStories(): List<SimpleStory> {
        TODO("Not yet implemented")
    }

    override fun getStory(storyId: String): SimpleStory {
        TODO("Not yet implemented")
    }

    override fun updateStoryDetail(simpleStory: SimpleStory) {
        TODO("Not yet implemented")
    }
}
