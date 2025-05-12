package cc.unitmesh.devti.devins.shireql

enum class JvmShireQLFuncType(val methodName: String, val description: String) {
    GET_NAME("getName", "Get class name"),
    NAME("name", "Get class name"),
    EXTENDS("extends", "Get class extends"),
    IMPLEMENTS("implements", "Get class implements"),
    METHOD_CODE_BY_NAME("methodCodeByName", "Get method code by name"),
    FIELD_CODE_BY_NAME("fieldCodeByName", "Get field code by name"),

    SUBCLASSES_OF("subclassesOf", "Get subclasses of class"),
    ANNOTATED_OF("annotatedOf", "Get annotated classes"),
    SUPERCLASS_OF("superclassOf", "Get superclass of class"),
    IMPLEMENTS_OF("implementsOf", "Get implemented interfaces of class"),
}