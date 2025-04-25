package cc.unitmesh.devti.language.ast.action

class RuleBasedPatternAction(val pattern: String, override val processors: List<PatternActionFunc>) :
    DirectAction(processors)
