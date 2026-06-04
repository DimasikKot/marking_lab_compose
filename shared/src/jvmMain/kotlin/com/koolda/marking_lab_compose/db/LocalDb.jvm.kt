package com.koolda.marking_lab_compose.db

import com.koolda.marking_lab_compose.api.FileListResponse
import com.koolda.marking_lab_compose.api.ModelListResponse
import com.koolda.marking_lab_compose.api.OriginFileResponse
import com.koolda.marking_lab_compose.api.ProjectDbResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

actual object LocalDb {
    private val json = Json { ignoreUnknownKeys = true }

    private val connection: Connection by lazy {
        val dir = File(System.getProperty("user.home"), ".marking_lab")
        dir.mkdirs()
        val url = "jdbc:sqlite:${File(dir, "data.db").absolutePath}"
        DriverManager.getConnection(url).also { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS projects (
                        id          INTEGER PRIMARY KEY,
                        name        TEXT    NOT NULL,
                        description TEXT    NOT NULL DEFAULT '',
                        is_public   INTEGER NOT NULL DEFAULT 1,
                        created_at  TEXT    NOT NULL DEFAULT '',
                        updated_at  TEXT    NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS models (
                        id               INTEGER PRIMARY KEY,
                        project_id       INTEGER NOT NULL,
                        redis_id         TEXT,
                        name             TEXT    NOT NULL,
                        progress         INTEGER NOT NULL DEFAULT 0,
                        parameters       TEXT    NOT NULL DEFAULT '{}',
                        metrics          TEXT    NOT NULL DEFAULT '{}',
                        graphs           TEXT    NOT NULL DEFAULT '{}',
                        training_files   TEXT    NOT NULL DEFAULT '[]',
                        prediction_files TEXT    NOT NULL DEFAULT '[]',
                        created_at       TEXT    NOT NULL DEFAULT '',
                        updated_at       TEXT    NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS files (
                        id             INTEGER NOT NULL,
                        project_id     INTEGER NOT NULL,
                        name           TEXT    NOT NULL,
                        total_rows     INTEGER NOT NULL DEFAULT 0,
                        origin_file_id INTEGER,
                        local_path     TEXT    NOT NULL DEFAULT '',
                        is_labeled     INTEGER NOT NULL DEFAULT 0,
                        tags           TEXT    NOT NULL DEFAULT '[]',
                        created_at     TEXT    NOT NULL DEFAULT '',
                        updated_at     TEXT    NOT NULL DEFAULT '',
                        PRIMARY KEY (id, project_id)
                    )
                    """.trimIndent()
                )
                // Миграция: добавляем колонки в старые БД (SQLite не поддерживает IF NOT EXISTS для колонок)
                listOf(
                    "ALTER TABLE files ADD COLUMN total_rows INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE files ADD COLUMN origin_file_id INTEGER",
                    "ALTER TABLE files ADD COLUMN tags TEXT NOT NULL DEFAULT '[]'",
                    "ALTER TABLE models ADD COLUMN redis_id TEXT",
                    "ALTER TABLE models ADD COLUMN parameters TEXT NOT NULL DEFAULT '{}'",
                    "ALTER TABLE models ADD COLUMN metrics TEXT NOT NULL DEFAULT '{}'",
                    "ALTER TABLE models ADD COLUMN graphs TEXT NOT NULL DEFAULT '{}'"
                ).forEach { sql ->
                    try { stmt.execute(sql) } catch (_: Exception) { /* колонка уже существует */ }
                }
            }
        }
    }

    // ==================== PROJECTS ====================

    actual fun getProjects(): List<ProjectDbResponse> {
        val result = mutableListOf<ProjectDbResponse>()
        connection.createStatement().executeQuery(
            "SELECT * FROM projects ORDER BY updated_at DESC"
        ).use { rs ->
            while (rs.next()) {
                result += ProjectDbResponse(
                    id = rs.getInt("id"),
                    name = rs.getString("name"),
                    description = rs.getString("description"),
                    isPublic = rs.getInt("is_public") != 0,
                    createdAt = rs.getString("created_at"),
                    updatedAt = rs.getString("updated_at")
                )
            }
        }
        return result
    }

    actual fun saveProjects(projects: List<ProjectDbResponse>) {
        connection.createStatement().execute("DELETE FROM projects")
        projects.forEach { saveProject(it) }
    }

    actual fun saveProject(project: ProjectDbResponse) {
        connection.prepareStatement(
            """
            INSERT OR REPLACE INTO projects (id, name, description, is_public, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, project.id)
            stmt.setString(2, project.name)
            stmt.setString(3, project.description)
            stmt.setInt(4, if (project.isPublic) 1 else 0)
            stmt.setString(5, project.createdAt)
            stmt.setString(6, project.updatedAt)
            stmt.execute()
        }
    }

    actual fun deleteProject(projectId: Int) {
        connection.prepareStatement("DELETE FROM projects WHERE id = ?").use { stmt ->
            stmt.setInt(1, projectId)
            stmt.execute()
        }
        connection.prepareStatement("DELETE FROM models WHERE project_id = ?").use { stmt ->
            stmt.setInt(1, projectId)
            stmt.execute()
        }
    }

    // ==================== MODELS ====================

    actual fun getModels(projectId: Int): List<ModelListResponse> {
        val result = mutableListOf<ModelListResponse>()
        connection.prepareStatement(
            "SELECT * FROM models WHERE project_id = ? ORDER BY updated_at DESC"
        ).use { stmt ->
            stmt.setInt(1, projectId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result += ModelListResponse(
                        id = rs.getInt("id"),
                        name = rs.getString("name"),
                        redisId = rs.getString("redis_id"),
                        progress = rs.getInt("progress"),
                        trainingFiles = json.decodeFromString<List<FileListResponse>>(
                            rs.getString("training_files")
                        ),
                        predictionFiles = json.decodeFromString<List<FileListResponse>>(
                            rs.getString("prediction_files")
                        ),
                        createdAt = rs.getString("created_at"),
                        updatedAt = rs.getString("updated_at")
                    )
                }
            }
        }
        return result
    }

    actual fun saveModels(projectId: Int, models: List<ModelListResponse>) {
        connection.prepareStatement("DELETE FROM models WHERE project_id = ?").use { stmt ->
            stmt.setInt(1, projectId)
            stmt.execute()
        }
        models.forEach { saveModel(projectId, it) }
    }

    actual fun saveModel(projectId: Int, model: ModelListResponse) {
        connection.prepareStatement(
            """
            INSERT OR REPLACE INTO models
                (id, project_id, redis_id, name, progress, training_files, prediction_files, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, model.id)
            stmt.setInt(2, projectId)
            stmt.setString(3, model.redisId)
            stmt.setString(4, model.name)
            stmt.setInt(5, model.progress)
            stmt.setString(6, json.encodeToString(model.trainingFiles))
            stmt.setString(7, json.encodeToString(model.predictionFiles))
            stmt.setString(8, model.createdAt)
            stmt.setString(9, model.updatedAt)
            stmt.execute()
        }
    }

    actual fun deleteModel(projectId: Int, modelId: Int) {
        connection.prepareStatement("DELETE FROM models WHERE id = ? AND project_id = ?").use { stmt ->
            stmt.setInt(1, modelId)
            stmt.setInt(2, projectId)
            stmt.execute()
        }
    }

    // ==================== FILES ====================

    actual fun getFiles(projectId: Int): List<FileListResponse> {
        val result = mutableListOf<FileListResponse>()
        connection.prepareStatement(
            "SELECT * FROM files WHERE project_id = ? ORDER BY created_at DESC"
        ).use { stmt ->
            stmt.setInt(1, projectId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val originFileId = rs.getObject("origin_file_id") as? Int
                    result += FileListResponse(
                        id = rs.getInt("id"),
                        name = rs.getString("name"),
                        totalRows = rs.getInt("total_rows"),
                        originFile = originFileId?.let {
                            OriginFileResponse(id = it, name = "")
                        },
                        isLabeled = rs.getInt("is_labeled") != 0,
                        createdAt = rs.getString("created_at"),
                        updatedAt = rs.getString("updated_at")
                    )
                }
            }
        }
        return result
    }

    actual fun saveFiles(projectId: Int, files: List<FileListResponse>) {
        // Сохраняем существующие localPath перед очисткой
        val existingPaths = mutableMapOf<Int, String>()
        connection.prepareStatement("SELECT id, local_path FROM files WHERE project_id = ?").use { stmt ->
            stmt.setInt(1, projectId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) existingPaths[rs.getInt("id")] = rs.getString("local_path")
            }
        }
        connection.prepareStatement("DELETE FROM files WHERE project_id = ?").use { stmt ->
            stmt.setInt(1, projectId)
            stmt.execute()
        }
        files.forEach { file ->
            upsertFile(projectId, file, existingPaths[file.id] ?: "")
        }
    }

    actual fun saveFileRecord(projectId: Int, file: FileListResponse, localPath: String) {
        upsertFile(projectId, file, localPath)
    }

    actual fun deleteFileRecord(projectId: Int, fileId: Int) {
        connection.prepareStatement("DELETE FROM files WHERE id = ? AND project_id = ?").use { stmt ->
            stmt.setInt(1, fileId)
            stmt.setInt(2, projectId)
            stmt.execute()
        }
    }

    actual fun getLocalPath(projectId: Int, fileId: Int): String? {
        connection.prepareStatement(
            "SELECT local_path FROM files WHERE id = ? AND project_id = ?"
        ).use { stmt ->
            stmt.setInt(1, fileId)
            stmt.setInt(2, projectId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val path = rs.getString("local_path")
                    return path.ifBlank { null }
                }
            }
        }
        return null
    }

    private fun upsertFile(projectId: Int, file: FileListResponse, localPath: String) {
        connection.prepareStatement(
            """
            INSERT OR REPLACE INTO files
                (id, project_id, name, total_rows, origin_file_id, local_path, is_labeled, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, file.id)
            stmt.setInt(2, projectId)
            stmt.setString(3, file.name)
            stmt.setInt(4, file.totalRows)
            val originId = file.originFile?.id
            if (originId != null) stmt.setInt(5, originId) else stmt.setNull(5, java.sql.Types.INTEGER)
            stmt.setString(6, localPath)
            stmt.setInt(7, if (file.isLabeled) 1 else 0)
            stmt.setString(8, file.createdAt)
            stmt.setString(9, file.updatedAt)
            stmt.execute()
        }
    }
}
