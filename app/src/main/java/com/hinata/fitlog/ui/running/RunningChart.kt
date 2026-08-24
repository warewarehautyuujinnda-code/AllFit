package com.hinata.fitlog.ui.running

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.domain.RunningTrend
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatPace
import com.hinata.fitlog.domain.formatShortDate

/**
 * これまでの記録のグラフ（距離の推移）。タブを開いてすぐの画面に置く。
 * 体重タブの [com.hinata.fitlog.ui.home.WeightChart] と同じ作りで、ライブラリを足さず Canvas で描く。
 */
@Composable
fun RunningChart(
    trend: RunningTrend,
    monthlyTotalKm: Double,
    modifier: Modifier = Modifier,
) {
    val points = trend.points
    val latest = points.lastOrNull()

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("これまでの記録", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryBlock(
                        // いつ時点の記録かが分からないと判断できないため日付を添える
                        label = latest?.let { "直近(${formatShortDate(it.date)})" } ?: "直近",
                        value = latest?.let { "${formatAmount(it.dist)} km" } ?: "未記録",
                    )
                    SummaryBlock(
                        label = "直近のペース",
                        value = latest?.let { formatPace(it.dist, it.min)?.let { p -> "$p /km" } } ?: "—",
                    )
                    SummaryBlock(
                        label = "今月の合計距離",
                        value = "${formatAmount(monthlyTotalKm)} km",
                    )
                }

                Box(modifier = Modifier.weight(2f)) {
                    when {
                        points.isEmpty() -> ChartMessage("記録するとグラフが出ます")
                        points.size == 1 -> ChartMessage("2件目からグラフが出ます")
                        else -> RunningLineChart(points = points)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBlock(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 2件以上のときだけ呼ばれる折れ線グラフ本体（距離の推移） */
@Composable
private fun RunningLineChart(points: List<RunningEntity>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val values = points.map { it.dist }
    val min = values.min()
    val max = values.max()
    // すべて同じ距離だと max-min が 0 になり0除算になるため、真ん中に横一直線として描く
    val flat = max - min < 0.1
    val range = max - min

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val h = size.height
            val padY = h * 0.1f
            val padX = 6f
            val usableH = h - padY * 2
            val usableW = size.width - padX * 2

            fun yOf(dist: Double): Float {
                val ratio = if (flat) 0.5f else ((dist - min) / range).toFloat()
                return padY + usableH * (1f - ratio)
            }

            fun offsetAt(index: Int): Offset =
                Offset(padX + usableW * index / (points.size - 1), yOf(points[index].dist))

            drawLine(gridColor, Offset(0f, padY), Offset(size.width, padY), strokeWidth = 1f)
            drawLine(
                gridColor,
                Offset(0f, padY + usableH),
                Offset(size.width, padY + usableH),
                strokeWidth = 1f,
            )

            val path = Path().apply {
                val start = offsetAt(0)
                moveTo(start.x, start.y)
                for (i in 1 until points.size) {
                    val o = offsetAt(i)
                    lineTo(o.x, o.y)
                }
            }
            drawPath(path, color = lineColor, style = Stroke(width = 3f))

            val dotRadius = if (points.size > 10) 2f else 4f
            for (i in points.indices) {
                drawCircle(lineColor, radius = dotRadius, center = offsetAt(i))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ChartAxisLabel(formatShortDate(points.first().date))
            ChartAxisLabel("${formatAmount(min)}〜${formatAmount(max)} km")
            ChartAxisLabel(formatShortDate(points.last().date))
        }
    }
}

@Composable
private fun ChartAxisLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ChartMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
