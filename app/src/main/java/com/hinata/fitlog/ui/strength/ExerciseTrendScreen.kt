package com.hinata.fitlog.ui.strength

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.domain.ExerciseRef
import com.hinata.fitlog.domain.ExerciseTrendPoint
import com.hinata.fitlog.domain.TrendPeriod
import com.hinata.fitlog.domain.exerciseTrendOf
import com.hinata.fitlog.domain.formatGrouped
import com.hinata.fitlog.domain.formatShortDate
import java.time.LocalDate

/**
 * 種目ごとの記録推移（Issue #48）。種目選択画面の各行から個別に開く、その種目単体の履歴グラフ。
 * 縦軸は推定1RM（Epley式）。1RMが計算できない日はその日の最大重量で代える（詳細は
 * [exerciseTrendOf] 参照）。期間は上部のセグメントボタンで切り替える。
 *
 * 行タップ＝記録入力という既存の導線は変えず、「推移を見る」を右上のアイコンから独立させた
 * ([ExercisePickerScreen] 側)。この画面自体からも右上の＋で同じ入力画面に入れるようにして、
 * 推移を見ながら記録を追加する操作を両立できるようにしている。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTrendScreen(
    ref: ExerciseRef,
    records: List<StrengthEntity>,
    today: LocalDate,
    onBack: () -> Unit,
    onAddRecord: () -> Unit,
) {
    var period by remember { mutableStateOf(TrendPeriod.ALL) }

    val trend = remember(records, ref.ex, period, today) {
        exerciseTrendOf(records, ref.ex, period, today)
    }
    // 期間で絞って0件になったのか、そもそも記録が無いのかでメッセージを変える
    val hasAnyRecord = remember(records, ref.ex) { records.any { it.ex == ref.ex } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(bodyPartColor(ref.part))
                        )
                        Text(
                            ref.ex,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = onAddRecord) {
                        Icon(Icons.Filled.Add, contentDescription = "記録する")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            PeriodSwitcher(selected = period, onSelect = { period = it })

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("推定1RMの推移", style = MaterialTheme.typography.titleMedium)
                    Box(modifier = Modifier.padding(top = 8.dp)) {
                        when {
                            trend.isEmpty() -> ChartMessage(
                                if (hasAnyRecord) "この期間の記録はありません" else "記録するとグラフが出ます"
                            )
                            // 1件では線が引けない。2件目からグラフになることを伝える
                            trend.size == 1 -> ChartMessage("2件目からグラフが出ます")
                            else -> ExerciseTrendLineChart(points = trend)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSwitcher(
    selected: TrendPeriod,
    onSelect: (TrendPeriod) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TrendPeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = TrendPeriod.entries.size,
                ),
            ) {
                Text(period.label)
            }
        }
    }
}

/**
 * 2件以上のときだけ呼ばれる折れ線グラフ本体。
 * 体重タブ [com.hinata.fitlog.ui.home.WeightChart] ・ランニングタブ
 * [com.hinata.fitlog.ui.running.RunningChart] と同じ、ライブラリを足さない Canvas 描画。
 */
@Composable
private fun ExerciseTrendLineChart(points: List<ExerciseTrendPoint>) {
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
                .height(200.dp),
        ) {
            val h = size.height
            // 折れ線が枠線と重ならないよう上下に、端の点が切れないよう左右に余白を取る
            val padY = h * 0.1f
            val padX = 6f
            val usableH = h - padY * 2
            val usableW = size.width - padX * 2

            fun yOf(value: Double): Float {
                val ratio = if (flat) 0.5f else ((value - min) / range).toFloat()
                // Canvas は上が y=0 なので、値が大きいほど上に来るよう反転する
                return padY + usableH * (1f - ratio)
            }

            // ここに来るのは2件以上のときだけなので、点の間隔は必ず求められる
            fun offsetAt(index: Int): Offset =
                Offset(padX + usableW * index / (points.size - 1), yOf(points[index].value))

            // 上下の基準線
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
            ChartAxisLabel("${formatGrouped(min)}〜${formatGrouped(max)} kg")
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
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
