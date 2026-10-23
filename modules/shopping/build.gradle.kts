plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(project(":platform:household"))
    implementation(libs.ktor.server.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
