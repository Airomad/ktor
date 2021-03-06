package org.jetbrains.ktor.netty

import io.netty.handler.codec.http.*
import org.jetbrains.ktor.util.*

class NettyApplicationRequestHeaders(request: HttpRequest) : ValuesMap {
    private val headers: HttpHeaders = request.headers()
    override fun get(name: String): String? = headers.get(name)
    override fun contains(name: String): Boolean = headers.contains(name)
    override fun contains(name: String, value: String): Boolean = headers.contains(name, value, true)
    override fun getAll(name: String): List<String> = headers.getAll(name)
    override fun forEach(body: (String, List<String>) -> Unit) {
        val names = headers.names()
        names.forEach { body(it, headers.getAll(it)) }
    }

    override fun entries(): Set<Map.Entry<String, List<String>>> {
        val names = headers.names()
        return names.mapTo(LinkedHashSet(names.size)) {
            object : Map.Entry<String, List<String>> {
                override val key: String get() = it
                override val value: List<String> get() = headers.getAll(it)
            }
        }
    }

    override fun isEmpty(): Boolean = headers.isEmpty
    override val caseInsensitiveKey: Boolean get() = true
    override fun names(): Set<String> = headers.names()
}