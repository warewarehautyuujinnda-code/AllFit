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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.hinata.fitlog.domain.RunningMetric
import com.hinata.fitlog.domain.RunningTrend
import com.hinata.fitlog.domain.RunningTrendPeriod
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatPace
import com.hinata.fitlog.domain.formatShortDate
import com.hinata.fitlog.domain.valueFor

/**
 * これまでの記録のグラフ（距離／スピードの推移）。タブを開いてすぐの画面に置く。
 * 体重タブの [com.hinata.fitlog.ui.home.WeightChart] と同じ作りで、ライブラリを足さず Canvas で描く。
 *
 * @param latest 直近の記録（期間の絞り込みに関わらず常に全履歴内の最新1件）。
 *   グラフ右上の期間を変えても左のサマリーの「直近」表示が消えたりしないようにするため、
 *   [trend]（期間で絞られる）とは別に渡す
 */
@Composable
fun RunningChart(
    trend: RunningTrend,
    latest: RunningEntity?,
    monthlyTotalKm: Double,
    period: RunningTrendPeriod,
    metric: RunningMetric,
    onPeriodChange: (RunningTrendPeriod) -> Unit,
    onMetricChange: (RunningMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val points = trend.points
    // スピード表示では時間未入力の記録が対象外になるため、実際にグラフに描ける件数は別に数える
    val plottablePoints = points.mapNotNull { record ->
        metric.valueFor(record)?.let { value -> ChartPoint(record, value) }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("これまでの記録", style = MaterialTheme.typography.titleMedium)

            PeriodSelector(
                selected = period,
                onSelect = onPeriodChange,
                modifier = Modifier.padding(top = 8.dp),
            )
            MetricSelector(
                selected = metric,
                onSelect = onMetricChange,
                modifier = Modifier.padding(top = 8.dp),
            )

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
                        plottablePoints.size < 2 ->
                            ChartMessage("時間を入力した記録が2件以上でグラフが出ます")
                        else -> RunningLineChart(points = plottablePoints, metric = metric)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    selected: RunningTrendPeriod,
    onSelect: (RunningTrendPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RunningTrendPeriod.entries.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricSelector(
    selected: RunningMetric,
    onSelect: (RunningMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RunningMetric.entries.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.label) },
            )
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

/** グラフ描画用に、記録と選択中の指標での値をひとまとめにしたもの */
private data class ChartPoint(val record: RunningEntity, val value: Double)

/**
 * 2件以上の値が求まる記録があるときだけ呼ばれる折れ線グラフ本体（距離またはスピードの推移）。
 * @param points 値が求まらない（null になる）記録は呼び出し側で既に除いてある
 */
@Composable
private fun RunningLineChart(points: List<ChartPoint>, metric: RunningMetric) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val values = points.map { it.value }
    val min = values.min()
    val max = values.max()
    // すべて同じ値だと max-min が 0 になり0除算になるため、真ん中に横一直線として描く
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

            fun yOf(value: Double): Float {
                val ratio = if (flat) 0.5f else ((value - min) / range).toFloat()
                return padY + usableH * (1f - ratio)
            }

            fun offsetAt(index: Int): Offset =
                Offset(padX + usableW * index / (points.size - 1), yOf(points[index].value))

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
            ChartAxisLabel(formatShortDate(points.first().record.date))
            ChartAxisLabel(rangeLabel(points, metric, min, max))
            ChartAxisLabel(formatShortDate(points.last().record.date))
        }
    }
}

/**
 * グラフ下部中央のレンジ表示。
 * スピードは縦軸のスケーリングにこそ km/h の値を使うが、数値表示は「直近のペース」と
 * 揃えるため formatPace によるペース表記にする（FR-03 に揃える）。
 */
private fun rangeLabel(
    points: List<ChartPoint>,
    metric: RunningMetric,
    min: Double,
    max: Double,
): String = when (metric) {
    RunningMetric.DISTANCE -> "${formatAmount(min)}〜${formatAmount(max)} km"
    RunningMetric.SPEED -> {
        val slowest = points.first { it.value == min }.record
        val fastest = points.first { it.value == max }.record
        val slowPace = formatPace(slowest.dist, slowest.min) ?: "-"
        val fastPace = formatPace(fastest.dist, fastest.min) ?: "-"
        "$slowPace〜$fastPace /km"
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
