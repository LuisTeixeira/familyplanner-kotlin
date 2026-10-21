package com.luisteixeira.familyplanner

import com.luisteixeira.familyplanner.auth.SessionValidator
import com.luisteixeira.familyplanner.config.AppConfig
import com.luisteixeira.familyplanner.core.module.AppModule
import com.luisteixeira.familyplanner.wiring.createAppContext
import com.luisteixeira.familyplanner.wiring.modules
import com.luisteixeira.familyplanner.wiring.storage

class ComposedApp (
    val modules: List<AppModule>,
    val sessionValidator: SessionValidator,
)

fun composeApp(config: AppConfig = AppConfig()): ComposedApp {
    val storage = config.persistence.storage()
    val context = createAppContext(storage)

    return ComposedApp(context.modules(), context.auth)
}