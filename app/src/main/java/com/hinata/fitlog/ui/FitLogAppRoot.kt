package com.hinata.fitlog.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hinata.fitlog.ui.data.DataScreen
import com.hinata.fitlog.ui.feedback.FeedbackAnnotateScreen
import com.hinata.fitlog.ui.feedback.FeedbackButton
import com.hinata.fitlog.ui.feedback.captureWindow
import com.hinata.fitlog.ui.home.HomeScreen
import com.hinata.fitlog.ui.meal.MealScreen
import com.hinata.fitlog.ui.navigation.Destination
import com.hinata.fitlog.ui.running.RunningScreen
import com.hinata.fitlog.ui.strength.StrengthScreen
import com.hinata.fitlog.ui.weight.WeightScreen
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("画面キャプチャに必要な Activity が見つかりません")
}

/**
 * アプリのルート。下部ナビゲーションで6画面を切り替える。
 * 全画面共通のフィードバックボタン（虫アイコン）もここでオーバーレイ表示する。
 */
@Composable
fun FitLogAppRoot() {
    val navController = rememberNavController()
    val activity = LocalContext.current.findActivity()
    val scope = rememberCoroutineScope()
    var captured by remember { mutableStateOf<ImageBitmap?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBar {
                    Destination.entries.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == dest.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    // バックスタックが積み上がらないようホームまで戻す
                                    popUpTo(Destination.HOME.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Destination.HOME.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Destination.HOME.route) { HomeScreen() }
                composable(Destination.WEIGHT.route) { WeightScreen() }
                composable(Destination.STRENGTH.route) { StrengthScreen() }
                composable(Destination.RUNNING.route) { RunningScreen() }
                composable(Destination.MEAL.route) { MealScreen() }
                composable(Destination.DATA.route) { DataScreen() }
            }
        }

        FeedbackButton(
            onClick = {
                scope.launch {
                    captureWindow(activity)?.let { bitmap -> captured = bitmap.asImageBitmap() }
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
        )

        captured?.let { bitmap ->
            FeedbackAnnotateScreen(
                screenshot = bitmap,
                onDismiss = { captured = null },
            )
        }
    }
}
