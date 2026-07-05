package com.github.db1996.taskerha.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.yaml.snakeyaml.Yaml

/**
 * Converts YAML text (as typed in Home Assistant's own "data" field editor) into the
 * equivalent compact JSON text, so it can be embedded as a nested object/array when
 * calling a service.
 */
object YamlJsonConverter {
    private val yaml = Yaml()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns compact JSON text for [text] if it parses as a YAML mapping or sequence,
     * or null if it's blank, invalid YAML, or just a plain scalar (not worth converting).
     */
    fun yamlToJsonString(text: String): String? {
        if (text.isBlank()) return null

        val parsed = try {
            yaml.load<Any?>(text)
        } catch (_: Exception) {
            return null
        }

        if (parsed !is Map<*, *> && parsed !is List<*>) return null

        return json.encodeToString(JsonElement.serializer(), toJsonElement(parsed))
    }

    private fun toJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to toJsonElement(v) })
        is List<*> -> JsonArray(value.map { toJsonElement(it) })
        is Boolean -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is Float -> JsonPrimitive(value.toDouble())
        is Number -> JsonPrimitive(value.toDouble())
        else -> JsonPrimitive(value.toString())
    }
}
