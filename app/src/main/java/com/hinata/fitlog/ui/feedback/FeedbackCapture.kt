package com.hinata.fitlog.ui.feedback

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 現在のウィンドウ全体（フィードバックボタン自身も含む）をキャプチャする。
 * Jetpack ComposeのgraphicsLayerキャプチャAPIはCompose BOMのバージョンによって
 * 使えないことがあるため、より枯れたPixelCopy（API 26+のWindow版）を使う。
 */
suspend fun captureWindow(activity: Activity): Bitmap? = suspendCancellableCoroutine { cont ->
    val window = activity.window
    val decorView = window.decorView
    if (decorView.width <= 0 || decorView.height <= 0) {
        cont.resume(null)
        return@suspendCancellableCoroutine
    }
    val bitmap = Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
    PixelCopy.request(
        window,
        bitmap,
        { result -> cont.resume(if (result == PixelCopy.SUCCESS) bitmap else null) },
        Handler(Looper.getMainLooper()),
    )
}
