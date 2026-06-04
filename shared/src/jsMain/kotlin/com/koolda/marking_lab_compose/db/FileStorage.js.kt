package com.koolda.marking_lab_compose.db

import com.koolda.marking_lab_compose.util.LocalStorage

actual object FileStorage {

    private fun storageKey(localPath: String) = "fs_$localPath"

    actual fun saveFile(projectId: Int, fileName: String, bytes: ByteArray): String {
        val localPath = "${projectId}_${fileName}"
        // Кодируем байты как строку вида "12,34,255,..."
        val encoded = bytes.joinToString(",") { (it.toInt() and 0xFF).toString() }
        LocalStorage.setItem(storageKey(localPath), encoded)
        return localPath
    }

    actual fun loadFile(localPath: String): ByteArray? {
        val encoded = LocalStorage.getItem(storageKey(localPath)) ?: return null
        return runCatching {
            val parts = encoded.split(",")
            ByteArray(parts.size) { i -> parts[i].trim().toInt().toByte() }
        }.getOrNull()
    }

    actual fun deleteFile(localPath: String) {
        LocalStorage.removeItem(storageKey(localPath))
    }
}
