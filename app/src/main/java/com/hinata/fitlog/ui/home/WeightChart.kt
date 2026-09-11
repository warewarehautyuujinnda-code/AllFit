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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.WeightEntity
import com.hinata.fitlog.domain.TrendPeriod
import com.hinata.fitlog.domain.WeightTrend
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatShortDate
import com.hinata.fitlog.domain.formatTrend
import com.hinata.fitlog.domain.weightAxisOf
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * 体重推移（FR-07）。カード内の左に数値サマリー、右に折れ線グラフを並べる。
 *
 * ライブラリを足さず Canvas で描いている。表示するのは1系列の折れ線だけで、
 * 目盛りも整数kgの罫線とその数値を添えれば足りるため。
 *
 * 記録が0件・1件でも落ちないように、描画は2件以上のときだけ行う。
 *
 * @param goal 目標体重(kg)。未設定なら null
 * @param period 選択中の表示期間
 * @param onPeriodChange 期間セグメントが選ばれたときに呼ばれる
 * @param onGoalClick 目標体重の設定を開く。設定の入り口を持たない画面では null
 */
@Composable
fun WeightChart(
    trend: WeightTrend,
    goal: Double?,
    period: TrendPeriod,
    onPeriodChange: (TrendPeriod) -> Unit,
    modifier: Modifier = Modifier,
    onGoalClick: (() -> Unit)? = null,
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
 * グラフの系列色（折れ線・点・目標線）と縦軸の数値の色。
 *
 * この画面は端末の壁紙連動カラー（Material You / `ui.theme.FitLogTheme` の dynamicColor）を
 * 使っているため、[MaterialTheme.colorScheme] をそのまま使うと壁紙によっては彩度の低い
 * グレーに近い色になり、折れ線がほとんど見えなくなってしまう。データを運ぶ色は
 * 壁紙に関係なく読み取れる必要があるため、ここだけはテーマ本来のブランドカラー
 * （`ui.theme.LightColors`/`DarkColors` と同じ色相）を固定で使い、ライト/ダークの
 * 切り替えにだけ追従する。
 *
 * 横罫線だけは [WeightLineChart] でカードの文字色を薄く重ねて作る。固定のグレーだと
 * カード背景（壁紙連動の `surfaceContainerHighest`）とほぼ同じ明るさになり、線が消えるため。
 */
private val ChartLineColorLight = Color(0xFF2E7D32)
private val ChartLineColorDark = Color(0xFF81C784)
private val ChartGoalColorLight = Color(0xFFF9A825)
private val ChartGoalColorDark = Color(0xFFFFD54F)
private val ChartAxisTextColorLight = Color(0xFF6B6B6B)
private val ChartAxisTextColorDark = Color(0xFFB0B0B0)

/** 折れ線部分の高さ（横軸の日付は含まない） */
private val ChartHeight = 140.dp

/** 一番上・一番下の罫線と枠の間の余白。端の点と、罫線の高さに中心を合わせた数値が切れない幅 */
private val ChartPadY = 10.dp

/** 縦軸の数値と、罫線の左端との間隔 */
private val AxisLabelGap = 6.dp

/** 2件以上のときだけ呼ばれる折れ線グラフ本体 */
@Composable
private fun WeightLineChart(points: List<WeightEntity>, goal: Double?) {
    val dark = isSystemInDarkTheme()
    val lineColor = if (dark) ChartLineColorDark else ChartLineColorLight
    val goalColor = if (dark) ChartGoalColorDark else ChartGoalColorLight
    val axisTextColor = if (dark) ChartAxisTextColorDark else ChartAxisTextColorLight
    // 点の縁取りはカードの背景色そのものにする。以前は surface（ほぼ白）を使っていたため、
    // カード背景（surfaceContainerHighest）の上で白い縁が浮いて見えていた
    val cardColor = CardDefaults.cardColors().containerColor
    // 罫線はカードの文字色を薄く重ねて作る。固定のグレーだと壁紙連動のカード背景と
    // 明るさが近くなり、線が見えなくなることがある
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

    // 目標線が枠の外に出ると見えないので、縦軸の範囲に目標体重も含める
    val values = points.map { it.weight } + listOfNotNull(goal)
    val axis = weightAxisOf(values.min(), values.max())
    // 通常は罫線が2本以上あり上下端は必ず異なるが、極端な値の安全弁で上下端が
    // 一致したときだけ 0 除算にならないよう、高さの真ん中に描く
    val axisRange = axis.max - axis.min

    // 縦軸の数値は折れ線に重ねず、グラフの左に専用の列を取って罫線の高さに並べる。
    // 背景を敷かなくても線と重ならないので、カードの背景にそのままなじむ。
    // 列の幅はいちばん長い数値に合わせる
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = axisTextColor)
    val labelLayouts = remember(axis, labelStyle) {
        axis.labels.map { v -> v to textMeasurer.measure(formatAmount(v), labelStyle) }
    }
    val gutterWidth = with(LocalDensity.current) {
        (labelLayouts.maxOfOrNull { it.second.size.width } ?: 0).toDp()
    } + AxisLabelGap

    // 横軸に出す日付は、期間が長いほど間引き・粒度を粗くして詰まらないようにする
    val axisTicks = axisTickIndices(points.size)

    // 期間全体を一目で見比べられることを優先し、点数が多くても横スクロールはしない。
    // カード幅にそのまま収め、点同士の間隔は件数に応じて詰まる（点マーカーは
    // points.size > 20 で非表示にして潰れを防ぐ。下の dotRadius 分岐を参照）
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight),
        ) {
            val plotLeft = gutterWidth.toPx()
            val padY = ChartPadY.toPx()
            // 端の点が数値の列や枠で切れないよう左右にも少し余白を取る
            val padX = 6f
            val usableH = size.height - padY * 2
            val usableW = size.width - plotLeft - padX * 2

            fun yOf(weight: Double): Float {
                val ratio = if (axisRange > 0) ((weight - axis.min) / axisRange).toFloat() else 0.5f
                // Canvas は上が y=0 なので、値が大きいほど上に来るよう反転する
                return padY + usableH * (1f - ratio)
            }

            // ここに来るのは2件以上のときだけなので、点の間隔は必ず求められる
            fun offsetAt(index: Int): Offset = Offset(
                plotLeft + padX + usableW * index / (points.size - 1),
                yOf(points[index].weight),
            )

            // 横罫線（整数kg）。数値の列には伸ばさない
            axis.lines.forEach { v ->
                val y = yOf(v)
                drawLine(gridColor, Offset(plotLeft, y), Offset(size.width, y), strokeWidth = 1f)
            }

            // 縦軸の数値。罫線の高さに中心を合わせ、列の右端（罫線の左端側）にそろえる
            labelLayouts.forEach { (v, layout) ->
                drawText(
                    layout,
                    topLeft = Offset(
                        plotLeft - AxisLabelGap.toPx() - layout.size.width,
                        yOf(v) - layout.size.height / 2f,
                    ),
                )
            }

            goal?.let {
                drawLine(
                    goalColor,
                    Offset(plotLeft, yOf(it)),
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
            // ある程度を超えたら線だけにする。点は線と重なっても分かるよう、
            // カード背景色で縁取りしてから塗る
            if (points.size <= 20) {
                val dotRadius = if (points.size > 10) 2.5f else 4.5f
                for (i in points.indices) {
                    val center = offsetAt(i)
                    drawCircle(cardColor, radius = dotRadius + 2f, center = center)
                    drawCircle(lineColor, radius = dotRadius, center = center)
                }
            }
        }

        // 横軸の日付。数値の列を除いた、折れ線と同じ幅の中で均等割りにする
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = gutterWidth, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            axisTicks.forEach { i -> ChartAxisLabel(axisDateLabel(points, i), color = axisTextColor) }
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
