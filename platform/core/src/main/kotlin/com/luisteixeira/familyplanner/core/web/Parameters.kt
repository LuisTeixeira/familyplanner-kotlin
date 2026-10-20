package com.luisteixeira.familyplanner.core.web

import java.util.UUID

fun String?.toUuidOrNull(): UUID? =
    this?.let { runCatching { UUID.fromString(it) }.getOrNull() }
