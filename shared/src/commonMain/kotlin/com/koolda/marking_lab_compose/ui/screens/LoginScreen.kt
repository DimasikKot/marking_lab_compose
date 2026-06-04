package com.koolda.marking_lab_compose.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import com.koolda.marking_lab_compose.api.ApiClient
import com.koolda.marking_lab_compose.api.LoginRequest
import com.koolda.marking_lab_compose.util.TokenManager
import kotlinx.coroutines.launch


class LoginScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { LoginScreenModel() }
        LoginContent(screenModel)
    }
}

class LoginScreenModel : ScreenModel {
    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun handleLogin(navigator: Navigator?) {
        if (login.isBlank()) {
            errorMessage = "Введите имя пользователя или email"
            return
        }
        if (password.isBlank()) {
            errorMessage = "Введите пароль"
            return
        }
        screenModelScope.launch {
            isLoading = true
            try {
                val response = ApiClient.api.login(LoginRequest(login, password))
                TokenManager.accessToken = response.accessToken
                TokenManager.username = response.username
                navigator?.replaceAll(HomeScreen())
            } catch (e: Exception) {
                errorMessage = e.message ?: "Неверный логин или пароль"
            } finally {
                isLoading = false
            }
        }
    }
}

@Composable
fun LoginContent(screenModel: LoginScreenModel) {
    val navigator = LocalNavigator.current ?: return Text("Hi")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вход") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.Default.ArrowCircleLeft, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .widthIn(max = 400.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Вход в аккаунт",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Введите имя пользователя или email",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = screenModel.login,
                        onValueChange = { screenModel.login = it },
                        label = { Text("Имя пользователя или email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !screenModel.isLoading
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = screenModel.password,
                        onValueChange = { screenModel.password = it },
                        label = { Text("Пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !screenModel.isLoading
                    )

                    screenModel.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { screenModel.handleLogin(navigator) },
                        enabled = !screenModel.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (screenModel.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (screenModel.isLoading) "Вход..." else "Войти")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { navigator.push(RegisterScreen()) }) {
                        Text("Нет аккаунта? Зарегистрироваться")
                    }
                }
            }
        }
    }
}
