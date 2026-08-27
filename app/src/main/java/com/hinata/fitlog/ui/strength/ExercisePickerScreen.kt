package com.hinata.fitlog.ui.strength

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import com.hinata.fitlog.domain.BodyPart
import com.hinata.fitlog.domain.ExerciseRef
import com.hinata.fitlog.domain.bodyPartLabel
import com.hinata.fitlog.domain.elapsedLabelOf
import com.hinata.fitlog.domain.exercisesOf
import com.hinata.fitlog.domain.frequentExercises
import com.hinata.fitlog.domain.lastPerformedByExercise
import java.time.LocalDate

/**
 * 部位タブから種目を選ぶ画面。
 * プリセットに無い種目も右上の鉛筆から自由入力で記録できる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    records: List<StrengthRecordWithSets>,
    today: LocalDate,
    onBack: () -> Unit,
    onPick: (ExerciseRef) -> Unit,
) {
    // タブ0が「よくやる種目」。以降は BodyPart の並び順
    var tabIndex by remember { mutableIntStateOf(0) }
    var showCustom by remember { mutableStateOf(false) }

    val selectedPart = BodyPart.entries.getOrNull(tabIndex - 1)
    val lastPerformed = remember(records) { lastPerformedByExercise(records) }
    val rows = remember(records, selectedPart) {
        if (selectedPart == null) {
            frequentExercises(records)
        } else {
            exercisesOf(selectedPart, records).map { ExerciseRef(it, selectedPart) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("種目を選択") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { showCustom = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "種目を自由入力")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ScrollableTabRow(selectedTabIndex = tabIndex, edgePadding = 8.dp) {
                PartTab(
                    selected = tabIndex == 0,
                    label = "よくやる種目",
                    onClick = { tabIndex = 0 },
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = FavoriteColor,
                        modifier = Modifier.size(12.dp),
                    )
                }
                BodyPart.entries.forEachIndexed { index, part ->
                    PartTab(
                        selected = tabIndex == index + 1,
                        label = part.label,
                        onClick = { tabIndex = index + 1 },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(bodyPartColor(part))
                        )
                    }
                }
            }

            if (rows.isEmpty()) {
                Text(
                    "記録がまだありません。右上から種目を入力できます",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(rows, key = { it.ex }) { ref ->
                        ExerciseRow(
                            ref = ref,
                            elapsed = elapsedLabelOf(lastPerformed[ref.ex], today),
                            onClick = { onPick(ref) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showCustom) {
        CustomExerciseDialog(
            part = selectedPart,
            onDismiss = { showCustom = false },
            onConfirm = { name ->
                showCustom = false
                onPick(ExerciseRef(name, selectedPart))
            },
        )
    }
}

@Composable
private fun PartTab(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    marker: @Composable () -> Unit,
) {
    Tab(
        selected = selected,
        onClick = onClick,
        selectedContentColor = MaterialTheme.colorScheme.primary,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            marker()
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ExerciseRow(
    ref: ExerciseRef,
    elapsed: String?,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(bodyPartColor(ref.part))
        )
        Text(
            ref.ex,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        if (elapsed != null) {
            Text(
                elapsed,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CustomExerciseDialog(
    part: BodyPart?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("種目を自由入力") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("種目名") },
                    singleLine = true,
                )
                Text(
                    "部位は「${bodyPartLabel(part)}」として記録します",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) },
            ) { Text("次へ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}
