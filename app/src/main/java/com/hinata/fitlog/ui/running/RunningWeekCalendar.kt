package com.hinata.fitlog.ui.running

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.runningDistanceByDate
import com.hinata.fitlog.ui.strength.SaturdayColor
import com.hinata.fitlog.ui.strength.SundayColor
import java.time.DayOfWeek
import java.time.LocalDate

private val WEEKDAY_LABELS = listOf("月", "火", "水", "木", "金", "土", "日")

/** 走った日の印。ランニングは部位の色分けが無いので、テーマの主色1色の丸で表す */
private val RunDotSize = 8.dp

private fun weekdayColor(index: Int, fallback: Color): Color = when (index) {
    5 -> SaturdayColor
    6 -> SundayColor
    else -> fallback
}

/** その日を含む週の月曜日。週の開始は筋トレのカレンダーに合わせて月曜 */
private fun LocalDate.weekStart(): LocalDate =
    minusDays(((dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7).toLong())

/**
 * 1週間（月〜日）分のカレンダー。筋トレタブの月カレンダーと同じ見た目で、走った日に印と距離を出す。
 * 月表示だとタイマーやグラフと並べたときに縦に場所を取りすぎるため、ランニングでは週単位にしている。
 * 表示中の週はこの画面の中だけの状態なので、ここで持つ。
 *
 * @param records 全ランニング記録（並び順は問わない）
 */
@Composable
fun RunningWeekCalendar(
    records: List<RunningEntity>,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val thisWeek = today.weekStart()
    // LocalDate は Bundle に入らないので文字列で保持する
    var weekStartText by rememberSaveable { mutableStateOf(thisWeek.toString()) }
    val weekStart = LocalDate.parse(weekStartText)
    val weekEnd = weekStart.plusDays(6)
    val distanceByDate = remember(records) { runningDistanceByDate(records) }
    val weekTotal = (0L..6L).sumOf { distanceByDate[weekStart.plusDays(it).toString()] ?: 0.0 }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { weekStartText = weekStart.minusWeeks(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "前の週")
                }
                Text(
                    "${weekStart.monthValue}/${weekStart.dayOfMonth} 〜 ${weekEnd.monthValue}/${weekEnd.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { weekStartText = weekStart.plusWeeks(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "次の週")
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAY_LABELS.forEachIndexed { index, label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = weekdayColor(index, MaterialTheme.colorScheme.onSurfaceVariant),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { index ->
                    val date = weekStart.plusDays(index.toLong())
                    RunDayCell(
                        date = date,
                        weekdayIndex = index,
                        isToday = date == today,
                        distanceKm = distanceByDate[date.toString()],
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "この週の合計 ${formatAmount(weekTotal)} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                // 今週を表示しているときは押しても変わらないので出さない
                if (weekStart != thisWeek) {
                    TextButton(onClick = { weekStartText = thisWeek.toString() }) {
                        Text("今週へ")
                    }
                } else {
                    // ボタンの有無でカードの高さが変わらないよう、同じ高さを確保しておく
                    Box(Modifier.defaultMinSize(minHeight = 40.dp))
                }
            }
        }
    }
}

@Composable
private fun RunDayCell(
    date: LocalDate,
    weekdayIndex: Int,
    isToday: Boolean,
    distanceKm: Double?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    val ran = distanceKm != null
    val primary = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(1.dp)
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(if (ran) primary.copy(alpha = 0.12f) else Color.Transparent)
            .then(
                if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                else Modifier
            )
            .padding(vertical = 3.dp),
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = weekdayColor(weekdayIndex, MaterialTheme.colorScheme.onSurface),
        )
        if (distanceKm != null) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(RunDotSize)
                    .clip(CircleShape)
                    .background(primary),
            )
            Text(
                formatAmount(distanceKm),
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                maxLines = 1,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}
