package com.xinxu.calendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xinxu.calendar.data.ColorSnapshot
import com.xinxu.calendar.data.DayEntry
import com.xinxu.calendar.data.Palette
import com.xinxu.calendar.data.PaletteColor

/**
 * 取某个色板颜色在**当前明暗模式**下该用的那一个色值。
 *
 * 十二色的 light / dark 是两套各自算过的独立取值（深色模式走 `tone + 15` 重新生成，
 * 不是把浅色调暗或加透明度），所以任何要画颜色的地方都必须走这个函数，
 * 不要直接写 `color.light` —— 否则深色模式下会得到一堆发闷的暗块。
 *
 * 2026-10-29 起还要叠一层**用户覆盖**：调色盘里改过、保存过的颜色优先。
 */
@Composable
fun PaletteColor.currentColor(): Color = if (isSystemInDarkTheme()) currentDark() else currentLight()

/** 这个颜色**现在**的浅色值（用户覆盖优先，否则出厂值）。 */
@Composable
fun PaletteColor.currentLight(): Color =
    PaletteOverrideStore.overrides[id]?.let { Color(it.toInt()) } ?: light

/** 这个颜色**现在**的深色值。覆盖过的按 tone + 15 现算，没覆盖的直接用出厂深色值。 */
@Composable
fun PaletteColor.currentDark(): Color {
    val override = PaletteOverrideStore.overrides[id] ?: return dark
    return Color(PaletteOverrideStore.darkOf(override).toInt())
}

/**
 * 这个颜色现在**叫什么**。
 *
 * 三条规则，按优先级：
 * 1. 用户在调色盘里起过名字 → 用那个名字；
 * 2. 没起名、但颜色被改过 → 「自定义」（用户 2026-10-29 的要求：改了色还顶着「雾紫」这个名字
 *    会误导人）；
 * 3. 既没改名也没改色 → 出厂名字。
 *
 * 名字**只影响显示**。历史记录、冷暖统计、备注归属都认 id，不认名字。
 */
@Composable
fun PaletteColor.displayName(): String {
    val custom = PaletteOverrideStore.names[id]
    if (!custom.isNullOrBlank()) return custom
    return if (PaletteOverrideStore.overrides.containsKey(id)) "自定义" else name
}

/**
 * 一条记录里某个颜色**当时**的样子。
 *
 * 有快照用快照 —— 这样改色板时，已经涂过的日子不会跟着变（用户 2026-10-29 定下的语义）。
 * 没有快照的是"快照制度之前"涂的，回退到当前色板：那是这些记录还能追溯到的、
 * 最接近当时的颜色。
 *
 * 返回 `null` 表示记录里的颜色 id 在当前版本里已经不存在了（色板删过色），
 * 调用方要能接受"这一格画不出来"。
 */
@Composable
fun recordColor(entry: DayEntry?, id: String): Color? {
    val snapshot = entry?.colorSnapshots?.get(id)
    if (snapshot != null) {
        return if (isSystemInDarkTheme()) Color(snapshot.dark.toInt()) else Color(snapshot.light.toInt())
    }
    return Palette.byId(id)?.currentColor()
}

/**
 * 这个颜色**此刻**的 light/dark 快照 —— 涂色时随记录一起存下来。
 *
 * 存两个值而不是一个：深色模式用的是另一套明度，只存浅色的话，切模式时
 * 历史颜色就得按当前色板重算，等于又让它跟着色板走了。
 */
@Composable
fun PaletteColor.currentSnapshot(): ColorSnapshot = ColorSnapshot(
    light = currentLight().toArgb().toLong() and 0xFFFFFFFFL,
    dark = currentDark().toArgb().toLong() and 0xFFFFFFFFL,
)
