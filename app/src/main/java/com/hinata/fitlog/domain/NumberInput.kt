package com.hinata.fitlog.domain

/**
 * 任意の数値項目の解析結果。
 * 「未入力（null 値）」と「入力はあるが数値として読めない（解析失敗）」を区別するための入れ物。
 * 解析失敗は関数の戻り値 null で表し、未入力は `Parsed(null)` で表す。
 */
class Parsed<T>(val value: T?)

/**
 * 任意入力の小数を解析する。
 * 未入力なら null 値、正の有限な数ならその値、それ以外（数値でない・0以下・NaN や Infinity）は
 * 解析失敗として null を返す。
 *
 * toDoubleOrNull() は "NaN" / "Infinity" も解析するため、大小比較だけでは弾けない。
 * NaN はすべての比較が false になり 0以下チェックをすり抜けるので isFinite() で先に確認する。
 */
fun parseOptionalDouble(text: String): Parsed<Double>? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return Parsed(null)
    val value = trimmed.toDoubleOrNull() ?: return null
    return if (value.isFinite() && value > 0.0) Parsed(value) else null
}

/**
 * 任意入力の整数を解析する。
 * 未入力なら null 値、正の整数ならその値、それ以外（数値でない・0以下）は解析失敗として null を返す。
 */
fun parseOptionalInt(text: String): Parsed<Int>? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return Parsed(null)
    val value = trimmed.toIntOrNull() ?: return null
    return if (value > 0) Parsed(value) else null
}

/**
 * 必須入力の小数を解析する。未入力・数値でない・0以下・非有限値はすべて null を返す。
 */
fun parseRequiredDouble(text: String): Double? {
    val value = text.trim().toDoubleOrNull() ?: return null
    return if (value.isFinite() && value > 0.0) value else null
}
