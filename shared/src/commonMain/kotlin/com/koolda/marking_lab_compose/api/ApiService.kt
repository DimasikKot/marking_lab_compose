package com.koolda.marking_lab_compose.api

import com.koolda.marking_lab_compose.util.TokenManager
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
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
import io.ktor.http.ContentType
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
    val username: String, val email: String, val password: String
)

@Serializable
data class LoginRequest(
    val login: String, val password: String
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
    val name: String, val description: String?, @SerialName("is_public") val isPublic: Boolean
)

@Serializable
data class PatchProjectRequest(
    val name: String, val description: String, @SerialName("is_public") val isPublic: Boolean
)

@Serializable
data class CreateModelRequest(
    val name: String
)

@Serializable
data class AddTrainingFilesRequest(
    @SerialName("file_ids") val fileIds: List<Int>
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

    // Projects endpoints
    @Headers("Content-Type: application/json")
    @GET("projects")
    suspend fun getProjects(
        @Query("sort") sort: String? = null, @Query("search") search: String? = null
    ): ProjectsResponse

    @Headers("Content-Type: application/json")
    @POST("projects")
    suspend fun createProject(@Body request: CreateProjectRequest): ProjectDbResponse

    @Headers("Content-Type: application/json")
    @GET("projects/{id}")
    suspend fun getProjectById(@Path("id") projectId: Int): ProjectDbResponse

    @Headers("Content-Type: application/json")
    @POST("projects/{id}")
    suspend fun updateProject(
        @Path("id") projectId: Int, @Body request: PatchProjectRequest
    ): ProjectDbResponse

    @DELETE("projects/{id}")
    suspend fun deleteProject(@Path("id") projectId: Int)

    // Files endpoints
    @Headers("Content-Type: application/json")
    @GET("projects/{projectId}/files")
    suspend fun getFiles(@Path("projectId") projectId: Int): FilesResponse

    @DELETE("projects/{projectId}/files/{fileId}")
    suspend fun deleteFile(
        @Path("projectId") projectId: Int,
        @Path("fileId") fileId: Int
    )

    // Models endpoints
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
        @Path("projectId") projectId: Int, @Body request: CreateModelRequest
    ): ModelListResponse

    @DELETE("projects/{projectId}/models/{modelId}")
    suspend fun deleteModel(
        @Path("projectId") projectId: Int,
        @Path("modelId") modelId: Int
    )

    @Headers("Content-Type: application/json")
    @POST("projects/{projectId}/models/{modelId}/training-files")
    suspend fun addTrainingFiles(
        @Path("projectId") projectId: Int,
        @Path("modelId") modelId: Int,
        @Body request: AddTrainingFilesRequest
    ): ModelListResponse

    @DELETE("projects/{projectId}/models/{modelId}/training-files/{fileId}")
    suspend fun removeTrainingFile(
        @Path("projectId") projectId: Int,
        @Path("modelId") modelId: Int,
        @Path("fileId") fileId: Int
    )

    @POST("projects/{projectId}/models/{modelId}/train")
    suspend fun trainModel(
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

    suspend fun uploadFile(projectId: Int, fileName: String, fileBytes: ByteArray): FileListResponse {
        val response = httpClient.post("${BASE_URL}projects/$projectId/files") {
            header(HttpHeaders.Authorization, "Bearer ${TokenManager.accessToken}")
            setBody(
                MultiPartFormDataContent(formData {
                    append("file", fileBytes, io.ktor.http.Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        append(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
                    })
                })
            )
        }
        return response.body()
    }

    suspend fun downloadFile(projectId: Int, fileId: Int): Pair<String, ByteArray> {
        val response = httpClient.get("${BASE_URL}projects/$projectId/files/$fileId/download") {
            header(HttpHeaders.Authorization, "Bearer ${TokenManager.accessToken}")
        }
        val contentDisposition = response.headers[HttpHeaders.ContentDisposition]
        val fileName = contentDisposition?.substringAfter("filename=")?.trim('"') ?: "file_$fileId"
        return Pair(fileName, response.body())
    }
}
