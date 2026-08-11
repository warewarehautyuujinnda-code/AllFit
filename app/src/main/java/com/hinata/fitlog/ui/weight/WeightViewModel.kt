package com.hinata.fitlog.ui.weight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.data.entity.WeightEntity
import com.hinata.fitlog.domain.parseOptionalDouble
import com.hinata.fitlog.domain.parseRequiredDouble
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 体重・体組成の記録（FR-01）。保存と一覧の取得を担う。
 */
class WeightViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FitLogApp).database.weightDao()

    /** 保存済みの記録（日付降順）。DBの変更に追従する */
    val items: StateFlow<List<WeightEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 保存する。体重は必須、体脂肪率は任意。
     * @return 入力が正しく保存できたら true
     */
    fun save(date: String, weightText: String, fatText: String): Boolean {
        if (date.isBlank()) return false
        val weight = parseRequiredDouble(weightText) ?: return false
        // 体脂肪率は任意。入力があるのに数値として読めない場合は黙って捨てずに保存を失敗させる
        val fat = parseOptionalDouble(fatText) ?: return false

        viewModelScope.launch {
            dao.upsert(WeightEntity(date = date, weight = weight, fat = fat.value))
        }
        return true
    }

    /** 記録を1件削除する（FR-05） */
    fun delete(item: WeightEntity) {
        viewModelScope.launch { dao.deleteById(item.id) }
    }
}
