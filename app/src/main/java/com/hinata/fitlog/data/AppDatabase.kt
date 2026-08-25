package com.hinata.fitlog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hinata.fitlog.data.dao.MealDao
import com.hinata.fitlog.data.dao.RunningDao
import com.hinata.fitlog.data.dao.RunningSplitDao
import com.hinata.fitlog.data.dao.StrengthDao
import com.hinata.fitlog.data.dao.WeightDao
import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.RunningSplitEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.WeightEntity

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
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao
    abstract fun strengthDao(): StrengthDao
    abstract fun runningDao(): RunningDao
    abstract fun mealDao(): MealDao
    abstract fun runningSplitDao(): RunningSplitDao

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

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitlog.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
                    .also { INSTANCE = it }
            }
        }
    }
}
