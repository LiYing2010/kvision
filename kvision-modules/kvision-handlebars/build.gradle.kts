buildscript {
    extra.set("production", (findProperty("prod") ?: findProperty("production") ?: "false") == "true")
}

plugins {
    kotlin("js")
    id("maven-publish")
}

repositories()

kotlin {
    kotlinJsTargets()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    api(rootProject)
    implementation(npm("handlebars", "4.7.6"))
    implementation(npm("handlebars-loader", "1.7.1"))
    testImplementation(kotlin("test-js"))
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets.main.get().kotlin)
}

publishing {
    publications {
        create<MavenPublication>("kotlin") {
            from(components["kotlin"])
            artifact(tasks["sourcesJar"])
            pom {
                defaultPom()
            }
        }
    }
}

setupPublication()

tasks {
    getByName("JsJar", Jar::class) {
        from("${rootProject.buildDir}/js/packages/kvision-${project.name}/package.json") {
            filter { it.replace("\"main\": \"kotlin/kvision-kvision", "\"main\": \"kvision-kvision") }
        }
    }
}