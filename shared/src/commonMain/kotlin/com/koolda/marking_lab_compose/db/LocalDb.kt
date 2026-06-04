package com.koolda.marking_lab_compose.db

import com.koolda.marking_lab_compose.api.ModelListResponse
import com.koolda.marking_lab_compose.api.ProjectDbResponse

expect object LocalDb {
    // Projects
    fun getProjects(): List<ProjectDbResponse>
    fun saveProjects(projects: List<ProjectDbResponse>)
    fun saveProject(project: ProjectDbResponse)
    fun deleteProject(projectId: Int)

    // Models
    fun getModels(projectId: Int): List<ModelListResponse>
    fun saveModels(projectId: Int, models: List<ModelListResponse>)
    fun saveModel(projectId: Int, model: ModelListResponse)
    fun deleteModel(projectId: Int, modelId: Int)
}
