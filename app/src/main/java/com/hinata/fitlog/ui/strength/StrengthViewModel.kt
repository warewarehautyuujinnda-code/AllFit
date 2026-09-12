package com.hinata.fitlog.ui.strength

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.data.entity.ExerciseEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import com.hinata.fitlog.data.entity.StrengthSetEntity
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
import com.hinata.fitlog.domain.BodyPart
import com.hinata.fitlog.domain.StrengthPlanProgress
import com.hinata.fitlog.domain.parseOptionalDouble
import com.hinata.fitlog.domain.parseOptionalInt
import com.hinata.fitlog.domain.strengthProgressOf
import com.hinata.fitlog.domain.weekStartOf
import com.hinata.fitlog.domain.weeklyPlanFor
import com.hinata.fitlog.ui.common.currentDateFlow
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 筋トレの記録（FR-02）。保存と一覧の取得、種目の定義（説明・一覧からの削除）を担う。
 */
class StrengthViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as FitLogApp).strengthRepository
    private val planRepository = (app as FitLogApp).planRepository

    /** 保存済みの記録とセットごとの内訳（日付降順）。DBの変更に追従する */
    val items: StateFlow<List<StrengthRecordWithSets>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 種目の定義（説明・一覧から削除したかどうか）。DBの変更に追従する */
    val exercises: StateFlow<List<ExerciseEntity>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 種目の追加・編集・一覧からの削除の結果。画面側でスナックバーに出す */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val currentDate = currentDateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now())

    /** 今週の計画。無い週は null */
    private val currentPlan: StateFlow<WeeklyPlanEntity?> = combine(
        planRepository.observeWeeklyPlans(), currentDate,
    ) { plans, date -> weeklyPlanFor(plans, weekStartOf(date)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val strengthProgress: StateFlow<StrengthPlanProgress?> = combine(
        currentPlan, planRepository.observeStrengthTargets(), items,
    ) { plan, targets, records ->
        plan?.let { strengthProgressOf(targets, records, it.weekStart) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 今週の計画のうち、まだ実施していない種目名（種目選択画面のバッジ表示に使う） */
    val pendingExercises: StateFlow<Set<String>> = strengthProgress
        .map { it?.pendingExercises?.toSet() ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** 今週の計画にある種目ごとの目標(reps/sets)。セット入力画面の目標表示に使う */
    val weekTargets: StateFlow<List<WeeklyStrengthTargetEntity>> = combine(
        currentPlan, planRepository.observeStrengthTargets(),
    ) { plan, targets ->
        plan?.let { p -> targets.filter { it.weekPlanId == p.weekStart } } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 保存する。種目名とセット1件以上は必須。各セットの重量・回数は任意（自重トレ等）。
     * 任意項目は未入力なら null として保存し、入力があるのに数値として読めない場合は
     * 黙って捨てずに保存を失敗させる。
     * @param sets 各セットの（重量入力, 回数入力）のテキスト。順番がそのままセットの順になる
     * @param memo その回のメモ（感覚・感想など）。未入力なら null として保存する
     * @return 入力が正しく保存できたら true
     */
    fun save(
        date: String,
        exText: String,
        part: BodyPart?,
        sets: List<Pair<String, String>>,
        memo: String = "",
    ): Boolean {
        val ex = exText.trim()
        if (date.isBlank() || ex.isEmpty() || sets.isEmpty()) return false

        val parsedSets = sets.map { (weightText, repsText) ->
            val weight = parseOptionalDouble(weightText) ?: return false
            val reps = parseOptionalInt(repsText) ?: return false
            weight.value to reps.value
        }

        val record = StrengthEntity(
            date = date,
            ex = ex,
            part = part?.id,
            memo = memo.trim().ifEmpty { null },
        )
        val setEntities = parsedSets.mapIndexed { index, (weight, reps) ->
            StrengthSetEntity(recordId = record.id, setIndex = index, weight = weight, reps = reps)
        }

        viewModelScope.launch { repository.save(record, setEntities) }
        return true
    }

    /**
     * 記録を削除する（FR-05）。
     * 画面では1種目1日分のカードにまとめて出しているため、その元になった複数件を一度に消す。
     */
    fun deleteRecords(ids: List<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch { repository.deleteRecords(ids) }
    }

    /** 種目を追加する（鉛筆ボタン）。一覧から削除していた種目なら、説明も記録もそのままに再表示する */
    fun addExercise(name: String, part: BodyPart?) {
        viewModelScope.launch { repository.addExercise(name, part?.id) }
    }

    /** 種目名・説明を変更する。名前を変えた場合はこれまでの記録も新しい名前で表示されるようになる */
    fun updateExercise(currentName: String, newName: String, description: String, part: BodyPart?) {
        viewModelScope.launch {
            val saved = repository.updateExercise(
                currentName = currentName,
                newName = newName,
                description = description.trim().ifEmpty { null },
                part = part?.id,
            )
            _messages.emit(
                if (saved) "種目を保存しました"
                else "同じ名前の種目があるため保存できませんでした"
            )
        }
    }

    /**
     * 種目を一覧から削除する。表示から外すだけで、記録・説明・メモはDBにそのまま残る
     * （CLAUDE.md の絶対ルール）。
     */
    fun hideExercise(name: String, part: BodyPart?) {
        viewModelScope.launch {
            repository.hideExercise(name, part?.id)
            _messages.emit("「$name」を一覧から削除しました（記録は残っています）")
        }
    }
}
