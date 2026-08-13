package com.hinata.fitlog.ui.meal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.domain.MealTotals
import com.hinata.fitlog.domain.mealTotalsOf
import com.hinata.fitlog.domain.parseOptionalDouble
import com.hinata.fitlog.domain.parseOptionalInt
import com.hinata.fitlog.ui.common.DateUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 食事の記録（FR-04）。保存・一覧の取得・削除（FR-05）・当日合計（FR-10）を担う。
 */
class MealViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FitLogApp).database.mealDao()

    /** 保存済みの記録（日付降順）。DBの変更に追従する（FR-09） */
    val items: StateFlow<List<MealEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 当日の合計（FR-10）。
     * 「今日」は購読を開始した時点ではなく集計するたびに評価するため、
     * 日付をまたいでも記録を追加した時点で正しい日の合計になる。
     */
    val todayTotals: StateFlow<MealTotals> = items
        .map { mealTotalsOf(it, DateUtil.today()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MealTotals())

    /**
     * 保存する。日付と内容は必須、カロリー・PFC は任意。
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

    /** 記録を1件削除する（FR-05） */
    fun delete(item: MealEntity) {
        viewModelScope.launch { dao.deleteById(item.id) }
    }
}
