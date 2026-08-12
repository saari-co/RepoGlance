package co.saari.repoglance.data

import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
) {
    override fun toString(): String = "HttpRequest(method=$method, url=<redacted>, headers=<redacted>, body=<redacted>)"
}

class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val body: String,
) {
    fun header(name: String): String? = headers.entries
        .firstOrNull { (headerName, _) -> headerName.equals(name, ignoreCase = true) }
        ?.value
        ?.firstOrNull()

    override fun toString(): String = "HttpResponse(statusCode=$statusCode, headers=<redacted>, body=<redacted>)"
}

fun interface HttpTransport {
    fun execute(request: HttpRequest): HttpResponse
}

class UrlConnectionTransport : HttpTransport {
    override fun execute(request: HttpRequest): HttpResponse {
        val url = URL(request.url)
        require(url.protocol == "https" && url.host in ALLOWED_HOSTS) { "Blocked network destination" }
        val connection = url.openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = request.method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            request.headers.forEach(connection::setRequestProperty)
            request.body?.let { body ->
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            val responseStream = if (status in 200..299) connection.inputStream else connection.errorStream
            val bytes = responseStream?.use(::readBounded) ?: ByteArray(0)
            return HttpResponse(
                statusCode = status,
                headers = connection.headerFields,
                body = bytes.toString(StandardCharsets.UTF_8),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun readBounded(stream: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_RESPONSE_BYTES) { "GitHub response is too large" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        val ALLOWED_HOSTS = setOf("github.com", "api.github.com")
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024
    }
}
