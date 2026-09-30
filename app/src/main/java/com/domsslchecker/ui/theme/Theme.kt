package com.domsslchecker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6FED),
    secondary = Color(0xFF006874),
    tertiary = Color(0xFF6B5E00),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB1C5FF),
    secondary = Color(0xFF4FD8EB),
    tertiary = Color(0xFFDBC66E),
)

@Composable
fun DomSSLCheckerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}

