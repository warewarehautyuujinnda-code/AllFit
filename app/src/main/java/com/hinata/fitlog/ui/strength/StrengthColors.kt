package com.hinata.fitlog.ui.strength

import androidx.compose.ui.graphics.Color
import com.hinata.fitlog.domain.BodyPart

/**
 * 部位の色。カレンダーのバッジ・タブ・種目カードの丸で共通に使う。
 * 端末の壁紙連動カラーに合わせて変わると部位の見分けが付かなくなるため、
 * テーマ色ではなく固定色を持たせている。
 */
fun bodyPartColor(part: BodyPart?): Color = when (part) {
    BodyPart.CHEST -> Color(0xFFE8483C)
    BodyPart.BACK -> Color(0xFF3B82F6)
    BodyPart.SHOULDER -> Color(0xFF22C55E)
    BodyPart.ARM -> Color(0xFFEC4899)
    BodyPart.ABS -> Color(0xFF78453A)
    BodyPart.LEG -> Color(0xFFA855F7)
    null -> Color(0xFF9E9E9E)
}

/** よくやる種目タブの星 */
val FavoriteColor = Color(0xFFF5B301)

/** 土曜・日曜と、選択中の日付セルの背景 */
val SaturdayColor = Color(0xFF3B82F6)
val SundayColor = Color(0xFFE8483C)
val SelectedDayBackground = Color(0xFF3B82F6).copy(alpha = 0.18f)
