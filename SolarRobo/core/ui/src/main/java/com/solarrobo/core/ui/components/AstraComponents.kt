package com.solarrobo.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.core.ui.theme.AstraDanger
import com.solarrobo.core.ui.theme.AstraMint
import com.solarrobo.core.ui.theme.AstraOutline
import com.solarrobo.core.ui.theme.AstraPrimary
import com.solarrobo.core.ui.theme.AstraPrimaryDark
import com.solarrobo.core.ui.theme.SolarRoboTheme
import kotlin.math.cos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstraTopBar(title: String, onStop: () -> Unit, modifier: Modifier = Modifier) {
    var confirmStop by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(title, color = MaterialTheme.colorScheme.onPrimary) },
        actions = {
            IconButton(onClick = { confirmStop = true }) {
                Icon(Icons.Default.StopCircle, contentDescription = "STOP", tint = AstraDanger)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AstraPrimary),
        modifier = modifier
    )
    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Stop tracking?") },
            text = { Text("Send the emergency stop through the existing safety gate now?") },
            confirmButton = {
                OutlinedButton(onClick = { confirmStop = false; onStop() }) { Text("STOP", color = AstraDanger) }
            },
            dismissButton = { OutlinedButton(onClick = { confirmStop = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SourceTag(source: String, modifier: Modifier = Modifier) {
    val dotColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(7.dp)) { drawCircle(dotColor) }
        Text(source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MetricTile(label: String, value: String, source: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AstraMint),
        border = androidx.compose.foundation.BorderStroke(1.dp, AstraOutline)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
            SourceTag(source)
        }
    }
}

@Composable
fun AstraCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AstraOutline),
        content = content
    )
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun AlertBanner(message: String, danger: Boolean = false, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (danger) MaterialTheme.colorScheme.errorContainer else AstraMint)
    ) { Text(message, Modifier.padding(14.dp), color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) }
}

@Composable
fun SolarPanelHero(panelAngleDeg: Float?, status: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = com.solarrobo.core.ui.theme.AstraMintStrong),
        border = androidx.compose.foundation.BorderStroke(1.dp, AstraOutline)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Canvas(Modifier.size(142.dp)) {
                val angle = (panelAngleDeg ?: 0f).coerceIn(-70f, 70f)
                val tilt = cos(Math.toRadians(angle.toDouble())).toFloat()
                val panel = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * .12f, size.height * .28f)
                    lineTo(size.width * .82f, size.height * (.38f + .12f * (1f - tilt)))
                    lineTo(size.width * .70f, size.height * .72f)
                    lineTo(size.width * .05f, size.height * .60f)
                    close()
                }
                drawPath(panel, AstraPrimary)
                for (i in 1..4) {
                    val x = size.width * (.05f + i * .14f)
                    drawLine(com.solarrobo.core.ui.theme.AstraMintStrong, androidx.compose.ui.geometry.Offset(x, size.height * .31f), androidx.compose.ui.geometry.Offset(x - size.width * .08f, size.height * .62f), strokeWidth = 1.dp.toPx())
                }
                drawLine(AstraPrimaryDark, androidx.compose.ui.geometry.Offset(size.width * .42f, size.height * .68f), androidx.compose.ui.geometry.Offset(size.width * .42f, size.height), strokeWidth = 7.dp.toPx())
            }
            Column(Modifier.padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Clean energy", style = MaterialTheme.typography.titleMedium, color = AstraPrimary)
                Text("Smarter motion", style = MaterialTheme.typography.headlineMedium)
                Text(status, style = MaterialTheme.typography.bodyMedium)
                SourceTag(if (panelAngleDeg == null) "Not measured" else "Measured")
            }
        }
    }
}

data class AstraDestination(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AstraBottomNavigation(
    currentRoute: String?,
    destinations: List<AstraDestination>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AstraComponentsPreview() {
    SolarRoboTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Sun position", "Calculated from location and time")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Elevation", "Not measured", "Not measured", Modifier.weight(1f))
                MetricTile("Azimuth", "180°", "Calculated", Modifier.weight(1f))
            }
            AlertBanner("ESP32 link is offline")
        }
    }
}
