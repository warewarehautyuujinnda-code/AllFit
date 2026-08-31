package com.hinata.fitlog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hinata.fitlog.data.dao.MealDao
import com.hinata.fitlog.data.dao.RunningDao
import com.hinata.fitlog.data.dao.RunningPointDao
import com.hinata.fitlog.data.dao.RunningSplitDao
import com.hinata.fitlog.data.dao.StrengthDao
import com.hinata.fitlog.data.dao.StrengthSetDao
import com.hinata.fitlog.data.dao.WeightDao
import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.RunningPointEntity
import com.hinata.fitlog.data.entity.RunningSplitEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.StrengthSetEntity
import com.hinata.fitlog.data.entity.WeightEntity
import java.util.UUID

/**
 * アプリのローカルDB（Room）。外部サーバーには一切送信せず端末内に保存する。
 */
@Database(
    entities = [
        WeightEntity::class,
        StrengthEntity::class,
        RunningEntity::class,
        MealEntity::class,
        RunningSplitEntity::class,
        StrengthSetEntity::class,
        RunningPointEntity::class,
    ],
    version = 6,
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

    companion object {
        /**
         * 筋トレ記録に部位（part）を足した。列の追加だけなので既存の記録はそのまま残る。
         * 破壊的フォールバックは使わない（利用者の実データが消えるため）。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE strength ADD COLUMN part TEXT")
            }
        }

        /**
         * GPS計測したランの1分ごとの内訳を持つテーブルを追加した。
         * 既存の running テーブルには変更がないため、手入力済みの記録はそのまま残る。
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `running_split` (
                        `id` TEXT NOT NULL,
                        `runId` TEXT NOT NULL,
                        `minuteIndex` INTEGER NOT NULL,
                        `distanceKm` REAL NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_running_split_runId` ON `running_split` (`runId`)"
                )
            }
        }

        /**
         * ランの記録にメモを足した。列の追加だけなので既存の記録はそのまま残る。
         * 破壊的フォールバックは使わない（利用者の実データが消えるため）。
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE running ADD COLUMN memo TEXT")
            }
        }

        /**
         * 筋トレの記録をセット単位に分割した。1レコードにまとめていた重量・回数・セット数では
         * セットごとに重量や回数を変えられなかったため、セットごとの子テーブル（strength_set）に移す。
         * 既存記録は「セット数」件の strength_set 行として展開し、重量・回数はセット数ぶん複製することで
         * データを失わずに引き継ぐ（セット数が未入力でも重量か回数のどちらかがあれば1セット分は残す）。
         * 破壊的フォールバックは使わない（利用者の実データが消えるため）。
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `strength_set` (
                        `id` TEXT NOT NULL,
                        `recordId` TEXT NOT NULL,
                        `setIndex` INTEGER NOT NULL,
                        `weight` REAL,
                        `reps` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_strength_set_recordId` ON `strength_set` (`recordId`)"
                )

                db.query("SELECT id, weight, reps, sets FROM strength").use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow("id")
                    val weightIdx = cursor.getColumnIndexOrThrow("weight")
                    val repsIdx = cursor.getColumnIndexOrThrow("reps")
                    val setsIdx = cursor.getColumnIndexOrThrow("sets")
                    while (cursor.moveToNext()) {
                        val recordId = cursor.getString(idIdx)
                        val weight = if (cursor.isNull(weightIdx)) null else cursor.getDouble(weightIdx)
                        val reps = if (cursor.isNull(repsIdx)) null else cursor.getInt(repsIdx)
                        val setsCount = if (cursor.isNull(setsIdx)) 0 else cursor.getInt(setsIdx)
                        val expanded = when {
                            setsCount > 0 -> setsCount
                            weight != null || reps != null -> 1
                            else -> 0
                        }
                        for (setIndex in 0 until expanded) {
                            db.execSQL(
                                "INSERT INTO strength_set (id, recordId, setIndex, weight, reps) " +
                                    "VALUES (?, ?, ?, ?, ?)",
                                arrayOf<Any?>(UUID.randomUUID().toString(), recordId, setIndex, weight, reps),
                            )
                        }
                    }
                }

                // strength から重量・回数・セット数の列を落とす（SQLiteのDROP COLUMNに版差があるため作り直す）
                db.execSQL(
                    """
                    CREATE TABLE `strength_new` (
                        `id` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `ex` TEXT NOT NULL,
                        `part` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO strength_new (id, date, ex, part) SELECT id, date, ex, part FROM strength")
                db.execSQL("DROP TABLE strength")
                db.execSQL("ALTER TABLE strength_new RENAME TO strength")
            }
        }

        /**
         * GPS計測したランの経路（緯度経度の並び）を持つテーブルを追加した。
         * 既存のテーブルには変更がないため、それまでの記録はそのまま残る
         * （手入力の記録、およびこの変更より前に計測した記録には経路データがない）。
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `running_point` (
                        `id` TEXT NOT NULL,
                        `runId` TEXT NOT NULL,
                        `sequence` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_running_point_runId` ON `running_point` (`runId`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitlog.db",
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                ).build()
                    .also { INSTANCE = it }
            }
        }
    }
}
