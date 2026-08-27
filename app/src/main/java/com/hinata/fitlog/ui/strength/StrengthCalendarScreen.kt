package com.hinata.fitlog.ui.strength

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import com.hinata.fitlog.data.entity.StrengthSetEntity
import com.hinata.fitlog.domain.ExerciseStats
import com.hinata.fitlog.domain.dayStrengthOf
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatGrouped
import com.hinata.fitlog.domain.partBadgesByDate
import java.time.LocalDate
import java.time.YearMonth

/**
 * 筋トレタブの既定画面。月カレンダーで記録のある日を俯瞰し、選んだ日の集計を下に出す。
 */
@Composable
fun StrengthCalendarScreen(
    records: List<StrengthRecordWithSets>,
    today: LocalDate,
    selectedDate: LocalDate,
    snackbarHostState: SnackbarHostState,
    onSelectDate: (LocalDate) -> Unit,
    onAdd: () -> Unit,
    onDeleteRecords: (List<String>) -> Unit,
) {
    // 日付を選び直すたびにその月へ合わせる。前後の月めくりは選択日を変えないので状態が残る
    var visibleMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    // 日付を選び直したら、前の日付のカードに対する詳細ダイアログは閉じる
    var detailExercise by remember(selectedDate) { mutableStateOf<ExerciseStats?>(null) }

    val badges = remember(records) { partBadgesByDate(records) }
    val day = remember(records, selectedDate) { dayStrengthOf(records, selectedDate.toString()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                MonthCalendar(
                    yearMonth = visibleMonth,
                    selectedDate = selectedDate,
                    today = today,
                    badgesByDate = badges,
                    onPrevMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                    onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                    onSelectDate = onSelectDate,
                    onToday = { onSelectDate(today) },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            // 追加ボタンは浮かせず見出しの行に置く。カレンダーが画面の大半を占めるため
            // 記録カードは常に最下部に来てしまい、FAB にすると最右列の値が隠れて読めない
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${selectedDate.year}年${selectedDate.monthValue}月" +
                                "${selectedDate.dayOfMonth}日(${japaneseWeekday(selectedDate)})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "総ボリューム ${formatGrouped(day.totalVolume)} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    FilledTonalButton(onClick = onAdd) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("記録する", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            if (day.exercises.isEmpty()) {
                item {
                    Text(
                        "記録がありません",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            } else {
                items(day.exercises, key = { it.ex }) { stats ->
                    ExerciseStatsCard(
                        stats = stats,
                        dateLabel = selectedDate.toString(),
                        onDelete = { onDeleteRecords(stats.recordIds) },
                        onClick = { detailExercise = stats },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            item { Box(modifier = Modifier.height(16.dp)) }
        }
    }

    detailExercise?.let { stats ->
        ExerciseRecordsDialog(
            stats = stats,
            records = records.filter { it.record.id in stats.recordIds },
            onDismiss = { detailExercise = null },
        )
    }
}

@Composable
private fun ExerciseStatsCard(
    stats: ExerciseStats,
    dateLabel: String,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirm by remember { mutableStateOf(false) }

    // カード全体をタップで個々の記録の詳細を開く。右端の削除ボタンは自身の領域で
    // タップを消費するため、このクリックとは競合しない
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(bodyPartColor(stats.part))
                )
                Text(
                    stats.ex,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                IconButton(onClick = { showConfirm = true }) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "この種目の記録を削除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
            ) {
                Metric("セット", "${stats.sets} セット", Modifier.weight(1f))
                MetricDivider()
                Metric("ボリューム", "${formatGrouped(stats.volume)} kg", Modifier.weight(1f))
                MetricDivider()
                Metric(
                    "推定1RM",
                    stats.oneRepMax?.let { "${formatGrouped(it)} kg" } ?: "-",
                    Modifier.weight(1f),
                )
                MetricDivider()
                Metric("総レップ数", "${stats.totalReps} 回", Modifier.weight(1f))
                MetricDivider()
                Metric(
                    "最大レップ数",
                    stats.maxReps?.let { "$it 回" } ?: "-",
                    Modifier.weight(1f),
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("記録を削除しますか？") },
            text = {
                Text(
                    "$dateLabel　${stats.ex}（${stats.recordIds.size}件）\n\n削除すると元に戻せません。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onDelete()
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("キャンセル") }
            },
        )
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 2.dp),
    ) {
        Text(
            label,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            value,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun MetricDivider() {
    VerticalDivider(modifier = Modifier.height(32.dp))
}

/**
 * カードは集計しか出さないため、その日その種目の個々の記録をセット単位で一覧に見せる。
 * 新しい画面遷移は作らず、カレンダー画面内で完結するダイアログにしている。
 */
@Composable
private fun ExerciseRecordsDialog(
    stats: ExerciseStats,
    records: List<StrengthRecordWithSets>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stats.ex) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                records.forEachIndexed { index, record ->
                    Text(
                        "${index + 1}件目",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 12.dp),
                    )
                    record.sets.sortedBy { it.setIndex }.forEachIndexed { setIndex, set ->
                        Text(
                            "${setIndex + 1}セット目: ${describeSet(set)}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}

/** 重量・回数のうち入力がある項目だけをつなげる。両方未入力（自重トレ等）もありうる */
private fun describeSet(set: StrengthSetEntity): String {
    val parts = listOfNotNull(
        set.weight?.let { "${formatAmount(it)} kg" },
        set.reps?.let { "$it 回" },
    )
    return if (parts.isEmpty()) "記録あり" else parts.joinToString(" × ")
}
