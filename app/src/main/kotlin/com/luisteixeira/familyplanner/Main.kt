package com.luisteixeira.familyplanner

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("familyplanner")

fun main() {
    val app = composeApp(AppConfig.fromEnvironment())

    log.info("Server executing on http://localhost:8080")
    embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
        familyPlannerApp(app)
    }.start(wait = true)
}
