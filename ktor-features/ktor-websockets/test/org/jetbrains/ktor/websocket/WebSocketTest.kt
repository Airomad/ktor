package org.jetbrains.ktor.websocket

import kotlinx.coroutines.experimental.channels.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.cio.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.testing.*
import org.jetbrains.ktor.util.*
import org.junit.*
import org.junit.rules.*
import java.nio.*
import java.time.*
import java.util.concurrent.*
import kotlin.test.*

class WebSocketTest {
    @get:Rule
    val timeout = Timeout(30, TimeUnit.SECONDS)

    @Test
    fun testHello() {
        withTestApplication {
            application.install(WebSockets)
            application.routing {
                webSocketRaw("/echo") {
                    incoming.consumeEach { frame ->
                        if (!frame.frameType.controlFrame) {
                            send(frame.copy())
                            flush()
                            terminate()
                        }
                    }
                }
            }

            handleWebSocket("/echo") {
                bodyBytes = hex("""
                    0x81 0x05 0x48 0x65 0x6c 0x6c 0x6f
                """.trimHex())
            }.let { call ->
                call.awaitWebSocket(Duration.ofSeconds(10))
                assertEquals("810548656c6c6f", hex(call.response.byteContent!!))
            }
        }
    }

    @Test
    fun testMasking() {
        withTestApplication {
            application.install(WebSockets)
            application.routing {
                webSocketRaw("/echo") {
                    masking = true

                    incoming.consumeEach { frame ->
                        if (!frame.frameType.controlFrame) {
                            assertEquals("Hello", frame.buffer.copy().array().toString(Charsets.UTF_8))
                            send(frame.copy())
                            flush()
                            terminate()
                        }
                    }
                }
            }

            handleWebSocket("/echo") {
                bodyBytes = hex("""
                    0x81 0x85 0x37 0xfa 0x21 0x3d 0x7f 0x9f 0x4d 0x51 0x58
                """.trimHex())
            }.let { call ->
                call.awaitWebSocket(Duration.ofSeconds(10))

                val bb = ByteBuffer.wrap(call.response.byteContent!!)
                assertEquals(11, bb.remaining())
                val parser = FrameParser()
                parser.frame(bb)

                assertTrue { parser.bodyReady }
                assertTrue { parser.mask }
                val key = parser.maskKey!!

                SimpleFrameCollector(NoPool).use { collector ->
                    collector.start(parser.length.toInt(), bb)

                    assertFalse { collector.hasRemaining }
                    assertEquals("Hello", collector.take(key).buffer.copy().array().toString(Charsets.UTF_8))
                }
            }
        }
    }

    @Test
    fun testSendClose() {
        withTestApplication {
            application.install(WebSockets)

            application.routing {
                webSocket("/echo") {
                    incoming.consumeEach {  }
                }
            }

            handleWebSocket("/echo") {
                bodyBytes = hex("""
                    0x88 0x02 0xe8 0x03
                """.trimHex())
            }.let { call ->
                call.awaitWebSocket(Duration.ofSeconds(10))
                assertEquals("0x88 0x02 0xe8 0x03".trimHex(), hex(call.response.byteContent!!))
            }
        }
    }

    @Test
    fun testParameters() {
        withTestApplication {
            application.install(WebSockets)

            application.routing {
                webSocket("/{p}") {
                    outgoing.send(Frame.Text(call.parameters["p"] ?: "null"))
                }
            }

            handleWebSocket("/aaa") {}.let { call ->
                call.awaitWebSocket(Duration.ofSeconds(10))
                val p = FrameParser()
                val bb = ByteBuffer.wrap(call.response.byteContent)
                p.frame(bb)

                assertEquals(FrameType.TEXT, p.frameType)
                assertTrue { p.bodyReady }

                val bytes = ByteArray(p.length.toInt())
                bb.get(bytes)

                assertEquals("aaa", bytes.toString(Charsets.ISO_8859_1))
            }
        }
    }

    private fun String.trimHex() = replace("\\s+".toRegex(), "").replace("0x", "")
}
