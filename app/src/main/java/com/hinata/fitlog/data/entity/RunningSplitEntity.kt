package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * GPS計測したランの1分ごとの内訳。
 * ランの記録([RunningEntity])に対して、経過何分の時点で累積何kmだったかを持つ。
 * 記録の詳細画面で「毎分どのくらいのペースだったか」を出すために使う。
 * 手入力で保存した記録には内訳は存在しない。
 */
@Serializable
@Entity(tableName = "running_split", indices = [Index("runId")])
data class RunningSplitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** 対応する記録のID（[RunningEntity.id]） */
    val runId: String,
    /** 経過何分の時点かの内訳（1分刻み） */
    val minuteIndex: Int,
    /** その時点までの累積距離(km) */
    val distanceKm: Double,
)
