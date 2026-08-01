package com.hinata.fitlog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 各画面の骨組み。個別機能（FR-01〜14）は後続の issue で実装する。
 */
@Composable
fun PlaceholderScreen(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
fun HomeScreen() = PlaceholderScreen("ホーム", "当日サマリー・体重推移グラフ・最近の記録（FR-06〜08）を表示予定")

@Composable
fun RunningScreen() = PlaceholderScreen("ラン", "ランニングの入力と履歴（FR-03, FR-09）を実装予定")

@Composable
fun DataScreen() = PlaceholderScreen("データ", "書き出し・読み込み・統計・全削除（FR-11〜14）を実装予定")
