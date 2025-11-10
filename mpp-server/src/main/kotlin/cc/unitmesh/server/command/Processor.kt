package cc.unitmesh.server.command

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

object Processor {
    private val log = LoggerFactory.getLogger(Processor::class.java)

    fun executeWithLogs(pb: ProcessBuilder, workspace: File, logStream: StreamConsumer): Int {
        return executeWithLogsAndAppendToFile(pb, workspace, null, logStream)
    }

    fun executeWithLogsAndAppendToFile(
        pb: ProcessBuilder,
        workspace: File,
        report: File?,
        consumer: StreamConsumer,
    ): Int {
        pb.redirectErrorStream(true)
        pb.directory(workspace)
        if (report != null) {
            report.createNewFile()
            pb.redirectOutput(report)
        }
        val p = pb.start()
        val inputStream: InputStream = p.inputStream
        val bufferedReader = BufferedReader(InputStreamReader(inputStream, "gbk"))
        while (true) {
            val line = bufferedReader.readLine() ?: break
            log.info(line)
            consumer.consumeLine(line)
        }

        inputStream.close()
        p.waitFor()
        return p.exitValue()
    }
}
