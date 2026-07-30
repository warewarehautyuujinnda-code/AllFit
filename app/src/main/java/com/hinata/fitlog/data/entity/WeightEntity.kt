package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 体重・体組成の記録（FR-01 / データ要件 8.1）
 */
@Entity(tableName = "weight")
data class WeightEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** 記録日（ISO-8601: yyyy-MM-dd） */
    val date: String,
    /** 体重(kg) */
    val weight: Double,
    /** 体脂肪率(%)。任意 */
    val fat: Double? = null,
)
