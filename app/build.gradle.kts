plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(project(":platform:auth"))
    implementation(project(":platform:household"))
    implementation(project(":modules:shopping"))
    implementation(project(":modules:calendar"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.logback.classic)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.testcontainers.postgresql)
}

application {
    mainClass.set("com.luisteixeira.familyplanner.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
