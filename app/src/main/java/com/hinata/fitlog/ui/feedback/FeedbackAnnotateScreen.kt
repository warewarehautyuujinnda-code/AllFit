package com.hinata.fitlog.ui.feedback

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** 実機で確実に押せるよう、既定の TextButton の約2倍のタップ領域にする。 */
private val ActionButtonPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)

/**
 * キャプチャした画面に赤線で書き込み、メモを添えて共有するフィードバック画面。
 * どのタブからでも FeedbackButton 経由で開く（FitLogAppRoot 参照）。
 */
@Composable
fun FeedbackAnnotateScreen(
    screenshot: ImageBitmap,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var note by remember { mutableStateOf("") }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(96.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "閉じる",
                        modifier = Modifier.size(48.dp),
                    )
                }
                Text(
                    "フィードバック",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                )
                TextButton(
                    onClick = { strokes.clear() },
                    contentPadding = ActionButtonPadding,
                ) { Text("クリア", style = MaterialTheme.typography.titleLarge) }
                TextButton(
                    onClick = {
                        val scale = if (boxSize.width > 0) {
                            screenshot.width.toFloat() / boxSize.width.toFloat()
                        } else {
                            1f
                        }
                        shareFeedback(context, screenshot, strokes.toList(), scale, note)
                        onDismiss()
                    },
                    contentPadding = ActionButtonPadding,
                ) { Text("共有", style = MaterialTheme.typography.titleLarge) }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(screenshot.width.toFloat() / screenshot.height.toFloat())
                        .fillMaxSize()
                        .onSizeChanged { boxSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> currentStroke = listOf(offset) },
                                onDrag = { change, _ ->
                                    currentStroke = currentStroke + change.position
                                    change.consume()
                                },
                                onDragEnd = {
                                    if (currentStroke.size > 1) strokes.add(currentStroke)
                                    currentStroke = emptyList()
                                },
                            )
                        },
                ) {
                    Image(
                        bitmap = screenshot,
                        contentDescription = "現在の画面",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        (strokes + listOf(currentStroke)).forEach { points ->
                            if (points.size > 1) {
                                val path = Path().apply {
                                    moveTo(points[0].x, points[0].y)
                                    for (i in 1 until points.size) {
                                        lineTo(points[i].x, points[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color.Red,
                                    style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("気になった点を書いてください") },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            )
        }
    }
}
