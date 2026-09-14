package com.hinata.fitlog.ui.running

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.RunningPointEntity
import com.hinata.fitlog.data.entity.RunningSplitEntity
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatPace
import kotlinx.coroutines.launch

/**
 * 記録の詳細。GPS計測した記録には走った経路と、1分ごとの内訳（何分時点で何km、その時点のペース）
 * を出す。手入力の記録、またはGPS計測に失敗した記録ではどちらもない旨を表示する。
 *
 * メモはこの画面で書く。走っている最中は入力できないため、走り終わってこの画面を開いたときに
 * その回の感覚を書き足せる場所として置いている（GPS計測の記録は保存時点ではメモを持てない）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningDetailScreen(
    item: RunningEntity,
    splits: List<RunningSplitEntity>,
    points: List<RunningPointEntity>,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSaveMemo: (String) -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                .imePadding()
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

            MemoSection(
                memo = item.memo,
                onSave = { text ->
                    onSaveMemo(text)
                    scope.launch { snackbarHostState.showSnackbar("メモを保存しました") }
                },
                modifier = Modifier.padding(top = 16.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("経路", style = MaterialTheme.typography.titleMedium)
            RunningRouteMap(points = points, modifier = Modifier.padding(top = 8.dp))

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

/**
 * その回のメモ。読むだけでなくこの場で書き換えられる。
 *
 * メモがまだ無い記録でも見出しごと隠さずに出すのは、走ったあとにここへ書けること自体に
 * 気づけるようにするため（GPS計測の記録は必ずメモ無しの状態で保存される）。
 */
@Composable
private fun MemoSection(memo: String?, onSave: (String) -> Unit, modifier: Modifier = Modifier) {
    // 保存してDBの内容が変わったら、書きかけの下書きはその内容で引き直す
    var editing by rememberSaveable(memo) { mutableStateOf(false) }
    var draft by rememberSaveable(memo) { mutableStateOf(memo.orEmpty()) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "メモ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 「キャンセル」「保存」は入力欄の下ではなくこの見出しの行に置く。この画面は縦スクロール
            // しないため、下に置くとキーボードが出た狭い画面で入力欄に押し出されて押せなくなる。
            if (editing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        draft = memo.orEmpty()
                        editing = false
                    }) { Text("キャンセル") }
                    TextButton(onClick = {
                        onSave(draft)
                        editing = false
                    }) { Text("保存") }
                }
            } else {
                TextButton(onClick = { editing = true }) {
                    Text(if (memo.isNullOrBlank()) "メモを書く" else "編集")
                }
            }
        }

        if (editing) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("例: 後半で脚が残った。呼吸は最後まで乱れなかった") },
                supportingText = { Text("走り終わったあとの感覚・コース・体調など") },
                // 長いメモを書いても入力欄が画面いっぱいに伸びて他を押し出さないよう、高さの上限を決める
                // （これ以上は入力欄の中がスクロールする）
                minLines = 3,
                maxLines = MEMO_EDITOR_MAX_LINES,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        } else if (memo.isNullOrBlank()) {
            Text(
                "まだメモがありません",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Text(
                memo,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
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

/** メモの入力欄の高さの上限（行数）。これを超えると入力欄の中がスクロールする */
private const val MEMO_EDITOR_MAX_LINES = 6
