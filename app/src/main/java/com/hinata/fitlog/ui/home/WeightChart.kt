package com.hinata.fitlog.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.domain.WeightTrend
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatDelta

/**
 * 体重推移の折れ線グラフ（FR-07）。直近30件を古い→新しい順に左から描く。
 *
 * ライブラリを足さず Canvas で描いている。表示するのは1系列の折れ線だけで、
 * 目盛りは上下の値をテキストで添えれば足りるため。
 *
 * 記録が0件・1件でも落ちないように、描画は2件以上のときだけ行う。
 */
@Composable
fun WeightChart(trend: WeightTrend, modifier: Modifier = Modifier) {
    val points = trend.points
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("体重の推移（直近${points.size}件）", style = MaterialTheme.typography.titleMedium)
            trend.delta?.let { delta ->
                Text(
                    "${formatDelta(delta)} kg",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (delta > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }
        }

        when {
            points.isEmpty() -> EmptyChartMessage("体重を記録するとグラフが表示されます")
            // 1件では線が引けない。値だけ出して「まだ推移がない」ことを伝える
            points.size == 1 -> EmptyChartMessage(
                "記録が1件です（${formatAmount(points.first().weight)} kg）。" +
                    "2件目からグラフが表示されます"
            )
            else -> {
                val min = points.minOf { it.weight }
                val max = points.maxOf { it.weight }
                // すべて同じ体重（または差が表示に出ないほど小さい）だと max-min が 0 になり
                // 0除算になる。その場合は高さの真ん中に横一直線として描く
                val flat = max - min < 0.1
                val range = max - min

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(top = 8.dp),
                ) {
                    val w = size.width
                    val h = size.height
                    // 折れ線が枠線と重ならないよう上下に少しだけ余白を取る
                    val padY = h * 0.1f
                    val usableH = h - padY * 2

                    // ここに来るのは2件以上のときだけなので、点の間隔は必ず求められる
                    fun offsetAt(index: Int): Offset {
                        val x = w * index / (points.size - 1).toFloat()
                        val ratio =
                            if (flat) 0.5f else ((points[index].weight - min) / range).toFloat()
                        // Canvas は上が y=0 なので、値が大きいほど上に来るよう反転する
                        return Offset(x, padY + usableH * (1f - ratio))
                    }

                    // 上下の基準線
                    drawLine(gridColor, Offset(0f, padY), Offset(w, padY), strokeWidth = 1f)
                    drawLine(
                        gridColor,
                        Offset(0f, padY + usableH),
                        Offset(w, padY + usableH),
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
                    drawPath(path, color = lineColor, style = Stroke(width = 4f))

                    // 各記録の位置に点を打つ。件数が多いと潰れるので小さめにする
                    val dotRadius = if (points.size > 15) 3f else 5f
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
                    Text(
                        "${points.first().date}　${formatAmount(min)}〜${formatAmount(max)} kg",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(points.last().date, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EmptyChartMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
