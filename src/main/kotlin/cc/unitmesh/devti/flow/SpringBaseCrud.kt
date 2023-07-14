package cc.unitmesh.devti.flow

import cc.unitmesh.devti.analysis.DtClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile

// TODO: remove this interface
interface SpringBaseCrud {
    fun controllerList(): List<DtClass>
    fun entityList(): List<DtClass>
    fun serviceList(): List<DtClass>
    /**
     * return all entity class + dto class
     */
    fun modelList(): List<DtClass>
    fun getAllControllerFiles(): List<PsiFile>
    fun getAllEntityFiles(): List<PsiFile>
    fun getAllDtoFiles(): List<PsiFile>
    fun getAllServiceFiles(): List<PsiFile>


    fun createControllerOrUpdateMethod(targetController: String, code: String, isControllerExist: Boolean)
    fun createController(endpoint: String, code: String): DtClass?
    fun createEntity(code: String): DtClass?
    fun createService(code: String): DtClass?
    fun createDto(code: String): DtClass?
    fun createClass(code: String, packageName: String?): DtClass?

    fun dtoFilter(clazz: PsiClass): Boolean {
        val className = clazz.name?.lowercase()

        // endsWith dto, request, response
        return (className?.endsWith("dto") == true ||
                className?.endsWith("request") == true
                || className?.endsWith("response") == true)
    }

    fun controllerFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "org.springframework.stereotype.Controller" ||
                    it == "org.springframework.web.bind.annotation.RestController"
        }

    fun serviceFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "org.springframework.stereotype.Service"
        }

    fun entityFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "javax.persistence.Entity"
        }

    fun isController(code: String): Boolean {
        if (code.contains("@Controller")) {
            return true
        }

        if (code.contains("import org.springframework.stereotype.Controller")) {
            return true
        }

        // regex to match `public class xxController`
        val regex = Regex("public\\s+class\\s+\\w+Controller")
        return regex.containsMatchIn(code)
    }

    fun isService(code: String): Boolean {
        if (code.contains("@Service")) {
            return true
        }

        if (code.contains("import org.springframework.stereotype.Service")) {
            return true
        }

        // regex to match `public class xxService`
        val regex = Regex("public\\s+class\\s+\\w+Service")
        return regex.containsMatchIn(code)
    }

    fun isEntity(code: String): Boolean {
        if (code.contains("@Entity")) {
            return true
        }

        if (code.contains("import javax.persistence.Entity")) {
            return true
        }

        // regex to match `public class xxEntity`
        val regex = Regex("public\\s+class\\s+\\w+Entity")
        return regex.containsMatchIn(code)
    }

    fun isDto(code: String): Boolean {
        if (code.contains("import lombok.Data")) {
            return true
        }

        // regex to match `public class xxDto`
        val regex = Regex("public\\s+class\\s+\\w+(Dto|DTO|Request|Response|Res|Req)")
        return regex.containsMatchIn(code)
    }

}
