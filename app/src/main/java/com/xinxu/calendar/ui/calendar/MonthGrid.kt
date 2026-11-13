package com.xinxu.calendar.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * 一个月的网格模型。
 *
 * **有几行就几行**（4~6 行），不凑满 42 格。首尾用上/下个月的日期补齐，补齐的那几天不显示数字。
 *
 * 2026-10-16 那版是**固定 6 行 × 7 列 = 42 格**，理由是"那样 2 月是 4 行、3 月是 6 行，
 * 翻月时整个页面会上下跳"。2026-10-17 改成按实际行数，因为那条理由的前提已经变了：
 *
 * - 网格是**自然高度**、不参与 `weight` 分配，文字区吃的是"剩下的全部空间"（见 `MonthScreen`）；
 * - 因此翻月时**标题、列头、面板和每一个日期的位置全都不动**，只有"网格底边 / 文字区顶边"
 *   这条分界线会上下挪一行；
 * - 而固定 42 格的代价是**看得见的**：31 天的月份里，10 月要空掉整整一行（约 73dp），
 *   就在屏幕正中间，明晃晃一块没有日期的地。
 *
 * 一句话：**当初拿一条稳定的分界线换一行空白，现在这笔账反过来了。**
 */
data class MonthGrid(
    val yearMonth: YearMonth,
    /** 恰好 `行数 × 7` 个日期，第一格一定是周一。 */
    val cells: List<LocalDate>,
) {
    /** 按周切，每组 7 天，方便一行一行渲染。 */
    val weeks: List<List<LocalDate>> get() = cells.chunked(COLS)

    companion object {
        const val COLS = 7

        /** 一周从周一算起（跟魅族蓝本一致：表头是 一 二 三 四 五 六 日）。 */
        val FIRST_DAY_OF_WEEK: DayOfWeek = DayOfWeek.MONDAY

        fun of(yearMonth: YearMonth): MonthGrid {
            val firstOfMonth = yearMonth.atDay(1)
            // 往前退几天才能让第一格落在周一：周一 → 0，周日 → 6
            val leading = (firstOfMonth.dayOfWeek.value - FIRST_DAY_OF_WEEK.value + 7) % 7
            val gridStart = firstOfMonth.minusDays(leading.toLong())
            // 本月实际占几行：前面的空位 + 本月天数，向上取整。最短 4 行（2 月从周一开始），最长 6 行。
            val rows = (leading + yearMonth.lengthOfMonth() + COLS - 1) / COLS
            val cells = List(rows * COLS) { gridStart.plusDays(it.toLong()) }
            return MonthGrid(yearMonth, cells)
        }
    }
}
