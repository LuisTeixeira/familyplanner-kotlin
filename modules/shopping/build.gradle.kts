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
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)
    testImplementation(libs.hikari)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
}

tasks.test {
    useJUnitPlatform()
}
