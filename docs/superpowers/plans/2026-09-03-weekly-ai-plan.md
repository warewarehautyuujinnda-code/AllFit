# 週次AI計画機能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 長期目標→週次計画→実績比較→次週計画のループのうち、「FitLogアプリが何を持ち、何を表示するか」を実装する。計画データの作成・更新はアプリ内UIではなく、新スキル `/weekly-plan` によるGit更新（kaizenと同型）で行う。

**Architecture:** Room に3つの新テーブル（GoalEntity/WeeklyPlanEntity/WeeklyStrengthTargetEntity）を追加し、コード内シード（`PlanSeeds.kt`）をアプリ起動時に追記専用で同期する。既存4記録（weight/strength/running/meal）とその場で突き合わせる純粋なKotlin関数（`domain/PlanProgress.kt`）で達成状況を計算し、ホーム画面・筋トレタブ・ランニングタブに表示する。体重の長期目標は新規に作らず、既存の`WeightGoalStore`をそのまま使う。

**Tech Stack:** Kotlin, Jetpack Compose, Room（Kotlin Symbol Processing）, kotlinx.coroutines Flow/StateFlow, JUnit4（`app/src/test`、Robolectricなし・純粋KotlinのみJVMユニットテスト可能）

**Spec:** [docs/superpowers/specs/2026-09-03-weekly-ai-plan-design.md](../specs/2026-09-03-weekly-ai-plan-design.md)

## Global Constraints

- 外部送信をしない。ネットワーク通信機能をアプリに追加しない（要件定義書§4）。
- オフラインで全機能が動作すること。
- 既存の記録機能・データ構造（weight/strength/strength_set/running/meal/running_split/running_point）は無改造。
- 計画・目標データ（Goal/WeeklyPlan/WeeklyStrengthTarget）の作成・編集画面はアプリ内に一切作らない。値の変更は常に`PlanSeeds.kt`へのコード変更＋PR経由。
- 日付は `yyyy-MM-dd` 形式の文字列で統一し、既存の日付比較規約（文字列の辞書順＝日付順）に従う。
- 週は月曜始まり。
- 体重の長期目標は既存の `WeightGoalStore`（SharedPreferences）をそのまま使い、新規テーブルを作らない。

---

## Task 1: Room層 — Goal / WeeklyPlan / WeeklyStrengthTarget

**Files:**
- Create: `app/src/main/java/com/hinata/fitlog/data/entity/GoalEntity.kt`
- Create: `app/src/main/java/com/hinata/fitlog/data/entity/WeeklyPlanEntity.kt`
- Create: `app/src/main/java/com/hinata/fitlog/data/entity/WeeklyStrengthTargetEntity.kt`
- Create: `app/src/main/java/com/hinata/fitlog/data/dao/GoalDao.kt`
- Create: `app/src/main/java/com/hinata/fitlog/data/dao/WeeklyPlanDao.kt`
- Create: `app/src/main/java/com/hinata/fitlog/data/dao/WeeklyStrengthTargetDao.kt`
- Modify: `app/src/main/java/com/hinata/fitlog/data/AppDatabase.kt`

**Interfaces:**
- Produces: `GoalEntity(createdAt: String, title: String, targetDate: String? = null)` — PK `createdAt`。`WeeklyPlanEntity(weekStart: String, goalId: String? = null, targetRunningKm: Double? = null, memo: String? = null)` — PK `weekStart`。`WeeklyStrengthTargetEntity(weekPlanId: String, exerciseName: String, targetReps: Int? = null, targetSets: Int? = null)` — 複合PK `(weekPlanId, exerciseName)`。
- Produces: `GoalDao.observeAll(): Flow<List<GoalEntity>>` / `insertAll(items: List<GoalEntity>)`。`WeeklyPlanDao.observeAll(): Flow<List<WeeklyPlanEntity>>` / `insertAll(items: List<WeeklyPlanEntity>)`。`WeeklyStrengthTargetDao.observeAll(): Flow<List<WeeklyStrengthTargetEntity>>` / `insertAll(items: List<WeeklyStrengthTargetEntity>)`。
- Produces: `AppDatabase.goalDao()`, `.weeklyPlanDao()`, `.weeklyStrengthTargetDao()`。DB version を6→7に上げる。

この3テーブルは主キーに UUID を使わず、自然キー（`createdAt` / `weekStart` / `weekPlanId+exerciseName`）を使う。理由: このテーブルは常に `PlanSeeds.kt`（Task 3）からの再投入で埋まる。UUIDをデフォルト値にすると、アプリを再起動するたびに新しいUUIDが生成されて毎回「新規行」として重複挿入されてしまう。自然キーなら同じシードを何度投入しても同じ主キーになり、`OnConflictStrategy.IGNORE`で安全に「追記専用」を実現できる。

- [ ] **Step 1: GoalEntity を作成する**

`app/src/main/java/com/hinata/fitlog/data/entity/GoalEntity.kt`:

```kotlin
package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 長期目標。createdAt(作成日)をそのまま主キーにする。1日1件の作成を仮定しているため、
 * 記録のようなUUIDは使わない（このテーブルは /weekly-plan スキルがコードから投入する専用で、
 * アプリ内からは作成しない）。
 */
@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey val createdAt: String,
    val title: String,
    val targetDate: String? = null,
)
```

- [ ] **Step 2: WeeklyPlanEntity を作成する**

`app/src/main/java/com/hinata/fitlog/data/entity/WeeklyPlanEntity.kt`:

```kotlin
package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 週次計画のヘッダー。weekStart(週の開始日、月曜)をそのまま主キーにする。
 * 1週間に1件だけ存在する前提と一致するため、別途IDを持たない。
 */
@Entity(tableName = "weekly_plan")
data class WeeklyPlanEntity(
    @PrimaryKey val weekStart: String,
    /** 参照先は [GoalEntity.createdAt]。表示には使わず、相談履歴として残すためだけに持つ */
    val goalId: String? = null,
    val targetRunningKm: Double? = null,
    val memo: String? = null,
)
```

- [ ] **Step 3: WeeklyStrengthTargetEntity を作成する**

`app/src/main/java/com/hinata/fitlog/data/entity/WeeklyStrengthTargetEntity.kt`:

```kotlin
package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * 週次計画における、種目ごとの目標。(weekPlanId, exerciseName) を複合主キーにする。
 * 同じ週に同じ種目を二重登録できないようにする、という制約も兼ねている。
 */
@Entity(
    tableName = "weekly_strength_target",
    primaryKeys = ["weekPlanId", "exerciseName"],
    indices = [Index("weekPlanId")],
)
data class WeeklyStrengthTargetEntity(
    /** 参照先は [WeeklyPlanEntity.weekStart] */
    val weekPlanId: String,
    val exerciseName: String,
    val targetReps: Int? = null,
    val targetSets: Int? = null,
)
```

- [ ] **Step 4: 3つのDAOを作成する**

