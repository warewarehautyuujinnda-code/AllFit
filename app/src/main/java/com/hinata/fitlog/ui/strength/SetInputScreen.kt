package com.hinata.fitlog.ui.strength

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
import com.hinata.fitlog.domain.ExerciseRef
import com.hinata.fitlog.domain.bodyPartLabel
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.ui.common.DatePickerField

/** 入力中の1セット分の状態。重量・回数はどちらも任意 */
private class SetInput(weight: String = "", reps: String = "") {
    var weight by mutableStateOf(weight)
    var reps by mutableStateOf(reps)
}

/**
 * 選んだ種目のセットを1行ずつ入れて保存する画面。
 * セットごとに重量・回数を変えられるよう、セットを行として増減できるようにしている。
 * 新しい行を追加すると直前の行の値をコピーするため、値が変わらないセットは
 * そのまま追加するだけで済み、変えたいセットだけをその行で編集すればよい。
 * 重量・回数はいずれも任意（自重トレを記録できるようにするため）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetInputScreen(
    ref: ExerciseRef,
    date: String,
    lastRecord: StrengthRecordWithSets?,
    weekTarget: WeeklyStrengthTargetEntity?,
    snackbarHostState: SnackbarHostState,
    onDateChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: (sets: List<Pair<String, String>>) -> Unit,
) {
    val sets = remember { mutableStateListOf(SetInput()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(bodyPartColor(ref.part))
                        )
                        Text(ref.ex, modifier = Modifier.padding(start = 8.dp))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                "部位: ${bodyPartLabel(ref.part)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (lastRecord != null) {
                Text(
                    "前回(${lastRecord.record.date}): ${describeRecord(lastRecord)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (weekTarget != null) {
                Text(
                    "今週の目標: ${describeTarget(weekTarget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Text(
                "重量・回数は空欄のままでも記録できます（自重トレなど）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            DatePickerField(
                value = date,
                onValueChange = onDateChange,
                modifier = Modifier.padding(top = 16.dp),
            )

            sets.forEachIndexed { index, set ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    OutlinedTextField(
                        value = set.weight,
                        onValueChange = { set.weight = it },
                        label = { Text("重量 (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = set.reps,
                        onValueChange = { set.reps = it },
                        label = { Text("回数") },
                        placeholder = {
                            val targetReps = weekTarget?.targetReps
                            if (index == 0 && targetReps != null) {
                                Text(targetReps.toString())
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                    )
                    IconButton(
                        onClick = { sets.removeAt(index) },
                        enabled = sets.size > 1,
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "${index + 1}セット目を削除",
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val last = sets.last()
                    sets.add(SetInput(weight = last.weight, reps = last.reps))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("セットを追加", modifier = Modifier.padding(start = 4.dp))
            }

            Button(
                onClick = { onSave(sets.map { it.weight to it.reps }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text("保存")
            }
        }
    }
}

/** セットごとの「重量kg×回数」を並べて表示する。両方未入力のセットは "-" になる */
private fun describeRecord(record: StrengthRecordWithSets): String {
    val detail = record.sets.sortedBy { it.setIndex }.joinToString(" / ") { set ->
        listOfNotNull(
            set.weight?.let { "${formatAmount(it)}kg" },
            set.reps?.let { "${it}回" },
        ).joinToString("×").ifEmpty { "-" }
    }
    return detail.ifEmpty { "記録あり" }
}

/** 目標の「回数×セット数」を表示用に整形する */
private fun describeTarget(target: WeeklyStrengthTargetEntity): String {
    val reps = target.targetReps?.let { "${it}回" }
    val sets = target.targetSets?.let { "${it}セット" }
    return listOfNotNull(reps, sets).joinToString("×").ifEmpty { "設定あり" }
}
