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
import com.koolda.marking_lab_compose.api.RegisterRequest
import com.koolda.marking_lab_compose.api.ValidateEmailRequest
import com.koolda.marking_lab_compose.api.ValidateUsernameRequest
import com.koolda.marking_lab_compose.util.TokenManager
import kotlinx.coroutines.launch


class RegisterScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { RegisterScreenModel() }
        RegisterContent(screenModel)
    }
}

sealed class RegisterStep {
    object Username : RegisterStep()
    object Email : RegisterStep()
    object Password : RegisterStep()
}

class RegisterScreenModel : ScreenModel {
    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var step by mutableStateOf<RegisterStep>(RegisterStep.Username)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun handleNext(navigator: Navigator?) {
        screenModelScope.launch {
            when (step) {
                is RegisterStep.Username -> {
                    if (username.isBlank()) {
                        errorMessage = "Введите имя пользователя"
                        return@launch
                    }
                    isLoading = true
                    try {
                        ApiClient.api.validateUsername(ValidateUsernameRequest(username))
                        step = RegisterStep.Email
                        errorMessage = null
                    } catch (e: Exception) {
                        errorMessage = e.message
                    } finally {
                        isLoading = false
                    }
                }

                is RegisterStep.Email -> {
                    if (email.isBlank()) {
                        errorMessage = "Введите электронную почту"
                        return@launch
                    }
                    isLoading = true
                    try {
                        ApiClient.api.validateEmail(ValidateEmailRequest(email))
                        step = RegisterStep.Password
                        errorMessage = null
                    } catch (e: Exception) {
                        errorMessage = e.message
                    } finally {
                        isLoading = false
                    }
                }

                is RegisterStep.Password -> {
                    if (password.isBlank()) {
                        errorMessage = "Введите пароль"
                        return@launch
                    }
                    isLoading = true
                    try {
                        val response = ApiClient.api.register(RegisterRequest(username, email, password))
                        TokenManager.accessToken = response.accessToken
                        TokenManager.username = response.username
                        navigator?.replaceAll(HomeScreen())
                    } catch (e: Exception) {
                        errorMessage = e.message
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    fun handleBack() {
        when (step) {
            is RegisterStep.Email -> step = RegisterStep.Username
            is RegisterStep.Password -> step = RegisterStep.Email
            else -> {}
        }
    }
}

@Composable
fun RegisterContent(screenModel: RegisterScreenModel) {
    val navigator = LocalNavigator.current ?: return Text("Hi")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Регистрация") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.Default.ArrowCircleLeft, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp).widthIn(max = 400.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (screenModel.step) {
                        is RegisterStep.Username -> {
                            Text(
                                text = "Регистрация аккаунта",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Придумайте имя пользователя",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = screenModel.username,
                                onValueChange = { screenModel.username = it },
                                label = { Text("Имя пользователя") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !screenModel.isLoading
                            )
                        }

                        is RegisterStep.Email -> {
                            Text(
                                text = "Электронная почта",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Для аккаунта: ${screenModel.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = screenModel.email,
                                onValueChange = { screenModel.email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !screenModel.isLoading
                            )
                        }

                        is RegisterStep.Password -> {
                            Text(
                                text = "Придумайте пароль",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Для аккаунта: ${screenModel.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = screenModel.password,
                                onValueChange = { screenModel.password = it },
                                label = { Text("Пароль") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !screenModel.isLoading
                            )
                        }
                    }

                    screenModel.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (screenModel.step !is RegisterStep.Username) {
                            TextButton(onClick = { screenModel.handleBack() }) {
                                Text("Назад")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = { screenModel.handleNext(navigator) },
                            enabled = !screenModel.isLoading
                        ) {
                            if (screenModel.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                when (screenModel.step) {
                                    is RegisterStep.Password ->
                                        if (screenModel.isLoading) "Регистрируем..." else "Зарегистрироваться"
                                    else -> "Далее"
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { navigator.push(LoginScreen()) }) {
                        Text("Уже есть аккаунт? Войти")
                    }
                }
            }
        }
    }
}
