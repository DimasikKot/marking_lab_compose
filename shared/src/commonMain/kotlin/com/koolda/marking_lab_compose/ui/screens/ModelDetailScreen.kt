package com.koolda.marking_lab_compose.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.koolda.marking_lab_compose.api.ApiClient
import com.koolda.marking_lab_compose.api.FileListResponse
import com.koolda.marking_lab_compose.api.ModelListResponse
import com.koolda.marking_lab_compose.api.PatchModelRequest
import com.koolda.marking_lab_compose.db.LocalDb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.koolda.marking_lab_compose.api.ModelMetrics
import org.jetbrains.skia.Image as SkiaImage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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
        // Показываем кэш немедленно
        model = LocalDb.getModels(projectId).find { it.id == modelId }
        loadModel()
        loadProjectFiles()
    }

    fun loadModel() {
        screenModelScope.launch {
            isLoading = true
            try {
                val fetched = ApiClient.api.getModel(projectId, modelId)
                LocalDb.saveModel(projectId, fetched)
                model = fetched
            } catch (e: Exception) {
                if (model == null) {
                    errorMessage = e.message ?: "Ошибка загрузки модели"
                }
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

    // Добавить файлы в обучение через PATCH /models/{id}
    fun addTrainingFiles(fileIds: List<Int>) {
        screenModelScope.launch {
            try {
                val currentIds = model?.trainingFiles?.map { it.id } ?: emptyList()
                val newIds = (currentIds + fileIds).distinct()
                val updated = ApiClient.api.updateModel(
                    projectId, modelId, PatchModelRequest(trainingFilesIds = newIds)
                )
                LocalDb.saveModel(projectId, updated)
                model = updated
                showAddFilesDialog = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка добавления файлов"
            }
        }
    }

    // Убрать файл из обучения через PATCH /models/{id}
    fun removeTrainingFile(fileId: Int) {
        screenModelScope.launch {
            try {
                val newIds = model?.trainingFiles
                    ?.filter { it.id != fileId }
                    ?.map { it.id }
                    ?: emptyList()
                val updated = ApiClient.api.updateModel(
                    projectId, modelId, PatchModelRequest(trainingFilesIds = newIds)
                )
                LocalDb.saveModel(projectId, updated)
                model = updated
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка удаления файла из обучения"
            }
        }
    }

    // Запуск — GET /models/{id}/train
    fun startTraining() {
        screenModelScope.launch {
            isTraining = true
            try {
                val started = ApiClient.api.trainModel(projectId, modelId)
                LocalDb.saveModel(projectId, started)
                model = started
                pollProgress()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка запуска обучения"
                isTraining = false
            }
        }
    }

    // Остановка — DELETE /models/{id}/train
    fun stopTraining() {
        screenModelScope.launch {
            try {
                val updated = ApiClient.api.stopTraining(projectId, modelId)
                LocalDb.saveModel(projectId, updated)
                model = updated
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка остановки обучения"
            } finally {
                isTraining = false
            }
        }
    }

    private suspend fun pollProgress() {
        // progress 0-100: обучение; 100-200: предсказание; >=200: всё готово
        if ((model?.progress ?: 0) >= 200) {
            isTraining = false
            return
        }
        while (isTraining) {
            delay(3000)
            try {
                val updated = ApiClient.api.getModel(projectId, modelId)
                LocalDb.saveModel(projectId, updated)
                model = updated
                if (updated.progress >= 200) isTraining = false
            } catch (_: Exception) {
            }
        }
    }

    // Переименование через PATCH /models/{id}
    fun renameModel() {
        if (editName.isBlank()) {
            errorMessage = "Название не может быть пустым"
            return
        }
        screenModelScope.launch {
            try {
                val updated = ApiClient.api.updateModel(
                    projectId, modelId, PatchModelRequest(name = editName)
                )
                LocalDb.saveModel(projectId, updated)
                model = updated
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
                        // progress: 0-100 обучение, 100-200 предсказание, >=200 завершено
                        Text(
                            text = when {
                                progress >= 200 -> "Завершено"
                                progress > 100 -> "Предсказание... ${progress - 100}%"
                                screenModel.isTraining && progress in 1..100 -> "Обучение... $progress%"
                                screenModel.isTraining -> "Запуск..."
                                progress == 100 -> "Обучение завершено"
                                progress == 0 -> "Не запущено"
                                else -> "$progress%"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Кнопки старт / стоп обучения
            item {
                val progress = currentModel?.progress ?: 0
                val hasTrainingFiles = currentModel?.trainingFiles?.isNotEmpty() == true
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { screenModel.startTraining() },
                        modifier = Modifier.weight(1f),
                        enabled = !screenModel.isTraining && hasTrainingFiles && progress < 200
                    ) {
                        if (screenModel.isTraining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (screenModel.isTraining) "Обучение..." else "Начать")
                    }
                    if (screenModel.isTraining) {
                        OutlinedButton(
                            onClick = { screenModel.stopTraining() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Остановить")
                        }
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

            // Metrics & Graphs section — shown after training completion (progress >= 200)
            if ((currentModel?.progress ?: 0) >= 200) {
                val metrics = currentModel?.metrics ?: ModelMetrics()
                val graphs = currentModel?.graphs ?: emptyMap()
                if (metrics.hasData() || graphs.isNotEmpty()) {
                    item {
                        Column {
                            HorizontalDivider()
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Результаты обучения",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    if (metrics.hasData()) {
                        item { MetricsCard(metrics) }
                    }
                    items(
                        graphs.entries.toList(),
                        key = { (k, _) -> "graph_$k" }
                    ) { (title, value) ->
                        if (value.startsWith("data:image/")) {
                            GraphImageCard(title = title, dataUrl = value)
                        } else {
                            GraphTextCard(title = title, text = value)
                        }
                    }
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

// ========== METRICS CARD ==========
@Composable
private fun MetricsCard(metrics: ModelMetrics) {
    val entries = buildList {
        metrics.accuracy?.let { add("Точность (accuracy)" to it) }
        metrics.precision?.let { add("Точность (precision)" to it) }
        metrics.recall?.let { add("Полнота (recall)" to it) }
        metrics.f1?.let { add("F1-мера" to it) }
        metrics.trainingTime?.let { add("Время обучения (сек)" to it) }
        metrics.predictionTime?.let { add("Время разметки (сек)" to it) }
    }
    if (entries.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Метрики",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            entries.forEachIndexed { i, (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = value.formatMetric(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (i < entries.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

// ========== GRAPH IMAGE CARD ==========
@OptIn(ExperimentalEncodingApi::class)
@Composable
private fun GraphImageCard(title: String, dataUrl: String) {
    val imageBitmap: ImageBitmap? = remember(dataUrl) {
        runCatching {
            val base64 = dataUrl.substringAfter("base64,")
            val bytes = Base64.Default.decode(base64)
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        }.getOrNull()
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (imageBitmap != null) {
                Spacer(Modifier.height(8.dp))
                Image(
                    bitmap = imageBitmap,
                    contentDescription = title,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

// ========== GRAPH TEXT CARD ==========
@Composable
private fun GraphTextCard(title: String, text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Double.formatMetric(): String {
    val s = toString()
    val dot = s.indexOf('.')
    return if (dot >= 0 && s.length - dot > 5) s.substring(0, dot + 5) else s
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
