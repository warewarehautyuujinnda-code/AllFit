package com.hinata.fitlog.ui.strength

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.domain.BodyPart
import com.hinata.fitlog.domain.parseOptionalDouble
import com.hinata.fitlog.domain.parseOptionalInt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 筋トレの記録（FR-02）。保存と一覧の取得を担う。
 */
class StrengthViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FitLogApp).database.strengthDao()

    /** 保存済みの記録（日付降順）。DBの変更に追従する */
    val items: StateFlow<List<StrengthEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 保存する。種目名は必須、重量・回数・セット数は任意（自重トレ等）。
     * 任意項目は未入力なら null として保存し、入力があるのに数値として読めない場合は
     * 黙って捨てずに保存を失敗させる。
     * @return 入力が正しく保存できたら true
     */
    fun save(
        date: String,
        exText: String,
        part: BodyPart?,
        weightText: String,
        repsText: String,
        setsText: String,
    ): Boolean {
        val ex = exText.trim()
        if (date.isBlank() || ex.isEmpty()) return false

        val weight = parseOptionalDouble(weightText) ?: return false
        val reps = parseOptionalInt(repsText) ?: return false
        val sets = parseOptionalInt(setsText) ?: return false

        viewModelScope.launch {
            dao.upsert(
                StrengthEntity(
                    date = date,
                    ex = ex,
                    part = part?.id,
                    weight = weight.value,
                    reps = reps.value,
                    sets = sets.value,
                )
            )
        }
        return true
    }

    /**
     * 記録を削除する（FR-05）。
     * 画面では1種目1日分のカードにまとめて出しているため、その元になった複数件を一度に消す。
     */
    fun deleteRecords(ids: List<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch { dao.deleteByIds(ids) }
    }
}
