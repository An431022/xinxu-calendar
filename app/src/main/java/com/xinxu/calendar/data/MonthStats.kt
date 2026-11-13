package com.xinxu.calendar.data

import java.time.YearMonth

/**
 * 统计口径。**纯函数，不碰数据库也不碰界面** —— 这样口径本身可以被单独验证，
 * 将来改"什么算暖色"只需要动这一个文件。
 *
 * 设计红线 4：**统计只呈现经历，不制造焦虑**。所以这里没有任何"完成度""目标""连续天数"，
 * 只有"这个月出现过哪些颜色"。
 */

/**
 * 两种数法，让用户自己选，不替他决定。
 *
 * 为什么要有两个：一天涂三种颜色的时候，「这一天算什么」本身就没有唯一答案。
 * - [BY_COLOR_COUNT] 一种颜色一票 —— 反映"这个月我的颜色构成"
 * - [BY_DAY_LEAD] 一天一票，取当天**第一个**颜色 —— 反映"这个月我大多是哪种日子"
 *
 * 第一个颜色之所以能代表一天：点颜色的顺序就是当时心里的主次（见 8.4 的排序规则）。
 */
enum class StatsMode(val label: String) {
    BY_COLOR_COUNT("按颜色"),
    BY_DAY_LEAD("按天"),
}

/**
 * 三种温度的计数。
 *
 * [neutral] 单独数、**不混进冷暖**：云灰和石墨代表的是"平淡"和"沉重"，
 * 它们本来就不表达冷暖倾向，硬塞进比例里会把结论拉偏。
 */
data class TemperatureTally(val warm: Int, val cool: Int, val neutral: Int) {
    val total: Int get() = warm + cool + neutral

    /** 在"有冷暖倾向"的颜色里，暖占多少。中性不参与，所以分母是 warm+cool。 */
    val warmShare: Float get() = if (warm + cool == 0) 0f else warm.toFloat() / (warm + cool)
}

data class MonthStats(
    val yearMonth: YearMonth,
    /** 本月**涂过色**的日子，按日期升序。没涂的日子不在里面 —— 留白不留痕。 */
    val coloredDays: List<DayEntry>,
    val byColorCount: TemperatureTally,
    val byDayLead: TemperatureTally,
) {
    val isEmpty: Boolean get() = coloredDays.isEmpty()

    fun tally(mode: StatsMode): TemperatureTally =
        if (mode == StatsMode.BY_COLOR_COUNT) byColorCount else byDayLead
}

/**
 * 结论。刻意用"偏暖/偏冷/相当"这种模糊的说法，不给百分比 ——
 * 一旦写成"暖 62%"，它就从一句感受变成了一项指标。
 */
enum class Verdict(val label: String) {
    WARM("这个月偏暖"),
    COOL("这个月偏冷"),
    BALANCED("冷暖相当"),
    EMPTY("这个月还没有颜色"),
}

fun MonthStats.verdict(mode: StatsMode): Verdict {
    if (isEmpty) return Verdict.EMPTY
    val t = tally(mode)
    return when {
        t.warm > t.cool -> Verdict.WARM
        t.cool > t.warm -> Verdict.COOL
        else -> Verdict.BALANCED
    }
}

/**
 * 算出某个月的统计。
 *
 * 未知的颜色 id **静默跳过**而不是抛异常：将来色板若删掉某个颜色，
 * 用户几个月前涂的记录里还留着那个 id，统计不该因此整个崩掉（计划 5 的约定：颜色 id 只增不删，
 * 但代码不能依赖这个约定永远被遵守）。
 */
/**
 * 一份计数结果：一种颜色一票（[count]）和一天一票（[lead]）各算一遍。
 * 月度统计和年度趋势共用同一套口径 —— 否则同一天在两个月度视角下会得出不同结论。
 */
private data class Counts(val count: TemperatureTally, val lead: TemperatureTally)

private fun countTemperatures(days: List<DayEntry>): Counts {
    var countWarm = 0
    var countCool = 0
    var countNeutral = 0
    var leadWarm = 0
    var leadCool = 0
    var leadNeutral = 0

    for (day in days) {
        day.colorIds.forEachIndexed { index, id ->
            val isLead = index == 0
            when (Palette.byId(id)?.temperature) {
                ColorTemperature.WARM -> {
                    countWarm++
                    if (isLead) leadWarm++
                }
                ColorTemperature.COOL -> {
                    countCool++
                    if (isLead) leadCool++
                }
                ColorTemperature.NEUTRAL -> {
                    countNeutral++
                    if (isLead) leadNeutral++
                }
                null -> Unit
            }
        }
    }
    return Counts(
        count = TemperatureTally(countWarm, countCool, countNeutral),
        lead = TemperatureTally(leadWarm, leadCool, leadNeutral),
    )
}

/**
 * 一年的冷暖计数，给年度视图底部那句结论用。
 *
 * 只收口径、不含文案 —— "今年偏暖"这种话属于界面（[Verdict] 写死了"这个月"，年度另起一句）。
 */
data class YearTally(
    val byColorCount: TemperatureTally,
    val byDayLead: TemperatureTally,
    /** 这一年涂过色的天数；为 0 时界面显示"今年还没有颜色"。 */
    val coloredDays: Int,
) {
    fun tally(mode: StatsMode): TemperatureTally =
        if (mode == StatsMode.BY_COLOR_COUNT) byColorCount else byDayLead
}

fun computeYearTally(entries: List<DayEntry>, year: Int): YearTally {
    val days = entries.filter { it.date.year == year && it.colorIds.isNotEmpty() }
    val counts = countTemperatures(days)
    return YearTally(
        byColorCount = counts.count,
        byDayLead = counts.lead,
        coloredDays = days.size,
    )
}

fun computeMonthStats(entries: List<DayEntry>, yearMonth: YearMonth): MonthStats {
    val days = entries
        .filter { YearMonth.from(it.date) == yearMonth && it.colorIds.isNotEmpty() }
        .sortedBy { it.date }

    val counts = countTemperatures(days)

    return MonthStats(
        yearMonth = yearMonth,
        coloredDays = days,
        byColorCount = counts.count,
        byDayLead = counts.lead,
    )
}
