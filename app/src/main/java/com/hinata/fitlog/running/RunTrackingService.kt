package com.hinata.fitlog.running

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.MainActivity
import com.hinata.fitlog.R
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.RunningPointEntity
import com.hinata.fitlog.data.entity.RunningSplitEntity
import com.hinata.fitlog.data.isoNow
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatElapsed
import com.hinata.fitlog.domain.haversineMeters
import com.hinata.fitlog.ui.common.DateUtil
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * GPSでランを計測するフォアグラウンドサービス（要件定義: ランニングタブGPS計測）。
 *
 * 常駐通知を出しながら動くフォアグラウンドサービスとして実装することで、画面消灯・他アプリへの
 * 切り替え中も位置情報の取得を続けられる。この方式なら「アプリ使用中のみ許可」の通常の位置情報
 * 権限で足り、より重い「常に許可」(ACCESS_BACKGROUND_LOCATION)は不要になる。
 *
 * 位置は Google Play Services を使わず、端末標準の [LocationManager] から直接取得する
 * （本アプリは Play Services に依存していないため）。GPSの揺れによる距離のブレを抑えるため、
 * 精度(accuracy)が悪い位置情報は無視する。
 */
class RunTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob())
    private var tickerJob: Job? = null
    private lateinit var locationManager: LocationManager

    private var runId: String = ""
    private var startDate: String = ""
    private var startElapsedRealtime = 0L
    private var lastLocation: Location? = null
    private var distanceMeters = 0.0
    private var lastRecordedMinute = 0
    private val splits = mutableListOf<RunningSplitEntity>()
    private val points = mutableListOf<RunningPointEntity>()

    private val locationListener = LocationListener { location ->
        if (location.accuracy <= MAX_ACCURACY_METERS) {
            lastLocation?.let { prev ->
                distanceMeters += haversineMeters(
                    prev.latitude, prev.longitude, location.latitude, location.longitude,
                )
            }
            lastLocation = location
            points.add(
                RunningPointEntity(
                    runId = runId,
                    sequence = points.size,
                    latitude = location.latitude,
                    longitude = location.longitude,
                )
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(locationListener)
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun startTracking() {
        // 既に計測中の場合の二重開始は無視する
        if (tickerJob != null) return

        runId = UUID.randomUUID().toString()
        startDate = DateUtil.today()
        startElapsedRealtime = SystemClock.elapsedRealtime()
        distanceMeters = 0.0
        lastRecordedMinute = 0
        lastLocation = null
        splits.clear()
        points.clear()

        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification(elapsedSec = 0, distanceKm = 0.0))

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_MS,
                MIN_DISTANCE_M,
                locationListener,
            )
        } catch (e: SecurityException) {
            // 権限が取り消されている等で開始できない場合はここで終了する
            stopTracking()
            return
        }

        RunTracker.update(RunTrackState(status = RunStatus.TRACKING))

        tickerJob = scope.launch {
            while (isActive) {
                delay(1000)
                tick()
            }
        }
    }

    private fun tick() {
        val elapsedSec = (SystemClock.elapsedRealtime() - startElapsedRealtime) / 1000
        val minute = (elapsedSec / 60).toInt()
        if (minute > lastRecordedMinute) {
            lastRecordedMinute = minute
            splits.add(
                RunningSplitEntity(
                    runId = runId,
                    minuteIndex = minute,
                    distanceKm = distanceMeters / 1000.0,
                )
            )
        }

        val distanceKm = distanceMeters / 1000.0
        RunTracker.update(
            RunTrackState(
                status = RunStatus.TRACKING,
                elapsedSec = elapsedSec,
                distanceKm = distanceKm,
                splits = splits.toList(),
            )
        )
        updateNotification(elapsedSec, distanceKm)
    }

    private fun stopTracking() {
        tickerJob?.cancel()
        tickerJob = null
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(locationListener)
        }

        val elapsedSec = (SystemClock.elapsedRealtime() - startElapsedRealtime) / 1000
        val distanceKm = distanceMeters / 1000.0

        // 誤操作等での0件記録を防ぐため、極端に短い計測は保存しない
        if (elapsedSec >= MIN_SAVE_SECONDS && distanceKm > 0.0) {
            val run = RunningEntity(
                id = runId,
                date = startDate,
                dist = distanceKm,
                min = elapsedSec / 60.0,
                kcal = null,
                createdAt = isoNow(),
            )
            val savedSplits = splits.toList()
            val savedPoints = points.toList()
            scope.launch {
                (application as FitLogApp).runningRepository.saveTracked(run, savedSplits, savedPoints)
                RunTracker.reset()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } else {
            RunTracker.reset()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        // 以前の「重要度: 低」のチャンネルはサイレント通知扱いでロック画面に出ないため作り直した。
        // チャンネルの重要度はアプリ側から後で変えられないので、IDを変えて古い方は消す。
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ランニング計測",
                // 「重要度: 中」にしてロック画面に表示させる。1秒ごとに更新するので音とバイブは切る
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "ランニング計測中に、ロック画面にも距離と経過時間を表示する通知"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                // ロックを解除しなくても距離と経過時間が読めるように、内容まで公開する
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(elapsedSec: Long, distanceKm: Double) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(elapsedSec, distanceKm))
    }

    private fun buildNotification(elapsedSec: Long, distanceKm: Double): Notification {
        // ロック画面ではタイトルが一番大きく出るため、走りながら見たい距離と経過時間をタイトルに置く
        val contentTitle = String.format(
            Locale.US, "%s km ・ %s", formatAmount(distanceKm), formatElapsed(elapsedSec),
        )
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText("ランニング計測中")
            .setSmallIcon(R.drawable.ic_notification_run)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            // ロックがかかった状態でも内容（距離・経過時間）を隠さずに表示する
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.hinata.fitlog.running.action.START"
        const val ACTION_STOP = "com.hinata.fitlog.running.action.STOP"

        private const val CHANNEL_ID = "running_tracking_lockscreen"
        private const val LEGACY_CHANNEL_ID = "running_tracking"
        private const val NOTIFICATION_ID = 1001

        /** 位置情報の更新間隔・最小移動距離。頻度と電池消費のバランスを取った値 */
        private const val MIN_TIME_MS = 3000L
        private const val MIN_DISTANCE_M = 5f

        /** これより精度(誤差半径)が悪い位置情報は距離計算に使わない */
        private const val MAX_ACCURACY_METERS = 20f

        /** これより短い計測は誤操作とみなして保存しない */
        private const val MIN_SAVE_SECONDS = 10
    }
}