`app/src/main/java/com/hinata/fitlog/data/dao/GoalDao.kt`:

```kotlin
package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    /** IGNORE戦略のため、既に同じ主キーの行があれば何もしない（追記専用） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<GoalEntity>)
}
```

`app/src/main/java/com/hinata/fitlog/data/dao/WeeklyPlanDao.kt`:

```kotlin
package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyPlanDao {
    @Query("SELECT * FROM weekly_plan ORDER BY weekStart DESC")
    fun observeAll(): Flow<List<WeeklyPlanEntity>>

    /** IGNORE戦略のため、既に同じ主キーの行があれば何もしない（追記専用） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<WeeklyPlanEntity>)
}
```

`app/src/main/java/com/hinata/fitlog/data/dao/WeeklyStrengthTargetDao.kt`:

```kotlin
package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyStrengthTargetDao {
    @Query("SELECT * FROM weekly_strength_target")
    fun observeAll(): Flow<List<WeeklyStrengthTargetEntity>>

    /** IGNORE戦略のため、既に同じ主キーの行があれば何もしない（追記専用） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<WeeklyStrengthTargetEntity>)
}
```

- [ ] **Step 5: AppDatabase.kt を更新する（version 6→7、マイグレーション追加）**

`app/src/main/java/com/hinata/fitlog/data/AppDatabase.kt` の import 群に以下を追加する（既存の import ブロックの該当箇所に挿入）:

```kotlin
import com.hinata.fitlog.data.dao.GoalDao
import com.hinata.fitlog.data.dao.WeeklyPlanDao
import com.hinata.fitlog.data.dao.WeeklyStrengthTargetDao
import com.hinata.fitlog.data.entity.GoalEntity
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
```

`@Database` の `entities` リストと `version` を書き換える（既存の7エンティティに3つ追加、versionを7に）:

```kotlin
@Database(
    entities = [
        WeightEntity::class,
        StrengthEntity::class,
        RunningEntity::class,
        MealEntity::class,
        RunningSplitEntity::class,
        StrengthSetEntity::class,
        RunningPointEntity::class,
        GoalEntity::class,
        WeeklyPlanEntity::class,
        WeeklyStrengthTargetEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao
    abstract fun strengthDao(): StrengthDao
    abstract fun runningDao(): RunningDao
    abstract fun mealDao(): MealDao
    abstract fun runningSplitDao(): RunningSplitDao
    abstract fun strengthSetDao(): StrengthSetDao
    abstract fun runningPointDao(): RunningPointDao
    abstract fun goalDao(): GoalDao
    abstract fun weeklyPlanDao(): WeeklyPlanDao
    abstract fun weeklyStrengthTargetDao(): WeeklyStrengthTargetDao
```

`MIGRATION_5_6` の定義の直後（`companion object` 内、`@Volatile` の手前）に `MIGRATION_6_7` を追加する:

```kotlin
        /**
         * 長期目標・週次計画・週次の筋トレ種目別目標のテーブルを追加した。
         * 3つとも新規テーブルの追加のみで、既存テーブルには変更がないため、
         * それまでの記録はそのまま残る。
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goal` (
                        `createdAt` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetDate` TEXT,
                        PRIMARY KEY(`createdAt`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `weekly_plan` (
                        `weekStart` TEXT NOT NULL,
                        `goalId` TEXT,
                        `targetRunningKm` REAL,
                        `memo` TEXT,
                        PRIMARY KEY(`weekStart`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `weekly_strength_target` (
                        `weekPlanId` TEXT NOT NULL,
                        `exerciseName` TEXT NOT NULL,
                        `targetReps` INTEGER,
                        `targetSets` INTEGER,
                        PRIMARY KEY(`weekPlanId`, `exerciseName`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_weekly_strength_target_weekPlanId` " +
                        "ON `weekly_strength_target` (`weekPlanId`)"
                )
            }
        }
```

最後に `.addMigrations(...)` の行を更新する:

```kotlin
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7,
                ).build()
```

- [ ] **Step 6: ビルドして確認する**

Room はコンパイル時に `@Database` の entities と実際のマイグレーションSQLの整合性を検証しないため、ここでの確認は「コンパイルが通ること」が中心になる（本プロジェクトにはRobolectric等のインストルメンテーションテストが無く、Room DAOのJVMユニットテストは書けない。既存のkaizenスキルも同じ理由でビルド確認を最終確認手段としている）。

実行:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cmd /c "C:\Git\AllFit\gradlew.bat -p C:\Git\AllFit assembleDebug --console=plain"
```
期待結果: `BUILD SUCCESSFUL`

- [ ] **Step 7: コミット**

```bash
git add app/src/main/java/com/hinata/fitlog/data/entity/GoalEntity.kt \
  app/src/main/java/com/hinata/fitlog/data/entity/WeeklyPlanEntity.kt \
  app/src/main/java/com/hinata/fitlog/data/entity/WeeklyStrengthTargetEntity.kt \
  app/src/main/java/com/hinata/fitlog/data/dao/GoalDao.kt \
  app/src/main/java/com/hinata/fitlog/data/dao/WeeklyPlanDao.kt \
  app/src/main/java/com/hinata/fitlog/data/dao/WeeklyStrengthTargetDao.kt \
  app/src/main/java/com/hinata/fitlog/data/AppDatabase.kt
git commit -m "feat: 長期目標・週次計画のRoomテーブルを追加 (v6→v7)"
```

---

## Task 2: ドメイン層 — PlanProgress.kt（TDD）

**Files:**
- Create: `app/src/main/java/com/hinata/fitlog/domain/PlanProgress.kt`
- Test: `app/src/test/java/com/hinata/fitlog/domain/PlanProgressTest.kt`

**Interfaces:**
- Consumes: `WeeklyPlanEntity`, `WeeklyStrengthTargetEntity`（Task 1）, `StrengthRecordWithSets`, `RunningEntity`（既存）
- Produces: `weekStartOf(date: LocalDate): String`、`weeklyPlanFor(plans: List<WeeklyPlanEntity>, weekStart: String): WeeklyPlanEntity?`、`data class StrengthPlanProgress(plannedExercises: List<String>, doneExercises: Set<String>)`（`plannedCount`/`doneCount`/`pendingExercises` を計算プロパティとして持つ）、`strengthProgressOf(targets: List<WeeklyStrengthTargetEntity>, records: List<StrengthRecordWithSets>, weekStart: String): StrengthPlanProgress`、`runningDistanceOf(records: List<RunningEntity>, weekStart: String): Double`、`data class WeeklyRunningProgress(actualKm: Double, targetKm: Double)`（`remainingKm` 計算プロパティ）、`data class WeeklyGoalSummary(goalTitle: String? = null, goalTargetDate: String? = null, strength: StrengthPlanProgress? = null, runningActualKm: Double? = null, runningTargetKm: Double? = null, weightCurrent: Double? = null, weightGoal: Double? = null)` — Task 4（Home）が組み立てて使う

