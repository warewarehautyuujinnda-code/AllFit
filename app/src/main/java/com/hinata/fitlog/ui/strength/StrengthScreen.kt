package com.hinata.fitlog.ui.strength

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hinata.fitlog.domain.ExerciseRef
import com.hinata.fitlog.ui.common.DateUtil
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * 筋トレタブ（FR-02）の入り口。
 * カレンダー → 種目選択 → セット入力 の3画面をタブ内の状態で切り替える。
 * 下部ナビゲーションの構成は変えたくないため、ここでは入れ子の NavHost を作らず内部状態で持つ。
 */
private sealed interface StrengthRoute {
    data object Calendar : StrengthRoute
    data object Picker : StrengthRoute
    data class Input(val ref: ExerciseRef) : StrengthRoute
}

@Composable
fun StrengthScreen(viewModel: StrengthViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()
    val pendingExercises by viewModel.pendingExercises.collectAsState()
    val weekTargets by viewModel.weekTargets.collectAsState()

    var route by remember { mutableStateOf<StrengthRoute>(StrengthRoute.Calendar) }
    var selectedDate by rememberSaveable { mutableStateOf(DateUtil.today()) }

    // 画面を切り替えても同じ状態を使い回すので、保存直後のメッセージが遷移先で出せる
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val today = LocalDate.now()
    val selected = remember(selectedDate) {
        runCatching { LocalDate.parse(selectedDate) }.getOrDefault(today)
    }

    BackHandler(enabled = route != StrengthRoute.Calendar) {
        route = if (route is StrengthRoute.Input) StrengthRoute.Picker else StrengthRoute.Calendar
    }

    when (val current = route) {
        StrengthRoute.Calendar -> StrengthCalendarScreen(
            records = items,
            today = today,
            selectedDate = selected,
            snackbarHostState = snackbarHostState,
            onSelectDate = { selectedDate = it.toString() },
            onAdd = { route = StrengthRoute.Picker },
            onDeleteRecords = { ids -> viewModel.deleteRecords(ids) },
        )

        StrengthRoute.Picker -> ExercisePickerScreen(
            records = items,
            today = today,
            plannedExercises = pendingExercises,
            onBack = { route = StrengthRoute.Calendar },
            onPick = { ref -> route = StrengthRoute.Input(ref) },
        )

        is StrengthRoute.Input -> SetInputScreen(
            ref = current.ref,
            date = selectedDate,
            lastRecord = items.firstOrNull { it.record.ex == current.ref.ex },
            weekTarget = weekTargets.firstOrNull { it.exerciseName.trim() == current.ref.ex.trim() },
            snackbarHostState = snackbarHostState,
            onDateChange = { selectedDate = it },
            onBack = { route = StrengthRoute.Picker },
            onSave = { sets ->
                val ok = viewModel.save(
                    date = selectedDate,
                    exText = current.ref.ex,
                    part = current.ref.part,
                    sets = sets,
                )
                if (ok) route = StrengthRoute.Calendar
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (ok) "保存しました"
                        else "重量・回数は正の数で入力してください"
                    )
                }
            },
        )
    }
}
