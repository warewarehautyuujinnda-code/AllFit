package com.hinata.fitlog.running

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * GPS計測の開始に必要な権限。
 * Android 13(TIRAMISU)以降は常駐通知の表示にも許可が要る。
 * バックグラウンド計測は [RunTrackingService] をフォアグラウンドサービスとして動かすことで
 * 実現するため、より重い「常に許可」(ACCESS_BACKGROUND_LOCATION)は不要。
 */
fun trackingPermissions(): Array<String> {
    val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }
    return permissions.toTypedArray()
}

fun hasTrackingPermissions(context: Context): Boolean =
    trackingPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
