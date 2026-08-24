package com.hinata.fitlog.running

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** UI側から計測の開始・停止を[RunTrackingService]に伝える入り口 */
object RunTrackingController {
    fun start(context: Context) {
        val intent = Intent(context, RunTrackingService::class.java)
            .setAction(RunTrackingService.ACTION_START)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, RunTrackingService::class.java)
            .setAction(RunTrackingService.ACTION_STOP)
        context.startService(intent)
    }
}
