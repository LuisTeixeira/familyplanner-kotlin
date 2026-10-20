package com.luisteixeira.familyplanner.core.web

import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.extension.AbstractExtension
import io.pebbletemplates.pebble.extension.Filter
import io.pebbletemplates.pebble.loader.ClasspathLoader
import io.pebbletemplates.pebble.loader.DelegatingLoader
import io.pebbletemplates.pebble.template.EvaluationContext
import io.pebbletemplates.pebble.template.PebbleTemplate
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TemplateRegistry(roots: List<String>) {
    private val engine: PebbleEngine = PebbleEngine.Builder()
        .loader(
            DelegatingLoader(
                roots.map { root ->
                    ClasspathLoader(TemplateRegistry::class.java.classLoader).apply { prefix = "$root/" }
                },
            ),
        )
        .extension(FamilyPlannerExtension)
        .build()

    fun render(name: String, model: Map<String, Any?> = emptyMap()): String {
        val writer = StringWriter()
        engine.getTemplate(name).evaluate(writer, model)
        return writer.toString()
    }
}

private object FamilyPlannerExtension : AbstractExtension() {
    override fun getFilters(): Map<String, Filter> = mapOf("fmtDate" to DateFilter)
}

private object DateFilter : Filter {
    private val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
        .withZone(ZoneId.systemDefault())

    override fun apply(
        input: Any?,
        args: MutableMap<String, Any>?,
        self: PebbleTemplate?,
        context: EvaluationContext?,
        lineNumber: Int,
    ): Any? = when (input) {
        null -> ""
        is Instant -> formatter.format(input)
        else -> input.toString()
    }

    override fun getArgumentNames(): List<String>? = null
}
