package com.luisteixeira.familyplanner.core.persistence.postgres

import com.luisteixeira.familyplanner.core.persistence.TransactionRunner
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class PostgresTransactionRunner(
    private val database: Database
) : TransactionRunner{

    override suspend fun <T> inTransaction(block: suspend () -> T): T {
        return suspendTransaction(database) {
            block()
        }
    }

}