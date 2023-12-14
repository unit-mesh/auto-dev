package cc.unitmesh.devti.flow.kanban

import cc.unitmesh.devti.flow.model.SimpleProjectInfo
import cc.unitmesh.devti.flow.model.SimpleStory

/**
 * The Kanban interface represents a kanban board that manages user stories for a project.
 * It provides methods to validate stories, retrieve project information, retrieve stories,
 * retrieve a story by ID, and update story details.
 */
interface Kanban {
    /**
     * Checks if the given content is a valid story.
     *
     * @param content The content to be checked.
     * @return true if the content contains the keywords "用户故事" or "User Story", false otherwise.
     */
    fun isValidStory(content: String): Boolean = content.contains("用户故事") || content.contains("User Story")

    /**
     * Retrieves the basic information of the project.
     *
     * @return The SimpleProjectInfo object containing the basic info
     */
    fun getProjectInfo(): SimpleProjectInfo

    /**
     * Retrieves a list of simple stories.
     *
     * @return a list of SimpleStory objects representing the stories.
     */
    fun getStories(): List<SimpleStory>

    /**
     * Retrieves a user story by its ID.
     *
     * @param storyId The ID of the user story to retrieve.
     * @return The user story with the specified ID.
     */
    fun getStoryById(storyId: String): SimpleStory

    /**
     * Updates the detailed information of a user story.
     *
     * @param simpleStory The user story object containing the updated information.
     */
    fun updateStoryDetail(simpleStory: SimpleStory)
}
