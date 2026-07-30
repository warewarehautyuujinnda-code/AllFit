package com.hinata.fitlog

import android.app.Application
import com.hinata.fitlog.data.AppDatabase

/**
 * アプリ全体で共有する DB インスタンスを保持する Application クラス。
 */
class FitLogApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
