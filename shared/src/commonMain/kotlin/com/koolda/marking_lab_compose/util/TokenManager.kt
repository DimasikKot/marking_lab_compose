package com.koolda.marking_lab_compose.util

object TokenManager {
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USERNAME = "username"

    var accessToken: String?
        get() = LocalStorage.getItem(KEY_ACCESS_TOKEN)
        set(value) {
            if (value != null) {
                LocalStorage.setItem(KEY_ACCESS_TOKEN, value)
            } else {
                LocalStorage.removeItem(KEY_ACCESS_TOKEN)
            }
        }

    var username: String?
        get() = LocalStorage.getItem(KEY_USERNAME)
        set(value) {
            if (value != null) {
                LocalStorage.setItem(KEY_USERNAME, value)
            } else {
                LocalStorage.removeItem(KEY_USERNAME)
            }
        }

    fun clear() {
        LocalStorage.removeItem(KEY_ACCESS_TOKEN)
        LocalStorage.removeItem(KEY_USERNAME)
    }

    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrEmpty()
}

expect object LocalStorage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
}
