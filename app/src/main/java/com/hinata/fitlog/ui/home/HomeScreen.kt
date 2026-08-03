package com.hinata.fitlog.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val summary by viewModel.summary.collectAsState()

    // 入力欄が無く項目数も固定のため、画面全体を単純にスクロール可能にしている
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("ホーム", style = MaterialTheme.typography.titleLarge)
        Text(
            "${summary.date} のサマリー",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )

        SummaryCard(
            label = "最新体重",
            // 体重の記録が1件も無い場合は 0 ではなく未記録として見せる
            value = summary.latestWeight?.let { "$it kg" } ?: "未記録",
            caption = summary.latestWeightDate?.let { "$it の記録" },
            modifier = Modifier.padding(top = 16.dp),
        )

        SummaryCard(
            label = "摂取カロリー",
            value = "${summary.intakeKcal} kcal",
            caption = "当日の食事の合計",
            modifier = Modifier.padding(top = 8.dp),
        )

        SummaryCard(
            label = "消費カロリー",
            value = "${summary.burnedKcal} kcal",
            caption = "当日のランニングの合計",
            modifier = Modifier.padding(top = 8.dp),
        )

        SummaryCard(
            label = "筋トレ種目数",
            value = "${summary.strengthExerciseCount} 種目",
            caption = "当日の記録",
            modifier = Modifier.padding(top = 8.dp),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            "体重推移グラフ・最近の記録（FR-07, FR-08）は後続の issue で実装予定",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    caption: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (caption != null) {
                    Text(caption, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
