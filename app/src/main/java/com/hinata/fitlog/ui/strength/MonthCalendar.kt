package com.hinata.fitlog.ui.strength

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinata.fitlog.domain.PartBadge
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val WEEKDAY_LABELS = listOf("月", "火", "水", "木", "金", "土", "日")

/** 週の開始は月曜。月曜=0 … 日曜=6 */
private fun DayOfWeek.mondayFirstIndex(): Int = (value - DayOfWeek.MONDAY.value + 7) % 7

/** 見出し用の曜日1文字（月〜日） */
internal fun japaneseWeekday(date: LocalDate): String =
    WEEKDAY_LABELS[date.dayOfWeek.mondayFirstIndex()]

private fun weekdayColor(index: Int, fallback: Color): Color = when (index) {
    5 -> SaturdayColor
    6 -> SundayColor
    else -> fallback
}

/**
 * 月表示のカレンダー。外部ライブラリを使わず java.time と Row/Column の7列グリッドで組む。
 *
 * @param badgesByDate 日付(yyyy-MM-dd) → その日に鍛えた部位と種目数
 */
@Composable
fun MonthCalendar(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    badgesByDate: Map<String, List<PartBadge>>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstOfMonth = yearMonth.atDay(1)
    val offset = firstOfMonth.dayOfWeek.mondayFirstIndex()
    val gridStart = firstOfMonth.minusDays(offset.toLong())
    val weeks = (offset + yearMonth.lengthOfMonth() + 6) / 7

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "前の月")
                }
                Text(
                    "${yearMonth.year}年${yearMonth.monthValue}月",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "次の月")
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

            repeat(weeks) { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { index ->
                        val date = gridStart.plusDays((week * 7 + index).toLong())
                        DayCell(
                            date = date,
                            inMonth = YearMonth.from(date) == yearMonth,
                            selected = date == selectedDate,
                            isToday = date == today,
                            badges = badgesByDate[date.toString()].orEmpty(),
                            onClick = { onSelectDate(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            TextButton(onClick = onToday, modifier = Modifier.align(Alignment.End)) {
                Text("今日へ")
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    isToday: Boolean,
    badges: List<PartBadge>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    val numberColor = if (inMonth) {
        weekdayColor(date.dayOfWeek.mondayFirstIndex(), MaterialTheme.colorScheme.onSurface)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    val outline = MaterialTheme.colorScheme.outline

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(1.dp)
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(if (selected) SelectedDayBackground else Color.Transparent)
            .then(if (isToday && !selected) Modifier.border(1.dp, outline, shape) else Modifier)
            .clickable(onClickLabel = "${date.monthValue}月${date.dayOfMonth}日を選ぶ") { onClick() }
            .padding(vertical = 3.dp),
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = numberColor,
        )
        // 1日に鍛えた部位は多くても数個。横に並べきれない分は次の行に折り返す
        badges.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.padding(top = 1.dp),
            ) {
                row.forEach { badge -> PartCountBadge(badge) }
            }
        }
    }
}

@Composable
private fun PartCountBadge(badge: PartBadge) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(13.dp)
            .clip(CircleShape)
            .background(bodyPartColor(badge.part)),
    ) {
        Text(
            badge.exerciseCount.toString(),
            color = Color.White,
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
