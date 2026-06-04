package com.koolda.marking_lab_compose.db

import com.koolda.marking_lab_compose.api.FileListResponse
import com.koolda.marking_lab_compose.api.ModelListResponse
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
                        name             TEXT    NOT NULL,
                        progress         INTEGER NOT NULL DEFAULT 0,
                        training_files   TEXT    NOT NULL DEFAULT '[]',
                        prediction_files TEXT    NOT NULL DEFAULT '[]',
                        created_at       TEXT    NOT NULL DEFAULT '',
                        updated_at       TEXT    NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
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
                (id, project_id, name, progress, training_files, prediction_files, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, model.id)
            stmt.setInt(2, projectId)
            stmt.setString(3, model.name)
            stmt.setInt(4, model.progress)
            stmt.setString(5, json.encodeToString(model.trainingFiles))
            stmt.setString(6, json.encodeToString(model.predictionFiles))
            stmt.setString(7, model.createdAt)
            stmt.setString(8, model.updatedAt)
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
}
