package com.hinata.fitlog.running

import com.hinata.fitlog.data.entity.RunningSplitEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RunStatus { IDLE, TRACKING }

/** GPS計測中の状態。メイン画面のタイマー表示はこれをそのまま購読する */
data class RunTrackState(
    val status: RunStatus = RunStatus.IDLE,
    val elapsedSec: Long = 0,
    val distanceKm: Double = 0.0,
    val splits: List<RunningSplitEntity> = emptyList(),
)

/**
 * GPS計測中の状態を [RunTrackingService] と Compose 側（[com.hinata.fitlog.ui.running]）で
 * 共有するための入れ物。計測はアプリ内で完結する（外部プロセスと通信しない）ため、
 * プロセス間通信を使わずアプリ内シングルトンとして持つ。
 */
object RunTracker {
    private val _state = MutableStateFlow(RunTrackState())
    val state: StateFlow<RunTrackState> = _state.asStateFlow()

    internal fun update(newState: RunTrackState) {
        _state.value = newState
    }

    internal fun reset() {
        _state.value = RunTrackState()
    }
}
