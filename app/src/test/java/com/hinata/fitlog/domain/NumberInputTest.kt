package com.hinata.fitlog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** 入力欄の数値解析（FR-01〜04 の保存判定） */
class NumberInputTest {

    @Test
    fun `任意項目は未入力なら null 値として扱い、解析失敗とは区別する`() {
        val parsed = parseOptionalDouble("")
        assertNotNull("未入力は解析失敗ではない", parsed)
        assertNull(parsed!!.value)
        assertNotNull(parseOptionalDouble("   "))
    }

    @Test
    fun `任意項目は数値として読めなければ解析失敗にする`() {
        assertNull(parseOptionalDouble("abc"))
        assertNull(parseOptionalInt("3.5"))
    }

    @Test
    fun `NaN と Infinity は 0以下チェックをすり抜けるので明示的に弾く`() {
        assertNull(parseOptionalDouble("NaN"))
        assertNull(parseOptionalDouble("Infinity"))
        assertNull(parseRequiredDouble("NaN"))
        assertNull(parseRequiredDouble("-Infinity"))
    }

    @Test
    fun `0以下は記録として意味がないので弾く`() {
        assertNull(parseOptionalDouble("0"))
        assertNull(parseOptionalInt("-1"))
        assertNull(parseRequiredDouble("0"))
    }

    @Test
    fun `前後の空白を除いて解析する`() {
        assertEquals(70.2, parseRequiredDouble(" 70.2 "))
        assertEquals(12.5, parseOptionalDouble(" 12.5 ")!!.value)
        assertEquals(3, parseOptionalInt(" 3 ")!!.value)
    }

    @Test
    fun `必須項目は未入力なら失敗する`() {
        assertNull(parseRequiredDouble(""))
    }
}
