package com.koolda.marking_lab_compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import com.koolda.marking_lab_compose.ui.screens.HomeScreen

@Composable
//@Preview
fun App() {
    MaterialTheme {
        // Инициализируем Voyager и передаем HomeScreen в качестве стартового экрана
        Navigator(screen = HomeScreen())
    }
}