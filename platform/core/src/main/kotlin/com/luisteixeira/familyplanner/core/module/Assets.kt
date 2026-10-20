package com.luisteixeira.familyplanner.core.module

const val PLATFORM_MODULE = "platform"

fun templateRoot(module: String): String = "$module/templates"

fun staticRoot(module: String): String = "$module/static"
