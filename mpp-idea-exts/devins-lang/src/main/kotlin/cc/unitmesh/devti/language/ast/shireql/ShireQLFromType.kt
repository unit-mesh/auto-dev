package cc.unitmesh.devti.language.ast.shireql

enum class ShireQLFromType(val typeName: String) {
    // PSI Query
    PsiFile("PsiFile"),
    PsiPackage("PsiPackage"),
    PsiClass("PsiClass"),
    PsiMethod("PsiMethod"),
    PsiField("PsiField"),

    // GitQuery
    GitCommit("GitCommit"),
    GitBranch("GitBranch"),

    // Others
    Date("Date"),
}