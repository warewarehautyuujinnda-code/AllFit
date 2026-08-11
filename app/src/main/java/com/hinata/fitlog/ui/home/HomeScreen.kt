package com.hinata.fitlog.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hinata.fitlog.domain.HomeSummary
import com.hinata.fitlog.domain.RecentRecord
import com.hinata.fitlog.domain.RecordKind
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.ui.common.DateUtil

/**
 * ホーム画面。当日サマリー（FR-06）・体重推移グラフ（FR-07）・最近の記録（FR-08）。
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val summary by viewModel.summary.collectAsState()
    val trend by viewModel.weightTrend.collectAsState()
    val recent by viewModel.recentRecords.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("今日のサマリー", style = MaterialTheme.typography.titleLarge)
            Text(
                DateUtil.today(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            SummaryGrid(summary)
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            WeightChart(trend)
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            Text("最近の記録", style = MaterialTheme.typography.titleMedium)
        }

        if (recent.isEmpty()) {
            item {
                Text(
                    "まだ記録がありません。下のタブから記録を追加してください",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(recent, key = { "${it.kind}-${it.id}" }) { record ->
                RecentRecordRow(record)
            }
        }
    }
}

/** 当日サマリーの4項目（FR-06）。2列×2行に並べる */
@Composable
private fun SummaryGrid(summary: HomeSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(
                label = "最新体重",
                // 記録が1件もない場合は 0 ではなく未記録と分かる表示にする
                value = summary.latestWeight?.let { "${formatAmount(it.weight)} kg" } ?: "未記録",
                // いつ時点の体重かが分からないと判断できないため日付を添える
                note = summary.latestWeight?.date ?: "体重タブから記録できます",
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                label = "筋トレ種目数",
                value = "${summary.strengthExerciseCount} 種目",
                note = "今日",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(
                label = "摂取カロリー",
                value = "${summary.intakeKcal} kcal",
                note = "今日の食事",
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                label = "消費カロリー",
                value = "${summary.burnedKcal} kcal",
                note = "今日のラン",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    note: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 最近の記録一覧の1行（FR-08）。種別バッジで4種別を見分けられるようにする */
@Composable
private fun RecentRecordRow(record: RecentRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KindBadge(record.kind)
            Column(modifier = Modifier.weight(1f)) {
                Text(record.date, style = MaterialTheme.typography.bodySmall)
                Text(record.title, style = MaterialTheme.typography.bodyLarge)
                if (record.detail.isNotEmpty()) {
                    Text(record.detail, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun KindBadge(kind: RecordKind) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            kind.label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
