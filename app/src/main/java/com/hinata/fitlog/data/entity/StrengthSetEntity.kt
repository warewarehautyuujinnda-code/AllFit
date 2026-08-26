package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * 筋トレの記録（[StrengthEntity]）の1セット分。
 * 重量・回数はセットごとに変わりうるため、記録1件に対して複数持つ。version 5 で追加。
 */
@Serializable
@Entity(tableName = "strength_set", indices = [Index("recordId")])
data class StrengthSetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** 対応する記録のID（[StrengthEntity.id]） */
    val recordId: String,
    /** セットの順番（0始まり） */
    val setIndex: Int,
    /** 重量(kg)。任意 */
    val weight: Double? = null,
    /** 回数。任意 */
    val reps: Int? = null,
)
