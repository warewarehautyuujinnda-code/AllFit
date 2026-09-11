package com.hinata.fitlog.ui.running

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hinata.fitlog.data.entity.RunningPointEntity
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * 記録詳細画面で使う、走った経路の地図。GPSで記録した緯度経度を衛星写真の上に線で重ねる。
 *
 * 本アプリは記録データを外部サーバーへ一切送らない方針（[com.hinata.fitlog.data.AppDatabase]参照）。
 * ここで行う通信は地図画像（タイル）の取得だけで、記録そのものは送らない。
 * 通信できないとき（オフライン）はタイルが灰色のままになるが、経路の線と開始・ゴールの印は
 * 端末内のデータだけで描けるので、そのまま表示される。
 *
 * Google Maps SDK はAPIキーの発行（Google Cloud のプロジェクト作成）が必要なため、
 * キー不要で使える Esri の衛星写真タイルを osmdroid で表示している。
 */
@Composable
fun RunningRouteMap(points: List<RunningPointEntity>, modifier: Modifier = Modifier) {
    val route = remember(points) { points.map { GeoPoint(it.latitude, it.longitude) } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (route.size < 2) {
            Text(
                "経路データなし",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            // 経路が変わったら地図ごと作り直す（記録を開いた直後は経路の読み込み待ちで空のことがある）
            key(route) {
                RouteMapView(route = route, modifier = Modifier.fillMaxSize())
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(OVERLAY_BACKGROUND, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                RouteLegend(color = START_COLOR, label = "開始")
                RouteLegend(
                    color = MaterialTheme.colorScheme.error,
                    label = "ゴール",
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            // 衛星写真タイルの利用条件として出典を表示する
            Text(
                ESRI_ATTRIBUTION,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(OVERLAY_BACKGROUND, RoundedCornerShape(topStart = 8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun RouteMapView(route: List<GeoPoint>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val endColor = MaterialTheme.colorScheme.error.toArgb()
    val mapView = remember {
        configureOsmdroid(context)
        createRouteMapView(context, density, route, endColor)
    }

    // 画面の表示・非表示に合わせてタイル取得を止め、画面を離れたら地図の資源を解放する
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun createRouteMapView(
    context: Context,
    density: Density,
    route: List<GeoPoint>,
    endColor: Int,
): MapView = MapView(context).apply {
    setTileSource(ESRI_WORLD_IMAGERY)
    setMultiTouchControls(true)
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
    isTilesScaledToDpi = true
    minZoomLevel = 3.0
    maxZoomLevel = MAX_ZOOM_LEVEL

    // レイアウトが決まるまでは経路全体に合わせた縮尺が計算できないので、まず開始地点に寄せておく
    controller.setZoom(15.0)
    controller.setCenter(route.first())

    with(density) {
        // 衛星写真の上でも線が埋もれないよう、白い縁取りの上に色付きの線を重ねる
        overlays.add(routeLine(route, android.graphics.Color.WHITE, 9.dp.toPx()))
        overlays.add(routeLine(route, START_COLOR.toArgb(), 5.dp.toPx()))
        overlays.add(endpointMarker(this@apply, route.first(), START_COLOR.toArgb(), 14.dp.roundToPx(), 2.dp.roundToPx()))
        overlays.add(endpointMarker(this@apply, route.last(), endColor, 14.dp.roundToPx(), 2.dp.roundToPx()))

        val borderPx = 32.dp.roundToPx()
        addOnFirstLayoutListener { _, _, _, _, _ ->
            zoomToBoundingBox(BoundingBox.fromGeoPoints(route), false, borderPx)
        }
    }
}

private fun routeLine(route: List<GeoPoint>, color: Int, widthPx: Float): Polyline =
    // MapViewを渡さずに作ると、タップしても空の吹き出しが開かない
    Polyline().apply {
        setPoints(route)
        outlinePaint.color = color
        outlinePaint.strokeWidth = widthPx
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        outlinePaint.isAntiAlias = true
    }

private fun endpointMarker(
    mapView: MapView,
    position: GeoPoint,
    color: Int,
    sizePx: Int,
    borderPx: Int,
): Marker = Marker(mapView).apply {
    this.position = position
    icon = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(borderPx, android.graphics.Color.WHITE)
        setSize(sizePx, sizePx)
    }
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    // 印をタップしても何も開かないようにする（開始・ゴールは凡例で示している）
    setOnMarkerClickListener { _, _ -> true }
}

/**
 * osmdroid の保存先と User-Agent を設定する。既定では外部ストレージに最大600MBのタイルを溜めるため、
 * アプリのキャッシュ領域（端末の空きが減ればOSが消せる場所）に小さく収める。
 */
private fun configureOsmdroid(context: Context) {
    Configuration.getInstance().apply {
        userAgentValue = context.packageName
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(osmdroidBasePath, "tiles")
        tileFileSystemCacheMaxBytes = 50L * 1024 * 1024
        tileFileSystemCacheTrimBytes = 40L * 1024 * 1024
    }
}

@Composable
private fun RouteLegend(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(8.dp)
                .background(color, RoundedCornerShape(50)),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

/** 地図の最大ズーム。Esri の衛星写真は市街地でもこれ以上は画像が用意されていないことが多い */
private const val MAX_ZOOM_LEVEL = 19.0

private const val ESRI_ATTRIBUTION = "© Esri, Maxar, Earthstar Geographics"

/**
 * Esri World Imagery（衛星写真）。URLが z/y/x の順なので、z/x/y 前提の既定実装を差し替える。
 */
private val ESRI_WORLD_IMAGERY = object : OnlineTileSourceBase(
    "EsriWorldImagery",
    0,
    MAX_ZOOM_LEVEL.toInt(),
    256,
    "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    ESRI_ATTRIBUTION,
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl + "${MapTileIndex.getZoom(pMapTileIndex)}/" +
            "${MapTileIndex.getY(pMapTileIndex)}/${MapTileIndex.getX(pMapTileIndex)}"
}

/** 衛星写真の上に重ねる凡例・出典の背景。写真の明暗に関わらず白文字が読めるよう半透明の黒にする */
private val OVERLAY_BACKGROUND = Color(0x99000000)

/** 経路の線と開始地点マーカーの色。ゴール（[MaterialTheme.colorScheme.error]）と区別する */
private val START_COLOR = Color(0xFF2E7D32)
