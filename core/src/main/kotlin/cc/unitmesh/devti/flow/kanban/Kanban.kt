package cc.unitmesh.devti.flow.kanban

import cc.unitmesh.devti.flow.model.SimpleStory

interface Kanban {
    /**
     * Retrieves a user story by its ID.
     *
     * @param storyId The ID of the user story to retrieve.
     * @return The user story with the specified ID.
     */
    fun getStoryById(storyId: String): SimpleStory
}
