package com.example.linkgrab

import java.net.ServerSocket
import java.util.concurrent.Executors

/** Local HTTP media source for the real yt-dlp executable, no external service. */
class FixtureServer(private val bytes: ByteArray, private val throttleMs: Long = 0) : AutoCloseable {
    private val socket = ServerSocket(0)
    private val pool = Executors.newCachedThreadPool()
    val url = "http://127.0.0.1:${socket.localPort}/fixture.mp4"
    init {
        pool.submit {
            while (!socket.isClosed) runCatching {
                val client = socket.accept()
                pool.submit {
                    client.use {
                        val reader = it.getInputStream().bufferedReader()
                        val request = reader.readLine().orEmpty()
                        var start = 0
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            if (line.startsWith("Range:", true)) start = line.substringAfter("bytes=").substringBefore('-').toIntOrNull() ?: 0
                        }
                        start = start.coerceIn(0, bytes.lastIndex)
                        val count = bytes.size - start
                        val header = buildString {
                            append(if (start > 0) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
                            append("Content-Type: video/mp4\r\nContent-Length: $count\r\nAccept-Ranges: bytes\r\n")
                            if (start > 0) append("Content-Range: bytes $start-${bytes.lastIndex}/${bytes.size}\r\n")
                            append("Connection: close\r\n\r\n")
                        }
                        it.getOutputStream().use { out ->
                            out.write(header.toByteArray())
                            if (!request.startsWith("HEAD")) {
                                var offset = start
                                while (offset < bytes.size) {
                                    val length = minOf(512, bytes.size - offset)
                                    out.write(bytes, offset, length); out.flush(); offset += length
                                    if (throttleMs > 0) Thread.sleep(throttleMs)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    override fun close() { socket.close(); pool.shutdownNow() }
}
