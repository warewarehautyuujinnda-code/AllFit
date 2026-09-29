package com.hinata.fitlog.ui.strength

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import com.hinata.fitlog.domain.ExerciseTrendPeriod
import com.hinata.fitlog.domain.exerciseTrendOf
import com.hinata.fitlog.domain.formatGrouped
import com.hinata.fitlog.domain.frequentExercises
import com.hinata.fitlog.domain.weightAxisOf
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 種目別推移（Issue #97）。1種目のボリューム・推定1RM・最大負荷を、同じ期間の3つのグラフで縦に並べる。
 * 期間は「基準月を最後の月として何ヶ月分か」で決め、月送りで過去にさかのぼれる。
 *
 * @param initialExercise 最初に出す種目。カレンダーの種目カードの長押しから来たときはその種目、
 *   「種目別推移」ボタンから来たときは null（よくやる種目の先頭を出す）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTrendScreen(
    records: List<StrengthRecordWithSets>,
    today: LocalDate,
    initialExercise: String?,
    onBack: () -> Unit,
) {
    // 記録のある種目だけを選択肢にする。記録の無い種目はグラフにしようがないため
    val exerciseNames = remember(records) {
        frequentExercises(records, limit = Int.MAX_VALUE).map { it.ex }
    }
    var selectedExercise by rememberSaveable {
        mutableStateOf(initialExercise ?: exerciseNames.firstOrNull())
    }
    var period by rememberSaveable { mutableStateOf(ExerciseTrendPeriod.THREE_MONTHS) }
    var endMonth by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
    var menuExpanded by remember { mutableStateOf(false) }

    val exercise = selectedExercise
    val trend = remember(records, exercise) {
        if (exercise == null) emptyList() else exerciseTrendOf(records, exercise)
    }
    val (rangeStart, rangeEnd) = period.rangeEndingAt(endMonth)
    val inRange = trend.filter { it.date in rangeStart..rangeEnd }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // 種目名を押すと、記録のある別の種目に切り替えられる
                    Box {
                        TextButton(
                            onClick = { menuExpanded = true },
                            enabled = exerciseNames.isNotEmpty(),
                        ) {
                            Text(
                                exercise ?: "種目別推移",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (exerciseNames.isNotEmpty()) {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "種目を切り替える",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            exerciseNames.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedExercise = name
                                        menuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            MonthNavigator(
                month = endMonth,
                onMove = { months -> endMonth = endMonth.plusMonths(months) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                val options = ExerciseTrendPeriod.entries
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == period,
                        onClick = { period = option },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = {},
                    ) {
                        Text(option.label, maxLines = 1)
                    }
                }
            }
            HorizontalDivider()

            if (exercise == null) {
                Text(
                    "筋トレを記録すると、種目ごとの推移が見られます",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        TrendChartCard(
                            title = "ボリューム推移",
                            values = inRange.mapNotNull { p -> p.volume?.let { p.date to it } },
                            rangeStart = rangeStart,
                            rangeEnd = rangeEnd,
                            emptyMessage = "重量と回数を入力した記録がこの期間にありません",
                        )
                    }
                    item {
                        TrendChartCard(
                            title = "推定1RM推移",
                            values = inRange.mapNotNull { p -> p.oneRepMax?.let { p.date to it } },
                            rangeStart = rangeStart,
                            rangeEnd = rangeEnd,
                            emptyMessage = "重量と回数を入力した記録がこの期間にありません",
                        )
                    }
                    item {
                        TrendChartCard(
                            title = "最大負荷推移",
                            values = inRange.mapNotNull { p -> p.maxWeight?.let { p.date to it } },
                            rangeStart = rangeStart,
                            rangeEnd = rangeEnd,
                            emptyMessage = "重量を入力した記録がこの期間にありません",
                        )
                    }
                }
            }
        }
    }
}

/** « ‹ 2026年9月 › »。‹ › は1ヶ月、« » は1年ずつ動かす */
@Composable
private fun MonthNavigator(
    month: YearMonth,
    onMove: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onMove(-12) }) {
            Icon(Icons.Filled.KeyboardDoubleArrowLeft, contentDescription = "1年前")
        }
        IconButton(onClick = { onMove(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "前の月")
        }
        Text(
            "${month.year}年${month.monthValue}月",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = { onMove(1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "次の月")
        }
        IconButton(onClick = { onMove(12) }) {
            Icon(Icons.Filled.KeyboardDoubleArrowRight, contentDescription = "1年後")
        }
    }
}

/**
 * グラフの系列色。体重グラフ（[com.hinata.fitlog.ui.home.WeightChart]）と同じ理由で、
 * 壁紙連動カラーに左右されない固定色を使い、ライト/ダークだけ切り替える。
 */
private val TrendLineColorLight = Color(0xFFE0612F)
private val TrendLineColorDark = Color(0xFFFF8A65)
private val TrendAxisTextColorLight = Color(0xFF6B6B6B)
private val TrendAxisTextColorDark = Color(0xFFB0B0B0)

