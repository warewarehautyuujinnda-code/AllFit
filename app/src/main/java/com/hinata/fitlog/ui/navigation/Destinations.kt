package com.hinata.fitlog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 下部ナビゲーションの画面（要件定義書 §9 画面一覧 + 設定）。
 *
 * [toggleable] が true の画面だけ、設定画面から下部ナビゲーションへの表示/非表示を切り替えられる。
 * ホームと設定は常に表示する（タブを0個にできないようにするための固定枠）。
 */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val toggleable: Boolean = true,
) {
    HOME("home", "ホーム", Icons.Filled.Home, toggleable = false),
    WEIGHT("weight", "体重", Icons.Filled.MonitorWeight),
    STRENGTH("strength", "筋トレ", Icons.Filled.FitnessCenter),
    RUNNING("running", "ラン", Icons.AutoMirrored.Filled.DirectionsRun),
    MEAL("meal", "食事", Icons.Filled.Restaurant),
    DATA("data", "データ", Icons.Filled.Storage),
    SETTINGS("settings", "設定", Icons.Filled.Settings, toggleable = false),
}
