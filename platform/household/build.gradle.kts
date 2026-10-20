plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(project(":platform:auth"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
