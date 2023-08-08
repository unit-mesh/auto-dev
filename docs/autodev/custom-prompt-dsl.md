## Custom Prompt DSL

```json
[
  {
    "title": "转译",
    "autoInvoke": true,
    "template": "帮我重构下面的代码为 Kotlin 语言，方便于我理解: \n ${SPEC.XXX} ${SELECTION}"
  }
]
```

IDE Variable:

```
$SELECTION 
```

SPEC Variable:

```
$SPEC.xxx 
```

example: `$SPEC.controller`
