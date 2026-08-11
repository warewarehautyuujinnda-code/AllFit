package com.hinata.fitlog.ui.meal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.domain.MealTotals
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.ui.common.DatePickerField
import com.hinata.fitlog.ui.common.DateUtil
import com.hinata.fitlog.ui.common.RecordCard
import kotlinx.coroutines.launch

/**
 * 食事画面。入力（FR-04）・当日合計（FR-10）・履歴（FR-09）・削除（FR-05）。
 */
@Composable
fun MealScreen(viewModel: MealViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()
    val totals by viewModel.todayTotals.collectAsState()

    var date by remember { mutableStateOf(DateUtil.today()) }
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // 入力欄が6つあり、ソフトキーボード表示時は画面に収まらないため画面全体をスクロール可能にする。
        // 履歴の遅延読み込みを保つため、入力フォームと履歴を1つの LazyColumn にまとめている。
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(16.dp),
        ) {
            item {
                Text("食事の記録", style = MaterialTheme.typography.titleLarge)
            }

            item {
                TodayTotalsCard(totals, modifier = Modifier.padding(top = 12.dp))
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("内容 ※必須") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = { Text("カロリー (kcal) ※任意") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("たんぱく質 P (g) ※任意") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("脂質 F (g) ※任意") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("炭水化物 C (g) ※任意") },
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
                        val ok = viewModel.save(date, name, kcal, protein, fat, carbs)
                        scope.launch {
                            if (ok) {
                                name = ""
                                kcal = ""
                                protein = ""
                                fat = ""
                                carbs = ""
                                snackbarHostState.showSnackbar("保存しました")
                            } else {
                                snackbarHostState.showSnackbar(
                                    "内容を入力し、カロリー・PFCは数値で入力してください"
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
                    MealRow(
                        item = item,
                        onDelete = { viewModel.delete(item) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

/** 当日の摂取合計（FR-10） */
@Composable
private fun TodayTotalsCard(totals: MealTotals, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("今日の合計（${DateUtil.today()}）", style = MaterialTheme.typography.labelLarge)
            Text(
                "${totals.kcal} kcal",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    "P ${formatAmount(totals.protein)}g　" +
                        "F ${formatAmount(totals.fat)}g　" +
                        "C ${formatAmount(totals.carbs)}g",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MealRow(
    item: MealEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RecordCard(
        summary = "${item.date}　${item.name}",
        onDelete = onDelete,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(item.date, style = MaterialTheme.typography.bodySmall)
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            // カロリー・PFC は任意のため、入力があるものだけを並べる
            val detail = listOfNotNull(
                item.kcal?.let { "$it kcal" },
                item.p?.let { "P ${formatAmount(it)}g" },
                item.f?.let { "F ${formatAmount(it)}g" },
                item.c?.let { "C ${formatAmount(it)}g" },
            ).joinToString("  /  ")
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
