package cc.unitmesh.devins.demo

/**
 * DevIns 语言解析器演示主函数
 */
fun main() {
    println("DevIns Language Parser for Kotlin Multiplatform")
    println("===============================================")
    
    try {
        DevInsDemo.runAllDemos()
    } catch (e: Exception) {
        println("Demo failed with error: ${e.message}")
        e.printStackTrace()
    }
    
    println("\n===============================================")
    println("Demo completed!")
}
