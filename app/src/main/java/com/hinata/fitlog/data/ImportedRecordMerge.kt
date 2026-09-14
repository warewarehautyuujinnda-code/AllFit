package com.hinata.fitlog.data

/**
 * 読み込み（FR-12）で、読み込んだ記録に登録日時（createdAt）が無いときは、
 * 端末にある同じ記録（同じ id）の登録日時を残す。
 *
 * 登録日時は version 9 で足した項目なので、それより前に書き出したバックアップには入っていない。
 * そのまま上書きすると、古いファイルを読み込んだせいで端末にある記録の「いつ入力したか」が
 * 消えてしまう。読み込みは全置換ではなくマージにする決まり（CLAUDE.md）に合わせて、
 * 「読み込んだ側に値がある場合だけ上書きする」形にしてある。
 *
 * @param imported 読み込んだ記録
 * @param local 端末にすでにある記録
 * @param idOf 記録のid（どの記録が同じものかの判断に使う）
 * @param createdAtOf 記録の登録日時（未記録なら null）
 * @param withCreatedAt 登録日時を入れた記録を返す（値があるときだけ呼ばれる）
 */
fun <T> keepKnownCreatedAt(
    imported: List<T>,
    local: List<T>,
    idOf: (T) -> String,
    createdAtOf: (T) -> String?,
    withCreatedAt: (T, String) -> T,
): List<T> {
    if (imported.isEmpty()) return imported
    val known: Map<String, String> = local.mapNotNull { record ->
        createdAtOf(record)?.let { idOf(record) to it }
    }.toMap()
    return imported.map { record ->
        if (createdAtOf(record) != null) record
        else known[idOf(record)]?.let { withCreatedAt(record, it) } ?: record
    }
}
