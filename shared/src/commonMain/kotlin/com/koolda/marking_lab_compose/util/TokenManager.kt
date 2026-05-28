package com.koolda.marking_lab_compose.util

object TokenManager {
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USERNAME = "username"
    
    var accessToken: String?
        get() = localStorage.getItem(KEY_ACCESS_TOKEN)
        set(value) {
            if (value != null) {
                localStorage.setItem(KEY_ACCESS_TOKEN, value)
            } else {
                localStorage.removeItem(KEY_ACCESS_TOKEN)
            }
        }
    
    var username: String?
        get() = localStorage.getItem(KEY_USERNAME)
        set(value) {
            if (value != null) {
                localStorage.setItem(KEY_USERNAME, value)
            } else {
                localStorage.removeItem(KEY_USERNAME)
            }
        }
    
    fun clear() {
        localStorage.removeItem(KEY_ACCESS_TOKEN)
        localStorage.removeItem(KEY_USERNAME)
    }
    
    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrEmpty()
}

expect object localStorage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
}
