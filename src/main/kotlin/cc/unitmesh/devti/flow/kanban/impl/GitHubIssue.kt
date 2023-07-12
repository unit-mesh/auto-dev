package cc.unitmesh.devti.flow.kanban.impl

import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.model.SimpleProjectInfo
import cc.unitmesh.devti.flow.model.SimpleStory
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
        val repo = gitHub.getRepository(repoUrl)
        return SimpleProjectInfo(repo.fullName, repo.name, repo.description)
    }

    override fun getStories(): List<SimpleStory> {
        TODO("Not yet implemented")
    }

    override fun getStoryById(storyId: String): SimpleStory {
        val issue = gitHub.getRepository(repoUrl).getIssue(Integer.parseInt(storyId))
        if (issue.comments.size == 0) {
            return SimpleStory(issue.number.toString(), issue.title, issue.body)
        }

        // get all comments and filter body contains "用户故事"
        val comments = issue.comments
        val comment = comments.find { it.body.contains("用户故事") }
        if (comment != null) {
            return SimpleStory(issue.number.toString(), issue.title, comment.body)
        }

        return SimpleStory(issue.number.toString(), issue.title, issue.body)
    }

    override fun updateStoryDetail(simpleStory: SimpleStory) {
        // add comments to issues
        val issue = gitHub.getRepository(repoUrl).getIssue(Integer.parseInt(simpleStory.id))
        issue.comment(simpleStory.description)
    }
}
