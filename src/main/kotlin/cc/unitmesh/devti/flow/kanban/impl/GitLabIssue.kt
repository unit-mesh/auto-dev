package cc.unitmesh.devti.flow.kanban.impl

import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.model.SimpleProjectInfo
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

    override fun getProjectInfo(): SimpleProjectInfo {
        val project = gitLabApi.projectApi.getProject(apiUrl)
        return SimpleProjectInfo(project.nameWithNamespace, project.name, project.description ?: "")
    }

    override fun getStories(): List<SimpleStory> = listOf()

    override fun getStoryById(storyId: String): SimpleStory {
        val issue: Issue = gitLabApi.issuesApi.getIssue(apiUrl, storyId.toLong())
        return SimpleStory(issue.iid.toString(), issue.title, issue.description)
    }


    override fun updateStoryDetail(simpleStory: SimpleStory) {
        // Create a note as a comment on the issue
        val issue: Issue = gitLabApi.issuesApi.getIssue(apiUrl, simpleStory.id.toLong())
        gitLabApi.notesApi.createIssueNote(apiUrl, issue.iid, simpleStory.description)
    }

    // Other methods like getStories() and isValidStory() can be implemented as needed
}
