package com.example.wavelock

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(WaveConfig.PREFS, MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var style by remember { mutableIntStateOf(prefs.getInt(WaveConfig.STYLE, 0)) }
                var speed by remember { mutableFloatStateOf(prefs.getFloat(WaveConfig.SPEED, 1f)) }
                var amplitude by remember { mutableFloatStateOf(prefs.getFloat(WaveConfig.AMPLITUDE, .65f)) }
                var glow by remember { mutableFloatStateOf(prefs.getFloat(WaveConfig.GLOW, .7f)) }
                WaveSettingsScreen(
                    style, { style = it; prefs.edit().putInt(WaveConfig.STYLE, it).apply() },
                    speed, { speed = it; prefs.edit().putFloat(WaveConfig.SPEED, it).apply() },
                    amplitude, { amplitude = it; prefs.edit().putFloat(WaveConfig.AMPLITUDE, it).apply() },
                    glow, { glow = it; prefs.edit().putFloat(WaveConfig.GLOW, it).apply() },
                    onApply = { applyWallpaper() }
                )
            }
        }
    }

    private fun applyWallpaper() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@MainActivity, WaveWallpaperService::class.java)
            )
        }
        startActivity(intent)
    }
}

@Composable
private fun WaveSettingsScreen(
    style: Int, onStyle: (Int) -> Unit,
    speed: Float, onSpeed: (Float) -> Unit,
    amplitude: Float, onAmplitude: (Float) -> Unit,
    glow: Float, onGlow: (Float) -> Unit,
    onApply: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF071321))
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("WaveLock", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text("Живые волны для экрана блокировки", color = Color(0xFFB5C8D8))
        WavePreview(style, speed, amplitude, glow, Modifier.fillMaxWidth().height(230.dp))
        Text("Стиль волн", color = Color.White, style = MaterialTheme.typography.titleMedium)
        val names = listOf("Океан", "Спокойная вода", "Абстракция")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            names.forEachIndexed { index, name ->
                FilterChip(
                    selected = style == index,
                    onClick = { onStyle(index) },
                    label = { Text(name) }
                )
            }
        }
        SettingSlider("Скорость", speed, .25f..2f, "Медленно", "Быстро", onSpeed)
        SettingSlider("Высота волн", amplitude, .15f..1.2f, "Низкая", "Высокая", onAmplitude)
        SettingSlider("Свечение", glow, 0f..1f, "Мягкое", "Яркое", onGlow)
        Button(onClick = onApply, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text("Применить живые обои")
        }
        Text(
            "После нажатия Android откроет системный экран установки обоев. Если Realme UI предлагает выбор места, выбери экран блокировки.",
            color = Color(0xFF9FB2C4),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SettingSlider(
    title: String, value: Float, range: ClosedFloatingPointRange<Float>,
    left: String, right: String, onValue: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color.White)
            Text("${(value * 100).toInt()}%", color = Color(0xFF9FCFE0))
        }
        Slider(value = value, onValueChange = onValue, valueRange = range)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(left, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Text(right, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun WavePreview(
    style: Int, speed: Float, amplitude: Float, glow: Float, modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "preview")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            tween((7000 / speed).toInt().coerceAtLeast(1500), easing = LinearEasing),
            RepeatMode.Restart
        ), label = "phase"
    )
    Canvas(modifier.background(Color(0xFF06182D), RoundedCornerShape(22.dp))) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF09213D), Color(0xFF087D91), Color(0xFF03101F))))
        for (layer in 0..3) {
            val base = size.height * (.48f + layer * .12f)
            val path = Path().apply {
                moveTo(0f, size.height)
                for (x in 0..size.width.toInt() step 4) {
                    val wave = sin(x / size.width * 6.0 + phase * (1.0 + layer * .12) + layer).toFloat()
                    val y = base + wave * size.height * .045f * amplitude * (1f + layer * .12f)
                    lineTo(x.toFloat(), y)
                }
                lineTo(size.width, size.height); close()
            }
            val tint = when (style) {
                1 -> Color(0xFF56C8C8)
                2 -> listOf(Color(0xFF9B7BFF), Color(0xFF42D6C8), Color(0xFF55A8FF), Color(0xFF8DDBFF))[layer]
                else -> listOf(Color(0xFF0B6385), Color(0xFF087D9B), Color(0xFF16A6B0), Color(0xFF65D7D0))[layer]
            }
            drawPath(path, Brush.verticalGradient(listOf(tint.copy(alpha = .25f + glow * .25f), tint.copy(alpha = .9f))))
        }
    }
}