種目ごとの目標(reps/sets)を1件だけ引く処理は、独立した関数を切らず、Task 5でも既存の
`lastRecord`（前回参考表示）と同じやり方（`weekTargets.firstOrNull { ... }`）で画面側から
直接参照する。同じ目的の小さな1行フィルタをドメイン層に別途持つ必要はない。

- [ ] **Step 1: 失敗するテストを書く**

`app/src/test/java/com/hinata/fitlog/domain/PlanProgressTest.kt`:

```kotlin
package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanProgressTest {

    private fun record(date: String, ex: String) =
        StrengthRecordWithSets(record = StrengthEntity(date = date, ex = ex), sets = emptyList())

    @Test
    fun `週の途中の日付から月曜日を求める`() {
        assertEquals("2026-08-31", weekStartOf(LocalDate.of(2026, 9, 3)))
    }

    @Test
    fun `月曜日を渡すとそのまま返る`() {
        assertEquals("2026-08-31", weekStartOf(LocalDate.of(2026, 8, 31)))
    }

    @Test
    fun `週初日が一致する計画を返す`() {
        val plans = listOf(
            WeeklyPlanEntity(weekStart = "2026-08-24"),
            WeeklyPlanEntity(weekStart = "2026-08-31", targetRunningKm = 10.0),
        )
        assertEquals(10.0, weeklyPlanFor(plans, "2026-08-31")?.targetRunningKm)
    }

    @Test
    fun `該当する計画が無ければnullを返す`() {
        val plans = listOf(WeeklyPlanEntity(weekStart = "2026-08-24"))
        assertNull(weeklyPlanFor(plans, "2026-08-31"))
    }

    @Test
    fun `週内に記録した種目は実施済みになる`() {
        val targets = listOf(
            WeeklyStrengthTargetEntity(weekPlanId = "2026-08-31", exerciseName = "ベンチプレス"),
            WeeklyStrengthTargetEntity(weekPlanId = "2026-08-31", exerciseName = "スクワット"),
        )
        val records = listOf(record("2026-09-02", "ベンチプレス"))

        val result = strengthProgressOf(targets, records, "2026-08-31")

        assertEquals(2, result.plannedCount)
        assertEquals(1, result.doneCount)
        assertEquals(listOf("スクワット"), result.pendingExercises)
    }

    @Test
    fun `週の範囲外の記録は実施済みに数えない`() {
        val targets = listOf(WeeklyStrengthTargetEntity(weekPlanId = "2026-08-31", exerciseName = "ベンチプレス"))
        val records = listOf(record("2026-08-30", "ベンチプレス"), record("2026-09-07", "ベンチプレス"))

        val result = strengthProgressOf(targets, records, "2026-08-31")

        assertEquals(0, result.doneCount)
    }

    @Test
    fun `種目名の前後の空白を無視して一致判定する`() {
        val targets = listOf(WeeklyStrengthTargetEntity(weekPlanId = "2026-08-31", exerciseName = " ベンチプレス "))
        val records = listOf(record("2026-09-01", "ベンチプレス"))

        val result = strengthProgressOf(targets, records, "2026-08-31")

        assertEquals(1, result.doneCount)
    }

    @Test
    fun `他の週の計画は数えない`() {
        val targets = listOf(WeeklyStrengthTargetEntity(weekPlanId = "2026-08-24", exerciseName = "ベンチプレス"))
        val records = listOf(record("2026-09-01", "ベンチプレス"))

        val result = strengthProgressOf(targets, records, "2026-08-31")

        assertEquals(0, result.plannedCount)
    }

    @Test
    fun `週内の距離を合計する`() {
        val records = listOf(
            RunningEntity(date = "2026-08-31", dist = 5.0),
            RunningEntity(date = "2026-09-03", dist = 3.5),
            RunningEntity(date = "2026-09-06", dist = 2.0),
        )
        assertEquals(10.5, runningDistanceOf(records, "2026-08-31"), 0.001)
    }

    @Test
    fun `週の範囲外の記録は合計に含めない`() {
        val records = listOf(
            RunningEntity(date = "2026-08-30", dist = 5.0),
            RunningEntity(date = "2026-09-07", dist = 5.0),
        )
        assertEquals(0.0, runningDistanceOf(records, "2026-08-31"), 0.001)
    }
}
```

- [ ] **Step 2: テストを実行し、失敗（コンパイルエラー）を確認する**

実行:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cmd /c "C:\Git\AllFit\gradlew.bat -p C:\Git\AllFit testDebugUnitTest --console=plain"
```
期待結果: `weekStartOf` などの関数が存在せず、コンパイルエラーで失敗する。

- [ ] **Step 3: 最小実装を書く**

`app/src/main/java/com/hinata/fitlog/domain/PlanProgress.kt`:

```kotlin
package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * 週次計画（長期目標→週次計画→実績比較のループ）の実績突き合わせ。
 * Android に依存しない素の Kotlin で書いてあるので、画面や ViewModel を持ち出さずに
 * そのまま動かして確かめられる（[Stats.kt] と同じ方針）。
 *
 * 週は月曜始まり。date は yyyy-MM-dd 固定のため、文字列比較で範囲判定できる
 * （[Stats.kt] と同じ前提）。
 */

/** [date] を含む週の開始日（月曜）を yyyy-MM-dd で返す */
fun weekStartOf(date: LocalDate): String =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

/** [weekStart] の翌週の開始日。範囲判定の上限（この日を含まない）に使う */
private fun weekEndExclusiveOf(weekStart: String): String =
    LocalDate.parse(weekStart).plusDays(7).toString()

/** [weekStart] に一致する週次計画。無ければ null */
fun weeklyPlanFor(plans: List<WeeklyPlanEntity>, weekStart: String): WeeklyPlanEntity? =
    plans.firstOrNull { it.weekStart == weekStart }

/** 週次の筋トレ計画の実施状況 */
data class StrengthPlanProgress(
    val plannedExercises: List<String>,
    val doneExercises: Set<String>,
) {
    val plannedCount: Int get() = plannedExercises.size
    val doneCount: Int get() = plannedExercises.count { it in doneExercises }
    val pendingExercises: List<String> get() = plannedExercises.filterNot { it in doneExercises }
}

/**
 * 週次の筋トレ実施状況を求める。種目名（前後の空白を除いた完全一致）のレコードが
 * その週に1件でもあれば「実施」とみなす。reps/sets の達成可否は判定しない
 * （入力画面での目安表示にのみ使う）。
 */
