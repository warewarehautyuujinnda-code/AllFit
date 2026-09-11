package com.hinata.fitlog.data

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「種目のデータは消さない」という決まり（CLAUDE.md）をコード側で守り続けるための検査。
 *
 * 種目の削除は [com.hinata.fitlog.data.dao.ExerciseDao.hide] による論理削除だけで行い、
 * 行そのものは消さない。あとから「一覧から消えるのだから DB からも消してよい」と
 * 書き換えられてしまわないよう、ソースを走査して物理削除が入っていないことを確かめる。
 *
 * コメントは検査の対象外（この決まり自体をコメントで説明できるようにするため）。
 */
class ExerciseRetentionRuleTest {

    private val mainSources: List<File> by lazy {
        val roots = listOf(File("src/main/java"), File("app/src/main/java"))
        val root = roots.firstOrNull { it.isDirectory }
        assertTrue(
            "main のソースが見つからない（検査できていない）: " + roots.joinToString { it.absolutePath },
            root != null,
        )
        root!!.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    /** 文字列の中身までは見ないが、コメントに書いた説明で引っかからないようにしておく */
    private fun codeOf(file: File): String = file.readText()
        .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        .lines()
        .joinToString("\n") { it.substringBefore("//") }

    @Test
    fun `種目のテーブルを物理削除するコードが無い`() {
        val forbidden = listOf(
            Regex("""delete\s+from\s+`?exercise`?\b""", RegexOption.IGNORE_CASE),
            Regex("""drop\s+table\s+(if\s+exists\s+)?`?exercise`?\b""", RegexOption.IGNORE_CASE),
        )
        val offenders = mainSources.flatMap { file ->
            val code = codeOf(file)
            forbidden.filter { it.containsMatchIn(code) }.map { file.path }
        }
        assertTrue(
            "種目の定義は論理削除だけで扱う決まり（CLAUDE.md）に反している: " + offenders.joinToString(),
            offenders.isEmpty(),
        )
    }

    @Test
    fun `ExerciseDaoに削除や行を消す競合解決を置いていない`() {
        val dao = mainSources.first { it.name == "ExerciseDao.kt" }
        val code = codeOf(dao)
        assertTrue("@Delete を ExerciseDao に置かない", "@Delete" !in code)
        // REPLACE は競合した行を削除してから挿入するため、説明や記録を落としうる
        assertTrue("OnConflictStrategy.REPLACE を ExerciseDao で使わない", "REPLACE" !in code)
    }

    @Test
    fun `全データ削除でも種目の定義は消さない`() {
        val repository = mainSources.first { it.name == "FitLogRepository.kt" }
        val deleteAll = codeOf(repository).substringAfter("suspend fun deleteAll()")
        assertTrue(
            "全データ削除（FR-13）で種目の定義まで消している",
            "exerciseDao" !in deleteAll,
        )
    }
}
