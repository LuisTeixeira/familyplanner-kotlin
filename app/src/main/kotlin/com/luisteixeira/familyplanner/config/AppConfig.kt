package com.luisteixeira.familyplanner.config

enum class Persistence { MEMORY }

data class AppConfig(val persistence: Persistence = Persistence.MEMORY) {
    companion object {
        fun fromEnvironment(): AppConfig = AppConfig(
            persistence = System.getenv("FAMILYPLANNER_PERSISTENCE")
                ?.let { Persistence.valueOf(it.uppercase()) }
                ?: Persistence.MEMORY,
        )
    }
}
