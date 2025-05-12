package cc.unitmesh.devti.language.ast.action

/**
 * `PatternActionFuncType` was for show documentation when user hovers on the function.
 */
enum class PatternActionFuncDef(val funcName: String, val description: String, val example: String) {
    GREP(
        "grep", "`grep` function searches any given input files, selecting lines that match one or more patterns.",
        """
        | ```devin
        | ---
        | variables:
        |   "controllers": /.*.java/ { cat | grep("class\s+([a-zA-Z]*Controller)")  }
        | ---
        | ```
        """.trimMargin()
    ),
    FIND(
        "find", "`find` will find the first occurrence of a pattern in a file.",
        """
        | ```devin
        | ---
        | variables:
        |   "story": /any/ { find("epic") }
        | ---
        | ```
        """.trimMargin()
    ),
    SED(
        "sed", "`sed` will replace text in a file.",
        """ 
        | ```devin
        | ---
        | variables:
        |   "var2": /.*ple.devin/ { cat | find("openai") | sed("(?i)\b(sk-[a-zA-Z0-9]{20}T3BlbkFJ[a-zA-Z0-9]{20})(?:['|\"|\n|\r|\s|\x60|;]|${'$'})", "sk-***") }
        | ---
        | ```
    """.trimMargin()
    ),
    PRINT(
        "print", "`print` will print text or last output.",
        """ 
        | ```devin
        | ---
        | variables:
        |   "story": /BlogController\.java/ { print }
        | ---
        | ```
        | 
        | Text content Example:
        | 
        | ```devin
        | ---
        | variables:
        |   "story": /any/ { print("hello world") }
        | ---  
        | ```
    """.trimMargin()
    ),
    CAT(
        "cat", "`cat` will concatenate one or more files.",
        """
        | ```devin
        | ---
        | variables:
        |   "story": /BlogController\.java/ { cat } // Paths can be absolute or relative to the current working directory.
        | ---
        | ```
        | 
        | File path Example:
        | 
        | ```devin
        | ---
        | variables:
        |   "story": /any/ { cat("file.txt") }
        | ---  
        | ```
    """.trimMargin()
    ),
    EXECUTE(
        "execute", "`execute` will execute a new script，like `shell`, `bash`, `python`, `ruby` and `javascript`.",
        """
        | ```devin
        | ---
        | name: "Search"
        | afterStreaming: { execute("search.devin") }
        | ---
        | ```
        | 
    """.trimMargin()
    ),
    APPROVAL_EXECUTE(
        "approvalExecute", "`approvalExecute` will show a dialog to confirm is execute to next job.",
        """
        | ```devin
        | ---
        | name: "Search"
        | afterStreaming: { approvalExecute("search.devin") }
        | ---
        | ```
        | 
    """.trimMargin()
    ),
    NOTIFY(
        "notify", "`notify` will use IDEA's notification system to display a message.",
        """
        | ```devin
        | ---
        | name: "Search"
        | afterStreaming: { notify("Failed to Generate JSON") }
        | ---
        | ```
        | 
    """.trimMargin()
    ),
    CASE_MATCH("switch", "TODO, not implemented yet.", ""),
    SPLITTING(
        "splitting", "`splitting` (RAG function) will split the file into chunks.",
        """
        | ```devin
        | ---
        | variables:
        |   "testTemplate": /.*.kt/ { caching("disk") | splitting | embedding }
        | ---
        | ```
        | 
        | Support format: code, txt, pdf, html, doc, xls, ppt, md.
        """.trimMargin()
    ),
    EMBEDDING(
        "embedding", "`embedding` (RAG function) will embedding text.",
        """
        | ```devin
        | ---
        | variables:
        |  "testTemplate": /.*.kt/ { caching("disk") | splitting | embedding }
        | ---
        | ```
    """.trimMargin()
    ),
    SEARCHING(
        "searching", " `searching` (RAG function) will search embedding text.",
        """
        | ```devin
        | ---
        | variables:
        |   "story": /any/ { caching("disk") | splitting | embedding | searching("epic") }
        | ---
        | ```
        | with threshold:
        |
        | ```devin
        | ---
        | variables:
        |  "story": /any/ { caching("disk") | splitting | embedding | searching("epic", 0.5) }
        | ---
        | ```
        | 
        """.trimMargin()
    ),
    CACHING(
        "caching",
        """`caching` (RAG function) will cache the semantic. support "disk" and "memory", default is "memory".""",
        """
        | ```devin
        | ---
        | variables:
        |  "testTemplate": /.*.kt/ { caching("disk") }
        |  "story": /any/ { caching("memory") }
        | ---
        | ```
        |  
    """.trimMargin()
    ),
    RERANKING(
        "reranking",
        "`reranking` (RAG function) will rerank the result. current only support \"Lost In Middle\" pattern",
        """
        | ```devin
        | ---
        | variables:
        |    "story": /any/ {  caching("disk") | splitting | embedding | reranking }
        | ---
        | ```
        | 
    """.trimMargin()
    ),
    CRAWL(
        "crawl", "`crawl` will crawl a list of urls, get markdown from html and save it to a file.",
        """
        | ```devin
        | ---
        | variables:
        |   "websites": /*\.md/ { capture("docs/crawlSample.md", "link") | crawl() | thread("summary.devin") }
        |   "confluence": { thread("confluence.bash", param1, param2) }
        |   "pythonNode.js": { thread("python.py", param1, param2) }  
        | ---
        | ```
        | 
    """.trimMargin()
    ),
    CAPTURE(
        "capture", "`capture` function used to capture url link by NodeType, support Markdown only for now.",
        """
        | ```devin
        | ---
        | variables:
        |   "websites": /*\.md/ { capture("docs/crawlSample.md", "link") | crawl() | thread("summary.devin") }
        |   "confluence": { thread("confluence.bash", param1, param2) }
        |   "pythonNode.js": { thread("python.py", param1, param2) }  
        | ---
        | ```
        | 
    """.trimMargin()
    ),
    THREAD(
        "thread", "`thread` will run the function in a new thread",
        """ 
        | ```devin
        | ---
        | variables:
        |   "story": /any/ { thread(".devin/shell/dify-epic-story.curl.sh") | jsonpath("${'$'}.answer", true) }
        | ---
        | ```
        | 
        | With devin file Example:
        | 
        | ```devin
        | ---
        | variables:
        |  "story": /any/ { thread("dify-epic-story.devin") }
        | ---
        | ```  
        """.trimMargin()
    ),
    JSONPATH(
        "jsonpath", "`jsonpath` function will parse the json and get the value by jsonpath.",
        """
        | ```devin
        | ---
        | variables:
        |   "api": /sampl.sh/ { thread(".devin/toolchain/bigmodel.curl.sh") | jsonpath("${'$'}.choices[0].message.content") }
        | ---
        | ```
        | 
        | With SSE Example:
        | ```devin
        | ---
        | variables:
        |   "story": /any/ { thread(".devin/shell/dify-epic-story.curl.sh") | jsonpath("${'$'}.answer", true) }
        | ---
        | ```
        | 
        | """.trimMargin()
    ),
    SORT("sort", "`sort` will sorting with one or more arguments.", ""),
    UNIQ("uniq", "`uniq` will removing duplicates based on one or more arguments.", ""),
    HEAD(
        "head", "`head` will retrieving the first few lines.",
        """
        | ```devin
        | ---
        | variables:
        |   "controllers": /.*.java/ { find("Controller") | grep("src/main/java/.*") | head(1)  | cat }
        | ---
        | ```
        """.trimMargin()
    ),
    TAIL("tail", "`tail` will retrieving the last few lines.", ""),
    XARGS("xargs", "`xargs` will processing one or more variables.", ""),
    FROM("from", "`select` (ShireQL) will selecting one or more elements.", ""),
    WHERE("where", "`where` (ShireQL) will filtering elements.", ""),
    SELECT("select", "`select` (ShireQL) will select element.", ""),
    BATCH(
        "batch", "`batch` will execute a batch Shire script.",
        """
        | ```devin
        | ---
        | name: "Generate Swagger Doc"
        | variables:
        |   "controllers": /BlogController.java/ { cat }
        |   "gen-swagger": /any/ { batch("controller-with-swagger.devin", ${"$"}controllers, 2) }
        | ---
        | 
        | ```
        """.trimMargin()
    ),

