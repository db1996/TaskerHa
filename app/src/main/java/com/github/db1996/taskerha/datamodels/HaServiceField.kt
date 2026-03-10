package com.github.db1996.taskerha.datamodels

import com.github.db1996.taskerha.enums.HaServiceFieldType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject


object FieldsSerializer : KSerializer<Map<String, Map<String, JsonElement>>> {
    private val delegateSerializer = MapSerializer(String.serializer(), MapSerializer(String.serializer(), JsonElement.serializer()))

    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Map<String, Map<String, JsonElement>>) {
        delegateSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Map<String, Map<String, JsonElement>> {
        val jsonDecoder = decoder as? JsonDecoder ?: return emptyMap()
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonObject) return emptyMap()
        return element.mapValues { (_, value) ->
            if (value is JsonObject) {
                value.jsonObject.toMap()
            } else {
                // Field entry is not an object (e.g. a string), skip it
                emptyMap()
            }
        }.filterValues { it.isNotEmpty() }
    }
}

@Serializable
data class HaService(
    val name: String? = null,
    val description: String? = null,
    val target: Map<String, JsonElement>? = null,
    @Serializable(with = FieldsSerializer::class)
    val fields: Map<String, Map<String, JsonElement>>? = null
)

data class HaServiceField(
    val id: String,
    var name: String? = null,
    var description: String? = null,
    var required: Boolean? = null,
    var example: String? = null,
    var type: HaServiceFieldType? = null,

    var options: MutableList<Option>? = null,

    var min: Double? = null,
    var max: Double? = null,
    var unit_of_measurement: String? = null
)

data class Option(
    val label: String,
    val value: String
)
