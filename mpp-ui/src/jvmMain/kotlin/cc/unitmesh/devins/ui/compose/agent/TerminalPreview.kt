package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.terminal.AnsiTerminalRenderer
import cc.unitmesh.devins.ui.compose.terminal.LiveTerminalRenderer
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme

/**
 * Preview for basic ANSI color codes.
 */
@Preview
@Composable
fun TerminalColorPreview() {
    AutoDevTheme(darkTheme = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Basic ANSI Colors", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val colorTest = """
                    |${'\u001B'}[31mRed Text${'\u001B'}[0m
                    |${'\u001B'}[32mGreen Text${'\u001B'}[0m
                    |${'\u001B'}[33mYellow Text${'\u001B'}[0m
                    |${'\u001B'}[34mBlue Text${'\u001B'}[0m
                    |${'\u001B'}[35mMagenta Text${'\u001B'}[0m
                    |${'\u001B'}[36mCyan Text${'\u001B'}[0m
                    |${'\u001B'}[37mWhite Text${'\u001B'}[0m
                    |${'\u001B'}[1mBold Text${'\u001B'}[0m
                    |${'\u001B'}[3mItalic Text${'\u001B'}[0m
                    |${'\u001B'}[4mUnderlined Text${'\u001B'}[0m
                    |${'\u001B'}[1;31mBold Red${'\u001B'}[0m
                    |${'\u001B'}[4;32mUnderlined Green${'\u001B'}[0m
                    |${'\u001B'}[41mRed Background${'\u001B'}[0m
                    |${'\u001B'}[42;30mGreen BG, Black Text${'\u001B'}[0m
                """.trimMargin()

                AnsiTerminalRenderer(
                    ansiText = colorTest,
                    maxHeight = 400
                )
            }
        }
    }
}

/**
 * Preview for cursor movement and line manipulation.
 */
@Preview
@Composable
fun TerminalCursorPreview() {
    AutoDevTheme(darkTheme = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cursor Movement & Line Manipulation", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Simulate progress bar that updates in place
                val progressTest = """
                    |Loading...
                    |[          ] 0%${'\u001B'}[1A${'\u001B'}[0K
                    |[==        ] 20%${'\u001B'}[1A${'\u001B'}[0K
                    |[====      ] 40%${'\u001B'}[1A${'\u001B'}[0K
                    |[======    ] 60%${'\u001B'}[1A${'\u001B'}[0K
                    |[========  ] 80%${'\u001B'}[1A${'\u001B'}[0K
                    |[==========] 100%
                    |Complete!
                """.trimMargin()

                AnsiTerminalRenderer(
                    ansiText = progressTest,
                    maxHeight = 200
                )
            }
        }
    }
}

/**
 * Preview for the Gradle build output example.
 */
@Preview
@Composable
fun TerminalGradleBuildPreview() {
    AutoDevTheme(darkTheme = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gradle Build Output", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val gradleOutput = """
Starting a Gradle Daemon (subsequent builds will be faster)


[2A[1B[1m> Starting Daemon[m[17D[1B[2A[1m<[0;1m-------------> 0% INITIALIZING [44ms][m[38D[2B[2A[1m<[0;1m-------------> 0% INITIALIZING [248ms][m[39D[1B[1m> Evaluating settings[m[21D[1B[2A[1m<[0;1m-------------> 0% INITIALIZING [449ms][m[39D[2B[2A[1m<[0;1m-------------> 0% INITIALIZING [645ms][m[39D[1B[1m> Evaluating settings > Resolve dependencies of detachedConfiguration1[m[70D[1B[2A[1m<[0;1m-------------> 0% INITIALIZING [849ms][m[39D[2B[2A[1m<[0;1m-------------> 0% INITIALIZING [1s][m[0K[36D[2B[1A[1m> root project[m[0K[14D[1B[2A[1m<[0;1m-------------> 0% CONFIGURING [3s][m[35D[2B[2A[1m<[0;32;1m=============[0;39;1m> 100% CONFIGURING [3s][m[37D[1B> IDLE[0K[6D[1B[1A[1m> :compileJava > Resolve dependencies of :compileClasspath > Resolve dependenci[m[79D[1B[1A[1m> :compileJava[m[0K[14D[1B[2A[1m<[0;32;1m=======[0;39;1m------> 53% EXECUTING [3s][m[34D[1B[1m> :compileTestJava[m[18D[1B[2A[1m<[0;32;1m==========[0;39;1m---> 76% EXECUTING [4s][m[34D[1B[1m> :test > 0 tests completed[m[27D[1B[3A[0K
[31;1m> Task :test[0;39m[31m FAILED[39m[0K
[0K
DemoApplicationTests > contextLoads() [31mFAILED[39m
    java.lang.IllegalStateException at DefaultCacheAwareContextLoaderDelegate.java:98
        Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException at ConstructorResolver.java:800
            Caused by: org.springframework.beans.factory.BeanCreationException at ConstructorResolver.java:658
                Caused by: org.springframework.beans.BeanInstantiationException at SimpleInstantiationStrategy.java:185
                    Caused by: java.lang.IllegalStateException at Assert.java:97

2 tests completed, 1 failed
[0K
[0K
[0K
[3A[1m<[0;31;1m===========[0;39;1m--> 84% EXECUTING [5s][m[34D[1B> IDLE[6D[1B> IDLE[6D[1B[3A[2K[1B[2K[1B[2K[2A[0m[?12l[?25h
"""

                AnsiTerminalRenderer(
                    ansiText = gradleOutput,
                    maxHeight = 500
                )
            }
        }
    }
}

/**
 * Preview for bright colors and 256 color mode.
 */
@Preview
@Composable
fun TerminalBrightColorsPreview() {
    AutoDevTheme(darkTheme = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Bright Colors & Styles", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val brightTest = """
                    |${'\u001B'}[90mBright Black (Gray)${'\u001B'}[0m
                    |${'\u001B'}[91mBright Red${'\u001B'}[0m
                    |${'\u001B'}[92mBright Green${'\u001B'}[0m
                    |${'\u001B'}[93mBright Yellow${'\u001B'}[0m
                    |${'\u001B'}[94mBright Blue${'\u001B'}[0m
                    |${'\u001B'}[95mBright Magenta${'\u001B'}[0m
                    |${'\u001B'}[96mBright Cyan${'\u001B'}[0m
                    |${'\u001B'}[97mBright White${'\u001B'}[0m
                    |${'\u001B'}[7mInverse Video${'\u001B'}[0m
                    |${'\u001B'}[2mDim Text${'\u001B'}[0m
                    |${'\u001B'}[1;4;31mBold Underlined Red${'\u001B'}[0m
                """.trimMargin()

                AnsiTerminalRenderer(
                    ansiText = brightTest,
                    maxHeight = 300
                )
            }
        }
    }
}

/**
 * Preview for live terminal with streaming output.
 */
@Preview
@Composable
fun LiveTerminalPreview() {
    AutoDevTheme(darkTheme = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Live Terminal (Simulated)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                LiveTerminalRenderer(
                    maxHeight = 400,
                    showCursor = true
                ) { appendText, _ ->
                    // Simulate some output
                    appendText("$ ls -la\n")
                    appendText("total 48\n")
                    appendText("drwxr-xr-x  12 user  staff   384 Nov 11 10:30 ${'\u001B'}[34m.[0m\n")
                    appendText("drwxr-xr-x   8 user  staff   256 Nov 10 15:20 ${'\u001B'}[34m..[0m\n")
                    appendText("-rw-r--r--   1 user  staff  1234 Nov 11 09:15 ${'\u001B'}[32mREADME.md[0m\n")
                    appendText("-rwxr-xr-x   1 user  staff  5678 Nov 11 10:30 ${'\u001B'}[31mbuild.sh[0m\n")
                }
            }
        }
    }
}

/**
 * Preview for complex terminal output with multiple styles.
 */
@Preview
@Composable
fun TerminalComplexPreview() {
    AutoDevTheme(darkTheme = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Complex Terminal Output", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val complexOutput = """
                    |${'\u001B'}[1;32m✓${'\u001B'}[0m Build successful
                    |${'\u001B'}[1;33m⚠${'\u001B'}[0m ${'\u001B'}[33mWarning:${'\u001B'}[0m Deprecated API usage
                    |${'\u001B'}[1;31m✗${'\u001B'}[0m ${'\u001B'}[31mError:${'\u001B'}[0m Test failed
                    |
                    |${'\u001B'}[1mTest Results:${'\u001B'}[0m
                    |  ${'\u001B'}[32mPassed:${'\u001B'}[0m  15
                    |  ${'\u001B'}[31mFailed:${'\u001B'}[0m   1
                    |  ${'\u001B'}[33mSkipped:${'\u001B'}[0m 2
                    |
                    |${'\u001B'}[1;4mStack Trace:${'\u001B'}[0m
                    |  ${'\u001B'}[90mat com.example.Test.method(Test.java:42)${'\u001B'}[0m
                    |  ${'\u001B'}[90mat com.example.Main.main(Main.java:10)${'\u001B'}[0m
                """.trimMargin()

                AnsiTerminalRenderer(
                    ansiText = complexOutput,
                    maxHeight = 400
                )
            }
        }
    }
}

