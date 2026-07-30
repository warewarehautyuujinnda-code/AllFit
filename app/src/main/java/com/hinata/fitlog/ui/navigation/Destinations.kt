package com.hinata.fitlog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 下部ナビゲーションの6画面（要件定義書 §9 画面一覧）。
 */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "ホーム", Icons.Filled.Home),
    WEIGHT("weight", "体重", Icons.Filled.MonitorWeight),
    STRENGTH("strength", "筋トレ", Icons.Filled.FitnessCenter),
    RUNNING("running", "ラン", Icons.AutoMirrored.Filled.DirectionsRun),
    MEAL("meal", "食事", Icons.Filled.Restaurant),
    DATA("data", "データ", Icons.Filled.Storage),
}
