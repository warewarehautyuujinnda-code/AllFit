package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 筋トレの「種目」そのものの定義。version 8 で追加。
 *
 * 記録（[StrengthEntity]）が「いつ・どれだけやったか」という出来事なのに対し、こちらは
 * 「その種目がどういうものか」を持つ。同じベンチプレスでもナロー・ワイドで別物なので、
 * どういうパターンか・どういう意識やフォームで行うかを [description] に書き残せるようにしてある。
 *
 * 種目名（[name]）が記録（[StrengthEntity.ex]）や週次計画（[WeeklyStrengthTargetEntity.exerciseName]）と
 * 種目を結ぶキー。種目の性質（プロパティ）を増やすときはこのテーブルに列を足す。書き出しJSONにも
 * 含めるので、AI に渡したときに記録と説明を突き合わせて読める。
 *
 * 一覧からの削除は [hiddenAt] を入れる論理削除だけで行い、行も記録も消さない（CLAUDE.md の絶対ルール）。
 */
@Serializable
@Entity(tableName = "exercise")
data class ExerciseEntity(
    /** 種目名。記録と紐づくキーなので、別の種目と同じ名前は持てない */
    @PrimaryKey val name: String,
    /** 部位（BodyPart の id）。任意。決まらない場合は種目名からプリセットを引く */
    val part: String? = null,
    /**
     * 種目の説明。どういうパターン（ナロー／ワイドなど）か、どういう意識・フォームで行う種目か。
     * 未記入は null
     */
    val description: String? = null,
    /** 一覧から削除（非表示）にした日時（ISO-8601）。null なら一覧に出る */
    val hiddenAt: String? = null,
    /** この定義を作った日時（ISO-8601）。記録から取り込んだ場合はその時刻 */
    val createdAt: String? = null,
    /** 説明などを最後に更新した日時（ISO-8601） */
    val updatedAt: String? = null,
)
