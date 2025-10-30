package cc.unitmesh.devti.language.ast.shireql

import java.time.ZoneId
import java.util.*
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class ShireDateSchema : ShireQLSchema {
    @OptIn(ExperimentalTime::class)
    private val date: Instant = Clock.System.now()

    @OptIn(ExperimentalTime::class)
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
    @OptIn(ExperimentalTime::class)
    fun format(outputFormat: String) : String {
        val localDateTime: java.time.Instant = date.toJavaInstant()
        val formatter = DateTimeFormatter.ofPattern(outputFormat)
        return localDateTime.atZone(ZoneId.systemDefault()).format(formatter)
    }

    @OptIn(ExperimentalTime::class)
    override fun toString(): String = "ShireDate(date=$date)"
}