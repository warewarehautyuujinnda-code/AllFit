package com.hinata.fitlog.data

import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.RunningPointEntity
import com.hinata.fitlog.data.entity.RunningSplitEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.StrengthSetEntity
import com.hinata.fitlog.data.entity.WeightEntity
import kotlinx.serialization.Serializable

/**
 * 書き出し／読み込みで使う JSON の中身（FR-11 / FR-12）。
 *
 * 4種別をそのまま並べた素直な構造にしてある。AI に渡したときに構造の説明が要らないことと、
 * 要件定義書 §8 のデータ要件（項目名・型）と1対1で対応することを優先した。
 *
 * 4つの配列はすべて既定値を持つ。書き出し済みファイルに新しい種別が増えても、
 * 古いファイルの読み込みが「その種別が空」として通るようにするため。
 */
@Serializable
data class FitLogBackup(
    /** 書き出し形式の版。将来構造を変えたときに読み込み側で分岐できるようにしておく */
    val version: Int = FORMAT_VERSION,
    /** 書き出した日時（ISO-8601）。どの時点のバックアップかを人が判別するためだけに使う */
    val exportedAt: String = "",
    val weight: List<WeightEntity> = emptyList(),
    val strength: List<StrengthEntity> = emptyList(),
    val running: List<RunningEntity> = emptyList(),
    val meal: List<MealEntity> = emptyList(),
    /** GPS計測したランの1分ごとの内訳。手入力の記録では対応する要素はない */
    val runningSplit: List<RunningSplitEntity> = emptyList(),
    /** 筋トレ記録のセットごとの内訳（重量・回数） */
    val strengthSet: List<StrengthSetEntity> = emptyList(),
    /** GPS計測したランの経路（緯度経度の並び）。手入力の記録では対応する要素はない */
    val runningPoint: List<RunningPointEntity> = emptyList(),
) {
    /** 収録されている記録の総件数（内訳は記録そのものではないため件数には含めない） */
    val totalCount: Int
        get() = weight.size + strength.size + running.size + meal.size

    companion object {
        const val FORMAT_VERSION = 1
    }
}
