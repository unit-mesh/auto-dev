package cc.unitmesh.devti.template.context

/**
 * `TemplateContext` is an interface used to provide data to the template rendering process in the `cc.unitmesh.devti.template.TemplateRender` class. It serves as a container for the data that should be made available within the template for use in the rendering process.
 *
 * This interface defines the contract for objects that will be passed to the template engine. Implementations of `TemplateContext` should provide the necessary data members or methods that will be used to populate the template with dynamic content.
 *
 * TODO: spike the common data element for the template context
 */
interface TemplateContext {
}

class EmptyContext : TemplateContext {
}