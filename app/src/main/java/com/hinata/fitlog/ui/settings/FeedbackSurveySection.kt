package com.hinata.fitlog.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.BuildConfig
import com.hinata.fitlog.domain.FeedbackSurveyAnswers
import com.hinata.fitlog.domain.FeedbackTarget
import com.hinata.fitlog.domain.FeedbackWant
import com.hinata.fitlog.domain.buildFeedbackSurveyText
import com.hinata.fitlog.domain.canSubmit
import com.hinata.fitlog.ui.feedback.shareFeedbackSurvey

/** 長い要望を書いても入力欄が画面いっぱいに伸びないよう、高さの上限を決める（RunningDetailScreen と同じ考え方）。 */
private const val INPUT_MAX_LINES = 8

// 画面回転やタブの行き来で書きかけが消えないよう、回答をそのまま保存する。
// 選択肢は enum の名前で持ち、未選択は空文字で表す。
private val AnswersSaver = listSaver<FeedbackSurveyAnswers, String>(
    save = { answers ->
        listOf(
            answers.target?.name.orEmpty(),
            answers.request,
            answers.problem,
            answers.want?.name.orEmpty(),
            answers.other,
        )
    },
    restore = { saved ->
        FeedbackSurveyAnswers(
            target = saved[0].takeIf { it.isNotEmpty() }?.let { FeedbackTarget.valueOf(it) },
            request = saved[1],
            problem = saved[2],
            want = saved[3].takeIf { it.isNotEmpty() }?.let { FeedbackWant.valueOf(it) },
            other = saved[4],
        )
    },
)

/**
 * 設定画面のフィードバック欄。
 * 「どのタブに、どんな機能を追加してほしいか」を答える形のアンケートで、
 * 画面キャプチャに赤線を書くフィードバックでは伝えられない要望の受け皿にする。
 * 書いた内容は共有シート経由で送る（アプリはサーバーを持たない）。
 */
@Composable
fun FeedbackSurveySection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var answers by rememberSaveable(stateSaver = AnswersSaver) {
        mutableStateOf(FeedbackSurveyAnswers())
    }
    var shared by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("フィードバック", style = MaterialTheme.typography.titleMedium)
        Text(
            "「このタブにこういう機能がほしい」といった要望を送れます。" +
                "画面の見た目そのものへの指摘は、どの画面にも出ているフィードバックボタンから" +
                "キャプチャに書き込んで送るほうが伝わります。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Question("1. どのタブについてのお話ですか？")
                ChoiceChips(
                    labels = FeedbackTarget.entries.map { it.label },
                    selectedIndex = answers.target?.ordinal,
                    onSelect = { index ->
                        answers = answers.copy(
                            target = index?.let { FeedbackTarget.entries[it] },
                        )
                    },
                )

                Question("2. そのタブにどんな機能を追加してほしいですか？")
                OutlinedTextField(
                    value = answers.request,
                    onValueChange = { answers = answers.copy(request = it) },
                    placeholder = { Text("例: 筋トレの種目ごとに、前回の重量をすぐ見られるようにしてほしい") },
                    minLines = 3,
                    maxLines = INPUT_MAX_LINES,
                    modifier = Modifier.fillMaxWidth(),
                )

                Question("3. 今はどうしていて、何に困っていますか？（任意）")
                OutlinedTextField(
                    value = answers.problem,
                    onValueChange = { answers = answers.copy(problem = it) },
                    placeholder = { Text("例: 毎回カレンダーから前回の日を探して開き直している") },
                    minLines = 2,
                    maxLines = INPUT_MAX_LINES,
                    modifier = Modifier.fillMaxWidth(),
                )

                Question("4. どれくらい欲しいですか？（任意）")
                ChoiceChips(
                    labels = FeedbackWant.entries.map { it.label },
                    selectedIndex = answers.want?.ordinal,
                    onSelect = { index ->
                        answers = answers.copy(want = index?.let { FeedbackWant.entries[it] })
                    },
                )

                Question("5. その他ひとこと（任意）")
                OutlinedTextField(
                    value = answers.other,
                    onValueChange = { answers = answers.copy(other = it) },
                    placeholder = { Text("例: 毎日ちゃんと続けられています。ありがとう") },
                    minLines = 2,
                    maxLines = INPUT_MAX_LINES,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    if (answers.canSubmit) {
                        "「送る」を押すと共有シートが開きます。送り終わったら「クリア」で消せます。"
                    } else {
                        "1のタブを選んで、2を書くと送れます。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            answers = FeedbackSurveyAnswers()
                            shared = false
                        },
                    ) {
                        Text("クリア")
                    }
                    Button(
                        onClick = {
                            shareFeedbackSurvey(
                                context,
                                buildFeedbackSurveyText(answers, BuildConfig.VERSION_NAME),
                            )
                            shared = true
                        },
                        enabled = answers.canSubmit,
                    ) {
                        Text("送る")
                    }
                }

                if (shared) {
                    Text(
                        "共有シートを開きました。ありがとうございます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Question(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

/**
 * ひとつだけ選べるチップの並び。選択中のものをもう一度押すと未選択に戻せる
 * （任意の設問で、一度押したら取り消せないのを避けるため）。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChoiceChips(
    labels: List<String>,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            FilterChip(
                selected = selected,
                onClick = { onSelect(if (selected) null else index) },
                label = { Text(label) },
            )
        }
    }
}
