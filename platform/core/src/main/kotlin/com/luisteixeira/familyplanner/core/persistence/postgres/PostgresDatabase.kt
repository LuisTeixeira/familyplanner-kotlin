package com.luisteixeira.familyplanner.core.persistence.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

class PostgresDatabase(
    val dataSource: HikariDataSource,
    val database: Database,
) : AutoCloseable {

    override fun close() {
        dataSource.close()
    }

}

fun connectPostgres(url: String, user: String, password: String): PostgresDatabase {
    val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = 10
        },
    )
    return PostgresDatabase(dataSource, Database.connect(dataSource))
}

fun migrate(dataSource: DataSource, schema: String, module: String) {
    Flyway.configure()
        .dataSource(dataSource)
        .schemas(schema)
        .defaultSchema(schema)
        .createSchemas(true)
        .table("flyway_schema_history")
        .locations("classpath:$module/db/migration")
        .load()
        .migrate()
}