package com.koolda.marking_lab_compose.util

import java.util.prefs.Preferences

actual object localStorage {
    private val prefs: Preferences by lazy {
        Preferences.userNodeForPackage(TokenManager::class.java)
    }
    
    actual fun getItem(key: String): String? = prefs.get(key, null)
    
    actual fun setItem(key: String, value: String) {
        prefs.put(key, value)
    }
    
    actual fun removeItem(key: String) {
        prefs.remove(key)
    }
}
