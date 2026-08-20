package com.hinata.fitlog.domain

/**
 * 筋トレの対象部位。DB には [id] の文字列で保存する。
 *
 * 色は画面ごとの見た目の話なので、ここでは持たずに UI 層で対応付ける
 * （domain を Android 非依存のまま保つため）。
 */
enum class BodyPart(val id: String, val label: String) {
    CHEST("chest", "胸"),
    BACK("back", "背中"),
    SHOULDER("shoulder", "肩"),
    ARM("arm", "腕"),
    ABS("abs", "腹"),
    LEG("leg", "足"),
    ;

    companion object {
        /** 保存値から部位を引く。未設定の既存記録や知らない文字列は null（＝その他） */
        fun fromId(id: String?): BodyPart? = entries.firstOrNull { it.id == id }
    }
}

/** 部位なし（既存記録・自由入力）は「その他」として扱う */
const val OTHER_PART_LABEL = "その他"

/** 部位の表示名。null は「その他」 */
fun bodyPartLabel(part: BodyPart?): String = part?.label ?: OTHER_PART_LABEL
