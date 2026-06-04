package com.koolda.marking_lab_compose.util

expect object FilePicker {
    suspend fun pickFile(): Pair<String, ByteArray>?
    suspend fun saveFile(fileName: String, bytes: ByteArray)
}
