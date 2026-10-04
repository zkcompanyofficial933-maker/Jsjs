package com.cineai.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val Dark=darkColorScheme(primary=Color(0xFFE8B36A),secondary=Color(0xFF8FB5FF),background=Color(0xFF090A0C),surface=Color(0xFF121417),surfaceVariant=Color(0xFF1A1D21))
@Composable fun CineTheme(content: @Composable () -> Unit){MaterialTheme(colorScheme=Dark,content=content)}
