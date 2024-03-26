---
layout: default
title: Custom Test Prompt Template
parent: Customize Features
nav_order: 15
---

Create template files under `prompts/templates` directory, like:

- Java language: `ControllerTest.java`, `ServiceTest.java`, `Test.java`
- Kotlin language: `ControllerTest.kt`, `ServiceTest.kt`, `Test.kt`

when generate test file, will use these templates.

For example:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
class /* Here some be {ControllerName} */ ControllerTest {

    private lateinit var mockMvc: MockMvc

    // other some mock beans

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(/* {ControllerName} */Controller(/* some mock beans */)).build()
    }

    // the test methods
}
```
