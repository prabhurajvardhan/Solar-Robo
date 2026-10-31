package com.solarrobo.feature.auth.presentation

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.core.ui.theme.AstraMint
import com.solarrobo.core.ui.theme.AstraPrimary
import com.solarrobo.core.ui.theme.SolarRoboTheme

@Composable
fun LockScreen(viewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val prompt = remember(activity, viewModel) {
        activity?.let { owner ->
            BiometricPrompt(owner, ContextCompat.getMainExecutor(owner), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.unlockWithBiometric()
                }
            })
        }
    }
    LockScreenContent(
        state = state,
        onUnlock = viewModel::unlockWithPin,
        onViewer = viewModel::enterViewerMode,
        onBiometric = {
            prompt?.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock ASTRA")
                    .setSubtitle("Use the secure biometric or face unlock available on this phone")
                    .setAllowedAuthenticators(
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()
            )
        },
        modifier = modifier
    )
}

@Composable
fun LockScreenContent(
    state: AuthUiState,
    onUnlock: (String) -> Unit,
    onViewer: () -> Unit,
    onBiometric: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    fun append(digit: String) { if (pin.length < 6) pin += digit }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AstraAuthHero()
            Text("ASTRA", style = MaterialTheme.typography.headlineLarge, color = AstraPrimary, fontWeight = FontWeight.Bold)
            Text("AI SOLAR TRACKER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Welcome back${state.profile?.displayName?.let { ", $it" } ?: ""}", style = MaterialTheme.typography.titleLarge)
            Text("Enter PIN", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                repeat(6) { index ->
                    Box(Modifier.size(14.dp).background(if (index < pin.length) AstraPrimary else AstraMint, CircleShape))
                }
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            NumericPad(
                onDigit = ::append,
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                onSubmit = { if (pin.length == 6 && state.lockoutRemainingMs <= 0L) { onUnlock(pin); pin = "" } },
                enabled = state.lockoutRemainingMs <= 0L
            )
            if (state.profile?.biometricEnabled == true) {
                OutlinedButton(onClick = onBiometric, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Use fingerprint / face unlock")
                }
            }
            OutlinedButton(onClick = onViewer, modifier = Modifier.fillMaxWidth()) { Text("Continue as Viewer · read-only") }
            Text("ASTRA 2.1.0-astra-ui", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("PIN and biometric authentication stay local to this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NumericPad(onDigit: (String) -> Unit, onBackspace: () -> Unit, onSubmit: () -> Unit, enabled: Boolean) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit ->
                    PinKey(digit, enabled = enabled) { onDigit(digit) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PinKey("⌫", enabled = enabled, onClick = onBackspace)
            PinKey("0", enabled = enabled) { onDigit("0") }
            PinKey("✓", enabled = enabled, onClick = onSubmit)
        }
    }
}

@Composable
private fun PinKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.size(78.dp), shape = CircleShape) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun AstraAuthHero() {
    Box(Modifier.fillMaxWidth().height(190.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFEAF7F0), Color.White)))
            val center = Offset(size.width * .76f, size.height * .24f)
            drawCircle(Color(0xFFFFD45D), size.minDimension * .08f, center)
            val arc = Path().apply {
                moveTo(size.width * .08f, size.height * .48f)
                quadraticBezierTo(size.width * .46f, -size.height * .1f, size.width * .95f, size.height * .42f)
            }
            drawPath(arc, Color(0xFFE9BE35), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
            val panel = Path().apply {
                moveTo(size.width * .20f, size.height * .32f)
                lineTo(size.width * .70f, size.height * .46f)
                lineTo(size.width * .57f, size.height * .82f)
                lineTo(size.width * .08f, size.height * .66f)
                close()
            }
            drawPath(panel, AstraPrimary)
            for (i in 1..4) {
                val x = size.width * (.08f + i * .12f)
                drawLine(Color(0xFF9CC7B0), Offset(x, size.height * .36f), Offset(x - size.width * .1f, size.height * .72f), strokeWidth = 1.dp.toPx())
            }
            drawLine(AstraPrimary, Offset(size.width * .47f, size.height * .76f), Offset(size.width * .47f, size.height), strokeWidth = 8.dp.toPx())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LockScreenPreview() {
    SolarRoboTheme { LockScreenContent(AuthUiState(), {}, {}, {}) }
}
