package com.solarrobo.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.core.contracts.TrackerDailySummary
import com.solarrobo.core.contracts.TrackerLogRecord
import com.solarrobo.core.contracts.TrackerLogSink
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

 data class InsightsUiState(
    val recent: List<TrackerLogRecord> = emptyList(),
    val daily: List<TrackerDailySummary> = emptyList()
)

@HiltViewModel
class InsightsViewModel @Inject constructor(logSink: TrackerLogSink) : ViewModel() {
    val uiState: StateFlow<InsightsUiState> = combine(
        logSink.observeRecent(2_000),
        logSink.observeDailySummaries()
    ) { recent, daily -> InsightsUiState(recent, daily) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun latestCsv(): String = buildString {
        appendLine("t,az,el,th_sun,lum,blue_frac,white_frac,sun_found,cx,area,jitter,est,tps,wind_hold,jam,action")
        uiState.value.recent.sortedBy { it.t }.forEach { row ->
            val fields = listOf(
                row.t, row.az, row.el, row.thSun, row.lum, row.blueFrac, row.whiteFrac,
                row.sunFound, row.cx, row.area, row.jitter, row.est, row.tps,
                row.windHold, row.jam, row.action
            )
            appendLine(fields.joinToString(",") { it?.toString().orEmpty().csvCell() })
        }
    }

    private fun String.csvCell(): String = "\"${replace("\"", "\"\"")}\""
}
