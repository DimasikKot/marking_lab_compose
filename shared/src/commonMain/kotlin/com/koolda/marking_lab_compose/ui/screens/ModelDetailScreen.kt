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
import com.koolda.marking_lab_compose.api.AddTrainingFilesRequest
import com.koolda.marking_lab_compose.api.ApiClient
import com.koolda.marking_lab_compose.api.FileListResponse
import com.koolda.marking_lab_compose.api.ModelListResponse
import com.koolda.marking_lab_compose.api.UpdateModelRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ModelDetailScreen(
    val projectId: Int,
    val modelId: Int,
    val modelName: String
) : Screen {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel {
            ModelDetailScreenModel(projectId, modelId, modelName)
        }
        ModelDetailContent(screenModel)
    }
}

// ========== SCREEN MODEL ==========
class ModelDetailScreenModel(
    private val projectId: Int,
    private val modelId: Int,
    initialName: String
) : ScreenModel {
    var model by mutableStateOf<ModelListResponse?>(null)
    var projectFiles by mutableStateOf<List<FileListResponse>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isTraining by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var showAddFilesDialog by mutableStateOf(false)

    // Rename
    var currentName by mutableStateOf(initialName)
    var showRenameDialog by mutableStateOf(false)
    var editName by mutableStateOf(initialName)

    init {
        loadModel()
        loadProjectFiles()
    }

    fun loadModel() {
        screenModelScope.launch {
            isLoading = true
            try {
                model = ApiClient.api.getModel(projectId, modelId)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка загрузки модели"
            } finally {
                isLoading = false
            }
        }
    }

    private fun loadProjectFiles() {
        screenModelScope.launch {
            try {
                projectFiles = ApiClient.api.getFiles(projectId).data
            } catch (_: Exception) {
            }
        }
    }

    fun addTrainingFiles(fileIds: List<Int>) {
        screenModelScope.launch {
            try {
                model = ApiClient.api.addTrainingFiles(
                    projectId, modelId, AddTrainingFilesRequest(fileIds)
                )
                showAddFilesDialog = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка добавления файлов"
            }
        }
    }

    fun removeTrainingFile(fileId: Int) {
        screenModelScope.launch {
            try {
                ApiClient.api.removeTrainingFile(projectId, modelId, fileId)
                loadModel()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка удаления файла из обучения"
            }
        }
    }

    fun startTraining() {
        screenModelScope.launch {
            isTraining = true
            try {
                model = ApiClient.api.trainModel(projectId, modelId)
                pollProgress()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка запуска обучения"
                isTraining = false
            }
        }
    }

    private suspend fun pollProgress() {
        if ((model?.progress ?: 0) >= 100) {
            isTraining = false
            return
        }
        while (isTraining) {
            delay(3000)
            try {
                val updated = ApiClient.api.getModel(projectId, modelId)
                model = updated
                if (updated.progress >= 100) {
                    isTraining = false
                }
            } catch (_: Exception) {
            }
        }
    }

    fun renameModel() {
        if (editName.isBlank()) {
            errorMessage = "Название не может быть пустым"
            return
        }
        screenModelScope.launch {
            try {
                ApiClient.api.updateModel(projectId, modelId, UpdateModelRequest(editName))
                currentName = editName
                showRenameDialog = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка переименования модели"
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
fun ModelDetailContent(screenModel: ModelDetailScreenModel) {
    val navigator = LocalNavigator.current
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
                title = { Text(screenModel.currentName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navigator?.pop() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        screenModel.editName = screenModel.currentName
                        screenModel.showRenameDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Переименовать")
                    }
                    IconButton(onClick = { screenModel.loadModel() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val currentModel = screenModel.model

        if (screenModel.isLoading && currentModel == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Прогресс обучения", style = MaterialTheme.typography.titleMedium)
                            if (screenModel.isTraining) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (currentModel?.progress ?: 0) / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        val progress = currentModel?.progress ?: 0
                        Text(
                            text = when {
                                screenModel.isTraining -> "Обучение... $progress%"
                                progress >= 100 -> "Завершено"
                                progress == 0 -> "Не запущено"
                                else -> "$progress%"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Start training button
            item {
                Button(
                    onClick = { screenModel.startTraining() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !screenModel.isTraining
                            && (currentModel?.trainingFiles?.isNotEmpty() == true)
                            && (currentModel?.progress ?: 0) < 100
                ) {
                    if (screenModel.isTraining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Обучение...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Начать обучение")
                    }
                }
            }

            // Training files header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Файлы обучения (${currentModel?.trainingFiles?.size ?: 0})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(
                        onClick = { screenModel.showAddFilesDialog = true },
                        enabled = !screenModel.isTraining
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Добавить")
                    }
                }
            }

            val trainingFiles = currentModel?.trainingFiles ?: emptyList()
            if (trainingFiles.isEmpty()) {
                item {
                    Text(
                        text = "Нет файлов обучения. Добавьте файлы для начала обучения.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(trainingFiles, key = { "train_${it.id}" }) { file ->
                    TrainingFileRow(
                        file = file,
                        onRemove = { screenModel.removeTrainingFile(file.id) },
                        enabled = !screenModel.isTraining
                    )
                }
            }

            // Prediction files section
            val predictionFiles = currentModel?.predictionFiles ?: emptyList()
            if (predictionFiles.isNotEmpty()) {
                item {
                    Column {
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Файлы предсказаний (${predictionFiles.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                items(predictionFiles, key = { "pred_${it.id}" }) { file ->
                    PredictionFileRow(file = file)
                }
            }
        }
    }

    // Rename dialog
    if (screenModel.showRenameDialog) {
        AlertDialog(
            onDismissRequest = { screenModel.showRenameDialog = false },
            title = { Text("Переименовать модель") },
            text = {
                OutlinedTextField(
                    value = screenModel.editName,
                    onValueChange = { screenModel.editName = it },
                    label = { Text("Название *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { screenModel.renameModel() },
                    enabled = screenModel.editName.isNotBlank()
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { screenModel.showRenameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Add training files dialog
    if (screenModel.showAddFilesDialog) {
        val trainingFileIds = screenModel.model?.trainingFiles?.map { it.id }?.toSet() ?: emptySet()
        val availableFiles = screenModel.projectFiles.filter { it.id !in trainingFileIds }
        AddTrainingFilesDialog(
            availableFiles = availableFiles,
            onConfirm = { fileIds -> screenModel.addTrainingFiles(fileIds) },
            onDismiss = { screenModel.showAddFilesDialog = false }
        )
    }
}

// ========== TRAINING FILE ROW ==========
@Composable
fun TrainingFileRow(
    file: FileListResponse,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.isLabeled) {
                    Text(
                        text = "Размечен",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Убрать из обучения",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ========== PREDICTION FILE ROW ==========
@Composable
fun PredictionFileRow(file: FileListResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Analytics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ========== ADD TRAINING FILES DIALOG ==========
@Composable
fun AddTrainingFilesDialog(
    availableFiles: List<FileListResponse>,
    onConfirm: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить файлы обучения") },
        text = {
            if (availableFiles.isEmpty()) {
                Text(
                    text = "Все файлы проекта уже добавлены в обучение.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(availableFiles, key = { it.id }) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (file.id in selectedIds) {
                                        selectedIds - file.id
                                    } else {
                                        selectedIds + file.id
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = file.id in selectedIds,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) {
                                        selectedIds + file.id
                                    } else {
                                        selectedIds - file.id
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (file.isLabeled) {
                                    Text(
                                        text = "Размечен",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("Добавить (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