private val TrendChartHeight = 200.dp

/** 横軸に出す日付の数（両端を含む） */
private const val X_TICK_COUNT = 7

/**
 * 1指標分のグラフカード。横軸は期間の初日〜末日の実際の日付で、記録のある日の位置に点を打つ
 * （記録の間隔が空いた期間は、そのまま間が空いて見える）。
 */
@Composable
private fun TrendChartCard(
    title: String,
    values: List<Pair<LocalDate, Double>>,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    emptyMessage: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                "kg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (values.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TrendChartHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        emptyMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                TrendLineChart(values = values, rangeStart = rangeStart, rangeEnd = rangeEnd)
            }
        }
    }
}

@Composable
private fun TrendLineChart(
    values: List<Pair<LocalDate, Double>>,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
) {
    val dark = isSystemInDarkTheme()
    val lineColor = if (dark) TrendLineColorDark else TrendLineColorLight
    val axisTextColor = if (dark) TrendAxisTextColorDark else TrendAxisTextColorLight
    val cardColor = CardDefaults.cardColors().containerColor
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

    // 縦軸の刻みは体重グラフと同じ決め方（1・2・5 × 10^n の切りのいい値）を使う
    val axis = weightAxisOf(values.minOf { it.second }, values.maxOf { it.second })
    val axisRange = axis.max - axis.min

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = axisTextColor)
    val yLabels = remember(axis, labelStyle) {
        axis.labels.map { v -> v to textMeasurer.measure(formatGrouped(v), labelStyle) }
    }
    val totalDays = ChronoUnit.DAYS.between(rangeStart, rangeEnd).coerceAtLeast(1)
    val xLabels = remember(rangeStart, rangeEnd, labelStyle) {
        (0 until X_TICK_COUNT).map { step ->
            val date = rangeStart.plusDays(totalDays * step / (X_TICK_COUNT - 1))
            date to textMeasurer.measure("${date.monthValue}/${date.dayOfMonth}", labelStyle)
        }
    }
    val density = LocalDensity.current
    val gutter = with(density) { (yLabels.maxOfOrNull { it.second.size.width } ?: 0).toDp() } + 6.dp
    val xLabelHeight = with(density) { (xLabels.maxOfOrNull { it.second.size.height } ?: 0).toDp() }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(TrendChartHeight + xLabelHeight + 4.dp)
            .padding(top = 4.dp),
    ) {
        val plotLeft = gutter.toPx()
        // 右端の日付ラベルが半分はみ出さないよう、その幅の半分を右側の余白にする
        val plotRight = size.width - (xLabels.last().second.size.width / 2f)
        val plotTop = 8.dp.toPx()
        val plotBottom = TrendChartHeight.toPx()
        val plotW = plotRight - plotLeft
        val plotH = plotBottom - plotTop

        fun xOf(date: LocalDate): Float =
            plotLeft + plotW * ChronoUnit.DAYS.between(rangeStart, date) / totalDays.toFloat()

        fun yOf(value: Double): Float {
            val ratio = if (axisRange > 0) ((value - axis.min) / axisRange).toFloat() else 0.5f
            return plotTop + plotH * (1f - ratio)
        }

        // 横罫線と縦軸の数値
        axis.lines.forEach { v ->
            drawLine(
                gridColor,
                Offset(plotLeft, yOf(v)),
                Offset(plotRight, yOf(v)),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
            )
        }
        yLabels.forEach { (v, layout) ->
            drawText(
                layout,
                topLeft = Offset(
                    plotLeft - 6.dp.toPx() - layout.size.width,
                    yOf(v) - layout.size.height / 2f,
                ),
            )
        }

        // 縦罫線と横軸の日付
        xLabels.forEach { (date, layout) ->
            val x = xOf(date)
            drawLine(
                gridColor,
                Offset(x, plotTop),
                Offset(x, plotBottom),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
            )
            drawText(
                layout,
                topLeft = Offset(
                    (x - layout.size.width / 2f).coerceAtLeast(0f),
                    plotBottom + 4.dp.toPx(),
                ),
            )
        }

        // 枠
        drawRect(
            gridColor.copy(alpha = 0.5f),
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotW, plotH),
            style = Stroke(width = 1.5f),
        )

        val points = values.map { (date, v) -> Offset(xOf(date), yOf(v)) }

        if (points.size >= 2) {
            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            // 線の下を薄く塗って、量の増減を面でも読めるようにする
            val area = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, plotBottom)
                lineTo(points.first().x, plotBottom)
                close()
            }
            drawPath(
                area,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0.02f)),
                    startY = plotTop,
                    endY = plotBottom,
                ),
            )
            drawPath(line, color = lineColor, style = Stroke(width = 2.5.dp.toPx()))
        }

        // 記録のある日に白抜きの点を打つ。点が多い期間は小さくして潰れを防ぐ
        val radius = (if (points.size > 40) 3.dp else 4.5.dp).toPx()
        points.forEach { center ->
            drawCircle(cardColor, radius = radius, center = center)
            drawCircle(lineColor, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
