package com.luisteixeira.familyplanner.core.persistence.postgres

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

suspend fun <T> dbTransaction(database: Database, block: suspend () -> T): T =
    withContext(Dispatchers.IO) {
        suspendTransaction(database) { block() }
    }
