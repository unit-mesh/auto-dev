package cc.unitmesh.pycharm.provider

import cc.unitmesh.devti.prompting.model.TestStack
import cc.unitmesh.devti.provider.TechStackProvider

class PythonTechStackService : TechStackProvider() {
    override fun prepareLibrary(): TestStack {
        return TestStack()
    }
}
