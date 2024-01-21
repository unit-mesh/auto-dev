package cc.unitmesh.devti.pair.arch

data class LayeredArch(val layers: List<Layer>) {
    fun getLayerByName(name: String): Layer? {
        return layers.find { it.name == name }
    }
}

data class Layer(val name: String, val packages: List<String>) {
    fun getPackageByName(name: String): String? {
        return packages.find { it == name }
    }
}
