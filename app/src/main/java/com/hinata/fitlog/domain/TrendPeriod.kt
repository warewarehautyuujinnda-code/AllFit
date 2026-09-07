package com.hinata.fitlog.domain

import java.time.LocalDate

/**
 * 体重推移グラフ（FR-07）の表示期間。選んだ期間に応じてグラフの範囲と
 * 増減（[WeightTrend.delta]）が連動して変わる。
 */
enum class TrendPeriod(val label: String) {
    ONE_MONTH("1ヶ月"),
    THREE_MONTHS("3ヶ月"),
    SIX_MONTHS("半年"),
    ONE_YEAR("1年"),
    ALL("全期間"),
    ;

    /**
     * この期間の下限日。ALL は絞り込みなしを表すため null。
     * 「絞り込みなし」は [com.hinata.fitlog.domain.weightTrendOf] 側で、記録がある
     * 一番古い日から最新の日まで（=実データの範囲そのまま）として扱われる。
     */
    fun cutoffDate(today: LocalDate): LocalDate? = when (this) {
        ONE_MONTH -> today.minusMonths(1)
        THREE_MONTHS -> today.minusMonths(3)
        SIX_MONTHS -> today.minusMonths(6)
        ONE_YEAR -> today.minusYears(1)
        ALL -> null
    }
}
