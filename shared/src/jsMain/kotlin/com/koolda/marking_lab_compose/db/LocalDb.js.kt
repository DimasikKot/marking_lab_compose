package com.koolda.marking_lab_compose.db

import com.koolda.marking_lab_compose.api.FileListResponse
import com.koolda.marking_lab_compose.api.ModelListResponse
import com.koolda.marking_lab_compose.api.ProjectDbResponse
import com.koolda.marking_lab_compose.util.LocalStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual object LocalDb {
    private val json = Json { ignoreUnknownKeys = true }
    private const val KEY_PROJECTS = "db_projects"
    private fun modelsKey(projectId: Int) = "db_models_$projectId"
    private fun filesKey(projectId: Int) = "db_files_$projectId"

    // ==================== PROJECTS ====================

    actual fun getProjects(): List<ProjectDbResponse> {
        val raw = LocalStorage.getItem(KEY_PROJECTS) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ProjectDbResponse>>(raw) }.getOrElse { emptyList() }
    }

    actual fun saveProjects(projects: List<ProjectDbResponse>) {
        LocalStorage.setItem(KEY_PROJECTS, json.encodeToString(projects))
    }

    actual fun saveProject(project: ProjectDbResponse) {
        val list = getProjects().toMutableList()
        val idx = list.indexOfFirst { it.id == project.id }
        if (idx >= 0) list[idx] = project else list.add(0, project)
        saveProjects(list)
    }

    actual fun deleteProject(projectId: Int) {
        saveProjects(getProjects().filter { it.id != projectId })
        LocalStorage.removeItem(modelsKey(projectId))
        LocalStorage.removeItem(filesKey(projectId))
    }

    // ==================== MODELS ====================

    actual fun getModels(projectId: Int): List<ModelListResponse> {
        val raw = LocalStorage.getItem(modelsKey(projectId)) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ModelListResponse>>(raw) }.getOrElse { emptyList() }
    }

    actual fun saveModels(projectId: Int, models: List<ModelListResponse>) {
        LocalStorage.setItem(modelsKey(projectId), json.encodeToString(models))
    }

    actual fun saveModel(projectId: Int, model: ModelListResponse) {
        val list = getModels(projectId).toMutableList()
        val idx = list.indexOfFirst { it.id == model.id }
        if (idx >= 0) list[idx] = model else list.add(0, model)
        saveModels(projectId, list)
    }

    actual fun deleteModel(projectId: Int, modelId: Int) {
        saveModels(projectId, getModels(projectId).filter { it.id != modelId })
    }

    // ==================== FILES ====================

    @Serializable
    private data class FileRecord(
        val file: FileListResponse,
        val localPath: String = ""
    )

    private fun readFileRecords(projectId: Int): MutableList<FileRecord> {
        val raw = LocalStorage.getItem(filesKey(projectId)) ?: return mutableListOf()
        return runCatching { json.decodeFromString<MutableList<FileRecord>>(raw) }.getOrElse { mutableListOf() }
    }

    private fun writeFileRecords(projectId: Int, records: List<FileRecord>) {
        LocalStorage.setItem(filesKey(projectId), json.encodeToString(records))
    }

    actual fun getFiles(projectId: Int): List<FileListResponse> =
        readFileRecords(projectId).map { it.file }

    actual fun saveFiles(projectId: Int, files: List<FileListResponse>) {
        val existing = readFileRecords(projectId).associateBy { it.file.id }
        val updated = files.map { f -> FileRecord(f, existing[f.id]?.localPath ?: "") }
        writeFileRecords(projectId, updated)
    }

    actual fun saveFileRecord(projectId: Int, file: FileListResponse, localPath: String) {
        val records = readFileRecords(projectId)
        val idx = records.indexOfFirst { it.file.id == file.id }
        val record = FileRecord(file, localPath)
        if (idx >= 0) records[idx] = record else records.add(0, record)
        writeFileRecords(projectId, records)
    }

    actual fun deleteFileRecord(projectId: Int, fileId: Int) {
        writeFileRecords(projectId, readFileRecords(projectId).filter { it.file.id != fileId })
    }

    actual fun getLocalPath(projectId: Int, fileId: Int): String? =
        readFileRecords(projectId).find { it.file.id == fileId }?.localPath?.ifBlank { null }
}
