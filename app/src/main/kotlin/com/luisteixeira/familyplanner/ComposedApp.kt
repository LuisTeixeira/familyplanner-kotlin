package com.luisteixeira.familyplanner

import com.luisteixeira.familyplanner.auth.SessionValidator
import com.luisteixeira.familyplanner.config.AppConfig
import com.luisteixeira.familyplanner.core.module.AppModule
import com.luisteixeira.familyplanner.wiring.Storage
import com.luisteixeira.familyplanner.wiring.createAppContext
import com.luisteixeira.familyplanner.wiring.modules
import com.luisteixeira.familyplanner.wiring.storage

class ComposedApp (
    val modules: List<AppModule>,
    val sessionValidator: SessionValidator,
    private val storage: Storage,
) : AutoCloseable {

    override fun close() {
        storage.close()
    }

}

fun composeApp(config: AppConfig = AppConfig()): ComposedApp {
    val storage = config.persistence.storage(config.database)
    val context = createAppContext(storage)

    return ComposedApp(context.modules(), context.auth, storage)
}