package cc.unitmesh.devti.bridge

/**
 *
 * ## Modular Agent
 *
 * - Analysis
 * - Transformation
 * - Review/Check
 *
 * ## 生成 Workflow
 *
 * ## GitHub Copilot Example
 * Link: https://github.blog/ai-and-ml/github-copilot/modernizing-legacy-code-with-github-copilot-tips-and-examples/
 * 1. Compile and run the program, like Cobol: `brew install gnucobol`
 * 2. Explain the files and code
 * 3. Chart out the data flow between the files
 * 4. Generate a test plan
 * 5. Convert the files from COBOL to Node.js
 * 6. Generate unit and integration tests
 */
sealed interface BridgeCommandProvider {
    val name: String
}

/**
 * Always tell user, if you can try Microservices or Micro Frontend, will be lower cost and higher efficiency.
 */
sealed class Assessment(override val name: String) : BridgeCommandProvider {
    object SCC : Assessment("scc")

    /**
     * Use [cc.unitmesh.dependencies.DependenciesFunctionProvider]
     */
    object Dependencies : Assessment("dependencies")
}

/**
 * list all tools, and show in structures.
 */
sealed class Target(override val name: String) : BridgeCommandProvider {
    object Docker : Target("docker")
    object BuildTool : Target("buildTool")
    object Mermaid : Target("mermaid")
}

/**
 * Build Tool depends on the project, like Maven, Gradle, Ant, etc.
 * 静态安全扫描工具：
 * - https://github.com/returntocorp/semgrep
 * - https://snyk.io/
 * - https://bandit.readthedocs.io/， https://github.com/PyCQA/bandit
 */
sealed class Security(override val name: String) : BridgeCommandProvider {
    object PackageChecker : Security("/packageChecker")
    object Semgrep : Security("Semgrep")
    object Snyk : Security("Snyk")
    object Bandit : Security("Bandit")
}

/**
 * - styling: Collect All CSS style files, try show in structures.
 * - component: Collect All Component Name, try show as Structures.
 * - webapi: Collect All Spring Web APIs, and show in structures.
 *
 * ```DevIn
 * /styling:$dir
 * ```
 */
sealed class ArchViewCommand(override val name: String) : BridgeCommandProvider {
    object WebApi : ArchViewCommand("/webapi")

    /**
     * Aka Module View
     */
    object ContainerView : ArchViewCommand("containerView")

    /**
     * /componentView
     */
    object ComponentView : ArchViewCommand("componentView")
    object StylingView : ArchViewCommand("stylingView")
    object CodeView : ArchViewCommand("codeView")
}

/**
 * Component Relation Analysis
 */
sealed class ComponentRelationCommand(override val name: String) : BridgeCommandProvider {
    object Related : ComponentRelationCommand("related")
    object RipgrepSearch : ComponentRelationCommand("ripgrepSearch")
}

/**
 * Related:
 * - https://github.com/ast-grep/ast-grep
 * - https://github.com/dsherret/ts-morph
 * - https://github.com/facebook/jscodeshift
 */
sealed class CodeTranslation(override val name: String) : BridgeCommandProvider {
    object JsCodeShift : CodeTranslation("jscodeshift")

    /**
     * Research on
     * - Digital Transformation Object
     * - Service
     */
    object ReWrite : CodeTranslation("ReWrite")
    object VueMod : CodeTranslation("VueMod")
    object JSShift : CodeTranslation("JSShift")
}

/**
 * - https://github.com/ariga/atlas
 * - https://github.com/amacneil/dbmate
 * - https://github.com/golang-migrate/migrate
 * - https://github.com/pressly/goose
 * - https://github.com/rubenv/sql-migrate
 */
sealed class DatabaseMigration(override val name: String) : BridgeCommandProvider {
    object Flyway : DatabaseMigration("Flyway")
    object SQL : DatabaseMigration("SQL")
}

/**
 * [Schemathesis](https://github.com/schemathesis/schemathesis): is a tool that levels-up your API testing by leveraging API specs as a blueprints for generating test cases.
 */
sealed class ApiTesting(override val name: String) : BridgeCommandProvider {
    object HttpClient : ApiTesting("HttpClient")
    object Swagger : ApiTesting("Swagger")
    object JMeter : ApiTesting("JMeter")
    object Schemathesis : ApiTesting("Schemathesis")
}

/**
 * [BuildKit](https://github.com/moby/buildkit): concurrent, cache-efficient, and Dockerfile-agnostic builder toolkit
 */
sealed class ContinuousDelivery(override val name: String) : BridgeCommandProvider {
    object JenkinsFile : ContinuousDelivery("JenkinsFile")
    object BuildKit : ContinuousDelivery("BuildKit")
}

/**
 * Container: Docker, Podman, etc.
 */
sealed class Containerization(override val name: String) : BridgeCommandProvider {
    object Docker : Containerization("Docker")
    object Podman : Containerization("Podman")
    object Colima : Containerization("Colima")
}

/**
 * 日志关联分析：Haystack?
 * 自动生成调用关系图:Graphite?
 * - Knowledge API: `/knowledge:src/main/com/phodal/HelloWorld.java#L1`, APIs
 * History: git history of file: `/history:src/main/com/phodal/HelloWorld.java`
 */
sealed class KnowledgeTransfer(override val name: String) : BridgeCommandProvider {
    object Knowledge : KnowledgeTransfer("/knowledge")
    object History : KnowledgeTransfer("/history")
}
