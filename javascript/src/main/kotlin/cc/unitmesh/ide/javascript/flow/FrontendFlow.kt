package cc.unitmesh.ide.javascript.flow

import cc.unitmesh.devti.flow.TaskFlow

/**
 *
 * 1. Finding Function bootstrap, like the main function in java, ReactDom.render in React
 * 2. IO Handing
 * 3. Transform data, like State in React, Vuex in Vue
 * 4. Processing calling, like the fetch in React, the axios in Vue
 * 5. Output Transform, like the render in React, the template in Vue
 */
interface FrontendFlow : TaskFlow<String> {
    var userTask: String

    /**
     * Get all routes in the project, including the routes in the submodules
     * @return list of routes
     */
    fun getRoutes(): List<String>

    /**
     * Get all components in the project, based on the naming convention, like the PascalCase under `src/components`
     * @return list of components
     */
    fun getComponents(): List<DsComponent>

    /**
     * Get the design system components, like the Ant Design in React, the Element in Vue
     * @return list of design system components
     */
    fun getDesignSystemComponents(): List<DsComponent>

    /**
     * Get remote call as a sample, like the axios in Vue, the fetch in React
     * @return list of services
     */
    fun sampleRemoteCall(): String

    /**
     * Get the state management, like the Vuex in Vue, the Redux in React, maybe Empty
     * @return list of state management
     */
    fun sampleStateManagement(): String?
}
