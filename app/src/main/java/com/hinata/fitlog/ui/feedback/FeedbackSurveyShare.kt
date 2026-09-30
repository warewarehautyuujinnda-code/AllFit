package com.hinata.fitlog.ui.feedback

import android.content.Context
import android.content.Intent

/**
 * 設定タブのアンケートの回答を共有シートで送る。
 * 画像を添える shareFeedback と違い本文だけなので、メールでもメモアプリでもそのまま貼れる text/plain で渡す。
 */
fun shareFeedbackSurvey(context: Context, text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "FitLog への要望")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(sendIntent, "要望を送る"))
}
