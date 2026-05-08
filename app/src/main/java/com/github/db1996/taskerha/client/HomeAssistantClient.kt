package com.github.db1996.taskerha.client

import com.github.db1996.taskerha.datamodels.ActualService
import com.github.db1996.taskerha.datamodels.HaDomainService
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.datamodels.HaService
import com.github.db1996.taskerha.datamodels.HaServiceField
import com.github.db1996.taskerha.datamodels.Option
import com.github.db1996.taskerha.enums.HaServiceFieldType
import com.github.db1996.taskerha.enums.HomeassistantStatus
import com.github.db1996.taskerha.tasker.base.BaseLogger
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
import okhttp3.Response
import java.io.IOException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

class HomeAssistantClient(
    var baseUrl: String = "",
    var accessToken: String = "",
    httpClient: OkHttpClient = OkHttpClient()
): BaseLogger {

    override val logTag: String
        get() = "HomeAssistantClient"

    private val http = httpClient
    private val json = Json { ignoreUnknownKeys = true }

    var error: String = ""
    var homeAssistantStatus = HomeassistantStatus.NO_SETTINGS
    var result: String = ""
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

    // --- Error formatting helpers
    private fun formatHttpError(response: Response, body: String? = null): String {
        if (response.code == 401) return "Unauthorized, check your token"
        val reason = response.message.takeIf { it.isNotBlank() }
        // HTTP/2 omits the reason phrase; surface the status code so the user always sees something.
        val base = if (reason != null) "HTTP ${response.code} ($reason)" else "HTTP ${response.code}"
        val hint = extractBodyHint(body)
        return if (hint != null) "$base — $hint" else base
    }

    private fun extractBodyHint(body: String?): String? {
        if (body.isNullOrBlank()) return null
        // Cloudflare and other proxies often return an HTML error page; surface the
        // first <title> or <h1> so the user gets a real reason instead of just the code.
        val titleRegex = Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE)
        val h1Regex = Regex("<h1[^>]*>([^<]+)</h1>", RegexOption.IGNORE_CASE)
        val match = titleRegex.find(body) ?: h1Regex.find(body)
        if (match != null) {
            val text = match.groupValues[1].trim()
            if (text.isNotEmpty()) return text.take(200)
        }
        // Fallback for non-HTML bodies: take the first non-blank line.
        val firstLine = body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
        return firstLine?.take(200)
    }

    private fun formatException(e: Throwable, prefix: String = "Can't connect to Home Assistant"): String {
        val detail = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
        val hint = when (e) {
            is SSLHandshakeException ->
                " — TLS handshake failed (server may require a client certificate; enable 'Use client certificate' in settings)"
            is SSLException -> " — TLS error"
            else -> ""
        }
        return "$prefix: $detail$hint"
    }

    // --- Headers and request
    private fun request(path: String): Request =
        Request.Builder()
            .url("$baseUrl$path")
            .header("Authorization", "Bearer $accessToken")
            .build()

    private fun request(path: String, body: String?, method: String = "POST"): Request {
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
            logVerbose("Ping url ${response.request.url}")

            if (!response.isSuccessful) {
                val body = runCatching { response.body?.string() }.getOrNull()
                error = formatHttpError(response, body)
                homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
                false
            } else {
                homeAssistantStatus = HomeassistantStatus.CONNECTED
                true
            }
        } catch (e: Exception) {
            error = formatException(e)
            homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
            false
        }
    }

    suspend fun getEntities(): List<HaEntity> = withContext(Dispatchers.IO) {
        if (homeAssistantStatus != HomeassistantStatus.CONNECTED) return@withContext emptyList()
        if (entities.isNotEmpty()) return@withContext entities

        try {
            val response = http.newCall(request("/api/states")).execute()
            val body = response.body?.string()
            if (!response.isSuccessful) {
                error = formatHttpError(response, body)
                return@withContext emptyList()
            }

            if (body == null) return@withContext emptyList()
            entities = json.decodeFromString(body)
            entities


        } catch (e: Exception) {
            error = formatException(e)
            emptyList()
        }


    }

    suspend fun getServices(force: Boolean = false): List<HaDomainService> =
        withContext(Dispatchers.IO) {
            if (homeAssistantStatus != HomeassistantStatus.CONNECTED) return@withContext emptyList()
            if (services.isNotEmpty() && !force) return@withContext services

            try {
                val response = http.newCall(request("/api/services")).execute()
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    error = formatHttpError(response, body)
                    homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
                    return@withContext emptyList()
                }

                if (body == null) return@withContext emptyList()
                services = json.decodeFromString(body)
                services
            } catch (e: Exception) {
                error = formatException(e)
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

    suspend fun getState(entityId: String): Boolean =
        withContext(Dispatchers.IO) {
            if (homeAssistantStatus != HomeassistantStatus.CONNECTED) throw Exception(error)

            val req = request("/api/states/$entityId")
            logVerbose("url: ${req.url}")
            try {
                val response = http.newCall(req).execute()
                logVerbose("Response: $response")
                result = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    error = formatHttpError(response, result)
                    homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
                    false
                } else {
                    homeAssistantStatus = HomeassistantStatus.CONNECTED
                    true
                }
            } catch (e: IOException) {
                error = formatException(e)
                homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
                false
            }
        }

    suspend fun getEntityAttributeKeys(entityId: String): List<String> =
        withContext(Dispatchers.IO) {
            if (homeAssistantStatus != HomeassistantStatus.CONNECTED) return@withContext emptyList()
            try {
                val response = http.newCall(request("/api/states/$entityId")).execute()
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) return@withContext emptyList()
                val detail = json.decodeFromString<com.github.db1996.taskerha.datamodels.HaEntityStateDetail>(body)
                detail.attributes.keys.toList()
            } catch (_: Exception) {
                emptyList()
            }
        }

    suspend fun callService(domain: String, service: String, entityId: String, data: Map<String, Any>? = null): Boolean =
        withContext(Dispatchers.IO) {
            if (homeAssistantStatus != HomeassistantStatus.CONNECTED) throw Exception(error)

            val payload = mutableMapOf<String, Any>()
            if (entityId.isNotEmpty()) payload["entity_id"] = entityId
            data?.let { payload.putAll(it) }

            logVerbose("Payload: $payload")

            val body = json.encodeToString(
                MapSerializer(String.serializer(), JsonElement.serializer()),
                payload.mapValues { JsonPrimitive(it.value.toString()) }
            )

            val req = request("/api/services/$domain/$service", body)
            logVerbose("url: ${req.url}")

            try {
                val response = http.newCall(req).execute()

                result = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    error = formatHttpError(response, result)
                    homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
                    false
                } else {
                    homeAssistantStatus = HomeassistantStatus.CONNECTED
                    true
                }
            } catch (e: IOException) {
                error = formatException(e)
                homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
                false
            }
        }

    suspend fun fireEvent(eventType: String, data: Map<String, Any>? = null): Boolean =
        withContext(Dispatchers.IO) {
            if (homeAssistantStatus != HomeassistantStatus.CONNECTED) throw Exception(error)

            val payload = mutableMapOf<String, Any>()
            data?.let { payload.putAll(it) }

            logVerbose("Payload: $payload")


            val body = json.encodeToString(
                MapSerializer(String.serializer(), JsonElement.serializer()),
                payload.mapValues { JsonPrimitive(it.value.toString()) }
            )

            val req = request("/api/events/$eventType", body)
            logVerbose("url: ${req.url}")
            try {
                val response = http.newCall(req).execute()

                result = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    error = formatHttpError(response, result)
                    homeAssistantStatus = HomeassistantStatus.NO_CONNECTION
                    false
                } else {
                    homeAssistantStatus = HomeassistantStatus.CONNECTED
                    true
                }
            } catch (e: IOException) {
                error = formatException(e)
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