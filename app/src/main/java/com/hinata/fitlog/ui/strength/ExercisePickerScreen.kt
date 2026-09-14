package com.hinata.fitlog.ui.strength

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.data.entity.ExerciseEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import com.hinata.fitlog.domain.BodyPart
import com.hinata.fitlog.domain.ExerciseRef
import com.hinata.fitlog.domain.bodyPartLabel
import com.hinata.fitlog.domain.elapsedLabelOf
import com.hinata.fitlog.domain.exerciseNameError
import com.hinata.fitlog.domain.exercisePickerRows
import com.hinata.fitlog.domain.hiddenExerciseNames
import com.hinata.fitlog.domain.lastPerformedByExercise
import java.time.LocalDate
import kotlinx.coroutines.delay

/** 種目の行を押し続けて、編集・削除のメニューが出るまでの時間 */
private const val LONG_PRESS_MILLIS = 1_500L

/**
 * 押し始めてから進み具合を出し始めるまでの待ち。
 * ふつうのタップでゲージが一瞬光らないよう、少しだけ待ってから出す。
 */
private const val HOLD_FEEDBACK_DELAY_MILLIS = 400L

/**
 * 長押しの判定時間だけ差し替えた設定。Compose の長押し判定はこの値（longPressTimeoutMillis）を見るため、
 * これを配下に流すことで「1.5秒押し続けたらメニュー」を素の combinedClickable のまま実現できる。
 */
private class LongPressViewConfiguration(
    base: ViewConfiguration,
    override val longPressTimeoutMillis: Long,
) : ViewConfiguration by base

