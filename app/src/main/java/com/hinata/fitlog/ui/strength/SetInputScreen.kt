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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.domain.ExerciseRef
import com.hinata.fitlog.domain.bodyPartLabel
import com.hinata.fitlog.domain.formatAmount
import com.hinata.fitlog.ui.common.DatePickerField

/**
 * 選んだ種目の重量・回数・セット数を入れて保存する画面。
 * 重量・回数・セット数はいずれも任意（自重トレを記録できるようにするため）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetInputScreen(
    ref: ExerciseRef,
    date: String,
    lastRecord: StrengthEntity?,
    snackbarHostState: SnackbarHostState,
    onDateChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: (weight: String, reps: String, sets: String) -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }

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
                    "前回(${lastRecord.date}): ${describeRecord(lastRecord)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            DatePickerField(
                value = date,
                onValueChange = onDateChange,
                modifier = Modifier.padding(top = 16.dp),
            )

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("重量 (kg) ※任意") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text("回数 ※任意") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            OutlinedTextField(
                value = sets,
                onValueChange = { sets = it },
                label = { Text("セット数 ※任意") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            Button(
                onClick = { onSave(weight, reps, sets) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text("保存")
            }
        }
    }
}

private fun describeRecord(record: StrengthEntity): String {
    val detail = listOfNotNull(
        record.weight?.let { "${formatAmount(it)} kg" },
        record.reps?.let { "$it 回" },
        record.sets?.let { "$it セット" },
    ).joinToString(" × ")
    return detail.ifEmpty { "記録あり" }
}
