package com.hinata.fitlog.ui.weight

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
import com.hinata.fitlog.data.entity.WeightEntity
import com.hinata.fitlog.ui.common.DatePickerField
import com.hinata.fitlog.ui.common.DateUtil
import kotlinx.coroutines.launch

@Composable
fun WeightScreen(viewModel: WeightViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()

    var date by remember { mutableStateOf(DateUtil.today()) }
    var weight by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

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
            Text("体重・体組成の記録", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(0.dp))

            DatePickerField(
                value = date,
                onValueChange = { date = it },
                modifier = Modifier.padding(top = 12.dp),
            )

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

            OutlinedTextField(
                value = fat,
                onValueChange = { fat = it },
                label = { Text("体脂肪率 (%) ※任意") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            Button(
                onClick = {
                    val ok = viewModel.save(date, weight, fat)
                    scope.launch {
                        if (ok) {
                            weight = ""
                            fat = ""
                            snackbarHostState.showSnackbar("保存しました")
                        } else {
                            snackbarHostState.showSnackbar("体重を正しく入力してください")
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
                        WeightRow(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightRow(item: WeightEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(item.date, style = MaterialTheme.typography.bodyMedium)
            Text(
                buildString {
                    append("${item.weight} kg")
                    item.fat?.let { append("  /  ${it} %") }
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
