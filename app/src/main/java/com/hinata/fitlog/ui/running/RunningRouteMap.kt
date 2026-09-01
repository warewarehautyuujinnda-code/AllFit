package com.hinata.fitlog.ui.running

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.RunningPointEntity
import com.hinata.fitlog.domain.NormalizedPoint
import com.hinata.fitlog.domain.projectRoute

/**
 * 記録詳細画面で使う、走った経路の図。
 * 本アプリは外部サーバーへ一切通信しない方針（[com.hinata.fitlog.data.AppDatabase]参照）のため、
 * 地図タイルは使わず、GPSで記録した緯度経度から経路の「形」だけを[RunningChart]と同じ方針で
 * Canvasに自前で描く。
 */
@Composable
fun RunningRouteMap(points: List<RunningPointEntity>, modifier: Modifier = Modifier) {
    val route = remember(points) { projectRoute(points.map { it.latitude to it.longitude }) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (route.isEmpty()) {
            Text(
                "経路データなし",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            RouteCanvas(route = route, modifier = Modifier.fillMaxSize().padding(20.dp))
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            ) {
                RouteLegend(color = START_COLOR, label = "開始")
                RouteLegend(
                    color = MaterialTheme.colorScheme.error,
                    label = "ゴール",
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun RouteCanvas(route: List<NormalizedPoint>, modifier: Modifier = Modifier) {
    val endColor = MaterialTheme.colorScheme.error

    Canvas(modifier = modifier) {
        fun offsetAt(p: NormalizedPoint) = Offset(p.x * size.width, p.y * size.height)

        val path = Path().apply {
            val start = offsetAt(route.first())
            moveTo(start.x, start.y)
            for (i in 1 until route.size) {
                val o = offsetAt(route[i])
                lineTo(o.x, o.y)
            }
        }
        drawPath(path, color = START_COLOR, style = Stroke(width = 6.dp.toPx()))

        drawCircle(START_COLOR, radius = 7.dp.toPx(), center = offsetAt(route.first()))
        drawCircle(endColor, radius = 7.dp.toPx(), center = offsetAt(route.last()))
    }
}

@Composable
private fun RouteLegend(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(8.dp)
                .background(color, RoundedCornerShape(50)),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** 経路の線と開始地点マーカーの色。ゴール（[MaterialTheme.colorScheme.error]）と区別する */
private val START_COLOR = Color(0xFF2E7D32)
