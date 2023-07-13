package cc.unitmesh.devti.settings

val OPENAI_MODEL = arrayOf("gpt-3.5-turbo", "gpt-4")
val DEFAULT_PROMPTS = """
{
  "auto_complete": {
    "instruction": "",
    "input": ""
  },
  "auto_comment": {
    "instruction": "",
    "input": ""
  },
  "code_review": {
    "instruction": "",
    "input": ""
  },
  "refactor": {
    "instruction": "",
    "input": ""
  }
}
"""
val AI_ENGINES = arrayOf("OpenAI", "Custom", "Azure")
val DEFAULT_AI_ENGINE = AI_ENGINES[0]