package com.koolda.marking_lab_compose.db

actual object FileStorage {
    actual fun saveFile(projectId: Int, fileName: String, bytes: ByteArray): String = ""
    actual fun loadFile(localPath: String): ByteArray? = null
    actual fun deleteFile(localPath: String) {}
}
