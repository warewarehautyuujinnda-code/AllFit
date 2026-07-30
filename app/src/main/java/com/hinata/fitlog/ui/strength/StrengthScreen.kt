package com.hinata.fitlog.ui.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.ui.common.DatePickerField
import com.hinata.fitlog.ui.common.DateUtil
import kotlinx.coroutines.launch

@Composable
fun StrengthScreen(viewModel: StrengthViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()

    var date by remember { mutableStateOf(DateUtil.today()) }
    var ex by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text("筋トレの記録", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(0.dp))

            DatePickerField(
                value = date,
                onValueChange = { date = it },
                modifier = Modifier.padding(top = 12.dp),
            )

            OutlinedTextField(
                value = ex,
                onValueChange = { ex = it },
                label = { Text("種目 ※必須") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("重量 (kg) ※任意") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text("回数 ※任意") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            OutlinedTextField(
                value = sets,
                onValueChange = { sets = it },
                label = { Text("セット数 ※任意") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            Button(
                onClick = {
                    val ok = viewModel.save(date, ex, weight, reps, sets)
                    scope.launch {
                        if (ok) {
                            ex = ""
                            weight = ""
                            reps = ""
                            sets = ""
                            snackbarHostState.showSnackbar("保存しました")
                        } else {
                            snackbarHostState.showSnackbar(
                                "種目を入力し、重量・回数・セット数は数値で入力してください"
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                "記録一覧（${items.size}件）",
                style = MaterialTheme.typography.titleMedium,
            )

            if (items.isEmpty()) {
                Text(
                    "まだ記録がありません",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        StrengthRow(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun StrengthRow(item: StrengthEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(item.date, style = MaterialTheme.typography.bodySmall)
                Text(item.ex, style = MaterialTheme.typography.bodyLarge)
            }
            // 重量・回数・セット数はすべて任意のため、入力があるものだけを並べる
            val detail = listOfNotNull(
                item.weight?.let { "${it} kg" },
                item.reps?.let { "${it} 回" },
                item.sets?.let { "${it} セット" },
            ).joinToString("  /  ")
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
