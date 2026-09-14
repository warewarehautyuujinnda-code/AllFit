package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * 食事の記録（FR-04 / データ要件 8.4）
 */
@Serializable
@Entity(tableName = "meal")
data class MealEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** 記録日（ISO-8601: yyyy-MM-dd） */
    val date: String,
    /** 内容 */
    val name: String,
    /** カロリー(kcal)。任意 */
    val kcal: Int? = null,
    /** たんぱく質(g)。任意 */
    val p: Double? = null,
    /** 脂質(g)。任意 */
    val f: Double? = null,
    /** 炭水化物(g)。任意 */
    val c: Double? = null,
    /**
     * この記録を登録した日時（ISO-8601・秒まで・UTC）。記録日（[date]）が「いつの記録か」なのに対し、
     * こちらは「いつ入力したか」。同じ日に入れた記録の前後や、後からまとめて入力したのかが分かる。
     * version 9 で追加したため、それより前に登録した記録は null。
     */
    val createdAt: String? = null,
)
