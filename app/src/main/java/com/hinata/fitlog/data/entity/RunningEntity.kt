package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * ランニングの記録（FR-03 / データ要件 8.3）
 */
@Entity(tableName = "running")
data class RunningEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** 記録日（ISO-8601: yyyy-MM-dd） */
    val date: String,
    /** 距離(km) */
    val dist: Double,
    /** 時間(分)。任意 */
    val min: Double? = null,
    /** 消費カロリー(kcal)。任意 */
    val kcal: Int? = null,
)
