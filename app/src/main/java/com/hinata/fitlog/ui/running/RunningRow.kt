package com.hinata.fitlog.ui.running

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatPace
import com.hinata.fitlog.ui.common.RecordCard

/**
 * 記録一覧・直近3件の共通の1行。タップで詳細画面を開く（[onClick]がnullなら押せない）。
 */
@Composable
fun RunningRow(
    item: RunningEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    RecordCard(
        summary = "${item.date}　${formatAmount(item.dist)} km",
        onDelete = onDelete,
        modifier = modifier.let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(item.date, style = MaterialTheme.typography.bodySmall)
            Text("${formatAmount(item.dist)} km", style = MaterialTheme.typography.bodyLarge)
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
