package com.hinata.fitlog.ui.feedback

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.R

/** ドラッグ範囲のクランプ計算で FitLogAppRoot 側でも同じ値が要るため公開している。 */
val FeedbackButtonSize = 60.dp

/**
 * 全タブ共通で表示するフィードバック起動ボタン。
 * 短押しでその場の画面をキャプチャして書き込み画面を開き、長押しでドラッグ移動できる（FitLogAppRoot 参照）。
 */
@Composable
fun FeedbackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    grabbed: Boolean = false,
) {
    // 掴んだ状態が指で隠れても分かるよう、拡大と影で手がかりを出す
    val scale by animateFloatAsState(if (grabbed) 1.15f else 1f, label = "feedbackButtonScale")
    val elevation by animateDpAsState(if (grabbed) 16.dp else 6.dp, label = "feedbackButtonElevation")

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(FeedbackButtonSize)
            .scale(scale),
        containerColor = Color.White,
        contentColor = Color(0xFF3D2E28),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = elevation,
            pressedElevation = elevation,
            focusedElevation = elevation,
            hoveredElevation = elevation,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_feedback_character),
            contentDescription = "フィードバックを送る",
            tint = Color.Unspecified,
            modifier = Modifier.size(36.dp),
        )
    }
}
