package com.solarrobo.app.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.solarrobo.app.R
import com.solarrobo.core.ui.theme.InkPrimary
import com.solarrobo.core.ui.theme.HalfWhite
import com.solarrobo.core.ui.theme.OliveMutedTint
import kotlin.math.roundToInt

@Composable
fun MascotOverlay(route: String?, modifier: Modifier = Modifier) {
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    val bob by rememberInfiniteTransition(label = "mascot-idle").animateFloat(
        initialValue = 0f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "bob"
    )
    val walk by rememberInfiniteTransition(label = "mascot-walk").animateFloat(
        initialValue = 0f, targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "walk"
    )
    val expression = when (route) {
        "talk" -> "listening"
        "models" -> "thinking"
        "notifications" -> "concerned"
        "energy" -> "happy"
        else -> "wave"
    }
    Box(modifier.offset { IntOffset((x + walk).roundToInt(), (y + bob).roundToInt()) }.pointerInput(Unit) {
        detectDragGestures { change, drag ->
            change.consume()
            x += drag.x
            y += drag.y
        }
    }, contentAlignment = Alignment.BottomEnd) {
        Column(horizontalAlignment = Alignment.End) {
            Text(expression, color = InkPrimary, style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                modifier = Modifier.background(HalfWhite.copy(alpha = .94f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp))
            Image(painterResource(R.drawable.robo_mascot), "Robo mascot — drag to move", Modifier.size(82.dp).clip(RoundedCornerShape(28.dp)), contentScale = ContentScale.Crop)
        }
    }
}
