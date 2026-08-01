package com.hinata.fitlog.ui.meal

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.ui.common.DatePickerField
import com.hinata.fitlog.ui.common.DateUtil
import kotlinx.coroutines.launch

@Composable
fun MealScreen(viewModel: MealViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()

    var date by remember { mutableStateOf(DateUtil.today()) }
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // 入力欄が6つあり、ソフトキーボード表示時は画面に収まらないため画面全体をスクロール可能にする。
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
                Text("食事の記録", style = MaterialTheme.typography.titleLarge)
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
                    value = p,
                    onValueChange = { p = it },
                    label = { Text("たんぱく質 (g) ※任意") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = f,
                    onValueChange = { f = it },
                    label = { Text("脂質 (g) ※任意") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = c,
                    onValueChange = { c = it },
                    label = { Text("炭水化物 (g) ※任意") },
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
                        val ok = viewModel.save(date, name, kcal, p, f, c)
                        scope.launch {
                            if (ok) {
                                name = ""
                                kcal = ""
                                p = ""
                                f = ""
                                c = ""
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
                    MealRow(item, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun MealRow(item: MealEntity, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(item.date, style = MaterialTheme.typography.bodySmall)
                Text(item.name, style = MaterialTheme.typography.bodyLarge)
            }
            // カロリー・PFCはすべて任意のため、入力があるものだけを並べる
            Column(horizontalAlignment = Alignment.End) {
                item.kcal?.let {
                    Text("${it} kcal", style = MaterialTheme.typography.bodyMedium)
                }
                val pfc = listOfNotNull(
                    item.p?.let { "P ${it}" },
                    item.f?.let { "F ${it}" },
                    item.c?.let { "C ${it}" },
                ).joinToString("  /  ")
                if (pfc.isNotEmpty()) {
                    Text(pfc, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
