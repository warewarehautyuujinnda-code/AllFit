package com.hinata.fitlog.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.WeightEntity
import com.hinata.fitlog.domain.TrendPeriod
import com.hinata.fitlog.domain.WeightTrend
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatShortDate
import com.hinata.fitlog.domain.formatTrend
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
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

/**
 * グラフの系列色（折れ線・点・目標線・グリッド）。
 *
 * この画面は端末の壁紙連動カラー（Material You / `ui.theme.FitLogTheme` の dynamicColor）を
 * 使っているため、[MaterialTheme.colorScheme] をそのまま使うと壁紙によっては彩度の低い
 * グレーに近い色になり、折れ線やグリッドがほとんど見えなくなってしまう。データを運ぶ色は
 * 壁紙に関係なく読み取れる必要があるため、ここだけはテーマ本来のブランドカラー
 * （`ui.theme.LightColors`/`DarkColors` と同じ色相）を固定で使い、ライト/ダークの
 * 切り替えにだけ追従する。
 */
private val ChartLineColorLight = Color(0xFF2E7D32)
private val ChartLineColorDark = Color(0xFF81C784)
private val ChartGoalColorLight = Color(0xFFF9A825)
private val ChartGoalColorDark = Color(0xFFFFD54F)
private val ChartGridColorLight = Color(0xFFDDDDDD)
private val ChartGridColorDark = Color(0xFF3A3A3A)
private val ChartAxisTextColorLight = Color(0xFF6B6B6B)
private val ChartAxisTextColorDark = Color(0xFFB0B0B0)

/** 2件以上のときだけ呼ばれる折れ線グラフ本体 */
@Composable
private fun WeightLineChart(points: List<WeightEntity>, goal: Double?) {
    val dark = isSystemInDarkTheme()
    val lineColor = if (dark) ChartLineColorDark else ChartLineColorLight
    val goalColor = if (dark) ChartGoalColorDark else ChartGoalColorLight
    val gridColor = if (dark) ChartGridColorDark else ChartGridColorLight
    val axisTextColor = if (dark) ChartAxisTextColorDark else ChartAxisTextColorLight
    // 点は線と重なっても分かるよう、カード背景色の縁取りをしてから塗る
    val ringColor = MaterialTheme.colorScheme.surface

    // 目標線が枠の外に出ると見えないので、目盛りの範囲に目標体重も含める
    val values = points.map { it.weight } + listOfNotNull(goal)
    val min = values.min()
    val max = values.max()
    // すべて同じ体重（または差が表示に出ないほど小さい）だと max-min が 0 になり
    // 0除算になる。その場合は高さの真ん中に横一直線として描く
    val flat = max - min < 0.1
    val range = max - min

    val chartHeight = 140.dp
    val verticalPadFraction = 0.12f
    val padYDp = chartHeight * verticalPadFraction
    val usableHDp = chartHeight - padYDp * 2
    // Canvas 内の yOf() と同じ比率計算を Dp 側でも行い、横罫線の数値ラベルを
    // 線・グリッドと同じ高さに重ねて固定表示できるようにする
    fun yDpOf(weight: Double): Dp {
        val ratio = if (flat) 0.5f else ((weight - min) / range).toFloat()
        return padYDp + usableHDp * (1f - ratio)
    }

    Column {
        // 期間全体を一目で見比べられることを優先し、点数が多くても横スクロールはしない。
        // カード幅にそのまま収め、点同士の間隔は件数に応じて詰まる（点マーカーは
        // points.size > 20 で非表示にして潰れを防ぐ。下の dotRadius 分岐を参照）
        Box(modifier = Modifier.fillMaxWidth()) {
            // 横軸に出す日付は、期間が長いほど間引き・粒度を粗くして詰まらないようにする
            val axisTicks = axisTickIndices(points.size)
            // 横罫線＋左側の数値目盛り。記録の値幅に応じてキリのいい間隔にする
            val gridValues = gridTicks(min, max, goal)

            Column(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                ) {
                    val h = size.height
                    // 折れ線が枠線と重ならないよう上下に、端の点が切れないよう左右に余白を取る
                    val padY = h * verticalPadFraction
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

                    // 横罫線（候補Bと同じく、上下2本だけでなく値ごとに複数本引く）
                    gridValues.forEach { v ->
                        val y = yOf(v)
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    }

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

                    // 各記録の位置に点を打つ。件数が多いと潰れて見づらいので、
                    // ある程度を超えたら線だけにする
                    if (points.size <= 20) {
                        val dotRadius = if (points.size > 10) 2.5f else 4.5f
                        for (i in points.indices) {
                            val center = offsetAt(i)
                            drawCircle(ringColor, radius = dotRadius + 2f, center = center)
                            drawCircle(lineColor, radius = dotRadius, center = center)
                        }
                    }
                }

                // 横軸の日付。Canvas と同じ幅の中で均等割りにする
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    axisTicks.forEach { i -> ChartAxisLabel(axisDateLabel(points, i), color = axisTextColor) }
                }
            }

            // 横罫線の数値。線に重ねて固定表示する
            gridValues.forEach { v ->
                ChartValueLabel(
                    text = formatAmount(v),
                    color = axisTextColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 2.dp, y = yDpOf(v) - 8.dp),
                )
            }
        }
    }
}