fun strengthProgressOf(
    targets: List<WeeklyStrengthTargetEntity>,
    records: List<StrengthRecordWithSets>,
    weekStart: String,
): StrengthPlanProgress {
    val weekEndExclusive = weekEndExclusiveOf(weekStart)
    val plannedExercises = targets
        .filter { it.weekPlanId == weekStart }
        .map { it.exerciseName.trim() }
    val doneExercises = records
        .filter { it.record.date >= weekStart && it.record.date < weekEndExclusive }
        .map { it.record.ex.trim() }
        .toSet()
    return StrengthPlanProgress(plannedExercises = plannedExercises, doneExercises = doneExercises)
}

/** 週内のラン距離合計(km) */
fun runningDistanceOf(records: List<RunningEntity>, weekStart: String): Double {
    val weekEndExclusive = weekEndExclusiveOf(weekStart)
    return records
        .filter { it.date >= weekStart && it.date < weekEndExclusive }
        .sumOf { it.dist }
}

/** 週次のラン距離目標に対する実績 */
data class WeeklyRunningProgress(val actualKm: Double, val targetKm: Double) {
    val remainingKm: Double get() = (targetKm - actualKm).coerceAtLeast(0.0)
}

/** ホーム画面「今週の目標」セクションの表示内容 */
data class WeeklyGoalSummary(
    val goalTitle: String? = null,
    val goalTargetDate: String? = null,
    val strength: StrengthPlanProgress? = null,
    val runningActualKm: Double? = null,
    val runningTargetKm: Double? = null,
    val weightCurrent: Double? = null,
    val weightGoal: Double? = null,
)
```

- [ ] **Step 4: テストを実行し、全て成功することを確認する**

実行:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cmd /c "C:\Git\AllFit\gradlew.bat -p C:\Git\AllFit testDebugUnitTest --console=plain"
```
期待結果: `BUILD SUCCESSFUL`（10件のテストすべて成功）

- [ ] **Step 5: コミット**

```bash
git add app/src/main/java/com/hinata/fitlog/domain/PlanProgress.kt \
  app/src/test/java/com/hinata/fitlog/domain/PlanProgressTest.kt
git commit -m "feat: 週次計画の達成状況を計算するPlanProgressを追加"
```

---

## Task 3: PlanSeeds・PlanRepository・FitLogAppへの組み込み（シード同期）

**Files:**
- Create: `app/src/main/java/com/hinata/fitlog/data/PlanSeeds.kt`
- Create: `app/src/main/java/com/hinata/fitlog/data/PlanRepository.kt`
- Modify: `app/src/main/java/com/hinata/fitlog/FitLogApp.kt`

**Interfaces:**
- Consumes: `GoalDao`/`WeeklyPlanDao`/`WeeklyStrengthTargetDao`（Task 1）、`AppDatabase`
- Produces: `object PlanSeeds { val goals: List<GoalEntity>; val weeklyPlans: List<WeeklyPlanEntity>; val strengthTargets: List<WeeklyStrengthTargetEntity> }`（`/weekly-plan` スキルが今後ここに追記する）。`class PlanRepository(db: AppDatabase)` の `observeGoals()`/`observeWeeklyPlans()`/`observeStrengthTargets(): Flow<List<...>>` と `suspend fun syncSeeds()`。`FitLogApp.planRepository: PlanRepository`（Task 4〜6が使う）

- [ ] **Step 1: PlanSeeds.kt を作成する**

`app/src/main/java/com/hinata/fitlog/data/PlanSeeds.kt`:

```kotlin
package com.hinata.fitlog.data

import com.hinata.fitlog.data.entity.GoalEntity
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity

/**
 * 長期目標・週次計画のシードデータ。
 * `/weekly-plan` スキルがここに追記し、ビルドしたアプリの起動時に Room へ取り込まれる
 * （[PlanRepository.syncSeeds] が追記専用で同期する。上書き・削除はしない）。
 *
 * 例:
 * ```
 * val goals = listOf(
 *     GoalEntity(createdAt = "2026-09-03", title = "2027年3月までにハーフマラソン完走"),
 * )
 * val weeklyPlans = listOf(
 *     WeeklyPlanEntity(weekStart = "2026-09-07", goalId = "2026-09-03", targetRunningKm = 10.0),
 * )
 * val strengthTargets = listOf(
 *     WeeklyStrengthTargetEntity(weekPlanId = "2026-09-07", exerciseName = "ベンチプレス", targetReps = 10, targetSets = 3),
 * )
 * ```
 */
object PlanSeeds {
    val goals: List<GoalEntity> = emptyList()
    val weeklyPlans: List<WeeklyPlanEntity> = emptyList()
    val strengthTargets: List<WeeklyStrengthTargetEntity> = emptyList()
}
```

- [ ] **Step 2: PlanRepository.kt を作成する**

`app/src/main/java/com/hinata/fitlog/data/PlanRepository.kt`:

```kotlin
package com.hinata.fitlog.data

import com.hinata.fitlog.data.entity.GoalEntity
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 長期目標・週次計画の入り口。[PlanSeeds] からRoomへの同期と、購読用のFlowをまとめる。
 */
class PlanRepository(private val db: AppDatabase) {
    private val goalDao = db.goalDao()
    private val weeklyPlanDao = db.weeklyPlanDao()
    private val strengthTargetDao = db.weeklyStrengthTargetDao()

    fun observeGoals(): Flow<List<GoalEntity>> = goalDao.observeAll()
    fun observeWeeklyPlans(): Flow<List<WeeklyPlanEntity>> = weeklyPlanDao.observeAll()
    fun observeStrengthTargets(): Flow<List<WeeklyStrengthTargetEntity>> = strengthTargetDao.observeAll()

    /**
     * [PlanSeeds] の内容をRoomに取り込む。
     * INSERTはIGNORE戦略のため、既に同じ主キーの行があれば何もしない（追記専用、上書きなし）。
     */
    suspend fun syncSeeds() = withContext(Dispatchers.IO) {
        goalDao.insertAll(PlanSeeds.goals)
        weeklyPlanDao.insertAll(PlanSeeds.weeklyPlans)
        strengthTargetDao.insertAll(PlanSeeds.strengthTargets)
    }
}
```

- [ ] **Step 3: FitLogApp.kt に組み込む**

`app/src/main/java/com/hinata/fitlog/FitLogApp.kt` を以下の内容に書き換える:

