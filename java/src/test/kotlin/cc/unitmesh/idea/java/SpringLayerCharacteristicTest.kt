package cc.unitmesh.idea.java

import cc.unitmesh.idea.spring.SpringLayerCharacteristic
import org.junit.Test

class SpringLayerCharacteristicTest {

    @Test
    fun should_return_true_when_spring_controller() {
        val code = """
            package cc.unitmesh.devti.flow;
            
            import org.springframework.stereotype.Controller;
            import org.springframework.web.bind.annotation.RestController;
            
            @Controller
            public class UserController {
            }
        """.trimIndent()
        val result = SpringLayerCharacteristic.check(code, "controller")
        assert(result)
    }

    @Test
    fun should_return_true_when_is_a_mvc_service() {
        val serviceCode = """
            @Service
            public class HelloWorldService {
            }
        """.trimIndent()

        val result = SpringLayerCharacteristic.check(serviceCode, "service")
        assert(result)
    }

    @Test
    fun should_return_true_when_given_a_dto_code() {
        val dtoCode = """
            @Data
            public class UserDto {
            }
        """.trimIndent()

        val result = SpringLayerCharacteristic.check(dtoCode, "dto")
        assert(result)
    }

    @Test
    fun should_return_true_when_given_a_repository_code() {
        val repositoryCode = """
            @Repository
            public class UserRepository {
            }
        """.trimIndent()

        val result = SpringLayerCharacteristic.check(repositoryCode, "repository")
        assert(result)
    }
}