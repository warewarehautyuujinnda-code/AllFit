package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets

/**
 * 部位ごとの種目マスタ。コード内の定数として持ち、外部から取得しない。
 * ここに無い種目も自由入力で記録でき、記録済みの種目は一覧に差し込まれる。
 */
private val PRESETS: Map<BodyPart, List<String>> = mapOf(
    BodyPart.CHEST to listOf(
        "腕立て伏せ",
        "ディップス",
        "ベンチプレス",
        "インクラインベンチプレス",
        "インクラインダンベルプレス",
        "ダンベルプレス",
        "ダンベルフライ",
        "ペックフライ",
        "チェストプレス",
        "ケーブルフライ",
        "ケーブルディップス",
        "インクラインスミス",
    ),
    BodyPart.BACK to listOf(
        "懸垂",
        "ラットプルダウン",
        "シーテッドロー",
        "ベントオーバーロー",
        "ワンハンドダンベルロー",
        "デッドリフト",
        "Tバーロー",
        "ケーブルプルオーバー",
        "バックエクステンション",
        "インバーテッドロー",
        "ハイロー",
    ),
    BodyPart.SHOULDER to listOf(
        "ショルダープレス",
        "ダンベルショルダープレス",
        "サイドレイズ",
        "フロントレイズ",
        "リアレイズ",
        "アップライトロー",
        "アーノルドプレス",
        "シュラッグ",
        "ケーブルサイドレイズ",
        "フェイスプル",
    ),
    BodyPart.ARM to listOf(
        "バーベルカール",
        "ダンベルカール",
        "ハンマーカール",
        "プリーチャーカール",
        "インクラインダンベルカール",
        "ケーブルカール",
        "コンセントレーションカール",
        "トライセプスエクステンション",
        "ケーブルプレスダウン",
        "ナローベンチプレス",
        "キックバック",
        "リストカール",
    ),
    BodyPart.ABS to listOf(
        "クランチ",
        "シットアップ",
        "レッグレイズ",
        "プランク",
        "サイドプランク",
        "アブローラー",
        "ケーブルクランチ",
        "ロシアンツイスト",
        "ヒップレイズ",
        "ハンギングレッグレイズ",
    ),
    BodyPart.LEG to listOf(
        "スクワット",
        "バーベルスクワット",
        "レッグプレス",
        "レッグエクステンション",
        "レッグカール",
        "ブルガリアンスクワット",
        "ランジ",
        "ヒップスラスト",
        "カーフレイズ",
        "ルーマニアンデッドリフト",
        "ヒップアブダクション",
    ),
)

private val PART_BY_PRESET: Map<String, BodyPart> =
    PRESETS.entries.flatMap { (part, list) -> list.map { it to part } }.toMap()

/** その部位のプリセット種目 */
fun presetExercises(part: BodyPart): List<String> = PRESETS[part].orEmpty()

/** 種目名からプリセット上の部位を引く。マスタに無い名前は null */
fun presetPartOf(ex: String): BodyPart? = PART_BY_PRESET[ex.trim()]

/**
 * 記録の部位。部位カラムが空の既存記録でも、種目名がマスタにあればその部位として扱う。
 * どちらでも決まらなければ null（＝その他）。
 */
fun bodyPartOf(record: StrengthEntity): BodyPart? =
    BodyPart.fromId(record.part) ?: presetPartOf(record.ex)

/**
 * 部位タブに出す種目一覧。一度も記録していないプリセットは出さない。
 * プリセットの並びを保ったまま、記録済みのものだけを残し、
 * マスタに無い記録済みの種目（自由入力）を末尾に足す。
 */
fun exercisesOf(part: BodyPart, records: List<StrengthRecordWithSets>): List<String> {
    val recorded = records
        .filter { bodyPartOf(it.record) == part }
        .map { it.record.ex }
        .distinct()
    val presets = presetExercises(part).filter { it in recorded }
    val extra = recorded.filterNot { it in presets }
    return presets + extra
}
