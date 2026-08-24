package com.hinata.fitlog.ui.running

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.RunningEntity

/** 記録一覧。メイン画面右上の「記録」ボタンから開く */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningListScreen(
    items: List<RunningEntity>,
    onBack: () -> Unit,
    onOpenDetail: (RunningEntity) -> Unit,
    onDelete: (RunningEntity) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("記録一覧（${items.size}件）") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (items.isEmpty()) {
            Text(
                "まだ記録がありません",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(innerPadding).padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    RunningRow(
                        item = item,
                        onDelete = { onDelete(item) },
                        onClick = { onOpenDetail(item) },
                    )
                }
            }
        }
    }
}
