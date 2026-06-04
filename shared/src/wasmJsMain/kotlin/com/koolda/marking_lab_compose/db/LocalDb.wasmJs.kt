package com.koolda.marking_lab_compose.db

import com.koolda.marking_lab_compose.api.FileListResponse
import com.koolda.marking_lab_compose.api.ModelListResponse
import com.koolda.marking_lab_compose.api.ProjectDbResponse

actual object LocalDb {
    actual fun getProjects(): List<ProjectDbResponse> = emptyList()
    actual fun saveProjects(projects: List<ProjectDbResponse>) {}
    actual fun saveProject(project: ProjectDbResponse) {}
    actual fun deleteProject(projectId: Int) {}

    actual fun getModels(projectId: Int): List<ModelListResponse> = emptyList()
    actual fun saveModels(projectId: Int, models: List<ModelListResponse>) {}
    actual fun saveModel(projectId: Int, model: ModelListResponse) {}
    actual fun deleteModel(projectId: Int, modelId: Int) {}

    actual fun getFiles(projectId: Int): List<FileListResponse> = emptyList()
    actual fun saveFiles(projectId: Int, files: List<FileListResponse>) {}
    actual fun saveFileRecord(projectId: Int, file: FileListResponse, localPath: String) {}
    actual fun deleteFileRecord(projectId: Int, fileId: Int) {}
    actual fun getLocalPath(projectId: Int, fileId: Int): String? = null
}
