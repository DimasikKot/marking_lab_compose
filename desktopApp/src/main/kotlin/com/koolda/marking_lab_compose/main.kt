package com.koolda.marking_lab_compose

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "marking_lab_compose",
    ) {
        App()
    }
}