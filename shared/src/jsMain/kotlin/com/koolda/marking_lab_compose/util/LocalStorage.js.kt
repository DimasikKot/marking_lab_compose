package com.koolda.marking_lab_compose.util

import org.w3c.dom.Storage
import org.w3c.dom.get
import org.w3c.dom.set

actual object LocalStorage {
    private val storage: Storage by lazy {
        web.storage.localStorage
    }
    
    actual fun getItem(key: String): String? = storage[key]
    
    actual fun setItem(key: String, value: String) {
        storage[key] = value
    }
    
    actual fun removeItem(key: String) {
        storage.removeItem(key)
    }
}
