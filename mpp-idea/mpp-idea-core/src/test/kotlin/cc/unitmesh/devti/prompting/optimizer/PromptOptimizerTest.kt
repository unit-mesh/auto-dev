package cc.unitmesh.devti.prompting.optimizer

import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language

class PromptOptimizerTest {
    @Test
    fun should_trim_leading_and_trailing_spaces_from_each_line() {
        // given
        val input = """
            // Leading space in first line
             // Leading tab in second line
            Third line with trailing space 
            Fourth line with trailing tab	
        """.trimIndent()

        // when
        val result = PromptOptimizer.trimCodeSpace(input)

        // then
        val expected = """
            // Leading space in first line
            // Leading tab in second line
            Third line with trailing space
            Fourth line with trailing tab
        """.trimIndent()
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun should_remove_empty_lines_from_the_prompt() {
        // given
        val input = """
            First line
            
            Second line
            
            Third line
        """.trimIndent()

        // when
        val result = PromptOptimizer.trimCodeSpace(input)

        // then
        val expected = """
            First line
            Second line
            Third line
        """.trimIndent()
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun should_handle_prompt_with_only_empty_lines() {
        // given
        val input = """
            
            
            
        """.trimIndent()

        // when
        val result = PromptOptimizer.trimCodeSpace(input)

        // then
        val expected = ""
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun should_return_original_prompt_if_no_spaces_or_empty_lines() {
        // given
        val input = """
            First line
            Second line
            Third line
        """.trimIndent()

        // when
        val result = PromptOptimizer.trimCodeSpace(input)

        // then
        val expected = """
            First line
            Second line
            Third line
        """.trimIndent()
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun should_handle_for_rust_code_in_issue() {
        @Language("Rust")
        val code = """
use crate::{find_target, Plot};
use anyhow::{Context, Result};
use std::{env, process};

impl Plot {
    pub fn generate_plot(&mut self) -> Result<(), anyhow::Error> {
        eprintln!("Generating plot");

        self.target =
            find_target(&self.target).context("⚠️  couldn't find the target for plotting")?;

        // The cargo executable
        let cargo = env::var("CARGO").unwrap_or_else(|_| String::from("cargo"));

        let fuzzer_data_dir = format!(
            "{}/{}/afl/{}/",
            &self.ziggy_output.display(),
            &self.target,
            &self.input
        );

        let plot_dir = self
            .output
            .display()
            .to_string()
            .replace("{ziggy_output}", &self.ziggy_output.display().to_string())
            .replace("{target_name}", &self.target);
        println!("{plot_dir}");
        println!("{}", self.target);

        // We run the afl-plot command
        process::Command::new(cargo)
            .args(["afl", "plot", &fuzzer_data_dir, &plot_dir])
            .spawn()
            .context("⚠️  couldn't spawn afl plot")?
            .wait()
            .context("⚠️  couldn't wait for the afl plot")?;

        Ok(())
    }
}
        """.trimIndent()

        // when
        val result = PromptOptimizer.trimCodeSpace(code)

        // then
        val expected = """use crate::{find_target, Plot};
use anyhow::{Context, Result};
use std::{env, process};
impl Plot {
pub fn generate_plot(&mut self) -> Result<(), anyhow::Error> {
eprintln!("Generating plot");
self.target =
find_target(&self.target).context("⚠️  couldn't find the target for plotting")?;
// The cargo executable
let cargo = env::var("CARGO").unwrap_or_else(|_| String::from("cargo"));
let fuzzer_data_dir = format!(
"{}/{}/afl/{}/",
&self.ziggy_output.display(),
&self.target,
&self.input
);
let plot_dir = self
.output
.display()
.to_string()
.replace("{ziggy_output}", &self.ziggy_output.display().to_string())
.replace("{target_name}", &self.target);
println!("{plot_dir}");
println!("{}", self.target);
// We run the afl-plot command
process::Command::new(cargo)
.args(["afl", "plot", &fuzzer_data_dir, &plot_dir])
.spawn()
.context("⚠️  couldn't spawn afl plot")?
.wait()
.context("⚠️  couldn't wait for the afl plot")?;
Ok(())
}
}"""

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun should_handle_for_python_code_in_issue() {
        @Language("Markdown")
        val code = """
Here is the code:       
```python
def foo():
    print("Hello, World!")
```
        """.trimIndent()

        // when
        val result = PromptOptimizer.trimCodeSpace(code)

        // then
        val expected = """Here is the code:
```python
def foo():
    print("Hello, World!")
```"""

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun should_handle_with_devin_block() {
        val content = """
            |```devin
            |/write:HelloWorld.java
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
            |```
            |
        """.trimMargin()

        // when
        val result = PromptOptimizer.trimCodeSpace(content)

        // then
        val expected = """```DevIn
/write:HelloWorld.java
```java
public class HelloWorld {
public static void main(String[] args) {
System.out.println("Hello, World");
}
}
```
```"""

        assertThat(result).isEqualTo(expected)
    }
}
