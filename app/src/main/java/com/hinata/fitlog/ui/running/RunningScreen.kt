package com.hinata.fitlog.ui.running

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.domain.formatElapsed
import com.hinata.fitlog.domain.formatPace
import com.hinata.fitlog.running.RunStatus
import com.hinata.fitlog.running.RunTrackState
import com.hinata.fitlog.running.RunTrackingController
import com.hinata.fitlog.running.hasTrackingPermissions
import com.hinata.fitlog.running.trackingPermissions
import kotlinx.coroutines.launch

/**
 * ランニングタブの入り口。
 * メイン（グラフ+タイマー+直近3件）→ 手入力 / 一覧 → 詳細 の画面をタブ内の状態で切り替える。
 * 筋トレタブと同様、下部ナビゲーションの構成は変えたくないため、入れ子の NavHost は作らない。
 */
private sealed interface RunningRoute {
    data object Main : RunningRoute
    data object ManualEntry : RunningRoute
    data object List : RunningRoute
    data class Detail(val item: RunningEntity) : RunningRoute
}

@Composable
fun RunningScreen(viewModel: RunningViewModel = viewModel()) {
    var route by remember { mutableStateOf<RunningRoute>(RunningRoute.Main) }
    val items by viewModel.items.collectAsState()

    BackHandler(enabled = route != RunningRoute.Main) {
        route = if (route is RunningRoute.Detail) RunningRoute.List else RunningRoute.Main
    }

    when (val current = route) {
        RunningRoute.Main -> RunningMainScreen(
            viewModel = viewModel,
            onOpenList = { route = RunningRoute.List },
            onOpenManualEntry = { route = RunningRoute.ManualEntry },
        )

        RunningRoute.ManualEntry -> RunningManualEntryScreen(
            onBack = { route = RunningRoute.Main },
            onSave = viewModel::save,
        )

        RunningRoute.List -> RunningListScreen(
            items = items,
            onBack = { route = RunningRoute.Main },
            onOpenDetail = { item -> route = RunningRoute.Detail(item) },
            onDelete = { viewModel.delete(it) },
        )

        is RunningRoute.Detail -> {
            // 削除等で一覧から消えても最新の内容で表示できるよう、選択中の記録はidで引き直す
            val item = items.firstOrNull { it.id == current.item.id } ?: current.item
            val splits by remember(item.id) { viewModel.splitsFor(item.id) }
                .collectAsState(initial = emptyList())
            RunningDetailScreen(
                item = item,
                splits = splits,
                onBack = { route = RunningRoute.List },
                onDelete = {
                    viewModel.delete(item)
                    route = RunningRoute.List
                },
            )
        }
    }
}

@Composable
private fun RunningMainScreen(
    viewModel: RunningViewModel,
    onOpenList: () -> Unit,
    onOpenManualEntry: () -> Unit,
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsState()
    val trend by viewModel.trend.collectAsState()
    val trendPeriod by viewModel.trendPeriod.collectAsState()
    val trendMetric by viewModel.trendMetric.collectAsState()
    val monthlyTotalKm by viewModel.monthlyTotalKm.collectAsState()
    val trackState by viewModel.trackState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.all { it }) {
            RunTrackingController.start(context)
        } else {
            scope.launch { snackbarHostState.showSnackbar("位置情報の使用を許可すると計測できます") }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ランニング", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onOpenList) { Text("記録") }
            }

            RunningChart(
                trend = trend,
                latest = items.firstOrNull(),
                monthlyTotalKm = monthlyTotalKm,
                period = trendPeriod,
                metric = trendMetric,
                onPeriodChange = viewModel::selectTrendPeriod,
                onMetricChange = viewModel::selectTrendMetric,
                modifier = Modifier.padding(top = 12.dp),
            )

            TimerCard(
                state = trackState,
                onStart = {
                    if (hasTrackingPermissions(context)) {
                        RunTrackingController.start(context)
                    } else {
                        permissionLauncher.launch(trackingPermissions())
                    }
                },
                onStop = { RunTrackingController.stop(context) },
                modifier = Modifier.padding(top = 16.dp),
            )

            TextButton(
                onClick = onOpenManualEntry,
                modifier = Modifier.padding(top = 4.dp),
            ) { Text("手入力で記録を追加") }

            Text(
                "直近の記録",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (items.isEmpty()) {
                Text(
                    "まだ記録がありません",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    items.take(RECENT_COUNT).forEach { item ->
                        RunningRow(item = item, onDelete = { viewModel.delete(item) })
                    }
                }
            }
        }
    }
}

/** タイマー(この画面の主役)。開始/停止と、計測中の経過時間・距離・平均ペースを表示する */
@Composable
private fun TimerCard(
    state: RunTrackState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pace = formatPace(state.distanceKm, state.elapsedSec / 60.0)
    val tracking = state.status == RunStatus.TRACKING

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(formatElapsed(state.elapsedSec), style = MaterialTheme.typography.displayMedium)

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                TimerStat(label = "距離", value = "${formatAmount(state.distanceKm)} km")
                TimerStat(label = "ペース", value = pace?.let { "$it /km" } ?: "—")
            }

            Spacer(Modifier.height(20.dp))

            if (tracking) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("停止") }
            } else {
                Button(onClick = onStart) { Text("開始") }
            }
        }
    }
}

@Composable
private fun TimerStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

private const val RECENT_COUNT = 3
