package cc.unitmesh.devti.analysis

import cc.unitmesh.devti.analysis.DtClass.Companion.fromPsi
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementFactory
import com.intellij.testFramework.LightPlatformTestCase

class DtClassTest : LightPlatformTestCase() {
    private val javaFactory: PsiElementFactory get() = JavaPsiFacade.getElementFactory(project)

    fun testShould_fetch_java_endpoints() {
        val originCode = """
    BlogService blogService;

    public HelloController(BlogService blogService) {
        this.blogService = blogService;
    }

    public String hello() {
        return "Greetings from Spring Boot!";
    }
            """.trimIndent()
        val psiClass = javaFactory.createClassFromText(originCode, null)
        psiClass.setName("HelloController")
        psiClass.addBefore(javaFactory.createAnnotationFromText("@Controller", null), psiClass.firstChild)

        val dtClass = DtClass.fromPsi(psiClass)
        assertEquals(
            dtClass.format(), """// package: HelloController
// class HelloController {
//   blogService: BlogService
//   + hello(): String
// ' some getters and setters
// }
"""
        )
    }

    fun testShould_fetch_java_endpoints_with_getter_setter() {
        val originCode = """
private String roomId;
private String startTime;
private String endTime;
private List<String> attendees;

public BookingDTO(String roomId, String startTime, String endTime, List<String> attendees) {
    this.roomId = roomId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.attendees = attendees;
}

public String getRoomId() {
    return roomId;
}

public void setRoomId(String roomId) {
    this.roomId = roomId;
}

public String getStartTime() {
    return startTime;
}

public void setStartTime(String startTime) {
    this.startTime = startTime;
}

public String getEndTime() {
    return endTime;
}

public void setEndTime(String endTime) {
    this.endTime = endTime;
}

public List<String> getAttendees() {
    return attendees;
}

public void setAttendees(List<String> attendees) {
    this.attendees = attendees;
}
"""
        val psiClass = javaFactory.createClassFromText(originCode, null)
        psiClass.setName("BookingDTO")

        val dtClass = DtClass.fromPsi(psiClass)
        assertEquals(
            dtClass.format(), """// package: BookingDTO
// class BookingDTO {
//   roomId: String
//   startTime: String
//   endTime: String
//   attendees: List<String>
// ' some getters and setters
// }
"""
        )
    }

}
