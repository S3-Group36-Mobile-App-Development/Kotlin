package com.zenmind.kotlin.features.protocols.data

import androidx.collection.LruCache
import com.zenmind.kotlin.features.protocols.model.Protocol

class ProtocolCache(maxSize: Int = 5) {
    private val cache = LruCache<Long, Protocol>(maxSize)

    fun get(id: Long): Protocol? = cache[id]

    fun save(protocol: Protocol) {
        cache.put(protocol.id, protocol)
    }

    fun remove(id: Long) {
        cache.remove(id)
    }
}
