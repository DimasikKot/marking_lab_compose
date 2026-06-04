package com.koolda.marking_lab_compose.api

import com.koolda.marking_lab_compose.util.TokenManager
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ==================== Data Models ====================

@Serializable
data class ProjectDbResponse(
    val id: Int,
    val name: String,
    val description: String = "",
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

// Модель, которая предсказала файл (вложена в FileListResponse)
@Serializable
data class PredictionModelResponse(
    val id: Int,
    val name: String,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

// Вложенный объект origin_file (исходный файл для предсказания)
@Serializable
data class OriginFileResponse(
    val id: Int,
    val name: String,
    @SerialName("total_rows") val totalRows: Int = 0,
    @SerialName("is_labeled") val isLabeled: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class FileListResponse(
    val id: Int,
    val name: String,
    @SerialName("total_rows") val totalRows: Int = 0,
    @SerialName("origin_file") val originFile: OriginFileResponse? = null,
    @SerialName("prediction_model") val predictionModel: PredictionModelResponse? = null,
    @SerialName("is_labeled") val isLabeled: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class ModelListResponse(
    val id: Int,
    val name: String,
    @SerialName("redis_id") val redisId: String? = null,
    // 0-100: прогресс обучения; 100-200: прогресс предсказания
    val progress: Int = 0,
    // parameters, metrics, graphs — произвольный JSONB, игнорируем (ignoreUnknownKeys = true)
    @SerialName("training_files") val trainingFiles: List<FileListResponse> = emptyList(),
    @SerialName("prediction_files") val predictionFiles: List<FileListResponse> = emptyList(),
    @SerialName("predicted_files") val predictedFiles: List<FileListResponse> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
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
    val username: String, val email: String, val password: String
)

@Serializable
data class LoginRequest(
    val login: String, val password: String
)

@Serializable
data class ValidateUsernameRequest(val username: String)

@Serializable
data class ValidateEmailRequest(val email: String)

@Serializable
data class ValidateLoginRequest(val login: String)

// POST /projects — is_public не принимается при создании (default FALSE на сервере)
@Serializable
data class CreateProjectRequest(
    val name: String,
    val description: String = ""
)

// PATCH /projects/{id} — все поля опциональны (partial update)
@Serializable
data class PatchProjectRequest(
    val name: String? = null,
    val description: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null
)

// POST /projects/{id}/models — файлы можно передать сразу при создании
@Serializable
data class CreateModelRequest(
    val name: String,
    @SerialName("training_files_ids") val trainingFilesIds: List<Int>? = null,
    @SerialName("prediction_files_ids") val predictionFilesIds: List<Int>? = null
)

// PATCH /projects/{id}/models/{id} — все поля опциональны
@Serializable
data class PatchModelRequest(
    val name: String? = null,
    @SerialName("training_files_ids") val trainingFilesIds: List<Int>? = null,
    @SerialName("prediction_files_ids") val predictionFilesIds: List<Int>? = null
)

// ==================== Response Wrappers ====================

@Serializable
data class ProjectsResponse(val data: List<ProjectDbResponse>)

@Serializable
data class FilesResponse(val data: List<FileListResponse>)

@Serializable
data class ModelsResponse(val data: List<ModelListResponse>)

// ==================== API Interface ====================

interface MarkingLabApi {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Headers("Content-Type: application/json")
    @POST("users/")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @Headers("Content-Type: application/json")
    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): UserResponse

    @Headers("Content-Type: application/json")
    @POST("users/validate-username")
    suspend fun validateUsername(@Body request: ValidateUsernameRequest): ValidateResponse

    @Headers("Content-Type: application/json")
    @POST("users/validate-email")
    suspend fun validateEmail(@Body request: ValidateEmailRequest): ValidateResponse

    @Headers("Content-Type: application/json")
    @POST("users/validate-login")
    suspend fun validateLogin(@Body request: ValidateLoginRequest): ValidateResponse

    // ── Projects ──────────────────────────────────────────────────────────────

    @Headers("Content-Type: application/json")
    @GET("projects")
    suspend fun getProjects(
        @Query("sort") sort: String? = null,
        @Query("search") search: String? = null
    ): ProjectsResponse

    @Headers("Content-Type: application/json")
    @POST("projects")
    suspend fun createProject(@Body request: CreateProjectRequest): ProjectDbResponse

    @Headers("Content-Type: application/json")
    @GET("projects/{id}")
    suspend fun getProjectById(@Path("id") projectId: Int): ProjectDbResponse

    // Сервер принимает PATCH, не POST
    @Headers("Content-Type: application/json")
    @PATCH("projects/{id}")
    suspend fun updateProject(
        @Path("id") projectId: Int,
        @Body request: PatchProjectRequest
    ): ProjectDbResponse

    @DELETE("projects/{id}")
    suspend fun deleteProject(@Path("id") projectId: Int)

    // ── Files ─────────────────────────────────────────────────────────────────

    @Headers("Content-Type: application/json")
    @GET("projects/{projectId}/files")
    suspend fun getFiles(@Path("projectId") projectId: Int): FilesResponse

    @DELETE("projects/{projectId}/files/{fileId}")
    suspend fun deleteFile(
        @Path("projectId") projectId: Int,
        @Path("fileId") fileId: Int
    )

    // ── Models ────────────────────────────────────────────────────────────────

    @Headers("Content-Type: application/json")
    @GET("projects/{projectId}/models")
    suspend fun getModels(@Path("projectId") projectId: Int): ModelsResponse

    @Headers("Content-Type: application/json")
    @GET("projects/{projectId}/models/{modelId}")
    suspend fun getModel(
        @Path("projectId") projectId: Int,
        @Path("modelId") modelId: Int
    ): ModelListResponse

    @Headers("Content-Type: application/json")
    @POST("projects/{projectId}/models")
    suspend fun createModel(
        @Path("projectId") projectId: Int,
        @Body request: CreateModelRequest
    ): ModelListResponse

    // Сервер принимает PATCH, не POST. Используется для переименования И управления файлами.
    @Headers("Content-Type: application/json")
    @PATCH("projects/{projectId}/models/{modelId}")
    suspend fun updateModel(
        @Path("projectId") projectId: Int,
        @Path("modelId") modelId: Int,
        @Body request: PatchModelRequest
    ): ModelListResponse

    @DELETE("projects/{projectId}/models/{modelId}")
    suspend fun deleteModel(
        @Path("projectId") projectId: Int,
        @Path("modelId") modelId: Int
    )

    // Запуск обучения — GET, не POST
    @GET("projects/{projectId}/models/{modelId}/train")
    suspend fun trainModel(
        @Path("projectId") projectId: Int,
        @Path("modelId") modelId: Int
    ): ModelListResponse

    // Остановка обучения
    @DELETE("projects/{projectId}/models/{modelId}/train")
    suspend fun stopTraining(
        @Path("projectId") projectId: Int,
        @Path("modelId") modelId: Int
    ): ModelListResponse
}

// ==================== Ktorfit Setup ====================

object ApiClient {
    private const val BASE_URL = "http://localhost:8000/api/v1/"

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(DefaultRequest) {
            TokenManager.accessToken?.let { token ->
                header("Authorization", "Bearer $token")
            }
        }
    }

    val ktorfit: Ktorfit by lazy {
        Ktorfit.Builder().baseUrl(BASE_URL).httpClient(httpClient).build()
    }

    val api: MarkingLabApi by lazy {
        ktorfit.create<MarkingLabApi>()
    }

    // POST /projects/{id}/files
    // Сервер требует Form(...) поля: name, is_labeled + file
    // Возвращает FileDbResponse напрямую (без обёртки {"data": ...})
    suspend fun uploadFile(projectId: Int, fileName: String, fileBytes: ByteArray): FileListResponse? {
        val response = httpClient.post("${BASE_URL}projects/$projectId/files") {
            header(HttpHeaders.Authorization, "Bearer ${TokenManager.accessToken}")
            setBody(
                MultiPartFormDataContent(formData {
                    append("name", fileName)
                    append("is_labeled", "false")
                    append("file", fileBytes)
                })
            )
        }
        return runCatching { response.body<FileListResponse>() }.getOrNull()
    }

    // GET /projects/{id}/files/{fileId}/download
    suspend fun downloadFile(projectId: Int, fileId: Int): Pair<String, ByteArray> {
        val response = httpClient.get("${BASE_URL}projects/$projectId/files/$fileId/download") {
            header(HttpHeaders.Authorization, "Bearer ${TokenManager.accessToken}")
        }
        val contentDisposition = response.headers[HttpHeaders.ContentDisposition]
        val fileName = contentDisposition?.substringAfter("filename=")?.trim('"') ?: "file_$fileId"
        return Pair(fileName, response.body())
    }
}
