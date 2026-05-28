package com.koolda.marking_lab_compose.api

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ==================== Data Models ====================

@Serializable
data class ProjectDbResponse(
    val id: Int,
    val name: String,
    val description: String,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class FileListResponse(
    val id: Int,
    val name: String,
    @SerialName("is_labeled") val isLabeled: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ModelListResponse(
    val id: Int,
    val name: String,
    val progress: Int,
    @SerialName("training_files") val trainingFiles: List<FileListResponse>,
    @SerialName("prediction_files") val predictionFiles: List<FileListResponse>,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class UserResponse(
    val username: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String
)

@Serializable
data class ValidateResponse(
    val success: Boolean
)

// ==================== Request Models ====================

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class ValidateUsernameRequest(
    val username: String
)

@Serializable
data class ValidateEmailRequest(
    val email: String
)

@Serializable
data class ValidateLoginRequest(
    val login: String
)

@Serializable
data class CreateProjectRequest(
    val name: String,
    val description: String,
    @SerialName("is_public") val isPublic: Boolean
)

@Serializable
data class PatchProjectRequest(
    val name: String,
    val description: String,
    @SerialName("is_public") val isPublic: Boolean
)

@Serializable
data class CreateModelRequest(
    val name: String
)

// ==================== Response Wrappers ====================

@Serializable
data class ProjectsResponse(
    val data: List<ProjectDbResponse>
)

@Serializable
data class FilesResponse(
    val data: List<FileListResponse>
)

@Serializable
data class ModelsResponse(
    val data: List<ModelListResponse>
)

// ==================== API Interface ====================

interface MarkingLabApi {
    // Auth endpoints
    @POST("users/")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): UserResponse

    @POST("users/validate-username")
    suspend fun validateUsername(@Body request: ValidateUsernameRequest): ValidateResponse

    @POST("users/validate-email")
    suspend fun validateEmail(@Body request: ValidateEmailRequest): ValidateResponse

    @POST("users/validate-login")
    suspend fun validateLogin(@Body request: ValidateLoginRequest): ValidateResponse

    // Projects endpoints
    @GET("projects")
    suspend fun getProjects(
        @Query("sort") sort: String? = null,
        @Query("search") search: String? = null
    ): ProjectsResponse

    @POST("projects")
    suspend fun createProject(@Body request: CreateProjectRequest): ProjectDbResponse

    @GET("projects/{id}")
    suspend fun getProjectById(@Path("id") projectId: Int): ProjectDbResponse

    @POST("projects/{id}")
    suspend fun updateProject(
        @Path("id") projectId: Int,
        @Body request: PatchProjectRequest
    ): ProjectDbResponse

    // Files endpoints
    @GET("projects/{projectId}/files")
    suspend fun getFiles(@Path("projectId") projectId: Int): FilesResponse

    // Models endpoints
    @GET("projects/{projectId}/models")
    suspend fun getModels(@Path("projectId") projectId: Int): ModelsResponse

    @POST("projects/{projectId}/models")
    suspend fun createModel(
        @Path("projectId") projectId: Int,
        @Body request: CreateModelRequest
    ): ModelListResponse
}

// ==================== Ktorfit Setup ====================

object ApiClient {
    private const val BASE_URL = "http://localhost:8000/api"

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    val ktorfit: Ktorfit by lazy {
        Ktorfit.Builder()
            .baseUrl(BASE_URL)
            .httpClient(httpClient)
            .build()
    }

    val api: MarkingLabApi by lazy {
        ktorfit.create<MarkingLabApi>()
    }
}
