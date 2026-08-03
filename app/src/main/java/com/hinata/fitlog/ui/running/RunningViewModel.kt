package com.hinata.fitlog.ui.running

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.data.entity.RunningEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * ランニングの記録（FR-03）。保存と一覧の取得、ペースの計算を担う。
 */
class RunningViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FitLogApp).database.runningDao()

    /** 保存済みの記録（日付降順）。DBの変更に追従する */
    val items: StateFlow<List<RunningEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 保存する。距離は必須、時間・消費カロリーは任意。
     * 任意項目は未入力なら null として保存し、入力があるのに数値として読めない場合は
     * 黙って捨てずに保存を失敗させる。
     * ペースは距離と時間から都度計算できるため保存しない（データ要件 8.3）。
     * @return 入力が正しく保存できたら true
     */
    fun save(
        date: String,
        distText: String,
        minText: String,
        kcalText: String,
    ): Boolean {
        if (date.isBlank()) return false

        val dist = distText.trim().toDoubleOrNull() ?: return false
        // toDoubleOrNull() は "NaN" / "Infinity" も解析するため、比較だけでは弾けない。
        // 不正な値をDBに残すと履歴の表示側で毎回問題になるので、保存時点で拒否する
        if (!dist.isFinite() || dist <= 0.0) return false

        val min = parseOptionalDouble(minText) ?: return false
        val kcal = parseOptionalInt(kcalText) ?: return false

        viewModelScope.launch {
            dao.upsert(
                RunningEntity(
                    date = date,
                    dist = dist,
                    min = min.value,
                    kcal = kcal.value,
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
        return if (value.isFinite() && value > 0.0) Parsed(value) else null
    }

    /** 未入力なら null 値、正の整数ならその値、それ以外（数値でない・0以下）は解析失敗として null を返す */
    private fun parseOptionalInt(text: String): Parsed<Int>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Parsed(null)
        val value = trimmed.toIntOrNull() ?: return null
        return if (value > 0) Parsed(value) else null
    }

    companion object {
        /**
         * ペース（分/km）を 5'30" 形式で返す。
         * 距離・時間のどちらかが未入力・0以下・非有限値の場合は計算できないため null を返す。
         */
        fun formatPace(dist: Double?, min: Double?): String? {
            if (dist == null || min == null) return null
            // 入力中の文字列から直接呼ばれるため保存時の検証を通っていない。
            // NaN は大小比較がすべて false になり 0以下チェックをすり抜けるうえ、
            // roundToInt() に渡すと例外になるので isFinite() で先に弾く
            if (!dist.isFinite() || !min.isFinite()) return null
            if (dist <= 0.0 || min <= 0.0) return null
            // 秒に丸めてから分と秒に分けることで、59.6秒が「0'60"」になるのを防ぐ
            val totalSeconds = (min * 60.0 / dist).roundToInt()
            return String.format(Locale.US, "%d'%02d\"", totalSeconds / 60, totalSeconds % 60)
        }
    }
}
