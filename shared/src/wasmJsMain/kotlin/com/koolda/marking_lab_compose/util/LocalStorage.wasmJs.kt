package com.koolda.marking_lab_compose.util

import kotlinx.browser.window
import org.w3c.dom.Storage

actual object LocalStorage {

    private val storage: Storage
        get() = window.localStorage

    actual fun getItem(key: String): String? =
        storage.getItem(key)

    actual fun setItem(key: String, value: String) {
        storage.setItem(key, value)
    }

    actual fun removeItem(key: String) {
        storage.removeItem(key)
    }
}