package com.koolda.marking_lab_compose.db

import com.koolda.marking_lab_compose.api.FileListResponse
import com.koolda.marking_lab_compose.api.ModelListResponse
import com.koolda.marking_lab_compose.api.ProjectDbResponse

expect object LocalDb {
    // ── Projects ──────────────────────────────────────────────────────────────
    fun getProjects(): List<ProjectDbResponse>
    fun saveProjects(projects: List<ProjectDbResponse>)
    fun saveProject(project: ProjectDbResponse)
    fun deleteProject(projectId: Int)

    // ── Models ────────────────────────────────────────────────────────────────
    fun getModels(projectId: Int): List<ModelListResponse>
    fun saveModels(projectId: Int, models: List<ModelListResponse>)
    fun saveModel(projectId: Int, model: ModelListResponse)
    fun deleteModel(projectId: Int, modelId: Int)

    // ── Files ─────────────────────────────────────────────────────────────────
    /** Список файлов из локальной БД (включает localPath). */
    fun getFiles(projectId: Int): List<FileListResponse>

    /**
     * Сохранить список файлов, полученных с сервера.
     * Уже сохранённые localPath не перезаписываются.
     */
    fun saveFiles(projectId: Int, files: List<FileListResponse>)

    /**
     * Сохранить/обновить одну запись о файле с указанием localPath
     * (вызывается после реальной загрузки файла на устройство).
     */
    fun saveFileRecord(projectId: Int, file: FileListResponse, localPath: String)

    fun deleteFileRecord(projectId: Int, fileId: Int)

    /** Путь к локальной копии файла, или null если файл не сохранён. */
    fun getLocalPath(projectId: Int, fileId: Int): String?
}