```kotlin
package com.hinata.fitlog

import android.app.Application
import com.hinata.fitlog.data.AppDatabase
import com.hinata.fitlog.data.FitLogRepository
import com.hinata.fitlog.data.PlanRepository
import com.hinata.fitlog.data.RunningRepository
import com.hinata.fitlog.data.StrengthRepository
import com.hinata.fitlog.data.TabVisibilityStore
import com.hinata.fitlog.data.WeightGoalStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * アプリ全体で共有する DB インスタンスを保持する Application クラス。
 */
class FitLogApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    /** 4種別をまたぐ操作（書き出し・読み込み・全削除・件数）の入り口 */
    val repository: FitLogRepository by lazy { FitLogRepository(database) }

    /** ランニングの記録とGPS内訳をまとめて扱う入り口 */
    val runningRepository: RunningRepository by lazy { RunningRepository(database) }

    /** 筋トレの記録とセットごとの内訳をまとめて扱う入り口 */
    val strengthRepository: StrengthRepository by lazy { StrengthRepository(database) }

    /** 目標体重の保存先。体重タブとホームの両方から同じ値を読む */
    val weightGoalStore: WeightGoalStore by lazy { WeightGoalStore(this) }

    /** 表示するタブの保存先。設定タブと下部ナビゲーションの両方から同じ値を読む */
    val tabVisibilityStore: TabVisibilityStore by lazy { TabVisibilityStore(this) }

    /** 長期目標・週次計画の入り口。PlanSeeds をRoomへ同期する役目も持つ */
    val planRepository: PlanRepository by lazy { PlanRepository(database) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { planRepository.syncSeeds() }
    }
}
```

- [ ] **Step 4: ビルドして確認する**

Application起動時の処理はJVMユニットテストで検証できないため（Robolectric等が無い）、ここもコンパイル確認が中心になる。

実行:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cmd /c "C:\Git\AllFit\gradlew.bat -p C:\Git\AllFit assembleDebug --console=plain"
```
期待結果: `BUILD SUCCESSFUL`

(任意・手動): エミュレータ環境がある場合は、APKをインストールして起動しクラッシュしないことを確認する。`app-debug.apk` は `app/build/outputs/apk/debug/` に出力される。

- [ ] **Step 5: コミット**

```bash
git add app/src/main/java/com/hinata/fitlog/data/PlanSeeds.kt \
  app/src/main/java/com/hinata/fitlog/data/PlanRepository.kt \
  app/src/main/java/com/hinata/fitlog/FitLogApp.kt
git commit -m "feat: PlanSeedsのRoomへの起動時同期を追加"
```

---

## Task 4: ホーム画面「今週の目標」セクション

**Files:**
- Modify: `app/src/main/java/com/hinata/fitlog/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/hinata/fitlog/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `PlanRepository`（Task 3）、`WeeklyGoalSummary`/`weekStartOf`/`weeklyPlanFor`/`strengthProgressOf`/`runningDistanceOf`（Task 2）、既存の `HomeViewModel.weightGoal: StateFlow<Double?>`
- Produces: `HomeViewModel.weeklyGoalSummary: StateFlow<WeeklyGoalSummary>`

- [ ] **Step 1: HomeViewModel.kt に weeklyGoalSummary を追加する**

`app/src/main/java/com/hinata/fitlog/ui/home/HomeViewModel.kt` の import 群に追加:

```kotlin
import com.hinata.fitlog.domain.WeeklyGoalSummary
import com.hinata.fitlog.domain.runningDistanceOf
import com.hinata.fitlog.domain.strengthProgressOf
import com.hinata.fitlog.domain.weekStartOf
import com.hinata.fitlog.domain.weeklyPlanFor
import java.time.LocalDate
```

クラス本体の一番最後（既存の `val weightGoal: StateFlow<Double?> = (app as FitLogApp).weightGoalStore.goal` の行の直後）に追加する。**この位置に置く理由**: 下記コードは `weightGoal` プロパティを参照するため、Kotlinのプロパティ初期化順の都合上、`weightGoal` の宣言より後ろに置く必要がある（先に置くと初期化前の参照になり実行時にクラッシュする）。

```kotlin

    private val planRepository = (app as FitLogApp).planRepository
    private val strengthRecords = db.strengthDao().observeAll()
    private val runningRecords = db.runningDao().observeAll()

    private val planContext = combine(
        planRepository.observeGoals(),
        planRepository.observeWeeklyPlans(),
        planRepository.observeStrengthTargets(),
    ) { goals, plans, targets -> Triple(goals, plans, targets) }

    /** 今週の目標セクション。長期目標・筋トレ・ラン・体重の状況をまとめる */
    val weeklyGoalSummary: StateFlow<WeeklyGoalSummary> = combine(
        planContext, strengthRecords, runningRecords, weights, weightGoal,
    ) { (goals, plans, targets), strengthRecs, runningRecs, weightRecs, wGoal ->
        val weekStart = weekStartOf(LocalDate.now())
        val plan = weeklyPlanFor(plans, weekStart)
        val latestGoal = goals.maxByOrNull { it.createdAt }
        WeeklyGoalSummary(
            goalTitle = latestGoal?.title,
            goalTargetDate = latestGoal?.targetDate,
            strength = plan?.let { strengthProgressOf(targets, strengthRecs, weekStart) },
            runningActualKm = plan?.targetRunningKm?.let { runningDistanceOf(runningRecs, weekStart) },
            runningTargetKm = plan?.targetRunningKm,
            weightCurrent = weightRecs.firstOrNull()?.weight,
            weightGoal = wGoal,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyGoalSummary())
```

- [ ] **Step 2: HomeScreen.kt に「今週の目標」セクションを追加する**

`app/src/main/java/com/hinata/fitlog/ui/home/HomeScreen.kt` の import に追加:

```kotlin
import com.hinata.fitlog.domain.WeeklyGoalSummary
```

`HomeScreen` 関数内、既存の `val hasOlderRecords by viewModel.hasOlderWeightRecords.collectAsState()` の直後に追加:

```kotlin
    val weeklyGoal by viewModel.weeklyGoalSummary.collectAsState()
```

`LazyColumn` 内、既存の `item { SummaryGrid(summary) }` の直後・`item { HorizontalDivider(...) }` の直前に新しい `item` を挿入する:

```kotlin
        item {
            WeeklyGoalSection(weeklyGoal)
        }
```

ファイル末尾（`SummaryCard` 関数の後）に新しいComposableを追加する:

```kotlin

/** 今週の目標セクション。長期目標・筋トレ・ラン・体重の状況を表示する。対応データが無い項目は個別に隠す */
@Composable
private fun WeeklyGoalSection(summary: WeeklyGoalSummary) {
    val hasStrength = summary.strength != null && summary.strength.plannedCount > 0
    val hasRunning = summary.runningTargetKm != null
    val hasWeight = summary.weightGoal != null
    val hasAnyContent = summary.goalTitle != null || hasStrength || hasRunning || hasWeight
    if (!hasAnyContent) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("今週の目標", style = MaterialTheme.typography.titleLarge)

        if (summary.goalTitle != null) {
            Text(
                summary.goalTargetDate?.let { "${summary.goalTitle}（${it}まで）" } ?: summary.goalTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasStrength) {
                val progress = summary.strength!!
                SummaryCard(
                    label = "筋トレ",
                    value = "${progress.doneCount}/${progress.plannedCount} 種目",
                    note = "今週実施",
                    modifier = Modifier.weight(1f),
                )
            }
            if (hasRunning) {
                SummaryCard(
                    label = "ラン",
                    value = "${formatAmount(summary.runningActualKm ?: 0.0)} km",
                    note = "目標 ${formatAmount(summary.runningTargetKm!!)} km",
                    modifier = Modifier.weight(1f),
                )
            }
            if (hasWeight) {
                SummaryCard(
                    label = "体重",
                    value = summary.weightCurrent?.let { "${formatAmount(it)} kg" } ?: "未記録",
                    note = "目標 ${formatAmount(summary.weightGoal!!)} kg",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
```

