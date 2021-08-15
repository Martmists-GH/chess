package com.martmists.chess.utilities


// Source: Android, adapted for this project
class LruCache<K, V>(var maxSize: Int) {
    private val map = LinkedHashMap<K, V>(0, 0.75f, true)
    private var size = 0
    private var putCount = 0
    private var createCount = 0
    private var evictionCount = 0
    private var hitCount = 0
    private var missCount = 0

    fun resize(maxSize: Int) {
        require(maxSize > 0) { "maxSize <= 0" }
        synchronized(this) { this.maxSize = maxSize }
        trimToSize(maxSize)
    }

    operator fun get(key: K): V? {
        if (key == null) {
            throw NullPointerException("key == null")
        }
        var mapValue: V?
        synchronized(this) {
            mapValue = map[key]
            if (mapValue != null) {
                hitCount++
                return mapValue
            }
            missCount++
        }

        val createdValue = create(key) ?: return null
        synchronized(this) {
            createCount++
            mapValue = map.put(key, createdValue)
            if (mapValue != null) {
                // There was a conflict so undo that last put
                map.put(key, mapValue!!)
            } else {
                size += safeSizeOf(key, createdValue)
            }
        }
        return if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue)
            mapValue
        } else {
            trimToSize(maxSize)
            createdValue
        }
    }

    fun getOrPut(key: K, callback: () -> V): V {
        if (key == null) {
            throw NullPointerException("key == null")
        }
        var mapValue: V?
        synchronized(this) {
            mapValue = map[key]
            if (mapValue != null) {
                hitCount++
                return mapValue!!
            }
            missCount++
        }

        val createdValue = callback()
        synchronized(this) {
            createCount++
            mapValue = map.put(key, createdValue)
            if (mapValue != null) {
                // There was a conflict so undo that last put
                map.put(key, mapValue!!)
            } else {
                size += safeSizeOf(key, createdValue)
            }
        }
        return if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue)
            mapValue!!
        } else {
            trimToSize(maxSize)
            createdValue
        }
    }

    fun put(key: K?, value: V?): V? {
        if (key == null || value == null) {
            throw NullPointerException("key == null || value == null")
        }
        var previous: V?
        synchronized(this) {
            putCount++
            size += safeSizeOf(key, value)
            previous = map.put(key, value)
            if (previous != null) {
                size -= safeSizeOf(key, previous!!)
            }
        }
        if (previous != null) {
            entryRemoved(false, key, previous!!, value)
        }
        trimToSize(maxSize)
        return previous
    }

    fun trimToSize(maxSize: Int) {
        while (true) {
            var key: K
            var value: V
            check(!(size < 0 || map.isEmpty() && size != 0)) {
                (javaClass.name
                        + ".sizeOf() is reporting inconsistent results!")
            }
            if (size <= maxSize) {
                break
            }
            val (key1, value1) = map.entries.first()
            key = key1
            value = value1
            map.remove(key)
            size -= safeSizeOf(key, value)
            evictionCount++
            entryRemoved(true, key, value, null)
        }
    }

    fun remove(key: K?): V? {
        if (key == null) {
            throw NullPointerException("key == null")
        }
        var previous: V?
        synchronized(this) {
            previous = map.remove(key)
            if (previous != null) {
                size -= safeSizeOf(key, previous!!)
            }
        }
        if (previous != null) {
            entryRemoved(false, key, previous!!, null)
        }
        return previous
    }

    protected fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {}

    protected fun create(key: K): V? {
        return null
    }

    private fun safeSizeOf(key: K, value: V): Int {
        val result = sizeOf(key, value)
        check(result >= 0) { "Negative size: $key=$value" }
        return result
    }

    private fun sizeOf(key: K, value: V): Int {
        return 1
    }

    fun evictAll() {
        trimToSize(-1) // -1 will evict 0-sized elements
    }

    operator fun set(key: K, value: V) {
        put(key, value)
    }
}