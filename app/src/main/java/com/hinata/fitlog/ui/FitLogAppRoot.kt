package com.hinata.fitlog.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hinata.fitlog.ui.home.HomeScreen
import com.hinata.fitlog.ui.navigation.Destination
import com.hinata.fitlog.ui.running.RunningScreen
import com.hinata.fitlog.ui.screens.DataScreen
import com.hinata.fitlog.ui.screens.MealScreen
import com.hinata.fitlog.ui.strength.StrengthScreen
import com.hinata.fitlog.ui.weight.WeightScreen

/**
 * アプリのルート。下部ナビゲーションで6画面を切り替える。
 */
@Composable
fun FitLogAppRoot() {
    val navController = rememberNavController()

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
}
