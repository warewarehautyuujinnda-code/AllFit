package com.hinata.fitlog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hinata.fitlog.data.dao.MealDao
import com.hinata.fitlog.data.dao.RunningDao
import com.hinata.fitlog.data.dao.StrengthDao
import com.hinata.fitlog.data.dao.WeightDao
import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.data.entity.RunningEntity
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
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao
    abstract fun strengthDao(): StrengthDao
    abstract fun runningDao(): RunningDao
    abstract fun mealDao(): MealDao

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

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitlog.db",
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }
    }
}
