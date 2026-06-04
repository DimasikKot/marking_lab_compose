package com.koolda.marking_lab_compose.db

/**
 * Платформозависимое хранилище байтов файлов.
 *
 * JVM  → ~/.marking_lab/files/{projectId}/{fileName}  (обычные файлы на диске)
 * JS   → localStorage, base64-закодированные байты
 * WASM → заглушка
 */
expect object FileStorage {
    /** Сохранить байты файла и вернуть localPath — ключ для последующего доступа. */
    fun saveFile(projectId: Int, fileName: String, bytes: ByteArray): String

    /** Загрузить байты по localPath, полученному ранее от [saveFile]. */
    fun loadFile(localPath: String): ByteArray?

    /** Удалить файл. */
    fun deleteFile(localPath: String)
}
