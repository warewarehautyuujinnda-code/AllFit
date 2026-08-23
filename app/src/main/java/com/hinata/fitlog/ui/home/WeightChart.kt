package com.hinata.fitlog.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.WeightEntity
import com.hinata.fitlog.domain.WeightTrend
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatShortDate
import com.hinata.fitlog.domain.formatTrend

/**
 * 体重推移（FR-07）。カード内の左に数値サマリー、右に折れ線グラフを並べる。
 *
 * ライブラリを足さず Canvas で描いている。表示するのは1系列の折れ線だけで、
 * 目盛りは上下の値をテキストで添えれば足りるため。
 *
 * 記録が0件・1件でも落ちないように、描画は2件以上のときだけ行う。
 *
 * @param goal 目標体重(kg)。未設定なら null
 * @param onGoalClick 目標体重の設定を開く。設定の入り口を持たない画面では null
 */
@Composable
fun WeightChart(
    trend: WeightTrend,
    goal: Double?,
    modifier: Modifier = Modifier,
    onGoalClick: (() -> Unit)? = null,
) {
    val points = trend.points

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("体重の推移", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryBlock(
                        label = "目標体重",
                        value = goal?.let { "${formatAmount(it)} kg" } ?: "未設定",
                        onClick = onGoalClick,
                    )
                    SummaryBlock(
                        // いつ時点の体重かが分からないと判断できないため日付を添える
                        label = points.lastOrNull()
                            ?.let { "現在(${formatShortDate(it.date)})" } ?: "現在",
                        value = points.lastOrNull()
                            ?.let { "${formatAmount(it.weight)} kg" } ?: "未記録",
                    )
                    SummaryBlock(
                        label = "最近の傾向",
                        // 2件未満では増減が定義できない
                        value = trend.delta?.let { "${formatTrend(it)} kg" } ?: "—",
                        valueColor = trend.delta?.let { deltaColor(it) },
                    )
                }

                Box(modifier = Modifier.weight(2f)) {
                    when {
                        points.isEmpty() -> ChartMessage("記録するとグラフが出ます")
                        // 1件では線が引けない。2件目からグラフになることを伝える
                        points.size == 1 -> ChartMessage("2件目からグラフが出ます")
                        else -> WeightLineChart(points = points, goal = goal)
                    }
                }
            }
        }
    }
}

/** 減量を目指す前提なので、減少を primary・増加を error で色分けする */
@Composable
private fun deltaColor(delta: Double): Color =
    if (delta > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

@Composable
private fun SummaryBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // 押せる項目だと分かるように鉛筆を添える
            if (onClick != null) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "目標体重を設定",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(14.dp),
                )
            }
        }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 2件以上のときだけ呼ばれる折れ線グラフ本体 */
@Composable
private fun WeightLineChart(points: List<WeightEntity>, goal: Double?) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val goalColor = MaterialTheme.colorScheme.tertiary

    // 目標線が枠の外に出ると見えないので、目盛りの範囲に目標体重も含める
    val values = points.map { it.weight } + listOfNotNull(goal)
    val min = values.min()
    val max = values.max()
    // すべて同じ体重（または差が表示に出ないほど小さい）だと max-min が 0 になり
    // 0除算になる。その場合は高さの真ん中に横一直線として描く
    val flat = max - min < 0.1
    val range = max - min

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val h = size.height
            // 折れ線が枠線と重ならないよう上下に、端の点が切れないよう左右に余白を取る
            val padY = h * 0.1f
            val padX = 6f
            val usableH = h - padY * 2
            val usableW = size.width - padX * 2

            fun yOf(weight: Double): Float {
                val ratio = if (flat) 0.5f else ((weight - min) / range).toFloat()
                // Canvas は上が y=0 なので、値が大きいほど上に来るよう反転する
                return padY + usableH * (1f - ratio)
            }

            // ここに来るのは2件以上のときだけなので、点の間隔は必ず求められる
            fun offsetAt(index: Int): Offset =
                Offset(padX + usableW * index / (points.size - 1), yOf(points[index].weight))

            // 上下の基準線
            drawLine(gridColor, Offset(0f, padY), Offset(size.width, padY), strokeWidth = 1f)
            drawLine(
                gridColor,
                Offset(0f, padY + usableH),
                Offset(size.width, padY + usableH),
                strokeWidth = 1f,
            )

            goal?.let {
                drawLine(
                    goalColor,
                    Offset(0f, yOf(it)),
                    Offset(size.width, yOf(it)),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            }

            val path = Path().apply {
                val start = offsetAt(0)
                moveTo(start.x, start.y)
                for (i in 1 until points.size) {
                    val o = offsetAt(i)
                    lineTo(o.x, o.y)
                }
            }
            drawPath(path, color = lineColor, style = Stroke(width = 3f))

            // 各記録の位置に点を打つ。幅が狭いので件数が多いと潰れる。小さめにする
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
            ChartAxisLabel("${formatAmount(min)}〜${formatAmount(max)} kg")
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
