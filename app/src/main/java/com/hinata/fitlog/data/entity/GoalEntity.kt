package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 長期目標。createdAt（作成日）をそのまま主キーにする。
 * UUIDは使わず、アプリ再起動時のシード再投入でも同じレコードとして扱えるようにする。
 */
@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey val createdAt: String,
    val title: String,
    val targetDate: String? = null,
)
