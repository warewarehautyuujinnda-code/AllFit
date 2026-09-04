package com.hinata.fitlog.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hinata.fitlog.domain.HomeSummary
import com.hinata.fitlog.domain.WeeklyGoalSummary
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.weightGoalDifferenceLabel
import com.hinata.fitlog.ui.common.DateUtil

/**
 * ホーム画面。当日サマリー（FR-06）・体重推移グラフ（FR-07）。
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val summary by viewModel.summary.collectAsState()
    val trend by viewModel.weightTrend.collectAsState()
    val period by viewModel.weightTrendPeriod.collectAsState()
    val goal by viewModel.weightGoal.collectAsState()
    val hasOlderRecords by viewModel.hasOlderWeightRecords.collectAsState()
    val weeklyGoal by viewModel.weeklyGoalSummary.collectAsState()

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
            WeeklyGoalSection(weeklyGoal)
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            WeightChart(
                trend = trend,
                goal = goal,
                period = period,
                onPeriodChange = viewModel::onWeightTrendPeriodChange,
                hasRecordsBeforePeriod = hasOlderRecords,
            )
        }
    }
}

/** 長期目標と今週の筋トレ・ラン・体重目標を、当日サマリーの直後にまとめて表示する。 */
@Composable
private fun WeeklyGoalSection(summary: WeeklyGoalSummary) {
    val hasStrength = summary.strength != null && summary.strength.plannedCount > 0
    val hasRunning = summary.runningTargetKm != null
    val hasWeight = summary.weightGoal != null
    if (summary.goalTitle == null && !hasStrength && !hasRunning && !hasWeight) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("今週の目標", style = MaterialTheme.typography.titleLarge)
        summary.goalTitle?.let { title ->
            Text(
                summary.goalTargetDate?.let { "$title（${it}まで）" } ?: title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasStrength) {
                val progress = summary.strength!!
                SummaryCard("筋トレ", "${progress.doneCount}/${progress.plannedCount} 種目", "今週実施", Modifier.weight(1f))
            }
            if (hasRunning) {
                SummaryCard("ラン", "${formatAmount(summary.runningActualKm ?: 0.0)} km", "目標 ${formatAmount(summary.runningTargetKm!!)} km", Modifier.weight(1f))
            }
            if (hasWeight) {
                val goal = summary.weightGoal!!
                SummaryCard(
                    "体重",
                    summary.weightCurrent?.let { "${formatAmount(it)} kg" } ?: "未記録",
                    weightGoalDifferenceLabel(summary.weightCurrent, goal)
                        ?: "目標 ${formatAmount(goal)} kg",
                    Modifier.weight(1f),
                )
            }
        }
    }
}

/** 当日サマリーの2項目（FR-06）。1行に並べる */
@Composable
private fun SummaryGrid(summary: HomeSummary) {
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
            label = "摂取カロリー",
            value = "${summary.intakeKcal} kcal",
            note = "今日の食事",
            modifier = Modifier.weight(1f),
        )
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
