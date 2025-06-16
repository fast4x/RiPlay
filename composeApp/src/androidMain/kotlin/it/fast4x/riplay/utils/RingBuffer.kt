package it.fast4x.riplay.utils

import android.net.Uri
import java.time.Clock
import java.time.Instant


class RingBufferPrevious<T>(val size: Int, init: (index: Int) -> T) {
    private val list = MutableList(size, init)

    private var index = 0

    fun getOrNull(index: Int): T? = list.getOrNull(index)

    fun append(element: T) = list.set(index++ % size, element)
}


open class RingBuffer<T>(val size: Int, private val init: (index: Int) -> T) : Iterable<T> {
    private val list = MutableList(size, init)

    @get:Synchronized
    @set:Synchronized
    private var index = 0

    operator fun get(index: Int) = list.getOrNull(index)
    operator fun plusAssign(element: T) {
        list[index++ % size] = element
    }

    override fun iterator() = list.iterator()

    fun clear() = list.indices.forEach {
        list[it] = init(it)
    }
}


class UriCache<Key : Any, Meta>(size: Int = 16) {
    private val buffer = RingBuffer<CachedUri<Key, Meta>?>(size) { null }

    data class CachedUri<Key, Meta> internal constructor(
        val key: Key,
        val meta: Meta,
        val uri: Uri,
        val validUntil: Instant?
    )

    operator fun get(key: Key) = buffer.find {
        it != null &&
                it.key == key &&
                (it.validUntil == null || it.validUntil > Clock.system(
                    Clock.systemUTC().zone
                ).instant())
    }

    fun push(
        key: Key,
        meta: Meta,
        uri: Uri,
        validUntil: Instant?
    ) {
        if (validUntil != null && validUntil <= Clock.system(
                Clock.systemUTC().zone
            ).instant()) return

        buffer += CachedUri(key, meta, uri, validUntil)
    }

    fun clear() = buffer.clear()
}