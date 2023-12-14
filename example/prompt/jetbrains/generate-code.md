# Generate Code

"*Assume a caret is between two code blocks*:\n\n```\n%s\n```\n\nand\n\n```\n%s\n```\n\n*Modify the code to accomplish
the following task:*\n\n%s\n\n%s"

## Full Text

"*The answer should only contain the modified code without any additional text before or after code snippet,
description, or commentary.*"

## Patch

*The answer should only contain a valid unified format patch for original code without any additional text before or
after code snippet, description, or commentary. Don't omit code and don't use \"...\" to skip any code pieces.*

The sample of a patch:

```
--- original
+++ modified
@@ -1,2 +1,2 @@
unmodified      
-removed
+added
```

## Code Snippet

*Generate a concise code snippet, without any additional text before or after code snippet, description, or commentary.
The code needs to accomplish the following task*: %s

