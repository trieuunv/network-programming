package org.np.utils

import java.util.Collections

class BiMap<K, V> {
    private val keyToValue = Collections.synchronizedMap(mutableMapOf<K, V>())
    private val valueToKey = Collections.synchronizedMap(mutableMapOf<V, K>())

    fun put(key: K, value: V) {
        keyToValue[key] = value
        valueToKey[value] = key
    }

    fun removeByKey(key: K) {
        val value = keyToValue.remove(key)
        if (value != null) valueToKey.remove(value)
    }

    fun removeByValue(value: V) {
        val key = valueToKey.remove(value)
        if (key != null) keyToValue.remove(key)
    }

    fun getByKey(key: K): V? = keyToValue[key]
    fun getByValue(value: V): K? = valueToKey[value]

    fun containsKey(key: K): Boolean = keyToValue.containsKey(key)
    fun containsValue(value: V): Boolean = valueToKey.containsKey(value)
}
