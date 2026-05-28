package com.koolda.marking_lab_compose.ui.screens

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
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
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import com.koolda.marking_lab_compose.api.ApiClient
import com.koolda.marking_lab_compose.api.CreateProjectRequest
import com.koolda.marking_lab_compose.api.ProjectDbResponse
import kotlinx.coroutines.launch


class ProjectsScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { ProjectsScreenModel() }
        ProjectsContent(screenModel)
    }
}

// ========== SCREEN MODEL ==========
class ProjectsScreenModel : ScreenModel {
    var projects by mutableStateOf<List<ProjectDbResponse>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Диалог создания проекта
    var showCreateDialog by mutableStateOf(false)
    var newProjectName by mutableStateOf("")
    var newProjectDescription by mutableStateOf("")

    init {
        loadProjects()
    }

    fun loadProjects() {
        screenModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.api.getProjects()
                projects = response.data.map {
                    ProjectDbResponse(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        isPublic = it.isPublic,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                    )
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка загрузки проектов"
            } finally {
                isLoading = false
            }
        }
    }

    fun createProject(navigator: Navigator?) {
        if (newProjectName.isBlank()) {
            errorMessage = "Введите название проекта"
            return
        }

        screenModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.api.createProject(
                    CreateProjectRequest(
                        name = newProjectName,
                        description = newProjectDescription.ifBlank { null },
                        isPublic = true
                    )
                )
                // Очищаем поля и закрываем диалог
                newProjectName = ""
                newProjectDescription = ""
                showCreateDialog = false
                // Перезагружаем список
                loadProjects()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка создания проекта"
            } finally {
                isLoading = false
            }
        }
    }

    fun onProjectClick(projectId: Int, navigator: Navigator?) {
//        navigator?.push(ProjectDetailsScreen(projectId))
    }

    fun deleteProject(projectId: Int) {
        screenModelScope.launch {
            try {
//                ApiClient.api.deletePro(projectId)
//                projects = projects.filter { it.id != projectId }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка удаления"
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
fun ProjectsContent(screenModel: ProjectsScreenModel) {
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Проекты") },
                navigationIcon = {
                    IconButton(onClick = { navigator?.pop() }) {
                        Icon(Icons.Default.ArrowCircleLeft, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { screenModel.showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Создать проект")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { screenModel.showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Новый проект")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Список проектов
            if (screenModel.isLoading && screenModel.projects.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (screenModel.errorMessage != null && screenModel.projects.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = screenModel.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        screenModel.clearError()
                        screenModel.loadProjects()
                    }) {
                        Text("Повторить")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(screenModel.projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { screenModel.onProjectClick(project.id, navigator) },
                            onDelete = { screenModel.deleteProject(project.id) }
                        )
                    }

                    if (screenModel.projects.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Нет проектов. Создайте первый!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            val snackbarHostState = remember { SnackbarHostState() }

            // Snackbar для ошибок
            screenModel.errorMessage?.let { error ->
                LaunchedEffect(error) {
                    snackbarHostState.showSnackbar(error)
                    screenModel.clearError()
                }

                SnackbarHost(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    hostState = snackbarHostState
                )
            }
        }

        // Диалог создания проекта
        if (screenModel.showCreateDialog) {
            CreateProjectDialog(
                projectName = screenModel.newProjectName,
                onProjectNameChange = { screenModel.newProjectName = it },
                projectDescription = screenModel.newProjectDescription,
                onProjectDescriptionChange = { screenModel.newProjectDescription = it },
                onConfirm = { screenModel.createProject(navigator) },
                onDismiss = {
                    screenModel.showCreateDialog = false
                    screenModel.newProjectName = ""
                    screenModel.newProjectDescription = ""
                },
                isLoading = screenModel.isLoading
            )
        }
    }
}

// ========== PROJECT CARD ==========
@Composable
fun ProjectCard(
    project: ProjectDbResponse,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            project.description.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ModelTraining,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = project.createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ========== CREATE PROJECT DIALOG ==========
@Composable
fun CreateProjectDialog(
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    projectDescription: String,
    onProjectDescriptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый проект") },
        text = {
            Column {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = onProjectNameChange,
                    label = { Text("Название *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = onProjectDescriptionChange,
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = projectName.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isLoading) "Создаём..." else "Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Отмена")
            }
        }
    )
}