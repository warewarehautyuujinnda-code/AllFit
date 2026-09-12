package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.ExerciseEntity
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

/** 一覧から削除（非表示）にした種目名。記録は残っているので、一覧に出すかどうかの判定だけに使う */
fun hiddenExerciseNames(exercises: List<ExerciseEntity>): Set<String> =
    exercises.filter { it.hiddenAt != null }.mapTo(mutableSetOf()) { it.name.trim() }

/** 種目の部位。定義に入っていなければ種目名からプリセットを引く */
fun exercisePartOf(exercise: ExerciseEntity): BodyPart? =
    BodyPart.fromId(exercise.part) ?: presetPartOf(exercise.name)

/**
 * 種目名の入力チェック。問題があれば画面に出す文言を返す（問題なければ null）。
 *
 * 種目名は記録と種目の定義を結ぶキーなので、ほかの種目と同じ名前は付けられない。
 * 一覧から削除した種目は画面から見えないぶん、ぶつかったときにどうすれば戻せるかも伝える。
 */
fun exerciseNameError(
    input: String,
    currentName: String?,
    visibleNames: Set<String>,
    hiddenNames: Set<String>,
): String? {
    val name = input.trim()
    return when {
        name.isEmpty() -> "種目名を入力してください"
        name == currentName?.trim() -> null
        name in hiddenNames -> "一覧から削除した種目と同じ名前です。鉛筆ボタンから同じ名前で追加すると戻せます"
        name in visibleNames -> "同じ名前の種目がすでにあります"
        else -> null
    }
}
