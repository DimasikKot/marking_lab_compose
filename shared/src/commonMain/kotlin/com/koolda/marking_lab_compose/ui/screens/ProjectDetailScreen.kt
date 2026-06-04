package com.koolda.marking_lab_compose.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.koolda.marking_lab_compose.api.ApiClient
import com.koolda.marking_lab_compose.api.CreateModelRequest
import com.koolda.marking_lab_compose.api.FileListResponse
import com.koolda.marking_lab_compose.api.ModelListResponse
import com.koolda.marking_lab_compose.api.PatchProjectRequest
import com.koolda.marking_lab_compose.db.LocalDb
import com.koolda.marking_lab_compose.util.FilePicker
import kotlinx.coroutines.launch

data class ProjectDetailScreen(
    val projectId: Int,
    val projectName: String,
    val projectDescription: String = "",
    val projectIsPublic: Boolean = true
) : Screen {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel {
            ProjectDetailScreenModel(projectId, projectName, projectDescription, projectIsPublic)
        }
        ProjectDetailContent(screenModel)
    }
}

// ========== SCREEN MODEL ==========
class ProjectDetailScreenModel(
    val projectId: Int,
    initialName: String,
    initialDescription: String,
    initialIsPublic: Boolean
) : ScreenModel {
    var files by mutableStateOf<List<FileListResponse>>(emptyList())
    var models by mutableStateOf<List<ModelListResponse>>(emptyList())
    var isLoadingFiles by mutableStateOf(false)
    var isLoadingModels by mutableStateOf(false)
    var isUploading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Edit project dialog
    var showEditDialog by mutableStateOf(false)
    var editName by mutableStateOf(initialName)
    var editDescription by mutableStateOf(initialDescription)
    var editIsPublic by mutableStateOf(initialIsPublic)
    var isSaving by mutableStateOf(false)

    // Create model dialog
    var showCreateModelDialog by mutableStateOf(false)
    var newModelName by mutableStateOf("")

    init {
        // Показываем кэш немедленно
        models = LocalDb.getModels(projectId)
        loadFiles()
        loadModels()
    }

    fun loadFiles() {
        screenModelScope.launch {
            isLoadingFiles = true
            try {
                files = ApiClient.api.getFiles(projectId).data
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка загрузки файлов"
            } finally {
                isLoadingFiles = false
            }
        }
    }

    fun loadModels() {
        screenModelScope.launch {
            isLoadingModels = true
            try {
                val fetched = ApiClient.api.getModels(projectId).data
                LocalDb.saveModels(projectId, fetched)
                models = fetched
            } catch (e: Exception) {
                if (models.isEmpty()) {
                    errorMessage = e.message ?: "Ошибка загрузки моделей"
                }
            } finally {
                isLoadingModels = false
            }
        }
    }

    fun uploadFile() {
        screenModelScope.launch {
            isUploading = true
            try {
                val picked = FilePicker.pickFile()
                if (picked != null) {
                    val (name, bytes) = picked
                    ApiClient.uploadFile(projectId, name, bytes)
                    loadFiles()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка загрузки файла"
            } finally {
                isUploading = false
            }
        }
    }

    fun downloadFile(fileId: Int, fileName: String) {
        screenModelScope.launch {
            try {
                val (name, bytes) = ApiClient.downloadFile(projectId, fileId)
                FilePicker.saveFile(name.ifBlank { fileName }, bytes)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка скачивания файла"
            }
        }
    }

    fun deleteFile(fileId: Int) {
        screenModelScope.launch {
            try {
                ApiClient.api.deleteFile(projectId, fileId)
                files = files.filter { it.id != fileId }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка удаления файла"
            }
        }
    }

    fun saveProject() {
        if (editName.isBlank()) {
            errorMessage = "Название не может быть пустым"
            return
        }
        screenModelScope.launch {
            isSaving = true
            try {
                ApiClient.api.updateProject(
                    projectId,
                    PatchProjectRequest(editName, editDescription, editIsPublic)
                )
                showEditDialog = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка сохранения проекта"
            } finally {
                isSaving = false
            }
        }
    }

    fun createModel() {
        if (newModelName.isBlank()) {
            errorMessage = "Введите название модели"
            return
        }
        screenModelScope.launch {
            try {
                ApiClient.api.createModel(projectId, CreateModelRequest(newModelName))
                newModelName = ""
                showCreateModelDialog = false
                loadModels()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка создания модели"
            }
        }
    }

    fun deleteModel(modelId: Int) {
        screenModelScope.launch {
            try {
                ApiClient.api.deleteModel(projectId, modelId)
                LocalDb.deleteModel(projectId, modelId)
                models = models.filter { it.id != modelId }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка удаления модели"
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}

// ========== UI CONTENT ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailContent(screenModel: ProjectDetailScreenModel) {
    val navigator = LocalNavigator.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Файлы", "Модели")
    val snackbarHostState = remember { SnackbarHostState() }

    screenModel.errorMessage?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            screenModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenModel.editName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navigator?.pop() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { screenModel.showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать проект")
                    }
                }
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(onClick = { screenModel.uploadFile() }) {
                    if (screenModel.isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(Icons.Default.FileUpload, contentDescription = "Загрузить файл")
                    }
                }
                1 -> FloatingActionButton(onClick = { screenModel.showCreateModelDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Создать модель")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> FilesTabContent(screenModel, navigator)
                1 -> ModelsTabContent(screenModel, navigator)
            }
        }
    }

    // Edit project dialog
    if (screenModel.showEditDialog) {
        EditProjectDialog(screenModel)
    }

    // Create model dialog
    if (screenModel.showCreateModelDialog) {
        AlertDialog(
            onDismissRequest = {
                screenModel.showCreateModelDialog = false
                screenModel.newModelName = ""
            },
            title = { Text("Новая модель") },
            text = {
                OutlinedTextField(
                    value = screenModel.newModelName,
                    onValueChange = { screenModel.newModelName = it },
                    label = { Text("Название *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { screenModel.createModel() },
                    enabled = screenModel.newModelName.isNotBlank()
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    screenModel.showCreateModelDialog = false
                    screenModel.newModelName = ""
                }) { Text("Отмена") }
            }
        )
    }
}

// ========== EDIT PROJECT DIALOG ==========
@Composable
fun EditProjectDialog(screenModel: ProjectDetailScreenModel) {
    AlertDialog(
        onDismissRequest = { if (!screenModel.isSaving) screenModel.showEditDialog = false },
        title = { Text("Редактировать проект") },
        text = {
            Column {
                OutlinedTextField(
                    value = screenModel.editName,
                    onValueChange = { screenModel.editName = it },
                    label = { Text("Название *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !screenModel.isSaving
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = screenModel.editDescription,
                    onValueChange = { screenModel.editDescription = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    enabled = !screenModel.isSaving
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = screenModel.editIsPublic,
                        onCheckedChange = { screenModel.editIsPublic = it },
                        enabled = !screenModel.isSaving
                    )
                    Text("Публичный проект")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { screenModel.saveProject() },
                enabled = screenModel.editName.isNotBlank() && !screenModel.isSaving
            ) {
                if (screenModel.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (screenModel.isSaving) "Сохранение..." else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { screenModel.showEditDialog = false },
                enabled = !screenModel.isSaving
            ) { Text("Отмена") }
        }
    )
}

// ========== FILES TAB ==========
@Composable
fun FilesTabContent(
    screenModel: ProjectDetailScreenModel,
    navigator: cafe.adriel.voyager.navigator.Navigator?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            screenModel.isLoadingFiles && screenModel.files.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            screenModel.files.isEmpty() -> {
                Text(
                    text = "Нет файлов. Нажмите + для загрузки.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(screenModel.files, key = { it.id }) { file ->
                        FileCard(
                            file = file,
                            onDownload = { screenModel.downloadFile(file.id, file.name) },
                            onDelete = { screenModel.deleteFile(file.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileCard(
    file: FileListResponse,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (file.isLabeled) Icons.Default.CheckCircle else Icons.Default.Description,
                contentDescription = null,
                tint = if (file.isLabeled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (file.isLabeled) "Размечен" else "Не размечен",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDownload) {
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = "Скачать",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ========== MODELS TAB ==========
@Composable
fun ModelsTabContent(
    screenModel: ProjectDetailScreenModel,
    navigator: cafe.adriel.voyager.navigator.Navigator?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            screenModel.isLoadingModels && screenModel.models.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            screenModel.models.isEmpty() -> {
                Text(
                    text = "Нет моделей. Нажмите + для создания.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(screenModel.models, key = { it.id }) { model ->
                        ModelSummaryCard(
                            model = model,
                            onClick = {
                                navigator?.push(
                                    ModelDetailScreen(screenModel.projectId, model.id, model.name)
                                )
                            },
                            onDelete = { screenModel.deleteModel(model.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModelSummaryCard(
    model: ModelListResponse,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { model.progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Прогресс: ${model.progress}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Файлов обучения: ${model.trainingFiles.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
