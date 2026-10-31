package com.luisteixeira.familyplanner

import com.luisteixeira.familyplanner.auth.apikey.ApiKeyService
import com.luisteixeira.familyplanner.auth.session.SessionValidator
import com.luisteixeira.familyplanner.auth.csrf.CsrfTokens
import com.luisteixeira.familyplanner.auth.token.TokenVerifier
import com.luisteixeira.familyplanner.config.AppConfig
import com.luisteixeira.familyplanner.core.module.AppModule
import com.luisteixeira.familyplanner.wiring.Storage
import com.luisteixeira.familyplanner.wiring.createAppContext
import com.luisteixeira.familyplanner.wiring.modules
import com.luisteixeira.familyplanner.wiring.storage

class ComposedApp(
    val modules: List<AppModule>,
    val sessionValidator: SessionValidator,
    val apiKeys: ApiKeyService,
    val tokenVerifier: TokenVerifier,
    val csrfTokens: CsrfTokens,
    val secureCookies: Boolean,
    private val storage: Storage,
) : AutoCloseable {

    override fun close() {
        storage.close()
    }
}

fun composeApp(config: AppConfig = AppConfig()): ComposedApp {
    val storage = config.persistence.storage(config.database)
    val context = createAppContext(storage, config.secureCookies, config.jwt)

    return ComposedApp(
        modules = context.modules(),
        sessionValidator = context.sessions,
        apiKeys = context.apiKeys,
        tokenVerifier = context.tokenVerifier,
        csrfTokens = CsrfTokens(config.secret.value.toByteArray()),
        secureCookies = config.secureCookies,
        storage = storage,
    )
}
