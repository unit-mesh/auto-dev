---
layout: default
title: AutoTest Design
nav_order: 2
parent: Development
---

# AutoTest Design

## Basic Rule

- if test code exists and LLM returns with import syntax, AutoDev will replace all code.
- if test code exists and LLM returns with no import syntax, AutoDev will insert test code after the last import statement.
- if test code does not exist, AutoDev will insert test code.

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
