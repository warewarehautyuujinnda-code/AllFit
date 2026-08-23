package com.hinata.fitlog.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 目標体重(kg)の保存先。記録ではなく設定値が1つあるだけなので、
 * Room には載せず SharedPreferences に置く。
 *
 * 値は文字列で持つ。Float だと 65.3 のような入力が保存と読み出しで一致しなくなるため。
 */
class WeightGoalStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _goal = MutableStateFlow(read())

    /** 設定中の目標体重(kg)。未設定なら null */
    val goal: StateFlow<Double?> = _goal.asStateFlow()

    fun set(value: Double) {
        prefs.edit { putString(KEY, value.toString()) }
        _goal.value = value
    }

    /** 目標体重を未設定に戻す */
    fun clear() {
        prefs.edit { remove(KEY) }
        _goal.value = null
    }

    private fun read(): Double? {
        val value = prefs.getString(KEY, null)?.toDoubleOrNull() ?: return null
        return if (value.isFinite() && value > 0.0) value else null
    }

    private companion object {
        const val PREFS = "fitlog_settings"
        const val KEY = "weight_goal_kg"
    }
}
