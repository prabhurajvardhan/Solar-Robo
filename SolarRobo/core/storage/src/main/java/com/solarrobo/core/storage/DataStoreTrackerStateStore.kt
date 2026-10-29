package com.solarrobo.core.storage

import com.solarrobo.core.tracker.PersistedTrackerState
import com.solarrobo.core.tracker.TrackerStateStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DataStoreTrackerStateStore @Inject constructor(
    private val store: SecurePreferencesStore
) : TrackerStateStore {
    override suspend fun load(): PersistedTrackerState? {
        val encoded = store.preferences.first()[KEY] ?: return null
        val parts = encoded.split('|')
        if (parts.size !in 7..10) return null
        return runCatching {
            PersistedTrackerState(
                thetaDeg = parts[0].toDouble(),
                thetaPerSecond = parts[1].toDouble(),
                calibrated = parts[2].toBooleanStrict(),
                lastTargetDeg = parts[3].toDouble(),
                signX = parts[4].toInt(),
                cameraBiasDeg = parts[5].toDouble(),
                homeDeclared = parts[6].toBooleanStrict(),
                windHold = parts.getOrNull(7)?.toBooleanStrict() ?: false,
                jammed = parts.getOrNull(8)?.toBooleanStrict() ?: false,
                faultCodes = parts.getOrNull(9)?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            )
        }.getOrNull()
    }

    override suspend fun save(state: PersistedTrackerState) {
        val encoded = listOf(
            state.thetaDeg, state.thetaPerSecond, state.calibrated, state.lastTargetDeg,
            state.signX, state.cameraBiasDeg, state.homeDeclared, state.windHold, state.jammed,
            state.faultCodes.sorted().joinToString(",")
        ).joinToString("|")
        store.update { current -> current + (KEY to encoded) }
    }

    private companion object { const val KEY = "tracker_state_v1" }
}
