package com.hinata.fitlog.ui.running

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatPace
import com.hinata.fitlog.ui.common.DatePickerField
import com.hinata.fitlog.ui.common.DateUtil
import com.hinata.fitlog.ui.common.RecordCard
import kotlinx.coroutines.launch

@Composable
fun RunningScreen(viewModel: RunningViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()

    var date by remember { mutableStateOf(DateUtil.today()) }
    var dist by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }

    // 距離と時間が入力されている間だけ、保存前でもペースを表示する（FR-03）
    val pace = formatPace(
        dist.trim().toDoubleOrNull(),
        minutes.trim().toDoubleOrNull(),
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // 筋トレ画面と同様、ソフトキーボード表示時に画面が収まらないため全体をスクロール可能にし、
        // 入力フォームと履歴を1つの LazyColumn にまとめている
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(16.dp),
        ) {
            item {
                Text("ランニングの記録", style = MaterialTheme.typography.titleLarge)
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
                    value = dist,
                    onValueChange = { dist = it },
                    label = { Text("距離 (km) ※必須") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    label = { Text("時間 (分) ※任意") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            item {
                Text(
                    text = if (pace != null) "ペース: $pace /km" else "ペース: 距離と時間を入力すると表示されます",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            item {
                Button(
                    onClick = {
                        val ok = viewModel.save(date, dist, minutes)
                        scope.launch {
                            if (ok) {
                                dist = ""
                                minutes = ""
                                snackbarHostState.showSnackbar("保存しました")
                            } else {
                                snackbarHostState.showSnackbar(
                                    "距離を入力し、時間は数値で入力してください"
                                )
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            item {
                Text(
                    "記録一覧（${items.size}件）",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

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
                    RunningRow(
                        item = item,
                        onDelete = { viewModel.delete(item) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RunningRow(
    item: RunningEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RecordCard(
        summary = "${item.date}　${formatAmount(item.dist)} km",
        onDelete = onDelete,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(item.date, style = MaterialTheme.typography.bodySmall)
            Text("${formatAmount(item.dist)} km", style = MaterialTheme.typography.bodyLarge)
            // 時間は任意のため、入力があるものだけを並べる。
            // ペースは距離と時間が揃っているときだけ計算できる
            val detail = listOfNotNull(
                item.min?.let { "${formatAmount(it)} 分" },
                formatPace(item.dist, item.min)?.let { "$it /km" },
            ).joinToString("  /  ")
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
