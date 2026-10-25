plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.jbcrypt)
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
    filter {
        excludeTestsMatching("*Postgres*")
    }
}

tasks.register<Test>("postgresTest") {
    group = "verification"
    description = "Runs the Postgres repository tests. Requires Docker."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("*Postgres*")
    }
}
