package cc.unitmesh.idea.context.library

data class SpringDataLibraryDescriptor(val shortText: String, val coords: List<String>)
data class LibraryDescriptor(val shortText: String, val coords: String)

object SpringLibrary {
    // Spring
    private val SPRING_MVC_MAVEN = "org.springframework:spring-webmvc"
    private val SPRING_WEBFLUX_MAVEN = "org.springframework:spring-webflux"

    // Spring Data
    private val REACTOR_MAVEN = "io.projectreactor:reactor-core"
    private val MONGO_REACTIVE_STREAMS_MAVEN = "org.mongodb:mongodb-driver-reactivestreams"
    private val SPRING_DATA_COMMONS_MAVEN = "org.springframework.data:spring-data-commons"
    private val JPA_MAVEN = "org.springframework.data:spring-data-jpa"
    private val CASSANDRA_MAVEN = "org.springframework.data:spring-data-cassandra"
    private val COUCHBASE_MAVEN = "org.springframework.data:spring-data-couchbase"
    private val JDBC_MAVEN = "org.springframework.data:spring-data-jdbc"
    private val MONGO_MAVEN = "org.springframework.data:spring-data-mongodb"
    private val NEO4J_MAVEN = "org.springframework.data:spring-data-neo4j"
    private val R2DBC_MAVEN = "org.springframework.data:spring-data-r2dbc"
    private val REDIS_MAVEN = "org.springframework.data:spring-data-redis"

    val SPRING_DATA = listOf(
        SpringDataLibraryDescriptor("JPA ", listOf(JPA_MAVEN)),
        SpringDataLibraryDescriptor("CASSANDRA", listOf(CASSANDRA_MAVEN)),
        SpringDataLibraryDescriptor("REACTIVE CASSANDRA", listOf(CASSANDRA_MAVEN, REACTOR_MAVEN)),
        SpringDataLibraryDescriptor("COUCHBASE", listOf(COUCHBASE_MAVEN)),
        SpringDataLibraryDescriptor("REACTIVE COUCHBASE", listOf(COUCHBASE_MAVEN, REACTOR_MAVEN)),
        SpringDataLibraryDescriptor("JDBC", listOf(JDBC_MAVEN)),
        SpringDataLibraryDescriptor("MONGO", listOf(MONGO_MAVEN)),
        SpringDataLibraryDescriptor(
            "REACTIVE MONGO",
            listOf(MONGO_MAVEN, REACTOR_MAVEN, MONGO_REACTIVE_STREAMS_MAVEN)
        ),
        SpringDataLibraryDescriptor("NEO4J", listOf(NEO4J_MAVEN)),
        SpringDataLibraryDescriptor("R2DBC", listOf(R2DBC_MAVEN)),
        SpringDataLibraryDescriptor("REDIS", listOf(REDIS_MAVEN))
    )

    fun canApplySpringData(libName: String) = libName == SPRING_DATA_COMMONS_MAVEN

    val SPRING_MVC = listOf(
        LibraryDescriptor("Spring MVC", SPRING_MVC_MAVEN),
        LibraryDescriptor("Spring WebFlux", SPRING_WEBFLUX_MAVEN)
    )

    fun canApplySpringMvc(libName: String) = libName == SPRING_MVC_MAVEN || libName == SPRING_WEBFLUX_MAVEN
}