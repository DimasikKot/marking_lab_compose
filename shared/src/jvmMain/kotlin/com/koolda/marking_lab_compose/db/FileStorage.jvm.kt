package com.koolda.marking_lab_compose.db

import java.io.File

actual object FileStorage {

    private fun projectDir(projectId: Int): File =
        File(System.getProperty("user.home"), ".marking_lab/files/$projectId")
            .also { it.mkdirs() }

    actual fun saveFile(projectId: Int, fileName: String, bytes: ByteArray): String {
        val file = File(projectDir(projectId), fileName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    actual fun loadFile(localPath: String): ByteArray? {
        val file = File(localPath)
        return if (file.exists()) file.readBytes() else null
    }

    actual fun deleteFile(localPath: String) {
        File(localPath).delete()
    }
}
