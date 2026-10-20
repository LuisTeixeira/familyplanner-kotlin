package com.luisteixeira.familyplanner.core.persistence

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface TransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

class InMemoryTransactionRunner : TransactionRunner {
    private val mutex = Mutex()

    override suspend fun <T> inTransaction(block: suspend () -> T): T = mutex.withLock { block() }
}
