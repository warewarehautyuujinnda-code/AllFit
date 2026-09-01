package com.hinata.fitlog.data

import android.content.Context
import androidx.core.content.edit
import com.hinata.fitlog.ui.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 下部ナビゲーションに表示するタブの保存先。記録ではなく設定値なので、
 * Room には載せず SharedPreferences に置く（WeightGoalStore と同じ方針）。
 *
 * [Destination.toggleable] が false の画面（ホーム・設定）はここでは扱わない。常に表示される。
 */
class TabVisibilityStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _visibleTabs = MutableStateFlow(read())

    /** 表示中のタブ。未設定（初回起動）なら全タブを表示扱いにする */
    val visibleTabs: StateFlow<Set<Destination>> = _visibleTabs.asStateFlow()

    fun setVisible(destination: Destination, visible: Boolean) {
        if (!destination.toggleable) return
        val current = _visibleTabs.value
        val updated = if (visible) current + destination else current - destination
        prefs.edit { putStringSet(KEY, updated.map { it.name }.toSet()) }
        _visibleTabs.value = updated
    }

    private fun read(): Set<Destination> {
        val toggleable = Destination.entries.filter { it.toggleable }
        val saved = prefs.getStringSet(KEY, null) ?: return toggleable.toSet()
        return saved.mapNotNull { name -> toggleable.find { it.name == name } }.toSet()
    }

    private companion object {
        const val PREFS = "fitlog_settings"
        const val KEY = "visible_tabs"
    }
}
