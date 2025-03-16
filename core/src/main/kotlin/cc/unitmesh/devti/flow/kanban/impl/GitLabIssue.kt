package cc.unitmesh.devti.flow.kanban.impl

import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.model.SimpleStory
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.models.Issue

class GitLabIssue(private val apiUrl: String, private val personalAccessToken: String, gitlabUrl: String) : Kanban {
    private lateinit var gitLabApi: GitLabApi

    init {
        initializeGitLabApi(gitlabUrl, personalAccessToken)
    }

    private fun initializeGitLabApi(url: String, userToken: String) {
        gitLabApi = GitLabApi(url, userToken)
    }

    override fun getStoryById(storyId: String): SimpleStory {
        val issue: Issue = gitLabApi.issuesApi.getIssue(apiUrl, storyId.toLong())
        return SimpleStory(issue.iid.toString(), issue.title, issue.description)
    }
}