- [ ] **Step 3: ビルドして確認する**

Compose UIはJVMユニットテストで検証できないため、コンパイル確認が中心になる。

実行:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cmd /c "C:\Git\AllFit\gradlew.bat -p C:\Git\AllFit assembleDebug --console=plain"
```
期待結果: `BUILD SUCCESSFUL`

(任意・手動): `PlanSeeds.kt` の3つのリストに一時的にサンプルデータ（例: 今週の `weekStart` を持つ `WeeklyPlanEntity` と `GoalEntity`）を入れてビルド・実機/エミュレータで起動し、ホーム画面に「今週の目標」セクションが表示されることを確認する。確認後、`PlanSeeds.kt` の変更は元に戻す（実際の値投入は `/weekly-plan` スキル、Task 7の役目）。

- [ ] **Step 4: コミット**

```bash
git add app/src/main/java/com/hinata/fitlog/ui/home/HomeViewModel.kt \
  app/src/main/java/com/hinata/fitlog/ui/home/HomeScreen.kt
git commit -m "feat: ホーム画面に今週の目標セクションを追加"
```

---

## Task 5: 筋トレタブへの計画表示

**Files:**
- Modify: `app/src/main/java/com/hinata/fitlog/ui/strength/StrengthViewModel.kt`
- Modify: `app/src/main/java/com/hinata/fitlog/ui/strength/ExercisePickerScreen.kt`
- Modify: `app/src/main/java/com/hinata/fitlog/ui/strength/SetInputScreen.kt`
- Modify: `app/src/main/java/com/hinata/fitlog/ui/strength/StrengthScreen.kt`

**Interfaces:**
- Consumes: `PlanRepository`（Task 3）、`weekStartOf`/`weeklyPlanFor`/`strengthProgressOf`（Task 2）
- Produces: `StrengthViewModel.pendingExercises: StateFlow<Set<String>>`、`StrengthViewModel.weekTargets: StateFlow<List<WeeklyStrengthTargetEntity>>`。`ExercisePickerScreen(..., plannedExercises: Set<String>, ...)`。`SetInputScreen(..., weekTarget: WeeklyStrengthTargetEntity?, ...)`

- [ ] **Step 1: StrengthViewModel.kt に計画関連のStateFlowを追加する**

`app/src/main/java/com/hinata/fitlog/ui/strength/StrengthViewModel.kt` の import 群に追加:

```kotlin
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
import com.hinata.fitlog.domain.StrengthPlanProgress
import com.hinata.fitlog.domain.strengthProgressOf
import com.hinata.fitlog.domain.weekStartOf
import com.hinata.fitlog.domain.weeklyPlanFor
import java.time.LocalDate
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
```

クラス本体、`private val repository = (app as FitLogApp).strengthRepository` の直後に追加:

```kotlin
    private val planRepository = (app as FitLogApp).planRepository

    /** 今週の計画。無い週は null */
    private val currentPlan: StateFlow<WeeklyPlanEntity?> = planRepository.observeWeeklyPlans()
        .map { plans -> weeklyPlanFor(plans, weekStartOf(LocalDate.now())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val strengthProgress: StateFlow<StrengthPlanProgress?> = combine(
        currentPlan, planRepository.observeStrengthTargets(), items,
    ) { plan, targets, records ->
        plan?.let { strengthProgressOf(targets, records, it.weekStart) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 今週の計画のうち、まだ実施していない種目名（種目選択画面のバッジ表示に使う） */
    val pendingExercises: StateFlow<Set<String>> = strengthProgress
        .map { it?.pendingExercises?.toSet() ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** 今週の計画にある種目ごとの目標(reps/sets)。セット入力画面の目標表示に使う */
    val weekTargets: StateFlow<List<WeeklyStrengthTargetEntity>> = combine(
        currentPlan, planRepository.observeStrengthTargets(),
    ) { plan, targets ->
        plan?.let { p -> targets.filter { it.weekPlanId == p.weekStart } } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

- [ ] **Step 2: ExercisePickerScreen.kt にバッジ表示を追加する**

`app/src/main/java/com/hinata/fitlog/ui/strength/ExercisePickerScreen.kt` の import に追加:

```kotlin
import androidx.compose.material.icons.filled.Flag
```

`ExercisePickerScreen` 関数のシグネチャに `plannedExercises: Set<String>` を追加する:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    records: List<StrengthRecordWithSets>,
    today: LocalDate,
    plannedExercises: Set<String>,
    onBack: () -> Unit,
    onPick: (ExerciseRef) -> Unit,
) {
```

`ExerciseRow` の呼び出し箇所を更新する:

```kotlin
                items(rows, key = { it.ex }) { ref ->
                    ExerciseRow(
                        ref = ref,
                        elapsed = elapsedLabelOf(lastPerformed[ref.ex], today),
                        planned = ref.ex in plannedExercises,
                        onClick = { onPick(ref) },
                    )
                    HorizontalDivider()
                }
```

`ExerciseRow` 関数本体を書き換える:

```kotlin
@Composable
private fun ExerciseRow(
    ref: ExerciseRef,
    elapsed: String?,
    planned: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(bodyPartColor(ref.part))
        )
        Text(
            ref.ex,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        if (planned) {
            Icon(
                Icons.Filled.Flag,
                contentDescription = "今週の計画に含まれる種目",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 8.dp),
            )
        }
        if (elapsed != null) {
            Text(
                elapsed,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

- [ ] **Step 3: SetInputScreen.kt に今週の目標表示を追加する**

`app/src/main/java/com/hinata/fitlog/ui/strength/SetInputScreen.kt` の import に追加:

```kotlin
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
```

`SetInputScreen` 関数のシグネチャに `weekTarget: WeeklyStrengthTargetEntity?` を追加する:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetInputScreen(
    ref: ExerciseRef,
    date: String,
    lastRecord: StrengthRecordWithSets?,
    weekTarget: WeeklyStrengthTargetEntity?,
    snackbarHostState: SnackbarHostState,
    onDateChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: (sets: List<Pair<String, String>>) -> Unit,
) {
```

既存の「前回」表示ブロックの直後に、今週の目標表示を追加する:

```kotlin
            if (lastRecord != null) {
                Text(
                    "前回(${lastRecord.record.date}): ${describeRecord(lastRecord)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (weekTarget != null) {
                Text(
                    "今週の目標: ${describeTarget(weekTarget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
```

`sets.forEachIndexed { index, set -> ... }` ループ内、「回数」の `OutlinedTextField` に `placeholder` を追加する（最初のセット行にのみ目標回数を薄く表示する。2セット目以降は直前の行の値をコピーする既存動作を優先し、targetのplaceholderは出さない）:

```kotlin
                    OutlinedTextField(
                        value = set.reps,
                        onValueChange = { set.reps = it },
                        label = { Text("回数") },
                        placeholder = {
                            val targetReps = weekTarget?.targetReps
                            if (index == 0 && targetReps != null) {
                                Text(targetReps.toString())
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                    )
```

ファイル末尾（`describeRecord` 関数の後）に、目標の整形関数を追加する:

```kotlin

/** 目標の「回数×セット数」を表示用に整形する */
private fun describeTarget(target: WeeklyStrengthTargetEntity): String {
    val reps = target.targetReps?.let { "${it}回" }
    val sets = target.targetSets?.let { "${it}セット" }
    return listOfNotNull(reps, sets).joinToString("×").ifEmpty { "設定あり" }
}
```

- [ ] **Step 4: StrengthScreen.kt から新しいpropsを渡す**

`app/src/main/java/com/hinata/fitlog/ui/strength/StrengthScreen.kt` の `StrengthScreen` 関数冒頭、`val items by viewModel.items.collectAsState()` の直後に追加:

```kotlin
    val pendingExercises by viewModel.pendingExercises.collectAsState()
    val weekTargets by viewModel.weekTargets.collectAsState()
```

`StrengthRoute.Picker` の呼び出しに `plannedExercises` を追加する:

```kotlin
        StrengthRoute.Picker -> ExercisePickerScreen(
            records = items,
            today = today,
            plannedExercises = pendingExercises,
            onBack = { route = StrengthRoute.Calendar },
            onPick = { ref -> route = StrengthRoute.Input(ref) },
        )
```

`StrengthRoute.Input` の呼び出しに `weekTarget` を追加する:

```kotlin
        is StrengthRoute.Input -> SetInputScreen(
            ref = current.ref,
            date = selectedDate,
            lastRecord = items.firstOrNull { it.record.ex == current.ref.ex },
            weekTarget = weekTargets.firstOrNull { it.exerciseName.trim() == current.ref.ex.trim() },
            snackbarHostState = snackbarHostState,
            onDateChange = { selectedDate = it },
            onBack = { route = StrengthRoute.Picker },
            onSave = { sets ->
                val ok = viewModel.save(
                    date = selectedDate,
                    exText = current.ref.ex,
                    part = current.ref.part,
                    sets = sets,
                )
                if (ok) route = StrengthRoute.Calendar
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (ok) "保存しました"
                        else "重量・回数は正の数で入力してください"
                    )
                }
            },
        )
```

- [ ] **Step 5: ビルドして確認する**

実行:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cmd /c "C:\Git\AllFit\gradlew.bat -p C:\Git\AllFit assembleDebug --console=plain"
```
期待結果: `BUILD SUCCESSFUL`

(任意・手動): `PlanSeeds.kt` に今週の `WeeklyPlanEntity` と `WeeklyStrengthTargetEntity`（例: 種目「ベンチプレス」）を一時的に追加してビルド・起動し、種目選択画面に🚩バッジが出ること、セット入力画面に「今週の目標」テキストと1セット目のplaceholderが出ることを確認する。確認後、`PlanSeeds.kt` の変更は元に戻す。

- [ ] **Step 6: コミット**

```bash
git add app/src/main/java/com/hinata/fitlog/ui/strength/StrengthViewModel.kt \
  app/src/main/java/com/hinata/fitlog/ui/strength/ExercisePickerScreen.kt \
  app/src/main/java/com/hinata/fitlog/ui/strength/SetInputScreen.kt \
  app/src/main/java/com/hinata/fitlog/ui/strength/StrengthScreen.kt
git commit -m "feat: 筋トレタブに今週の計画（種目バッジ・目標表示）を追加"
```

---

## Task 6: ランニングタブへの残り目標表示

**Files:**
- Modify: `app/src/main/java/com/hinata/fitlog/ui/running/RunningViewModel.kt`
- Modify: `app/src/main/java/com/hinata/fitlog/ui/running/RunningScreen.kt`

**Interfaces:**
- Consumes: `PlanRepository`（Task 3）、`weekStartOf`/`weeklyPlanFor`/`runningDistanceOf`/`WeeklyRunningProgress`（Task 2）
- Produces: `RunningViewModel.weeklyRunningProgress: StateFlow<WeeklyRunningProgress?>`

- [ ] **Step 1: RunningViewModel.kt に週次進捗を追加する**

`app/src/main/java/com/hinata/fitlog/ui/running/RunningViewModel.kt` の import 群に追加:

```kotlin
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.domain.WeeklyRunningProgress
import com.hinata.fitlog.domain.runningDistanceOf
import com.hinata.fitlog.domain.weekStartOf
import com.hinata.fitlog.domain.weeklyPlanFor
import java.time.LocalDate
```

クラス本体、`private val repository = (app as FitLogApp).runningRepository` の直後に追加:

```kotlin
    private val planRepository = (app as FitLogApp).planRepository

    private val currentWeeklyPlan: StateFlow<WeeklyPlanEntity?> = planRepository.observeWeeklyPlans()
        .map { plans -> weeklyPlanFor(plans, weekStartOf(LocalDate.now())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 今週のラン距離目標に対する実績。目標が無い週は null */
    val weeklyRunningProgress: StateFlow<WeeklyRunningProgress?> = combine(
        currentWeeklyPlan, items,
    ) { plan, records ->
        plan?.targetRunningKm?.let { target ->
            WeeklyRunningProgress(actualKm = runningDistanceOf(records, plan.weekStart), targetKm = target)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

- [ ] **Step 2: RunningMainScreen に残り目標のテキストを追加する**

`app/src/main/java/com/hinata/fitlog/ui/running/RunningScreen.kt` の `RunningMainScreen` 関数内、`val trackState by viewModel.trackState.collectAsState()` の直後に追加:

```kotlin
    val weeklyProgress by viewModel.weeklyRunningProgress.collectAsState()
```

`RunningChart(...)` の呼び出しと `TimerCard(...)` の呼び出しの間に挿入する:

```kotlin
            RunningChart(
                trend = trend,
                latest = items.firstOrNull(),
                monthlyTotalKm = monthlyTotalKm,
                period = trendPeriod,
                metric = trendMetric,
                onPeriodChange = viewModel::selectTrendPeriod,
                onMetricChange = viewModel::selectTrendMetric,
                modifier = Modifier.padding(top = 12.dp),
            )

            if (weeklyProgress != null) {
                Text(
                    "残り目標: ${formatAmount(weeklyProgress!!.remainingKm)} km" +
                        "（目標 ${formatAmount(weeklyProgress!!.targetKm)} km）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            TimerCard(
                state = trackState,
                onStart = {
                    if (hasTrackingPermissions(context)) {
                        RunTrackingController.start(context)
                    } else {
                        permissionLauncher.launch(trackingPermissions())
                    }
                },
                onStop = { RunTrackingController.stop(context) },
                modifier = Modifier.padding(top = 16.dp),
            )
```

- [ ] **Step 3: ビルドして確認する**

実行:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cmd /c "C:\Git\AllFit\gradlew.bat -p C:\Git\AllFit assembleDebug --console=plain"
```
期待結果: `BUILD SUCCESSFUL`

(任意・手動): `PlanSeeds.kt` に今週の `WeeklyPlanEntity`（`targetRunningKm` 付き）を一時的に追加してビルド・起動し、ランニングタブのメイン画面に「残り目標」テキストが出ることを確認する。確認後、`PlanSeeds.kt` の変更は元に戻す。

- [ ] **Step 4: コミット**

```bash
git add app/src/main/java/com/hinata/fitlog/ui/running/RunningViewModel.kt \
  app/src/main/java/com/hinata/fitlog/ui/running/RunningScreen.kt
git commit -m "feat: ランニングタブに今週の残り目標距離を追加"
```

---

## Task 7: 新スキル `/weekly-plan` の追加

**Files:**
- Create: `.claude/skills/weekly-plan/SKILL.md`

**Interfaces:**
- Consumes: `PlanSeeds.kt`（Task 3）の構造、`PlanProgress.kt`（Task 2）の関数群
- Produces: `.claude/skills/weekly-plan/SKILL.md`（kaizenスキルと対になる、計画更新専用のワークフロー定義）

- [ ] **Step 1: kaizenスキルの内容を確認する**

`.claude/skills/kaizen/SKILL.md` を参照し、フロントマター（`name`/`description`）の書式と、本文の構成（なぜこの形か／手順／守ること）を踏襲する。

- [ ] **Step 2: SKILL.md を作成する**

`.claude/skills/weekly-plan/SKILL.md`:

```markdown
---
name: weekly-plan
description: 長期目標と先週の実績を踏まえてユーザーと来週の週次計画（筋トレの種目別目標・週間走行距離）を相談し、決まった数値をコード（PlanSeeds.kt）に追記してPRを作るスキル。FitLogの「AIと一緒に計画を立てる」ループのうち、決まった計画をアプリに反映する部分を担う。ユーザーが週次の振り返りや来週の目標について相談してきたとき、または /weekly-plan を明示的に呼んだときに使う。
---

# weekly-plan — 週次計画の更新ループ

FitLogの「長期目標→週次計画→実績比較→次週計画」というループのうち、
「決まった計画をアプリに反映する」部分を担う。kaizenと対になるスキルで、
対象が「機能の不満」ではなく「目標値」になる。

## なぜこの形なのか

- **kaizenと同じ理由により、マージだけは人間が判断する**: 週次計画は「本人が実際に従う目標」
  として実データ運用される。確認なしに自動反映すると、AIとの相談が浅いまま目標が
  書き換わってしまうリスクがある。
- **ブランチを切る理由**: mainは常に安全な状態を保つ。ブランチ上でのミスはいくらでも
  やり直しがきく。
- **実装まで確認なしで進めてよい理由**: 会話で決まった数値をコードに落とすだけの機械的な
  作業であり、ここで逐一確認を挟むと「相談してすぐ反映される」という体験が壊れる。

## 手順

### 1. 直近の実績を確認する

`app/src/main/java/com/hinata/fitlog/data/PlanSeeds.kt` の直近の `weeklyPlans` /
`strengthTargets` と、実際の記録（既存のデータ書き出し機能で得られるJSON、または
`app/src/main/java/com/hinata/fitlog/domain/PlanProgress.kt` の `strengthProgressOf` /
`runningDistanceOf`）を突き合わせ、先週の計画がどれだけ実施できたかを把握する。

### 2. 会話する

先週の実施状況と長期目標（`PlanSeeds.goals`）を踏まえて、ユーザーと相談しながら
来週の数値目標（筋トレの種目別目標・週間走行距離）を決める。
ここが「AIと一緒に計画を立てる」部分そのもの。曖昧な要望は、最も自然な解釈で進めてよい。

### 3. ブランチを切って `PlanSeeds.kt` に追記する

最新のmainから分岐する。命名は `plan-<週開始日>`（例: `plan-2026-09-07`）。

`weeklyPlans` / `strengthTargets` に、決まった内容を1件追記する。長期目標が変わった
（または新しく決めた）ときのみ `goals` にも追記する。**既存のエントリは書き換えない**
（追記専用。上書きすると過去の計画が変わってしまい、実績との比較が壊れる）。

- `weekStart` は必ずその週の月曜日の日付（`yyyy-MM-dd`）にする。
- `WeeklyStrengthTargetEntity.weekPlanId` は、対応する `WeeklyPlanEntity.weekStart` と
  同じ値にする。
- `GoalEntity.createdAt` は目標を決めた日（`yyyy-MM-dd`）。1日1件を仮定しているため、
  同じ日に複数の長期目標を追記しない。

### 4. ビルド確認 → プルリクエストを作成する

ビルドコマンドで確認する:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cmd /c "C:\Git\AllFit\gradlew.bat -p C:\Git\AllFit assembleDebug --console=plain"
```

何を来週の目標にしたか、先週の実施状況がどうだったかをPR本文に書く。
**ここでマージはしない。** PRを開いた時点でこのスキルの仕事は完了。マージするかどうかは
必ず人間が判断する。

## 守ること

- mainへの直接コミット・直接pushはしない。
- PRを自動でマージしない。承認・マージは常に人間が行う。
- `PlanSeeds.kt` の既存エントリは変更・削除しない。追記のみ。
- アプリ内に計画の入力・編集UIを作らない（値の変更は常にこのスキル経由）。
```

- [ ] **Step 3: 内容を見直す**

kaizenの `SKILL.md` と並べて、フロントマターの書式・見出し構成・トーンが揃っているか確認する（自動テストは無いため、目視での確認になる）。

- [ ] **Step 4: コミット**

```bash
git add .claude/skills/weekly-plan/SKILL.md
git commit -m "feat: 週次計画更新用のスキル /weekly-plan を追加"
```

---

## 実装後の確認（全タスク完了後）

- [ ] 全ユニットテストを実行する:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cmd /c "C:\Git\AllFit\gradlew.bat -p C:\Git\AllFit testDebugUnitTest --console=plain"
```
期待結果: `BUILD SUCCESSFUL`（既存テスト + `PlanProgressTest` すべて成功）

- [ ] リリースビルドも確認する（CI（`.github/workflows/build-apk.yml`）が `assembleRelease` を使うため）:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cmd /c "C:\Git\AllFit\gradlew.bat -p C:\Git\AllFit assembleRelease --console=plain"
```
期待結果: `BUILD SUCCESSFUL`
