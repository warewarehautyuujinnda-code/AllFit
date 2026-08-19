package com.hinata.fitlog.ui.feedback

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

private const val STROKE_WIDTH_PX = 8f

/**
 * キャプチャ画像に赤線の書き込みを合成してキャッシュへ保存し、共有シートを開く。
 * strokes は画面表示座標。scale は表示座標→ビットマップ座標への倍率
 * （FeedbackAnnotateScreen で aspectRatio を実サイズに合わせているため縦横共通の値でよい）。
 */
fun shareFeedback(
    context: Context,
    screenshot: ImageBitmap,
    strokes: List<List<Offset>>,
    scale: Float,
    note: String,
) {
    val composed = screenshot.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(composed)
    val paint = Paint().apply {
        color = android.graphics.Color.RED
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH_PX * scale
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    strokes.forEach { points ->
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points[0].x * scale, points[0].y * scale)
                for (i in 1 until points.size) {
                    lineTo(points[i].x * scale, points[i].y * scale)
                }
            }
            canvas.drawPath(path, paint)
        }
    }

    val dir = File(context.cacheDir, "feedback").apply { mkdirs() }
    val file = File(dir, "feedback_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out -> composed.compress(Bitmap.CompressFormat.PNG, 100, out) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        if (note.isNotBlank()) putExtra(Intent.EXTRA_TEXT, note)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "フィードバックを共有"))
}
