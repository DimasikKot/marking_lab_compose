package com.koolda.marking_lab_compose.util

// WasmJS file operations are not yet supported in this target.
// The desktop (JVM) and web JS targets have full implementations.
actual object FilePicker {
    actual suspend fun pickFile(): Pair<String, ByteArray>? = null
    actual suspend fun saveFile(fileName: String, bytes: ByteArray) {}
}
