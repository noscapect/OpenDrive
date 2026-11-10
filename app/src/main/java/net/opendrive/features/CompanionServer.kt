package net.opendrive.features

import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicReference

/** A temporary LAN-only, token-authenticated companion. No account, cloud relay or public binding instruction. */
internal class CompanionServer(private val onAction: (JSONObject) -> Unit) : AutoCloseable {
    private val socket = ServerSocket(0)
    val port get() = socket.localPort
    val token = ByteArray(24).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it.toInt() and 255) }
    private val expires = System.nanoTime() + 30 * 60 * 1_000_000_000L
    private val status = AtomicReference("{}")
    @Volatile private var active = true
    private var lastAction = 0L
    fun publish(value: JSONObject) { status.set(value.toString()) }
    fun start() {
        Thread({
            while (active) {
                try { socket.accept().use { client -> handle(client) } }
                catch (_: Exception) { if (socket.isClosed) break }
            }
        }, "opendrive-passenger").apply { isDaemon = true; start() }
    }
    private fun handle(client: Socket) {
        client.soTimeout = 3000
        if (!privateAddress(client.inetAddress) || System.nanoTime() > expires) {
            respond(client, 403, "text/plain", "Session expired or unavailable"); return
        }
        val input = BufferedInputStream(client.getInputStream())
        var headerSize = 0
        fun line(): String {
            val out = StringBuilder()
            while (true) {
                val byte = input.read()
                check(byte >= 0 && ++headerSize <= 8192)
                if (byte == 10) return out.toString().trimEnd('\r')
                out.append(byte.toChar())
            }
        }
        val request = line().split(' ')
        check(request.size == 3)
        val headers = HashMap<String, String>()
        while (true) {
            val line = line()
            if (line.isEmpty()) break
            check(line.contains(':'))
            val key = line.substringBefore(':').lowercase()
            check(key !in headers)
            headers[key] = line.substringAfter(':').trim()
        }
        if (request[0] == "GET" && request[1] == "/") { respond(client, 200, "text/html; charset=utf-8", PAGE); return }
        val auth = headers["authorization"].orEmpty().removePrefix("Bearer ")
        if (!MessageDigest.isEqual(auth.toByteArray(StandardCharsets.UTF_8), token.toByteArray(StandardCharsets.UTF_8))) {
            respond(client, 401, "text/plain", "Unauthorized"); return
        }
        if (request[0] == "GET" && request[1] == "/api/status") {
            respond(client, 200, "application/json", status.get()); return
        }
        if (request[0] != "POST" || request[1] != "/api/action") { respond(client, 404, "text/plain", "Not found"); return }
        val length = headers["content-length"]?.toIntOrNull()
        if (length == null || length !in 1..2048 || "transfer-encoding" in headers) { respond(client, 400, "text/plain", "Invalid body"); return }
        val bytes = ByteArray(length)
        var received = 0
        while (received < length) {
            val count = input.read(bytes, received, length - received)
            check(count > 0)
            received += count
        }
        val body = runCatching { JSONObject(String(bytes, Charsets.UTF_8)) }.getOrNull()
        if (body == null || !validAction(body)) { respond(client, 400, "text/plain", "Invalid action"); return }
        val now = System.nanoTime()
        if (now - lastAction < 1_000_000_000L) { respond(client, 429, "text/plain", "Please wait"); return }
        lastAction = now
        onAction(body)
        respond(client, 202, "application/json", "{\"accepted\":true}")
    }
    override fun close() { active = false; socket.close() }
    private fun respond(client: Socket, code: Int, type: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code Response\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'self'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'\r\n\r\n"
        client.getOutputStream().apply { write(header.toByteArray(Charsets.US_ASCII)); write(bytes); flush() }
    }
    companion object {
        fun privateAddress(address: InetAddress) = address.isSiteLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            (address.address.size == 16 && address.address[0].toInt() and 254 == 252)
        fun validAction(o: JSONObject): Boolean = when (o.optString("action")) {
            "destination" -> o.keys().asSequence().toSet() == setOf("action", "destination") &&
                o.opt("destination") is String && o.getString("destination").isNotBlank() && o.getString("destination").length <= 300
            "play_pause", "next", "previous" -> o.keys().asSequence().toSet() == setOf("action")
            else -> false
        }
        private val PAGE = """<!doctype html><html lang="nl"><meta name="viewport" content="width=device-width, initial-scale=1"><title>OpenDrive bijrijder</title>
<style>body{font:18px system-ui;background:#101923;color:#eef5fa;max-width:620px;margin:30px auto;padding:20px}button,input{font:inherit;padding:15px;border-radius:12px;margin:6px 0;width:100%;box-sizing:border-box}button{background:#a1e4ca;color:#10231c;border:0}small{color:#aac0d0}#result{min-height:3em}</style>
<h1>OpenDrive bijrijder</h1><p>Stel een bestemming voor of bedien de muziek. De bestuurder bevestigt bestemmingen in de auto.</p><p id="playing">Verbinden…</p>
<input id="destination" maxlength="300" placeholder="Adres of bestemming"><button id="send">Bestemming voorstellen</button><button data-action="previous">Vorige</button><button data-action="play_pause">Afspelen / pauzeren</button><button data-action="next">Volgende</button><p id="result" role="status"></p><small>Deze lokale koppeling verloopt na 30 minuten. Sluit hem in OpenDrive als je klaar bent.</small>
<script>const token=location.hash.slice(1);history.replaceState(null,'','/');const result=document.getElementById('result');async function request(path,body){const response=await fetch(path,{method:body?'POST':'GET',headers:{'Authorization':'Bearer '+token,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});if(!response.ok)throw Error(response.status===401||response.status===403?'Koppeling verlopen. Open een nieuwe link vanuit OpenDrive.':'Niet beschikbaar. Wacht even en probeer opnieuw.');return response.json();}async function act(body){try{await request('/api/action',body);result.textContent=body.action==='destination'?'Voorstel naar de bestuurder gestuurd.':'Muziekbediening aangevraagd.';}catch(e){result.textContent=e.message;}}document.getElementById('send').onclick=()=>act({action:'destination',destination:document.getElementById('destination').value});document.querySelectorAll('[data-action]').forEach(b=>b.onclick=()=>act({action:b.dataset.action}));async function refresh(){try{const s=await request('/api/status');document.getElementById('playing').textContent=s.title||'Geen actieve muziek';}catch(e){document.getElementById('playing').textContent=e.message;}}refresh();setInterval(refresh,5000);</script></html>"""
    }
}
