package com.hinata.fitlog.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 筋トレの記録（[StrengthEntity]）とそのセットごとの内訳（[StrengthSetEntity]）をまとめたもの。
 * 重量・回数はセットごとに持つため、集計・表示はこの単位で扱う。
 */
data class StrengthRecordWithSets(
    @Embedded val record: StrengthEntity,
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val sets: List<StrengthSetEntity>,
)
