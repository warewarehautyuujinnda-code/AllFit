package com.hinata.fitlog.ui.feedback

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 全タブ共通で表示するフィードバック起動ボタン。
 * 押すとその場の画面をキャプチャして書き込み画面を開く（FitLogAppRoot 参照）。
 */
@Composable
fun FeedbackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
    ) {
        Icon(Icons.Filled.BugReport, contentDescription = "フィードバックを送る")
    }
}
