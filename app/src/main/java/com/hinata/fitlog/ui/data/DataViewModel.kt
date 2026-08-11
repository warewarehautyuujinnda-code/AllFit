package com.hinata.fitlog.ui.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.data.RecordCounts
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * データ画面（FR-11〜14）。書き出し・読み込み・全削除・件数表示を担う。
 *
 * ファイルの選択自体は画面側のシステムのファイル選択（SAF）に任せ、
 * ここは受け取った Uri の読み書きだけを行う。保存先を利用者が選ぶので
 * ストレージの権限は必要ない。
 */
class DataViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as FitLogApp).repository

    /** 種別ごとの件数（FR-14）。記録の追加・削除に追従する */
    val counts: StateFlow<RecordCounts> = repository.counts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordCounts())

    /** 処理結果の通知。画面側でスナックバーに出す。1回きりの通知なので StateFlow ではなく SharedFlow */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** 書き出し・読み込み中は操作を止めるためのフラグ。二重に走らせないためにも使う */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** 書き出すファイルの既定名。いつのバックアップか一目で分かるよう日付を入れる */
    fun suggestedFileName(): String =
        "fitlog-backup-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.json"

    /** 全記録を選択されたファイルへ JSON で書き出す（FR-11） */
    fun export(uri: Uri) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val result = runCatching {
                val json = repository.exportToJson()
                withContext(Dispatchers.IO) {
                    // "wt" は既存の内容を切り詰めてから書く。既存ファイルを選ばれたときに
                    // 前の内容が末尾に残って壊れた JSON になるのを防ぐ
                    val resolver = getApplication<Application>().contentResolver
                    val stream = resolver.openOutputStream(uri, "wt")
                        ?: error("ファイルを開けませんでした")
                    stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
            }
            _busy.value = false
            _messages.emit(
                result.fold(
                    onSuccess = { "書き出しました（${counts.value.total}件）" },
                    onFailure = { "書き出しに失敗しました: ${it.messageForUser()}" },
                )
            )
        }
    }

    /**
     * 選択された JSON ファイルを読み込んで記録を復元する（FR-12）。
     * 不正なファイルの場合は既存データに一切触れずにエラーだけを通知する。
     */
    fun import(uri: Uri) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val readResult = runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val stream = resolver.openInputStream(uri)
                        ?: error("ファイルを開けませんでした")
                    stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            }

            val message = readResult.fold(
                onSuccess = { text ->
                    repository.import(text).fold(
                        onSuccess = { "読み込みました（${it.totalCount}件）" },
                        // 解析に失敗しても DB には書いていないので、そのことを明示して安心させる
                        onFailure = { "読み込めませんでした。ファイルの形式を確認してください（既存の記録はそのままです）" },
                    )
                },
                onFailure = { "ファイルを読めませんでした: ${it.messageForUser()}" },
            )
            _busy.value = false
            _messages.emit(message)
        }
    }

    /** すべての記録を削除する（FR-13）。確認は画面側で取る */
    fun deleteAll() {
        viewModelScope.launch {
            val deleted = counts.value.total
            runCatching { repository.deleteAll() }.fold(
                onSuccess = { _messages.emit("すべての記録を削除しました（${deleted}件）") },
                onFailure = { _messages.emit("削除に失敗しました: ${it.messageForUser()}") },
            )
        }
    }

    private fun Throwable.messageForUser(): String = message ?: this::class.java.simpleName
}
