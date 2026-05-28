package com.koolda.marking_lab_compose.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.screenmodel.ScreenModel
import cafe.adriel.voyager.screenmodel.rememberScreenModel
import com.koolda.marking_lab_compose.api.ApiClient
import com.koolda.marking_lab_compose.api.LoginRequest
import com.koolda.marking_lab_compose.api.UserResponse
import com.koolda.marking_lab_compose.api.ValidateLoginRequest
import com.koolda.marking_lab_compose.ui.navigation.Screen
import com.koolda.marking_lab_compose.util.TokenManager
import kotlinx.coroutines.launch

sealed class LoginStep {
    object Login : LoginStep()
    object Password : LoginStep()
}

class LoginScreenModel : ScreenModel {
    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var step by mutableStateOf<LoginStep>(LoginStep.Login)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    
    fun handleNext(navigator: Navigator) {
        coroutineScope.launch {
            when (step) {
                is LoginStep.Login -> {
                    if (login.isBlank()) {
                        errorMessage = "Введите имя пользователя или email"
                        return@launch
                    }
                    
                    isLoading = true
                    try {
                        ApiClient.api.validateLogin(ValidateLoginRequest(login))
                        step = LoginStep.Password
                        errorMessage = null
                    } catch (e: Exception) {
                        errorMessage = e.message
                    } finally {
                        isLoading = false
                    }
                }
                is LoginStep.Password -> {
                    if (password.isBlank()) {
                        errorMessage = "Введите пароль"
                        return@launch
                    }
                    
                    isLoading = true
                    try {
                        val response = ApiClient.api.login(LoginRequest(login, password))
                        TokenManager.accessToken = response.accessToken
                        TokenManager.username = response.username
                        navigator.replaceAll(Screen.Home)
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
        if (step is LoginStep.Password) {
            step = LoginStep.Login
        }
    }
}

@Composable
override fun Content() {
    val navigator = Navigator.current
    val screenModel: LoginScreenModel = rememberScreenModel()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вход") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
                    when (screenModel.step) {
                        is LoginStep.Login -> {
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
                        }
                        is LoginStep.Password -> {
                            Text(
                                text = "Введите пароль",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Для аккаунта: ${screenModel.login}",
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
                        if (screenModel.step is LoginStep.Password) {
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
                                    is LoginStep.Login -> "Далее"
                                    is LoginStep.Password -> if (screenModel.isLoading) "Вход..." else "Войти"
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(onClick = { navigator.push(Screen.Register) }) {
                        Text("Нет аккаунта? Зарегистрироваться")
                    }
                }
            }
        }
    }
}
