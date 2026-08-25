package com.hinata.fitlog.ui.running

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.RunningSplitEntity
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatPace

/**
 * 記録の詳細。GPS計測した記録には1分ごとの内訳（何分時点で何km、その時点のペース）を出す。
 * 手入力の記録、またはGPS計測に失敗した記録では内訳がない旨を表示する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningDetailScreen(
    item: RunningEntity,
    splits: List<RunningSplitEntity>,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.date) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { showConfirm = true }) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "この記録を削除",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SummaryStat(label = "距離", value = "${formatAmount(item.dist)} km")
                SummaryStat(label = "時間", value = item.min?.let { "${formatAmount(it)} 分" } ?: "—")
                SummaryStat(
                    label = "平均ペース",
                    value = formatPace(item.dist, item.min)?.let { "$it /km" } ?: "—",
                )
            }

            if (!item.memo.isNullOrBlank()) {
                Text(
                    "メモ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    item.memo,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("1分ごとの内訳", style = MaterialTheme.typography.titleMedium)

            if (splits.isEmpty()) {
                Text(
                    "内訳データなし（手入力の記録、またはGPS計測が行われなかった記録です）",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    itemsIndexed(splits, key = { _, split -> split.id }) { index, split ->
                        val previousDistance = if (index == 0) 0.0 else splits[index - 1].distanceKm
                        val minutePace = formatPace(split.distanceKm - previousDistance, 1.0)
                        SplitRow(
                            minuteIndex = split.minuteIndex,
                            cumulativeDistanceKm = split.distanceKm,
                            pace = minutePace,
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("記録を削除しますか？") },
            text = {
                Text("${item.date}　${formatAmount(item.dist)} km\n\n削除すると元に戻せません。")
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
private fun SummaryStat(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SplitRow(minuteIndex: Int, cumulativeDistanceKm: Double, pace: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("${minuteIndex}分", style = MaterialTheme.typography.bodyLarge)
        Text("${formatAmount(cumulativeDistanceKm)} km", style = MaterialTheme.typography.bodyLarge)
        Text(
            pace?.let { "$it /km" } ?: "—",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
