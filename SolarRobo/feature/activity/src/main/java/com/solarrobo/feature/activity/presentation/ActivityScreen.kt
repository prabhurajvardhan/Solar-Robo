package com.solarrobo.feature.activity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.ActivityType
import com.solarrobo.feature.activity.components.ActivityItem
import com.solarrobo.feature.activity.mock.FakeActivityRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Activity & Robo Memory") }) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filterType == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text("All") }
                )
                ActivityType.entries.take(3).forEach { type ->
                    FilterChip(
                        selected = state.filterType == type,
                        onClick = { viewModel.setFilter(type) },
                        label = { Text(type.name) }
                    )
                }
            }

            if (state.isLoading) {
                Text("Loading activity log...")
                return@Scaffold
            }

            if (state.errorMessage != null) {
                Text(state.errorMessage)
            }

            if (state.events.isEmpty()) {
                Text("No activity records available.")
                return@Scaffold
            }

            LazyColumn {
                items(state.events) { event ->
                    ActivityItem(event = event)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityScreenPreview() {
    val viewModel = ActivityViewModel(FakeActivityRepository())
    ActivityScreen(viewModel = viewModel)
}
