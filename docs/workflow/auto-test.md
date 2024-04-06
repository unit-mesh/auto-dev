---
layout: default
title: AutoTest
nav_order: 5
parent: Development
---

# AutoTest Design

## Design Principle 

Create rule:

- if test code exists and LLM returns with import syntax, AutoDev will replace all code.
- if test code exists and LLM returns with no import syntax, AutoDev will insert test code after the last import statement.
- if test code does not exist, AutoDev will insert test code.

Run rule:

- if run configuration exists, AutoDev will create a new run configuration.
- if run configuration does not exist, AutoDev will create a new run configuration.

For more, see in [AutoTestService](https://github.com/unit-mesh/auto-dev/blob/master/src/main/kotlin/cc/unitmesh/devti/provider/AutoTestService.kt) 

## Test Prompts

    Write unit test for the following Kotlin code.
    
    You are working on a project that uses Spring MVC, Spring WebFlux to build RESTful APIs.
    - You MUST use should_xx_xx style for test method name, You MUST use given-when-then style.
    - Test file should be complete and compilable, without need for further actions.
    - Ensure that each test focuses on a single use case to maintain clarity and readability.
    - Instead of using `@BeforeEach` methods for setup, include all necessary code initialization within each individual test method, do not write parameterized tests.
    - This project uses JUnit 5, you should import `org.junit.jupiter.api.Test` and use `@Test` annotation.
    - Use appropriate Spring test annotations such as `@MockBean`, `@Autowired`, `@WebMvcTest`, `@DataJpaTest`, `@AutoConfigureTestDatabase`, `@AutoConfigureMockMvc`, `@SpringBootTest` etc.
      Here is a template as example
    ```Kotlin
    // You Must use @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ExtendWith(SpringExtension::class)
    class PluginControllerTest {
    
        private lateinit var mockMvc: MockMvc
    
        @BeforeEach
        fun setup() {
            // You can use MockMvcBuilders.standaloneSetup() to build mockMvc
            mockMvc = MockMvcBuilders.standaloneSetup(PluginController()).build()
        }
    
        @Test
        fun shouldReturnPluginTypes() {
            mockMvc.perform(get("/api/plugin/type"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0]").value("DUBBO"))
        }
    }
    
    ```
    
    - Kotlin API version: 1.9
    
    // here are related classes:
    // 'package: com.thoughtworks.archguard.evolution.domain.BadSmellSuite
    // class BadSmellSuite {
    //   id
    //   suiteName
    //   isDefault
    //   thresholds
    //   
    // }
    // 'package: com.thoughtworks.archguard.evolution.domain.BadSmellSuiteWithSelected
    // class BadSmellSuiteWithSelected {
    //   id
    //   suiteName
    //   isDefault
    //   isSelected
    //   thresholds
    //   
    // }
    // 'package: com.thoughtworks.archguard.evolution.domain.BadSmellThresholdService
    // '@Service
    // class BadSmellThresholdService {
    //   badSmellSuitRepository
    //   + fun getAllSuits(): List<BadSmellSuite>
    //   + fun getBadSmellSuiteWithSelectedInfoBySystemId(systemId: Long): List<BadSmellSuiteWithSelected>
    // }
    
    // here is current class information:
    // 'package: com.thoughtworks.archguard.evolution.controller.EvolutionBadSmellController
    // '@RestController, @RequestMapping("/api/evolution")
    // class EvolutionBadSmellController {
    //   badSmellThresholdService
    //   + @GetMapping("/badsmell-thresholds")     fun getAllThresholds(): List<BadSmellSuite>
    //   + @GetMapping("/badsmell-thresholds/system/{systemId}")     fun getThresholdsBySystemId(@PathVariable("systemId") systemId: Long): List<BadSmellSuiteWithSelected>
    // }
    
    Code:
    // import com.thoughtworks.archguard.evolution.domain.BadSmellSuite
    // import com.thoughtworks.archguard.evolution.domain.BadSmellSuiteWithSelected
    // import com.thoughtworks.archguard.evolution.domain.BadSmellThresholdService
    // import org.springframework.web.bind.annotation.GetMapping
    // import org.springframework.web.bind.annotation.PathVariable
    // import org.springframework.web.bind.annotation.RequestMapping
    // import org.springframework.web.bind.annotation.RestController
    ```kotlin
    @RestController
    @RequestMapping("/api/evolution")
    class EvolutionBadSmellController(val badSmellThresholdService: BadSmellThresholdService) {
    
        @GetMapping("/badsmell-thresholds")
        fun getAllThresholds(): List<BadSmellSuite> {
            return badSmellThresholdService.getAllSuits()
        }
    
        @GetMapping("/badsmell-thresholds/system/{systemId}")
        fun getThresholdsBySystemId(@PathVariable("systemId") systemId: Long): List<BadSmellSuiteWithSelected> {
            return badSmellThresholdService.getBadSmellSuiteWithSelectedInfoBySystemId(systemId)
        }
    }
    ```
    
    Start  with `import` syntax here:

## Resources

### TestSpark

TestSpark 目前支持两种测试生成策略：
- 基于 LLM 的测试生成（使用 OpenAI 和 JetBrains 内部 AI Assistant 平台）
- 基于本地搜索的测试生成（使用 EvoSuite）

#### Method 1: LLM gen prompt example

GitHub: [TestSpark](https://github.com/JetBrains-Research/TestSpark/blob/development/src/main/resources/defaults/TestSpark.properties)

```vtl
Generate unit tests in $LANGUAGE for $NAME to achieve 100% line coverage for this class.
Dont use @Before and @After test methods.
Make tests as atomic as possible.
All tests should be for $TESTING_PLATFORM.
In case of mocking, use $MOCKING_FRAMEWORK. But, do not use mocking for all tests.
Name all methods according to the template - [MethodUnderTest][Scenario]Test, and use only English letters.
The source code of class under test is as follows:
$CODE
Here are some information about other methods and classes used by the class under test. Only use them for creating objects, not your own ideas.
$METHODS
$POLYMORPHISM
$TEST_SAMPLE
```

#### Method 2: EvoSuite gen 

[EvoSuite](https://www.evosuite.org/) 是一个工具，可以自动生成带有 Java 代码编写的类断言的测试用例。

