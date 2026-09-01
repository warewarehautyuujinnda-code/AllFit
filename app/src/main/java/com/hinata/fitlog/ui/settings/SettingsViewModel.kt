package com.hinata.fitlog.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.ui.navigation.Destination
import kotlinx.coroutines.flow.StateFlow

/**
 * 設定画面。下部ナビゲーションに表示するタブの切り替えを担う。
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val store = (app as FitLogApp).tabVisibilityStore

    /** 表示中のタブ。ホーム・設定は対象外（常に表示されるため候補に出さない） */
    val visibleTabs: StateFlow<Set<Destination>> = store.visibleTabs

    /** タブの表示/非表示を切り替える */
    fun setTabVisible(destination: Destination, visible: Boolean) {
        store.setVisible(destination, visible)
    }
}
