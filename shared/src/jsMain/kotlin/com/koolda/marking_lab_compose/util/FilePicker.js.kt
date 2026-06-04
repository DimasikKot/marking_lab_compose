package com.koolda.marking_lab_compose.util

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader
import kotlin.coroutines.resume

actual object FilePicker {
    actual suspend fun pickFile(): Pair<String, ByteArray>? = suspendCancellableCoroutine { cont ->
        val input = document.createElement("input").unsafeCast<HTMLInputElement>()
        input.type = "file"
        input.addEventListener("change", { _ ->
            val file = input.files?.item(0)
            document.body?.removeChild(input)
            if (file != null) {
                val reader = FileReader()
                reader.onload = { _ ->
                    val buffer = reader.result.unsafeCast<ArrayBuffer>()
                    val uint8 = Uint8Array(buffer)
                    val bytes = ByteArray(uint8.length) { i -> uint8[i].toByte() }
                    if (cont.isActive) cont.resume(Pair(file.name, bytes))
                }
                reader.onerror = { _ ->
                    if (cont.isActive) cont.resume(null)
                }
                reader.readAsArrayBuffer(file)
            } else {
                if (cont.isActive) cont.resume(null)
            }
        })
        document.body?.appendChild(input)
        input.click()
    }

    actual suspend fun saveFile(fileName: String, bytes: ByteArray) {
        val uint8 = Uint8Array(bytes.size)
        bytes.forEachIndexed { i, b -> uint8[i] = (b.toInt() and 0xFF).toShort() }
        val blob = Blob(arrayOf(uint8), BlobPropertyBag("application/octet-stream"))
        val url = URL.createObjectURL(blob)
        val a = document.createElement("a").unsafeCast<HTMLAnchorElement>()
        a.href = url
        a.setAttribute("download", fileName)
        document.body?.appendChild(a)
        a.click()
        document.body?.removeChild(a)
        URL.revokeObjectURL(url)
    }
}
