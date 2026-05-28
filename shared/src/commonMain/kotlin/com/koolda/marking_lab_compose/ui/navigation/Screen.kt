package com.koolda.marking_lab_compose.ui.navigation

sealed class Screen {
    object Home : Screen()
    object Login : Screen()
    object Register : Screen()
    object Projects : Screen()
    data class ProjectDetail(val projectId: Int, val tab: String = "files") : Screen()
    data class FileDetail(val projectId: Int, val fileId: Int) : Screen()
    data class ModelDetail(val projectId: Int, val modelId: Int) : Screen()
    object Models : Screen()
}
