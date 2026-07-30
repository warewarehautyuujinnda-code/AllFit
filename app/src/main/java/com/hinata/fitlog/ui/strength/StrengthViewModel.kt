package com.hinata.fitlog.ui.strength

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.data.entity.StrengthEntity
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
                    weight = weight.value,
                    reps = reps.value,
                    sets = sets.value,
                )
            )
        }
        return true
    }

    /** 任意の数値項目の解析結果。解析できなかった場合と「未入力（null）」を区別するための入れ物 */
    private class Parsed<T>(val value: T?)

    /** 未入力なら null 値、正の数ならその値、それ以外（数値でない・0以下）は解析失敗として null を返す */
    private fun parseOptionalDouble(text: String): Parsed<Double>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Parsed(null)
        val value = trimmed.toDoubleOrNull() ?: return null
        return if (value > 0.0) Parsed(value) else null
    }

    /** 未入力なら null 値、正の整数ならその値、それ以外（数値でない・0以下）は解析失敗として null を返す */
    private fun parseOptionalInt(text: String): Parsed<Int>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Parsed(null)
        val value = trimmed.toIntOrNull() ?: return null
        return if (value > 0) Parsed(value) else null
    }
}
