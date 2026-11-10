package net.opendrive.features

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CompanionServerTest {
    private fun request(server: CompanionServer, path: String, token: String? = null, body: String? = null): Pair<Int, String> {
        val connection = URL("http://127.0.0.1:${server.port}$path").openConnection() as HttpURLConnection
        connection.connectTimeout = 2000; connection.readTimeout = 2000
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        if (body != null) {
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        return try {
            val status = connection.responseCode
            val text = (if (status >= 400) connection.errorStream else connection.inputStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            status to text
        } finally { connection.disconnect() }
    }
    @Test fun statusAndActionsRequireTheRandomTokenButTheLandingPageDoesNotExposeIt() {
        CompanionServer {}.use { server ->
            server.publish(JSONObject().put("title", "Private music title")); server.start()
            val page = request(server, "/")
            assertEquals(200, page.first); assertFalse(page.second.contains(server.token))
            assertEquals(401, request(server, "/api/status").first)
            assertEquals(401, request(server, "/api/status", "wrong").first)
            val status = request(server, "/api/status", server.token)
            assertEquals(200, status.first); assertEquals("Private music title", JSONObject(status.second).getString("title"))
            assertEquals(48, server.token.length)
        }
    }
    @Test fun onlyValidatedBoundedActionsReachTheOwnerAndRapidRepeatsAreRejected() {
        val received = java.util.concurrent.LinkedBlockingQueue<JSONObject>()
        CompanionServer { received.offer(it) }.use { server ->
            server.start()
            assertEquals(400, request(server, "/api/action", server.token, "{\"action\":\"shell\"}").first)
            assertTrue(received.isEmpty())
            assertEquals(202, request(server, "/api/action", server.token, "{\"action\":\"destination\",\"destination\":\"Utrecht\"}").first)
            assertEquals("Utrecht", received.poll(2, java.util.concurrent.TimeUnit.SECONDS)!!.getString("destination"))
            assertEquals(429, request(server, "/api/action", server.token, "{\"action\":\"next\"}").first)
            assertTrue(received.isEmpty())
        }
    }
    @Test fun publicAddressesAndArbitraryPhoneCommandsAreNotAllowed() {
        assertTrue(CompanionServer.privateAddress(InetAddress.getByName("192.168.1.2")))
        assertFalse(CompanionServer.privateAddress(InetAddress.getByName("8.8.8.8")))
        assertFalse(CompanionServer.validAction(JSONObject().put("action", "destination").put("destination", "x".repeat(301))))
        assertFalse(CompanionServer.validAction(JSONObject().put("action", "next").put("extra", true)))
        assertFalse(CompanionServer.validAction(JSONObject().put("action", "send_message")))
    }
}
