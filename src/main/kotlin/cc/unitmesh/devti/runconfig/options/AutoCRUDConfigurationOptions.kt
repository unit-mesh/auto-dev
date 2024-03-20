package cc.unitmesh.devti.runconfig.options

import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class AutoCRUDConfigurationOptions : ModuleBasedConfigurationOptions() {
    private val githubRepo: StoredProperty<String?> = string("unit-mesh/untitled").provideDelegate(this, "githubRepo")
    private val storyId: StoredProperty<String?> = string("1").provideDelegate(this, "storyId")

    fun githubRepo(): String = githubRepo.getValue(this) ?: ""
    fun setGithubRepo(repo: String) {
        githubRepo.setValue(this, repo)
    }

    fun storyId(): String = storyId.getValue(this) ?: ""
    fun setStoryId(id: String) {
        storyId.setValue(this, id)
    }
}
