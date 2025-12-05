package cc.unitmesh.devti.vcs.gitignore

/**
 * Factory for creating IgnoreEngine instances.
 * Supports both custom high-performance engines and third-party library engines.
 */
object IgnoreEngineFactory {
    
    /**
     * Enumeration of available engine types.
     */
    enum class EngineType {
        /**
         * Custom high-performance engine with pre-compiled regex patterns and concurrent caching.
         */
        HOMESPUN,
        
        /**
         * Third-party library engine using nl.basjes.gitignore as fallback.
         */
        BASJES
    }
    
    /**
     * Creates an IgnoreEngine instance of the specified type.
     *
     * @param type the type of engine to create
     * @return a new IgnoreEngine instance
     * @throws IllegalArgumentException if the engine type is unknown
     */
    fun createEngine(type: EngineType): IgnoreEngine {
        return when (type) {
            EngineType.HOMESPUN -> HomeSpunIgnoreEngine()
            EngineType.BASJES -> BasjesIgnoreEngine()
        }
    }
    
    /**
     * Creates an IgnoreEngine instance based on a string type name.
     * This is useful for configuration-driven engine selection.
     *
     * @param typeName the name of the engine type (case-insensitive)
     * @return a new IgnoreEngine instance
     * @throws IllegalArgumentException if the engine type name is unknown
     */
    fun createEngine(typeName: String): IgnoreEngine {
        val type = try {
            EngineType.valueOf(typeName.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Unknown engine type: $typeName. Available types: ${EngineType.values().joinToString()}")
        }
        return createEngine(type)
    }
    
    /**
     * Creates an IgnoreEngine with the specified content pre-loaded.
     *
     * @param type the type of engine to create
     * @param gitIgnoreContent the gitignore content to load
     * @return a new IgnoreEngine instance with rules loaded
     */
    fun createEngineWithContent(type: EngineType, gitIgnoreContent: String): IgnoreEngine {
        val engine = createEngine(type)
        engine.loadFromContent(gitIgnoreContent)
        return engine
    }
    
    /**
     * Gets all available engine types.
     *
     * @return array of all available engine types
     */
    fun getAvailableTypes(): Array<EngineType> = EngineType.values()
    
    /**
     * Gets the default engine type.
     * This can be used when no specific type is configured.
     *
     * @return the default engine type
     */
    fun getDefaultType(): EngineType = EngineType.HOMESPUN
}