/**
 * 部位タブから種目を選ぶ画面。
 * プリセットに無い種目も右上の鉛筆から自由入力で追加できる。
 * 行を約1.5秒長押しすると、その種目の編集（種目名・説明）と一覧からの削除ができる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    records: List<StrengthRecordWithSets>,
    exercises: List<ExerciseEntity>,
    today: LocalDate,
    plannedExercises: Set<String>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPick: (ExerciseRef) -> Unit,
    onAddExercise: (name: String, part: BodyPart?) -> Unit,
    onEditExercise: (currentName: String, newName: String, description: String, part: BodyPart?) -> Unit,
    onHideExercise: (name: String, part: BodyPart?) -> Unit,
) {
    // タブ0が「よくやる種目」。以降は BodyPart の並び順
    var tabIndex by remember { mutableIntStateOf(0) }
    var showCustom by remember { mutableStateOf(false) }
    // 長押しで開くメニューと、そこから開く編集・削除のダイアログの対象
    var menuTarget by remember { mutableStateOf<ExerciseRef?>(null) }
    var editTarget by remember { mutableStateOf<ExerciseRef?>(null) }
    var deleteTarget by remember { mutableStateOf<ExerciseRef?>(null) }

    val selectedPart = BodyPart.entries.getOrNull(tabIndex - 1)
    val lastPerformed = remember(records) { lastPerformedByExercise(records) }
    val rows = remember(records, exercises, selectedPart, plannedExercises) {
        exercisePickerRows(records, selectedPart, plannedExercises, exercises)
    }
    val descriptions = remember(exercises) {
        exercises.associate { it.name.trim() to it.description }
    }
    val hiddenNames = remember(exercises) { hiddenExerciseNames(exercises) }
    val visibleNames = remember(records, exercises, hiddenNames) {
        (records.map { it.record.ex.trim() } + exercises.map { it.name.trim() }).toSet() - hiddenNames
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                Text(
                    "種目を長押し（約1.5秒）すると、編集・削除ができます",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                val baseViewConfiguration = LocalViewConfiguration.current
                val longPressViewConfiguration = remember(baseViewConfiguration) {
                    LongPressViewConfiguration(baseViewConfiguration, LONG_PRESS_MILLIS)
                }
                CompositionLocalProvider(LocalViewConfiguration provides longPressViewConfiguration) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(rows, key = { it.ex }) { ref ->
                            ExerciseRow(
                                ref = ref,
                                description = descriptions[ref.ex],
                                elapsed = elapsedLabelOf(lastPerformed[ref.ex], today),
                                planned = ref.ex in plannedExercises,
                                menuExpanded = menuTarget?.ex == ref.ex,
                                onClick = { onPick(ref) },
                                onLongClick = { menuTarget = ref },
                                onDismissMenu = { menuTarget = null },
                                onEdit = {
                                    menuTarget = null
                                    editTarget = ref
                                },
                                onDelete = {
                                    menuTarget = null
                                    deleteTarget = ref
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showCustom) {
        CustomExerciseDialog(
            part = selectedPart,
            hiddenNames = hiddenNames,
            onDismiss = { showCustom = false },
            onConfirm = { name ->
                showCustom = false
                onAddExercise(name, selectedPart)
                onPick(ExerciseRef(name, selectedPart))
            },
        )
    }

    editTarget?.let { ref ->
        ExerciseEditDialog(
            ref = ref,
            description = descriptions[ref.ex].orEmpty(),
            visibleNames = visibleNames,
            hiddenNames = hiddenNames,
            onDismiss = { editTarget = null },
            onConfirm = { newName, description ->
                editTarget = null
                onEditExercise(ref.ex, newName, description, ref.part)
            },
        )
    }

    deleteTarget?.let { ref ->
        HideExerciseDialog(
            ref = ref,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onHideExercise(ref.ex, ref.part)
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

/**
 * 種目1件分の行。タップでその種目の記録へ進み、約1.5秒の長押しで編集・削除のメニューを出す。
 * ふつうのタップより長く押し続ける必要があるので、押している間は行の下に進み具合のゲージを描く。
 * 長押し中かどうかを自分で持つために、interactionSource を渡せる combinedClickable を使っている。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseRow(
    ref: ExerciseRef,
    description: String?,
    elapsed: String?,
    planned: Boolean,
    menuExpanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val holdProgress = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val holdColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(HOLD_FEEDBACK_DELAY_MILLIS)
            holdProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (LONG_PRESS_MILLIS - HOLD_FEEDBACK_DELAY_MILLIS).toInt(),
                    easing = LinearEasing,
                ),
            )
        } else {
            holdProgress.snapTo(0f)
        }
    }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onLongClickLabel = "種目を編集・削除",
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                    onClick = onClick,
                )
                .drawWithContent {
                    drawContent()
                    val progress = holdProgress.value
                    if (progress > 0f) {
                        val height = 3.dp.toPx()
                        drawRect(
                            color = holdColor,
                            topLeft = Offset(0f, size.height - height),
                            size = Size(size.width * progress, height),
                        )
                    }
                }
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(bodyPartColor(ref.part))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(ref.ex, style = MaterialTheme.typography.bodyLarge)
                if (!description.isNullOrBlank()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (planned) {
                Icon(
                    Icons.Filled.Flag,
                    contentDescription = "今週の計画に含まれる種目",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 8.dp),
                )
            }
            if (elapsed != null) {
                Text(
                    elapsed,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
            DropdownMenuItem(
                text = { Text("編集") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = onEdit,
            )
            DropdownMenuItem(
                text = { Text("削除") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun CustomExerciseDialog(
    part: BodyPart?,
    hiddenNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val restoring = name.trim() in hiddenNames

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
                if (restoring) {
                    Text(
                        "一覧から削除した種目と同じ名前です。追加すると、これまでの記録と説明はそのままに一覧へ戻ります",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
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

/**
 * 種目の編集。種目名と説明を変える。
 * 説明は「どういうパターンか・どういう意識やフォームで行うか」を書き残す欄で、
 * 書き出したJSONにも入るため、あとからAIに渡したときに記録と突き合わせて読める。
 */
@Composable
private fun ExerciseEditDialog(
    ref: ExerciseRef,
    description: String,
    visibleNames: Set<String>,
    hiddenNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf(ref.ex) }
    var text by remember { mutableStateOf(description) }
    val error = exerciseNameError(name, ref.ex, visibleNames, hiddenNames)
    val renaming = error == null && name.trim() != ref.ex

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("種目を編集") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("種目名") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("説明（任意）") },
                    placeholder = {
                        Text("例: ナローグリップ（肩幅より拳ひとつ内側）。肘を体に沿わせて下ろし、上腕三頭筋で押す")
                    },
                    supportingText = { Text("パターン・意識すること・フォームなど") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                if (renaming) {
                    Text(
                        "これまでの記録もすべて新しい種目名で表示されます（記録は消えません）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = error == null,
                onClick = { onConfirm(name.trim(), text) },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

/**
 * 種目の削除の確認。消すのは一覧の表示だけで、記録も説明もDBに残ることをここで伝える
 * （利用者のデータは消さない、というのがこのアプリの決まりのため）。
 */
@Composable
private fun HideExerciseDialog(
    ref: ExerciseRef,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("一覧から削除しますか？") },
        text = {
            Text(
                "「${ref.ex}」を種目の一覧に表示しないようにします。\n\n" +
                    "これまでの記録・説明・メモは削除されず、データとして残ります。" +
                    "右上の鉛筆から同じ名前で追加すると、また一覧に表示されます。"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("削除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}
