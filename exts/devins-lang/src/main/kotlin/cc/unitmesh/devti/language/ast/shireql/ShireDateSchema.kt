package cc.unitmesh.shirelang.compiler.execute.shireql

import cc.unitmesh.devti.language.ast.shireql.ShireQLSchema
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.util.*
import java.time.format.DateTimeFormatter

class ShireDateSchema : ShireQLSchema {
    private val date: Instant = Clock.System.now()

    fun now(): Long {
        return date.toEpochMilliseconds()
    }

    fun dayOfWeek(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    }

    fun year(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    fun month(): Int {
        return Calendar.getInstance().get(Calendar.MONTH) + 1
    }

    fun dayOfMonth(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }

    /**
     * Formats the date using the specified output format.
     *
     * ```shire
     * format("yyyy-MM-dd HH:mm:ss")
     * ```
     */
    fun format(outputFormat: String) : String {
        val localDateTime = date.toLocalDateTime(TimeZone.currentSystemDefault())
        val formatter = DateTimeFormatter.ofPattern(outputFormat)
        return localDateTime.toJavaLocalDateTime().format(formatter)
    }

    override fun toString(): String {
        return "ShireDate(date=$date)"
    }
}