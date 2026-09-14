package com.hinata.fitlog.data

import com.hinata.fitlog.data.entity.ExerciseEntity
import java.time.Instant

/**
 * 読み込み（FR-12）で、端末にすでにある種目の定義と読み込んだ定義をすり合わせる。
 *
 * 記録は id が同じなら上書きでよいが、種目の説明は利用者が書いた文章なので、
 * 古いバックアップを読み込んだせいで新しく書いた説明が消えることがないようにする。
 * - 読み込んだ側が新しければ（updatedAt）そちらを採用する
 * - そうでなければ端末側を残し、端末側が空の項目だけ読み込んだ値で埋める
 */
fun mergeImportedExercise(local: ExerciseEntity, imported: ExerciseEntity): ExerciseEntity =
    if (isNewer(imported.updatedAt, local.updatedAt)) {
        imported.copy(name = local.name, createdAt = local.createdAt ?: imported.createdAt)
    } else {
        local.copy(
            part = local.part ?: imported.part,
            description = local.description?.takeIf { it.isNotBlank() } ?: imported.description,
        )
    }

/** ISO-8601 の日時の新旧。読めない値・無い値は「古い」として扱う */
private fun isNewer(candidate: String?, current: String?): Boolean {
    val left = candidate?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
    val right = current?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return true
    return left.isAfter(right)
}
