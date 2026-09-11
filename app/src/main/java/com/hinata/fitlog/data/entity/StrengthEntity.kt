package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * 筋トレの記録（FR-02 / データ要件 8.2）。
 * 1種目・1日分の記録の単位で、重量・回数はセットごとに異なりうるため
 * StrengthSetEntity（子テーブル）に持たせている。version 5 で分離した。
 */
@Serializable
@Entity(tableName = "strength")
data class StrengthEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** 記録日（ISO-8601: yyyy-MM-dd） */
    val date: String,
    /** 種目名。種目の説明（[ExerciseEntity]）とはこの名前で紐づく */
    val ex: String,
    /** 部位（BodyPart の id）。任意。version 2 で追加したため既存記録は null */
    val part: String? = null,
    /**
     * その回のメモ（感覚・感想・フォームで意識したことなど）。任意。
     * version 8 で追加したため、それより前の記録は null。
     *
     * 種目の説明（[ExerciseEntity.description]）が「その種目とは何か」なのに対し、
     * こちらは「その回どうだったか」を積み上げるもの。どちらも書き出しJSONに入るので、
     * AI に渡したときに種目ごとの解像度が上がる。
     */
    val memo: String? = null,
)
