package com.xinxu.calendar.data.almanac

import com.nlf.calendar.Solar
import java.time.LocalDate

/**
 * 一天里属于「日历」的那部分：农历、节气、节日。
 *
 * 用户 2026-11-08 定的边界：
 * - **不联网、能推算出来的都加**；
 * - **调休不做** —— 它每年都要更新数据，而且太「上班」了，这个产品想偏生活一点。
 *
 * 数据来源：
 * - 农历 / 节气 / 中国传统节日 / 中国公历节日 / 母亲节·父亲节·感恩节：`cn.6tail:lunar`（纯算法）；
 * - 世界与公益日（地球日、环境日…）：本文件里的 [WORLD_DAYS]，一张写死的公历表。
 *
 * 不做的：宜忌、星座、黄历吉凶 —— 这些是「算命」不是「生活」，这一版不掺进来。
 */
data class DayAlmanac(
    /** 农历日，如「十二」「廿三」。 */
    val lunarDay: String,
    /** 农历月，如「九」「冬」「腊」「正」。 */
    val lunarMonth: String,
    /** 当天的节气名，没有就是 null。 */
    val jieQi: String?,
    /** 当天的节日全名，按优先级排序（传统 > 世界公益 > 公历）。 */
    val festivals: List<String>,
    /** 格子里那一行小字；节日优先，其次节气，最后农历。 */
    val cellText: String,
) {
    /**
     * 带月份的完整说法：「十月三十」「九月初一」。
     *
     * 面板用这个，**不能只写「三十」** —— 只看一个「三十」根本分不清是哪个月，
     * 想找这个月的初一还得往前翻（用户 2026-11-08 指出）。
     */
    val lunarText: String
        get() = "${lunarMonth}月${lunarDay}"
}

/**
 * 世界 / 公益日。
 *
 * 这些日子都按**公历固定日期**，不需要任何数据更新。
 * `short` 是格子里用的简称 —— 格子只有一格宽，六七个字会被截成省略号；
 * `name` 留给面板，那里放得下全称。
 */
private data class WorldDay(
    val month: Int,
    val day: Int,
    val name: String,
    val short: String,
)

private val WORLD_DAYS = listOf(
    WorldDay(3, 22, "世界水日", "水日"),    WorldDay(3, 23, "世界气象日", "气象日"),
    WorldDay(4, 7, "世界卫生日", "卫生日"),
    WorldDay(4, 22, "世界地球日", "地球日"),
    WorldDay(4, 23, "世界读书日", "读书日"),
    WorldDay(5, 8, "世界红十字日", "红十字日"),
    WorldDay(5, 12, "国际护士节", "护士节"),
    WorldDay(5, 31, "世界无烟日", "无烟日"),
    WorldDay(6, 5, "世界环境日", "环境日"),
    WorldDay(6, 26, "国际禁毒日", "禁毒日"),
    WorldDay(7, 11, "世界人口日", "人口日"),
    WorldDay(10, 16, "世界粮食日", "粮食日"),
    WorldDay(12, 1, "世界艾滋病日", "艾滋病日"),
    WorldDay(12, 5, "国际志愿者日", "志愿者日"),
    WorldDay(12, 10, "世界人权日", "人权日"),
)

/**
 * 格子里放不下的节日名，换成简称（面板仍然显示全名）。
 * 目前只有「万圣节前夜」：五个字在 9sp 的一格里会被截成省略号，反而看不懂。
 */
private val SHORT_NAMES = mapOf(
    "万圣节前夜" to "万圣夜",
)

object Almanac {

    /**
     * 一次纯计算，没有缓存 —— 调用方自己 remember。
     *
     * 42 个格子各算一次的开销实测可以接受（微秒级）；真到了要优化的那天，
     * 再按月缓存也不迟，别提前把状态塞进来。
     */
    fun of(date: LocalDate): DayAlmanac {
        val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        val lunar = solar.lunar

        val world = WORLD_DAYS.firstOrNull {
            it.month == date.monthValue && it.day == date.dayOfMonth
        }

        // 优先级：传统节日 > 世界/公益 > 公历节日。
        // 「清明」在库里是节气、不进 festivals，所以它会走下面的 fallback，正好。
        val lunarFestivals = lunar.festivals
        val solarFestivals = solar.festivals
        val festivals = buildList {
            addAll(lunarFestivals)
            world?.let { add(it.name) }
            addAll(solarFestivals)
        }.distinct()

        val cellFestival = world?.short
            ?: lunarFestivals.firstOrNull()?.let { SHORT_NAMES[it] ?: it }
            ?: solarFestivals.firstOrNull()?.let { SHORT_NAMES[it] ?: it }

        val lunarDay = lunar.dayInChinese
        val lunarMonth = lunar.monthInChinese
        // 格子里只放得下短形式（「三十」/「十月」）；带月份的「十月三十」留给面板。
        val lunarShort = if (lunarDay == "初一") "${lunarMonth}月" else lunarDay
        val jieQi = lunar.jieQi.ifBlank { null }

        return DayAlmanac(
            lunarDay = lunarDay,
            lunarMonth = lunarMonth,
            jieQi = jieQi,
            festivals = festivals,
            cellText = cellFestival ?: jieQi ?: lunarShort,
        )
    }
}
