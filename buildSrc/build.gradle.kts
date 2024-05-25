plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
    implementation("io.github.gradle-nexus:publish-plugin:1.3.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.20")
    implementation(gradleApi())
}
