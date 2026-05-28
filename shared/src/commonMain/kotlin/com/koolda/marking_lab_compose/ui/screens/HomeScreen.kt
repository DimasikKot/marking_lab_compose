package com.koolda.marking_lab_compose.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import com.koolda.marking_lab_compose.api.ApiClient
import com.koolda.marking_lab_compose.api.ProjectDbResponse
import kotlinx.coroutines.launch


class HomeScreen : Screen {
    @Composable
    override fun Content() {
        // 2. Теперь мы внутри Screen, и rememberScreenModel доступен!
        val screenModel = rememberScreenModel { HomeScreenModel() }

        // Вызываем ваш UI контент
        HomeContent(screenModel)
    }
}

class HomeScreenModel : ScreenModel {
    var projects: List<ProjectDbResponse> = emptyList()
        private set

    var isLoading: Boolean = false
        private set

    var errorMessage: String? = null
        private set

    init {
        loadProjects()
    }

    fun loadProjects() {
        screenModelScope.launch {
            try {
                isLoading = true
                val response = ApiClient.api.getProjects()
                projects = response.data
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message
            }
        }
    }
}

@Composable
fun HomeContent(screenModel: HomeScreenModel) {
    val navigator = LocalNavigator.current ?: return Text("Hi")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Главная") }, actions = {
                IconButton(onClick = { navigator.push(LoginScreen()) }) {
                    Icon(Icons.Default.Person, contentDescription = "Profile")
                }
            })
        }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (screenModel.isLoading) {
                CircularProgressIndicator()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left card
                    InfoCard(
                        icon = Icons.Default.RocketLaunch,
                        title = "Наши плюсы",
                        description = "Современные инструменты разметки, высокая точность и удобный интерфейс для каждого пользователя."
                    )

                    // Center card
                    InfoCardBig(
                        icon = Icons.Default.AutoAwesome,
                        onClick = { })

                    // Right card
                    InfoCard(
                        icon = Icons.Default.Bolt,
                        title = "Мы предлагаем",
                        description = "Удобную платформу для разметки и работы с данными."
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Лаборатория разметки © 2026",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String
) {
    Card(
        modifier = Modifier.width(250.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title, style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoCardBig(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val title = "Создать новый проект"
    val description = "Начните работать с данными уже сегодня!"
    val buttonText = "Перейти к проектам"


    Card(
        modifier = Modifier.width(300.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title, style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}
