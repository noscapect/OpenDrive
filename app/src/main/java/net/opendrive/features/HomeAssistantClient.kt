package net.opendrive.features

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class HomeAssistantClient(private val base: String, private val token: String) {
    init { require(FeatureConfig.validHomeUrl(base)) }
    suspend fun ping() { request("GET", "") }
    suspend fun state(entity: String): HomeValue {
        require(FeatureConfig.validEntity(entity))
        val o = JSONObject(request("GET", "states/$entity"))
        return HomeValue(o.getString("state"), o.optJSONObject("attributes")?.optString("unit_of_measurement").orEmpty(), System.currentTimeMillis())
    }
    suspend fun action(action: HomeAction) {
        require(action.service in FeatureConfig.HOME_SERVICES && FeatureConfig.validEntity(action.entity) &&
            action.entity.substringBefore('.') == action.service.substringBefore('.'))
        request("POST", "services/${action.service.replace('.', '/')}", JSONObject().put("entity_id", action.entity).toString())
    }
    private suspend fun request(method: String, path: String, body: String? = null): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val connection = URL(base.trimEnd('/') + "/api/" + path).openConnection() as HttpURLConnection
            continuation.invokeOnCancellation { connection.disconnect() }
            try {
                connection.instanceFollowRedirects = false
                connection.requestMethod = method
                connection.connectTimeout = 5000; connection.readTimeout = 5000
                connection.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    val bytes = body.toByteArray(Charsets.UTF_8)
                    connection.setFixedLengthStreamingMode(bytes.size)
                    connection.outputStream.use { it.write(bytes) }
                }
                check(connection.responseCode in 200..299) { "Home Assistant HTTP ${connection.responseCode}" }
                val bytes = connection.inputStream.use { stream ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        check(output.size() + read <= 262_144) { "Response too large" }
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                }
                check(bytes.size <= 262_144) { "Response too large" }
                if (continuation.isActive) continuation.resume(String(bytes, Charsets.UTF_8))
            } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
            finally { connection.disconnect() }
        }
    }
}
