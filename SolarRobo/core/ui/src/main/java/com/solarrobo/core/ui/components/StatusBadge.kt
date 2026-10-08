package com.solarrobo.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarrobo.core.ui.theme.SolarEmerald
import com.solarrobo.core.ui.theme.SolarRoboTheme
import com.solarrobo.core.ui.theme.SolarRose

@Composable
fun StatusBadge(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = SolarEmerald,
    inactiveColor: Color = SolarRose
) {
    val color = if (isActive) activeColor else inactiveColor
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview
@Composable
private fun StatusBadgePreview() {
    SolarRoboTheme {
        StatusBadge(text = "Connected", isActive = true)
    }
}