    DESTROY("destroy", "Destroy the current task.", ""),

    TOOLCHAIN_FUNCTION(
        "toolchain",
        "Toolchain functions are defined by the different IDEA plugins. Supports JetBrains' plugin: Database, Shell Script plugin etc, Community plugin: SonarLint etc.",
        """
        | 
        | 
        | Database Plugin Example:
        | 
        | ```devin
        | ---
        | variables:
        |   "relatedTableInfo": /./ { column("user", "post", "tag") }
        | ---
        | ```
        | 
        | For more goto: [https://devin.phodal.com/devin/devin-toolchain-function](https://devin.phodal.com/devin/devin-toolchain-function)
        """.trimMargin()
    ),

    TOKENIZER(
        "tokenizer",
        "`tokenizer` tokenizes text using traditional NLP methods. Support type for : " + "`word`, `naming`, `stopwords`，`jieba`.",
        """
        | ```devin
        | ---
        | variables:
        |   "controllers": /.*.java/ { cat }
        |   "tokens": /any/ { tokenizer(${'$'}controllers, "word") }
        | ---
        | ```
        | 
        | Output Example: package, com, phodal, devinlang, controller, import, org, springframework, web, bind, ...
        | 
        """.trimMargin()
    ),

    LINE_NO(
        "lineNo",
        "`lineNo` will add line number to the text.",
        """
        | ```devin
        | ---
        | variables:
        |   "controllers": /.*.java/ { cat | lineNo }
        | ---
        | ```
        | 
        | Output Example: 1: package cc.unitmesh.devinlang.controller; 2: import org.springframework.web.bind.annotation.GetMapping; 3: import org.springframework.web.bind.annotation.RestController; 4: import org.springframework.web.bind.annotation.RequestMapping; 5: import org.springframework.web.bind.annotation.RequestParam;
        | 
        """.trimMargin()
    )
    ;

    override fun toString(): String = description
}