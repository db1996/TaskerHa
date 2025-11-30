package com.github.db1996.taskerha.client

import android.util.Log
import com.github.db1996.taskerha.datamodels.ActualService
import com.github.db1996.taskerha.datamodels.HaDomainService
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.datamodels.HaService
import com.github.db1996.taskerha.datamodels.HaServiceField
import com.github.db1996.taskerha.datamodels.Option
import com.github.db1996.taskerha.enums.HaServiceFieldType
import com.github.db1996.taskerha.enums.HomeassistantStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
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

    // --- Headers and request
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

    // --- API calls
    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = http.newCall(request("/api/")).execute()
            if (!response.isSuccessful) {
                error =
                    if (response.code == 401) "Unauthorized, check your token" else response.message
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

    suspend fun getServices(force: Boolean = false): List<HaDomainService> =
        withContext(Dispatchers.IO) {
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
                MapSerializer(String.Companion.serializer(), JsonElement.Companion.serializer()),
                payload.mapValues { JsonPrimitive(it.value.toString()) }
            )

            val req = request("/api/services/$domain/$service", "POST", body)

            try {
                val response = http.newCall(req).execute()
                if (!response.isSuccessful) {
                    error =
                        if (response.code == 401) "Unauthorized, check your token" else response.message
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

    // --- Conversions
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

        if(actualService.id == "tree_patch" && actualService.domain == "runelite"){
            Log.e("HA", "Found tree_patch service, $haService")
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
            selector?.let { sel ->
                HaServiceFieldType.entries.forEach { t ->
                    val selectorElement = sel[t.name.lowercase()] ?: return@forEach
                    val obj = (selectorElement as? JsonObject) ?: return@forEach
                    when (t) {
                        HaServiceFieldType.TEXT -> {
                            fieldData.type = HaServiceFieldType.TEXT
                        }

                        HaServiceFieldType.BOOLEAN -> {
                            fieldData.type = HaServiceFieldType.BOOLEAN
                        }

                        HaServiceFieldType.SELECT -> {
                            fieldData.type = HaServiceFieldType.SELECT
                            val optionsArray = obj["options"]?.jsonArray ?: JsonArray(emptyList())
                            fieldData.options = optionsArray.map { item ->
                                if (item is JsonPrimitive) {
                                    Option(item.content, item.content)
                                }else{
                                    Option(
                                        item.toString(),
                                        item.toString()
                                    )
                                }
                            }.toMutableList()
                            if(fieldData.name == "Tree type" && fieldData.id == "crop_type"){
                                Log.e("HA", "Found crop_type field, ${fieldData.options}")
                            }

                            if(fieldData.options?.isEmpty() ?: true){
                                fieldData.type = HaServiceFieldType.TEXT
                            }

                        }

                        HaServiceFieldType.NUMBER -> {
                            fieldData.type = HaServiceFieldType.NUMBER
                            fieldData.min = obj["min"]?.jsonPrimitive?.doubleOrNull
                            fieldData.max = obj["max"]?.jsonPrimitive?.doubleOrNull
                            fieldData.unit_of_measurement =
                                obj["unit"]?.jsonPrimitive?.contentOrNull
                                    ?: obj["unit_of_measurement"]?.jsonPrimitive?.contentOrNull
                        }

                        else -> fieldData.type = HaServiceFieldType.TEXT
                    }
                }
            }


        }

        return fieldData.type?.let { fieldData }
    }
}