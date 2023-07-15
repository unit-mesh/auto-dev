package cc.unitmesh.devti.parser

import junit.framework.TestCase.assertEquals
import org.junit.Test

class JavaCodeProcessorTest {
    @Test
    fun should_filter_called_service_line_inside_controller() {
        val code = """
            package cc.unitmesh.devti.controller;
            
            import cc.unitmesh.devti.service.UserService;
            import cc.unitmesh.devti.service.UserServiceImpl;
            
            public class UserController {
                private UserService userService = new UserServiceImpl();
                
                public void getUser() {
                    userService.getUser();
                }
            }
        """.trimIndent()

        val usedMethodCode = JavaCodeProcessor.findUsageCode(code, "UserService")
        assert(usedMethodCode.size == 1)

        assertEquals("userService.getUser();", usedMethodCode[0])
    }

    @Test
    fun should_filter_called_service_line_with_parameters_inside_controller() {
        val code = """
            package cc.unitmesh.devti.controller;
            
            import cc.unitmesh.devti.service.UserService;
            import cc.unitmesh.devti.service.UserServiceImpl;
            
            public class UserController {
                private UserService userService = new UserServiceImpl();
                
                public void getUser() {
                    userService.getUser("1");
                }
            }
        """.trimIndent()

        val usedMethodCode = JavaCodeProcessor.findUsageCode(code, "UserService")
        assert(usedMethodCode.size == 1)

        assertEquals("userService.getUser(\"1\");", usedMethodCode[0])
    }
}