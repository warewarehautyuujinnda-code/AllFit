package com.hinata.fitlog.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * 履歴一覧の1件を削除するボタン（FR-05）。
 * 記録画面（体重・筋トレ・ラン・食事）の履歴行で共通利用する。
 *
 * 削除は取り消せないため、押すと確認ダイアログを挟んでから [onConfirm] を呼ぶ。
 *
 * @param label 確認ダイアログに出す対象の説明（例: 「2026-08-02 の体重の記録」）
 */
@Composable
fun DeleteRecordButton(
    label: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    IconButton(onClick = { showDialog = true }, modifier = modifier) {
        Icon(Icons.Filled.DeleteOutline, contentDescription = "この記録を削除")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("記録を削除しますか？") },
            text = { Text("$label を削除します。削除した記録は元に戻せません。") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onConfirm()
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("キャンセル") }
            },
        )
    }
}
