package com.hinata.fitlog

import android.app.Application
import com.hinata.fitlog.data.AppDatabase
import com.hinata.fitlog.data.FitLogRepository

/**
 * アプリ全体で共有する DB インスタンスを保持する Application クラス。
 */
class FitLogApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    /** 4種別をまたぐ操作（書き出し・読み込み・全削除・件数）の入り口 */
    val repository: FitLogRepository by lazy { FitLogRepository(database) }
}
