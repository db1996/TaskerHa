package com.github.db1996.taskerha.client

import com.github.db1996.taskerha.datamodels.*
import com.github.db1996.taskerha.enums.HomeassistantStatus
import com.github.db1996.taskerha.enums.HaServiceFieldType
import com.github.db1996.taskerha.datamodels.Option
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class HomeAssistantClient(
    var baseUrl: String = "",
    var accessToken: String = ""
) {

    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    var error: String = ""
    var homeAssistantStatus = HomeassistantStatus.NO_SETTINGS
    private var services: List<HaDomainService> = emptyList()
    private var entities: List<HaEntity> = emptyList()

    init {
        validateSettings()
    }

    private fun validateSettings() {
        homeAssistantStatus = when {
            baseUrl.isEmpty() && accessToken.isEmpty() -> HomeassistantStatus.NO_URL_AND_TOKEN
            baseUrl.isEmpty() -> HomeassistantStatus.NO_URL
            accessToken.isEmpty() -> HomeassistantStatus.NO_TOKEN
            else -> HomeassistantStatus.CONNECTING
        }
    }

    private fun request(path: String): Request =
        Request.Builder()
            .url("$baseUrl$path")
            .header("Authorization", "Bearer $accessToken")
            .build()

    private fun request(path: String, method: String, body: String?): Request {
        val requestBody = body?.toRequestBody("application/json".toMediaType())
        return Request.Builder()
            .url("$baseUrl$path")
            .method(method, requestBody)
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .build()
    }

    // ------------------ SAFE NETWORK CALLS ------------------

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = http.newCall(request("/api/")).execute()
            if (!response.isSuccessful) {
                error = if (response.code == 401) "Unauthorized, check your token" else response.message
                homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
                false
            } else {
                homeAssistantStatus = HomeassistantStatus.CONNECTED
                true
            }
        } catch (e: Exception) {
            error = "Can't connect to Home Assistant: $e"
            homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
            false
        }
    }

    suspend fun getEntities(): List<HaEntity> = withContext(Dispatchers.IO) {
        if (homeAssistantStatus != HomeassistantStatus.CONNECTED) return@withContext emptyList()
        if (entities.isNotEmpty()) return@withContext entities

        try {
            val response = http.newCall(request("/api/states")).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            entities = json.decodeFromString(body)
            entities


        } catch (e: Exception) {
            error = e.toString()
            emptyList()
        }


    }

    suspend fun getServices(force: Boolean = false): List<HaDomainService> = withContext(Dispatchers.IO) {
        if (homeAssistantStatus != HomeassistantStatus.CONNECTED) return@withContext emptyList()
        if (services.isNotEmpty() && !force) return@withContext services

        try {
            val response = http.newCall(request("/api/services")).execute()
            if (!response.isSuccessful) {
                error = response.message
                homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            services = json.decodeFromString(body)
            services
        } catch (e: Exception) {
            error = e.toString()
            homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
            emptyList()
        }
    }

    suspend fun getServicesFront(force: Boolean = false): List<ActualService> {
        val domainServices = getServices(force)
        return domainServices.flatMap { domain ->
            domain.services.map { (serviceId, serviceData) ->
                convertService(serviceData, serviceId, domain.domain)
            }
        }
    }

    suspend fun callService(domain: String, service: String, entityId: String, data: Map<String, Any>? = null): Boolean =
        withContext(Dispatchers.IO) {
            if (homeAssistantStatus != HomeassistantStatus.CONNECTED) throw Exception(error)

            val payload = mutableMapOf<String, Any>()
            if (entityId.isNotEmpty()) payload["entity_id"] = entityId
            data?.let { payload.putAll(it) }

            val body = json.encodeToString(
                MapSerializer(String.serializer(), JsonElement.serializer()),
                payload.mapValues { JsonPrimitive(it.value.toString()) }
            )

            val req = request("/api/services/$domain/$service", "POST", body)

            try {
                val response = http.newCall(req).execute()
                if (!response.isSuccessful) {
                    error = if (response.code == 401) "Unauthorized, check your token" else response.message
                    homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
                    false
                } else {
                    homeAssistantStatus = HomeassistantStatus.CONNECTED
                    true
                }
            } catch (e: IOException) {
                error = e.toString()
                homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
                false
            }
        }

    // ------------------ CONVERSIONS ------------------

    private fun convertService(haService: HaService, serviceId: String, domain: String): ActualService {
        val hasEntityTarget = haService.target?.containsKey("entity") ?: false
        val actualService = ActualService(
            id = serviceId,
            name = haService.name,
            description = haService.description,
            type = domain,
            domain = domain,
            fields = mutableListOf(),
            targetEntity = hasEntityTarget
        )

        haService.fields?.forEach { (id, fieldData) ->
            if (id != "advanced_fields") convertField(fieldData, id)?.let { actualService.fields += it }
        }

        return actualService
    }

    private fun convertField(field: Map<String, JsonElement>, id: String): HaServiceField? {
        val fieldData = HaServiceField(id)
        field["name"]?.jsonPrimitive?.contentOrNull?.let { fieldData.name = it }
        field["description"]?.jsonPrimitive?.contentOrNull?.let { fieldData.description = it }
        field["required"]?.jsonPrimitive?.booleanOrNull?.let { fieldData.required = it }
        field["example"]?.let { element ->
            when (element) {
                is JsonPrimitive -> fieldData.example = element.contentOrNull
                is JsonArray -> fieldData.example = element.joinToString(", ") { it.toString() }
                is JsonObject -> fieldData.example = element.toString()
                else -> fieldData.example = null
            }
        }

        when (id) {
            "date" -> fieldData.type = HaServiceFieldType.DATE
            "time" -> fieldData.type = HaServiceFieldType.TIME
            "datetime" -> fieldData.type = HaServiceFieldType.DATETIME
        }

        if (fieldData.type == null) {
            val selector = field["selector"]?.jsonObject
            selector?.forEach { (type, value) ->
                when (type) {
                    "text" -> fieldData.type = HaServiceFieldType.TEXT
                    "boolean" -> fieldData.type = HaServiceFieldType.BOOLEAN
                    "entity" -> fieldData.type = HaServiceFieldType.TEXT
                    "select" -> {
                        fieldData.type = HaServiceFieldType.SELECT
                        fieldData.options = mutableListOf()
                        val options = value.jsonObject["options"]?.jsonArray
                        options?.forEach {
                            if (it is JsonPrimitive) {
                                fieldData.options!!.add(Option(it.content, it.content))
                            } else {
                                val obj = it.jsonObject
                                fieldData.options!!.add(
                                    Option(obj["label"]!!.jsonPrimitive.content, obj["value"]!!.jsonPrimitive.content)
                                )
                            }
                        }
                    }
                    "number", "color_temp", "color_rgb" -> {
                        fieldData.type = HaServiceFieldType.NUMBER
                        value.jsonObject["min"]?.jsonPrimitive?.doubleOrNull?.let { fieldData.min = it }
                        value.jsonObject["max"]?.jsonPrimitive?.doubleOrNull?.let { fieldData.max = it }
                        value.jsonObject["unit"]?.jsonPrimitive?.contentOrNull?.let { fieldData.unit_of_measurement = it }
                    }
                }
            }
        }

        return fieldData.type?.let { fieldData }
    }
}
