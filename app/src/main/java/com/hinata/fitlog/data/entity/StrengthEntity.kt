package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * 筋トレの記録（FR-02 / データ要件 8.2）
 */
@Serializable
@Entity(tableName = "strength")
data class StrengthEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** 記録日（ISO-8601: yyyy-MM-dd） */
    val date: String,
    /** 種目名 */
    val ex: String,
    /** 部位（BodyPart の id）。任意。version 2 で追加したため既存記録は null */
    val part: String? = null,
    /** 重量(kg)。任意 */
    val weight: Double? = null,
    /** 回数。任意 */
    val reps: Int? = null,
    /** セット数。任意 */
    val sets: Int? = null,
)
