package com.solarrobo.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

/** The child stays visually disabled; tapping its transparent overlay explains how to unlock it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerAccessTooltip(
    isOperator: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    val state = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text("Unlock with your local PIN to use this control.") } },
        state = state,
        modifier = modifier
    ) {
        Box {
            content(Modifier)
            if (!isOperator) {
                Box(
                    Modifier
                        .matchParentSize()
                        .pointerInput(state) {
                            detectTapGestures(onTap = { scope.launch { state.show() } })
                        }
                        .semantics {
                            contentDescription = "Disabled in Viewer mode. Unlock as Operator to enable."
                            onClick(label = "Why is this control disabled") {
                                scope.launch { state.show() }
                                true
                            }
                        }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ViewerAccessTooltipPreview() {
    MaterialTheme {
        ViewerAccessTooltip(isOperator = false) { modifier ->
            Button(onClick = {}, enabled = false, modifier = modifier) { Text("Disabled action") }
        }
    }
}
