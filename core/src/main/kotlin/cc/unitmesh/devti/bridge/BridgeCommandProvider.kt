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
enum class BridgeCommandProvider(vararg tools: String) {

    /**
     * Always tell user, if you can try Microservices or Micro Frontend, will be lower cost and higher efficiency.
     */
    ASSESSMENT("SCC", "CLOC", "/dependencies"),

    /**
     * list all tools, and show in structures.
     */
    TARGET("Docker", "/buildTool", "/mermaid"),

    /**
     * Build Tool depends on the project, like Maven, Gradle, Ant, etc.
     */
    PACKAGE_CHECKER("/packageChecker"),

    /**
     * - styling: Collect All CSS style files, try show in structures.
     * - component: Collect All Component Name, try show as Structures.
     * - webapi: Collect All Spring Web APIs, and show in structures.
     *
     * ```DevIn
     * /styling:$dir
     * ```
     */
    MODULAR_ANALYSIS("/styling", "/component", "/webapi", "/structure"),

    /**
     *
     */
    COMPONENT_ANALYSIS("/related", "/ripgrepSearch"),

    /**
     * - https://github.com/ast-grep/ast-grep
     * - https://github.com/dsherret/ts-morph
     * - https://github.com/facebook/jscodeshift
     */
    CODE_TRANSLATION("jscodeshift", "ReWrite", "VueMod", "JSShift"),

    /**
     * - https://github.com/ariga/atlas
     * - https://github.com/amacneil/dbmate
     * - https://github.com/golang-migrate/migrate
     * - https://github.com/pressly/goose
     * - https://github.com/rubenv/sql-migrate
     */
    DATABASE_MIGRATION("Flyway", "SQL"),

    /**
     * [Schemathesis](https://github.com/schemathesis/schemathesis): is a tool that levels-up your API testing by leveraging API specs as a blueprints for generating test cases.
     */
    API_TESTING("HttpClient", "Swagger", "JMeter", "Schemathesis"),

    /**
     * [BuildKit](https://github.com/moby/buildkit): concurrent, cache-efficient, and Dockerfile-agnostic builder toolkit
     */
    CONTINUES_DELIVERY("JenkinsFile", "BuildKit"),

    /**
     * 静态安全扫描工具：
     * - https://github.com/returntocorp/semgrep
     * - https://snyk.io/
     * - https://bandit.readthedocs.io/， https://github.com/PyCQA/bandit
     */
    SECURITY_ANALYSIS("Semgrep", "Snyk", "Bandit"),

    /**
     * Container: Docker, Podman, etc.
     */
    CONTAINERIZATION("Docker"),

    /**
     * 日志关联分析：Haystack?
     * 自动生成调用关系图:Graphite?
     * - Knowledge API: `/knowledge:src/main/com/phodal/HelloWorld.java#L1`, APIs
     * History: git history of file: `/history:src/main/com/phodal/HelloWorld.java`
     */
    KNOWLEDGE_TRANSFER("/knowledge", "/history")
    ;
}