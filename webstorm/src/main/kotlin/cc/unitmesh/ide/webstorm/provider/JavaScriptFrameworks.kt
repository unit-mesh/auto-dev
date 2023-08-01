package cc.unitmesh.ide.webstorm.provider

enum class JsWebFrameworks(val presentation: String, val packageName: String) {
    React("React", "react"),
    Vue("Vue", "vue"),
    Angular("Angular", "@angular/core"),
    AngularJS("AngularJS", "angular"),
    Svelte("Svelte", "svelte"),
    Astro("Astro", "astro"),
    Lit("Lit", "lit"),
    Solid("Solid", "solid-js"),
    Preact("Preact", "preact")

}

enum class JsTestFrameworks(val presentation: String, val packageName: String) {
    Jest("Jest", "jest"),
    Mocha("Mocha", "mocha"),
    Jasmine("Jasmine", "jasmine"),
    Karma("Karma", "karma"),
    Ava("Ava", "ava"),
    Tape("Tape", "tape"),
    Qunit("Qunit", "qunit"),
    Tap("Tap", "tap"),
    Cypress("Cypress", "cypress"),
    Protractor("Protractor", "protractor"),
    Nightwatch("Nightwatch", "nightwatch"),
    Vitest("Vitest", "vitest")
}
