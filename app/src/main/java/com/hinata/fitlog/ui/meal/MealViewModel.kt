package com.hinata.fitlog.ui.meal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.data.entity.MealEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 食事の記録（FR-04）。保存と一覧の取得を担う。
 */
class MealViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FitLogApp).database.mealDao()

    /** 保存済みの記録（日付降順）。DBの変更に追従する */
    val items: StateFlow<List<MealEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 保存する。内容は必須、カロリー・PFC は任意。
     * 任意項目は未入力なら null として保存し、入力があるのに数値として読めない場合は
     * 黙って捨てずに保存を失敗させる。
     * @return 入力が正しく保存できたら true
     */
    fun save(
        date: String,
        nameText: String,
        kcalText: String,
        pText: String,
        fText: String,
        cText: String,
    ): Boolean {
        val name = nameText.trim()
        if (date.isBlank() || name.isEmpty()) return false

        val kcal = parseOptionalInt(kcalText) ?: return false
        val p = parseOptionalDouble(pText) ?: return false
        val f = parseOptionalDouble(fText) ?: return false
        val c = parseOptionalDouble(cText) ?: return false

        viewModelScope.launch {
            dao.upsert(
                MealEntity(
                    date = date,
                    name = name,
                    kcal = kcal.value,
                    p = p.value,
                    f = f.value,
                    c = c.value,
                )
            )
        }
        return true
    }

    /** 任意の数値項目の解析結果。解析できなかった場合と「未入力（null）」を区別するための入れ物 */
    private class Parsed<T>(val value: T?)

    /**
     * 未入力なら null 値、0以上の数ならその値、それ以外（数値でない・負の数）は解析失敗として null を返す。
     * カロリー・PFC は「0」も正しい入力（例: ブラックコーヒー 0kcal、脂質 0g）のため 0 を許容する。
     */
    private fun parseOptionalDouble(text: String): Parsed<Double>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Parsed(null)
        val value = trimmed.toDoubleOrNull() ?: return null
        return if (value >= 0.0) Parsed(value) else null
    }

    /** 未入力なら null 値、0以上の整数ならその値、それ以外（数値でない・負の数）は解析失敗として null を返す */
    private fun parseOptionalInt(text: String): Parsed<Int>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Parsed(null)
        val value = trimmed.toIntOrNull() ?: return null
        return if (value >= 0) Parsed(value) else null
    }
}
