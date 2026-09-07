package com.hinata.fitlog.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.WeightEntity
import com.hinata.fitlog.domain.TrendPeriod
import com.hinata.fitlog.domain.WeightTrend
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatShortDate
import com.hinata.fitlog.domain.formatTrend
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * 体重推移（FR-07）。カード内の左に数値サマリー、右に折れ線グラフを並べる。
 *
 * ライブラリを足さず Canvas で描いている。表示するのは1系列の折れ線だけで、
 * 目盛りは上下の値をテキストで添えれば足りるため。
 *
 * 記録が0件・1件でも落ちないように、描画は2件以上のときだけ行う。
 *
 * @param goal 目標体重(kg)。未設定なら null
 * @param period 選択中の表示期間
 * @param onPeriodChange 期間セグメントが選ばれたときに呼ばれる
 * @param onGoalClick 目標体重の設定を開く。設定の入り口を持たない画面では null
 * @param hasRecordsBeforePeriod 選択中の期間より前にも記録があるか。期間で絞り込んでいるだけで
 *   データが消えたわけではないことが伝わるよう、trueなら「全期間で見る」の案内を出す
 */
@Composable
fun WeightChart(
    trend: WeightTrend,
    goal: Double?,
    period: TrendPeriod,
    onPeriodChange: (TrendPeriod) -> Unit,
    modifier: Modifier = Modifier,
    onGoalClick: (() -> Unit)? = null,
    hasRecordsBeforePeriod: Boolean = false,
) {
    val points = trend.points

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("体重の推移", style = MaterialTheme.typography.titleMedium)

            PeriodSelector(
                selected = period,
                onSelect = onPeriodChange,
                modifier = Modifier.padding(top = 8.dp),
            )

            // 選択中の期間で絞り込まれているだけで記録自体は残っていることを伝え、
            // タップ1つで「全期間」に切り替えられるようにする
            if (hasRecordsBeforePeriod) {
                Text(
                    "この期間より前にも記録があります。全期間で見る",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { onPeriodChange(TrendPeriod.ALL) },
                )
            }

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

/**
 * 表示期間の切り替え（1ヶ月/3ヶ月/半年/1年/全期間）。選択中の1つだけがオンになる。
 *
 * 均等幅のセグメントを1つの帯に並べ、選択中の背景（ピル）が滑らかに移動するUI。
 * 太字＋背景の移動という2つの手がかりで選択状態を伝えるため、以前のようなチェック
 * マークは不要になる。
 */
@Composable
private fun PeriodSelector(
    selected: TrendPeriod,
    onSelect: (TrendPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val periods = TrendPeriod.entries
    val selectedIndex = periods.indexOf(selected)

    // Column の中では高さが無限大の制約になり fillMaxHeight が効かないため、
    // 帯の高さは固定値にしてスライドするピルの土台にする
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / periods.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            label = "periodIndicatorOffset",
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.surface),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            periods.forEach { period ->
                val isSelected = period == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(period) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        period.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
        // 点数が多い期間（3ヶ月/半年/1年/全期間）でも点同士が潰れて読めなくならないよう、
        // 1点あたりの最低幅を確保する。収まる場合（点数が少ない期間）は今まで通り
        // カード幅いっぱいに描き、収まらない分だけ横スクロールで見せる
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val minPointSpacing = 20.dp
            val chartWidth = maxOf(minPointSpacing * (points.size - 1), maxWidth)
            // 横軸に出す日付は、期間が長いほど間引き・粒度を粗くして詰まらないようにする
            val axisTicks = axisTickIndices(points.size)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                Column(modifier = Modifier.width(chartWidth)) {
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

                    // 横軸の日付。Canvas と同じ幅の中で均等割りにすることで、
                    // 横スクロールしても点の位置とほぼ揃って見える
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        axisTicks.forEach { i -> ChartAxisLabel(axisDateLabel(points, i)) }
                    }
                }
            }

            // 上下の基準線が指す数値。横スクロールしても常に見えるよう線に重ねて固定表示する
            ChartValueLabel(
                text = "${formatAmount(max)} kg",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp, start = 2.dp),
            )
            ChartValueLabel(
                text = "${formatAmount(min)} kg",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 8.dp, start = 2.dp),
            )
        }
    }
}

/**
 * 横軸に出す日付ラベルの位置（[points] のインデックス）。
 * 多くても5個程度に間引きつつ、両端（最初と最後の記録）は必ず含める。
 */
private fun axisTickIndices(size: Int): List<Int> {
    if (size <= 1) return listOf(0).filter { it < size }
    val count = minOf(5, size)
    val lastIndex = size - 1
    return (0 until count)
        .map { step -> (step * lastIndex.toDouble() / (count - 1)).roundToInt() }
        .distinct()
}

/**
 * 横軸ラベルの粒度を、選択中の期間の長さに応じて変える。
 * 「1ヶ月」のような短い期間では日付(M/d)まで出し、「半年」「1年」「全期間」のように
 * 長くなるほど年月(yyyy/M)に切り替えて、ラベルが詰まって読めなくなるのを防ぐ。
 */
private fun axisDateLabel(points: List<WeightEntity>, index: Int): String {
    val spanDays = ChronoUnit.DAYS.between(
        LocalDate.parse(points.first().date),
        LocalDate.parse(points.last().date),
    )
    val date = LocalDate.parse(points[index].date)
    return if (spanDays <= 120) {
        "${date.monthValue}/${date.dayOfMonth}"
    } else {
        "${date.year}/${date.monthValue}"
    }
}

/** 基準線の脇に数値(kg)を出すための小さいラベル。線と重なっても読めるよう背景を敷く */
@Composable
private fun ChartValueLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