/**
 * 横罫線を引く値の一覧。値幅がどれだけ広くても目盛りの本数がだいたい一定になるよう、
 * 「1・2・5×10^n」のキリのいい間隔を選ぶ（全期間のように記録が数年分にわたって
 * 値幅が大きいときに、目盛りが2本しか出ず読みづらくなるのを防ぐ）。
 * 目標線とほぼ重なる目盛りは間引いて、同じ高さに2本線が並んで見えるのを防ぐ。
 *
 * - 生成する目盛りの数は [MAX_GRID_TICKS] で必ず打ち切る。体重・目標体重は
 *   上限を検証していない（[com.hinata.fitlog.domain.parseRequiredDouble] は
 *   正の有限数なら何でも通す）ため、誤って極端な値が入ると際限なく目盛りを
 *   作ろうとしてしまう安全弁。
 * - キリのいい値が範囲内に1つも収まらない（70.1〜70.4kgのような小さな増減など）
 *   場合は目盛りが空になり数値の手がかりが消えてしまうため、最小・最大の実測値に
 *   フォールバックする。
 */
private const val MAX_GRID_TICKS = 8
private const val TARGET_GRID_TICKS = 5

private fun gridTicks(min: Double, max: Double, goal: Double?): List<Double> {
    val range = max - min
    if (range <= 0.0) return listOf(min)
    val step = niceStep(range / TARGET_GRID_TICKS)
    val start = ceil(min / step) * step
    val roundedTicks = generateSequence(start) { it + step }
        .takeWhile { it <= max + 1e-6 }
        .take(MAX_GRID_TICKS)
        .toList()
    val filtered = if (goal == null) roundedTicks else roundedTicks.filter { abs(it - goal) > step * 0.25 }
    return filtered.ifEmpty { listOf(min, max) }
}

/**
 * [rawStep] にいちばん近い「1・2・5×10^n」の値に丸める（0.5kg未満にはしない）。
 *
 * しきい値は 1/2/5 それぞれの中間（1.5, 3.5, 7.5）にする。単純に
 * 「normalized <= 2.0 なら2、それ以外は5」のような切り上げ式にすると、
 * しきい値のすぐ上（例: normalized=2.02）で本来2を選びたいのに5まで
 * 一気に飛んでしまい、目盛りの本数が想定より大きく減ってしまう。
 */
private fun niceStep(rawStep: Double): Double {
    val safe = rawStep.coerceAtLeast(0.05)
    val magnitude = 10.0.pow(floor(log10(safe)))
    val normalized = safe / magnitude
    val niceNormalized = when {
        normalized <= 1.5 -> 1.0
        normalized <= 3.5 -> 2.0
        normalized <= 7.5 -> 5.0
        else -> 10.0
    }
    return (niceNormalized * magnitude).coerceAtLeast(0.5)
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

/** 横罫線の脇に数値(kg)を出すための小さいラベル。線と重なっても読めるよう背景を敷く */
@Composable
private fun ChartValueLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

@Composable
private fun ChartAxisLabel(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ChartMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
