/*
 * 橘瓣 OrangeChat
 * MCP OAuth loopback callback support, adapted from the upstream RikkaHub flow.
 */
package me.rerere.rikkahub.data.ai.mcp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.time.Duration

internal data class McpOAuthLoopbackCallback(
    val code: String?,
    val state: String?,
    val error: String?,
    val errorDescription: String?,
)

/** Temporary localhost OAuth callback server, bound only to IPv4 loopback. */
internal class McpOAuthLoopbackCallbackServer(
    private val port: Int = 52134,
    private val path: String = "/oauth/callback",
) {
    init {
        require(port in 1024..65535)
        require(path.startsWith("/") && !path.contains("?") && !path.contains("#"))
    }

    suspend fun openSession(expectedState: String): Session = withContext(Dispatchers.IO) {
        require(expectedState.isNotBlank())
        Session(
            expectedState,
            ServerSocket(port, 1, InetAddress.getByName("127.0.0.1")).apply {
                soTimeout = 1_000
            },
        )
    }

    fun redirectUri(): String = "http://127.0.0.1:$port$path"

    inner class Session internal constructor(
        private val expectedState: String,
        private val server: ServerSocket,
    ) {
        private val result = CompletableDeferred<McpOAuthLoopbackCallback>()

        suspend fun await(timeout: Duration): McpOAuthLoopbackCallback? =
            withContext(Dispatchers.IO) {
                try {
                    withTimeoutOrNull(timeout) {
                        while (!result.isCompleted) {
                            val socket = try {
                                server.accept()
                            } catch (_: java.net.SocketTimeoutException) {
                                continue
                            } catch (_: SocketException) {
                                break
                            }
                            handle(socket)
                        }
                        result.await()
                    }
                } finally {
                    close()
                }
            }

        fun close() {
            runCatching { server.close() }
            result.cancel()
        }

        private fun handle(socket: Socket) {
            socket.use { s ->
                s.soTimeout = 5_000
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.US_ASCII))
                val requestLine = reader.readLine().orEmpty()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                }
                val target = requestLine.split(' ').getOrNull(1).orEmpty()
                val query = target.substringAfter('?', "")
                val params = query.split('&').asSequence()
                    .filter { it.isNotBlank() }
                    .mapNotNull { pair ->
                        val parts = pair.split('=', limit = 2)
                        if (parts.size == 2) urlDecode(parts[0]) to urlDecode(parts[1]) else null
                    }.toMap()
                val valid = target.substringBefore('?') == path && params["state"] == expectedState
                val callback = if (valid) {
                    McpOAuthLoopbackCallback(
                        code = params["code"],
                        state = params["state"],
                        error = params["error"],
                        errorDescription = params["error_description"],
                    )
                } else null
                val html = if (callback != null) {
                    "<html><body><h2>Authorization complete</h2><p>You can close this tab.</p></body></html>"
                } else {
                    "<html><body><h2>Authorization callback rejected</h2><p>Return to the app and try again.</p></body></html>"
                }
                val writer = OutputStreamWriter(s.getOutputStream(), Charsets.US_ASCII)
                writer.write("HTTP/1.1 ${if (callback != null) "200 OK" else "400 Bad Request"}\r\n")
                writer.write("Content-Type: text/html; charset=utf-8\r\n")
                writer.write("Cache-Control: no-store\r\n")
                writer.write("Content-Length: ${html.toByteArray(Charsets.UTF_8).size}\r\n")
                writer.write("Connection: close\r\n\r\n$html")
                writer.flush()
                if (callback != null) result.complete(callback)
            }
        }
    }

    private fun urlDecode(value: String): String =
        java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
}
