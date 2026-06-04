package com.koolda.marking_lab_compose.util

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import kotlin.coroutines.resume

actual object FilePicker {
    actual suspend fun pickFile(): Pair<String, ByteArray>? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            SwingUtilities.invokeLater {
                if (!cont.isActive) return@invokeLater
                val chooser = JFileChooser()
                val result = chooser.showOpenDialog(null)
                if (!cont.isActive) return@invokeLater
                if (result == JFileChooser.APPROVE_OPTION) {
                    val file = chooser.selectedFile
                    cont.resume(Pair(file.name, file.readBytes()))
                } else {
                    cont.resume(null)
                }
            }
        }
    }

    actual suspend fun saveFile(fileName: String, bytes: ByteArray): Unit = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            SwingUtilities.invokeLater {
                if (!cont.isActive) return@invokeLater
                val chooser = JFileChooser()
                chooser.selectedFile = java.io.File(fileName)
                val result = chooser.showSaveDialog(null)
                if (!cont.isActive) return@invokeLater
                if (result == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile.writeBytes(bytes)
                }
                cont.resume(Unit)
            }
        }
    }
}
