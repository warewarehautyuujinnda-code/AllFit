package com.hinata.fitlog.ui.weight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hinata.fitlog.data.entity.WeightEntity
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.ui.common.DatePickerField
import com.hinata.fitlog.ui.common.DateUtil
import com.hinata.fitlog.ui.common.RecordCard
import com.hinata.fitlog.ui.home.WeightChart
import kotlinx.coroutines.launch

@Composable
fun WeightScreen(viewModel: WeightViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()
    val trend by viewModel.trend.collectAsState()
    val period by viewModel.period.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val hasOlderRecords by viewModel.hasOlderRecords.collectAsState()

    var date by remember { mutableStateOf(DateUtil.today()) }
    var weight by remember { mutableStateOf("") }
    var showGoalDialog by remember { mutableStateOf(false) }
    // 記録が増えても入力欄が押し下げられないよう、一覧は既定で畳んでおく
    var historyExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // 履歴の遅延読み込みを保つため、入力フォームと履歴を1つの LazyColumn にまとめている
        // （Column に verticalScroll を付けて LazyColumn を入れ子にすると測定時に例外になる）。
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(16.dp),
        ) {
            item {
                Text("体重の記録", style = MaterialTheme.typography.titleLarge)
            }

            item {
                DatePickerField(
                    value = date,
                    onValueChange = { date = it },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("体重 (kg) ※必須") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            item {
                Button(
                    onClick = {
                        val ok = viewModel.save(date, weight)
                        scope.launch {
                            if (ok) {
                                weight = ""
                                snackbarHostState.showSnackbar("保存しました")
                            } else {
                                snackbarHostState.showSnackbar("体重を数値で入力してください")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text("保存")
                }
            }

            item {
                WeightChart(
                    trend = trend,
                    goal = goal,
                    period = period,
                    onPeriodChange = viewModel::onPeriodChange,
                    modifier = Modifier.padding(top = 16.dp),
                    onGoalClick = { showGoalDialog = true },
                    hasRecordsBeforePeriod = hasOlderRecords,
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { historyExpanded = !historyExpanded }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "記録一覧（${items.size}件）",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Icon(
                        if (historyExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (historyExpanded) "記録一覧を閉じる" else "記録一覧を開く",
                    )
                }
            }

            if (historyExpanded) {
                if (items.isEmpty()) {
                    item {
                        Text(
                            "まだ記録がありません",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                } else {
                    items(items, key = { it.id }) { item ->
                        WeightRow(
                            item = item,
                            onDelete = { viewModel.delete(item) },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }

    if (showGoalDialog) {
        WeightGoalDialog(
            current = goal,
            onDismiss = { showGoalDialog = false },
            onSave = { viewModel.setGoal(it) },
            onClear = { viewModel.clearGoal() },
        )
    }
}

/**
 * 目標体重の設定ダイアログ。
 * @param onSave 入力が数値として読めたら true を返す。false なら閉じずに入力し直させる
 */
@Composable
private fun WeightGoalDialog(
    current: Double?,
    onDismiss: () -> Unit,
    onSave: (String) -> Boolean,
    onClear: () -> Unit,
) {
    var text by remember { mutableStateOf(current?.let { formatAmount(it) } ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("目標体重") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        isError = false
                    },
                    label = { Text("目標体重 (kg)") },
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isError) {
                    Text(
                        "体重を数値で入力してください",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (onSave(text)) onDismiss() else isError = true
            }) { Text("保存") }
        },
        dismissButton = {
            Row {
                // 設定済みのときだけ、未設定に戻す手段を出す
                if (current != null) {
                    TextButton(onClick = {
                        onClear()
                        onDismiss()
                    }) { Text("クリア") }
                }
                TextButton(onClick = onDismiss) { Text("キャンセル") }
            }
        },
    )
}

@Composable
private fun WeightRow(
    item: WeightEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RecordCard(
        summary = "${item.date}　${formatAmount(item.weight)} kg",
        onDelete = onDelete,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(item.date, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${formatAmount(item.weight)} kg",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
