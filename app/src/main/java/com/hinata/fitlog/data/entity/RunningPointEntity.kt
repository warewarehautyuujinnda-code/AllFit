package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * GPS計測したランの経路。1分ごとの内訳([RunningSplitEntity])とは別に、実際に計測できた
 * 緯度経度の並びを持つ。記録の詳細画面で「どの経路を走ったか」を描くために使う。
 * 手入力で保存した記録には経路は存在しない。
 */
@Serializable
@Entity(tableName = "running_point", indices = [Index("runId")])
data class RunningPointEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** 対応する記録のID（[RunningEntity.id]） */
    val runId: String,
    /** 計測順（0始まり）。経路を描く際の並び順に使う */
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
)
