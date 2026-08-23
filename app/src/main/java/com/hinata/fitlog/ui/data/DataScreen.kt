package com.hinata.fitlog.ui.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hinata.fitlog.BuildConfig
import com.hinata.fitlog.data.RecordCounts

/**
 * データ画面。書き出し（FR-11）・読み込み（FR-12）・全削除（FR-13）・統計（FR-14）。
 */
@Composable
fun DataScreen(viewModel: DataViewModel = viewModel()) {
    val counts by viewModel.counts.collectAsState()
    val busy by viewModel.busy.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 書き出し先・読み込み元はシステムのファイル選択に任せる。
    // 利用者がファイルを選ぶ方式なのでストレージの権限は不要。
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::export) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::import) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("データ", style = MaterialTheme.typography.titleLarge)

            StatsCard(counts)

            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            HorizontalDivider()

            Text("バックアップ", style = MaterialTheme.typography.titleMedium)
            Text(
                "記録は端末内にだけ保存されています。端末の紛失やアプリの削除で消えないよう、" +
                    "ときどき書き出して保管してください。書き出したJSONはそのままAIに渡せます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("JSONで書き出す（${counts.total}件）")
            }

            OutlinedButton(
                // 書き出したファイルの MIME が端末によって変わるため種類を絞らない。
                // 絞ると自分で書き出したファイルが選択画面に出てこないことがある
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("JSONから読み込む")
            }
            Text(
                "読み込んだ記録は今ある記録に追加されます（同じ記録は上書き）。" +
                    "今の記録が消えることはありません。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            Text("全データ削除", style = MaterialTheme.typography.titleMedium)
            Text(
                "4種別すべての記録を消します。元に戻せないので、先に書き出しておくことをおすすめします。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { showDeleteConfirm = true },
                enabled = !busy && counts.total > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("すべての記録を削除")
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            // 新しいAPKを入れたあと本当に入れ替わったのかを、本人がここで確認できるようにする。
            // Playストア経由ではないので「更新されたはず」を信じるしかない状態を避けたい。
            Text("バージョン", style = MaterialTheme.typography.titleMedium)
            Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium)
            Text(
                "新しいAPKを入れたあと、ここの表示が変わっていれば入れ替わっています。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("すべての記録を削除しますか？") },
            text = {
                Text(
                    "体重 ${counts.weight}件 / 筋トレ ${counts.strength}件 / " +
                        "ラン ${counts.running}件 / 食事 ${counts.meal}件、" +
                        "合計 ${counts.total}件をすべて削除します。\n\n" +
                        "この操作は元に戻せません。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteAll()
                }) { Text("すべて削除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("キャンセル") }
            },
        )
    }
}

/** 種別ごとの記録件数（FR-14） */
@Composable
private fun StatsCard(counts: RecordCounts) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("記録件数", style = MaterialTheme.typography.titleMedium)
            CountRow("体重", counts.weight)
            CountRow("筋トレ", counts.strength)
            CountRow("ラン", counts.running)
            CountRow("食事", counts.meal)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            CountRow("合計", counts.total, emphasize = true)
        }
    }
}

@Composable
private fun CountRow(label: String, count: Int, emphasize: Boolean = false) {
    val style =
        if (emphasize) MaterialTheme.typography.titleSmall
        else MaterialTheme.typography.bodyMedium
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = style)
        Text("${count}件", style = style)
    }
}
