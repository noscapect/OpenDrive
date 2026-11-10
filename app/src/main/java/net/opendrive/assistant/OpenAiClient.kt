package net.opendrive.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AssistantApiException(val status: Int) : Exception("OpenAI request failed ($status)")

/** Fixed HTTPS destination; credentials and request/response bodies are never logged. */
internal class OpenAiClient(private val key: String) {
    suspend fun respond(body: JSONObject): JSONObject = post("responses", "application/json", body.toString().toByteArray())

    suspend fun transcribe(wav: ByteArray): String {
        val boundary = "OpenDrive-${UUID.randomUUID()}"
        val data = ByteArrayOutputStream()
        fun part(text: String) { data.write(text.toByteArray(Charsets.UTF_8)) }
        part("--$boundary\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\ngpt-transcribe\r\n")
        part("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\nContent-Type: audio/wav\r\n\r\n")
        data.write(wav)
        part("\r\n--$boundary--\r\n")
        return post("audio/transcriptions", "multipart/form-data; boundary=$boundary", data.toByteArray()).getString("text").trim()
    }

    private suspend fun post(path: String, contentType: String, bytes: ByteArray): JSONObject = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val connection = URL("https://api.openai.com/v1/$path").openConnection() as HttpURLConnection
            continuation.invokeOnCancellation { connection.disconnect() }
            try {
                connection.requestMethod = "POST"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 45_000
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $key")
                connection.setRequestProperty("Content-Type", contentType)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
                val status = connection.responseCode
                if (status !in 200..299) throw AssistantApiException(status)
                val result = connection.inputStream.use { stream ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        check(output.size() + count <= 1_048_576) { "Response too large" }
                        output.write(buffer, 0, count)
                    }
                    JSONObject(output.toString(Charsets.UTF_8.name()))
                }
                if (continuation.isActive) continuation.resume(result)
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resumeWithException(e)
            } finally { connection.disconnect() }
        }
    }
}
