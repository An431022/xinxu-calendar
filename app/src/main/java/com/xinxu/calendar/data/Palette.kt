package com.xinxu.calendar.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 颜色的冷暖归属。统计页要把「这个月偏暖还是偏冷」算出来，靠的就是这个字段。
 * NEUTRAL 单独计数，不混进冷暖对比。
 */
enum class ColorTemperature(val label: String) {
    WARM("暖"),
    COOL("冷"),
    NEUTRAL("中性"),
}

/**
 * 一个可选颜色。
 *
 * [id] 一旦发布就**永久不变**：用户的记录里存的是颜色 id 而不是色值，
 * 改了 id 等于把历史记录里的颜色弄丢。色值可以微调，id 不行。
 */
data class PaletteColor(
    val id: String,
    val name: String,
    val meaning: String,
    val light: Color,
    val dark: Color,
    val temperature: ColorTemperature,
)

/**
 * 十二色固定色板。
 *
 * 全部由 Google Material 3 的 HCT 色彩空间（Hue 色相 / Chroma 彩度 / Tone 感知明度）算出，
 * 守三条硬约束，目的是「不刺眼」：
 *   1. 彩度 chroma <= 48 —— Material 色调板的实际上限，再高就开始尖锐
 *      （例外：朱红 C79，用户指定，见下方注释）
 *   2. 明度 tone 52..78 —— 白底上看得清，又不至于压住日期数字
 *      （例外：石墨 T44 是有意的，它代表「沉重、黑暗」）
 *   3. 色相分布 暖 5 / 冷 5 / 中性 2 —— 冷暖统计要对半开才公平
 * 这些约束不是靠人眼把关，`tools/palette/generate.mjs` 每次生成都会自检并打印破例项。
 *
 * 深色模式不是重新挑色，而是把 tone 抬到 min(88, tone + 15)，
 * 色相与彩度保持不变 —— 于是同一个 id 在浅色/深色下是「同一个颜色的两种明度」，
 * 而不是两个不相干的颜色。
 *
 * ## 2026-10-29 起：默认值可以被用户覆盖
 *
 * 下面这十二组 light/dark 仍然是**出厂默认**，由 tools/palette/generate.mjs 生成。
 * 但调色盘里现在可以改色并保存（存进 SharedPreferences，见 `PaletteOverrideStore`），
 * 改完只影响"以后新涂的颜色"—— 历史记录里存着涂色当时的快照，不会跟着变。
 * 所以想调整**默认**色板，还是改脚本；想调整**自己的**色板，用调色盘。
 *
 * 色值由 tools/palette/generate.mjs 生成，改默认色板请改那个脚本再回填，不要手调。
 */
object Palette {

    val colors: List<PaletteColor> = listOf(
        PaletteColor(
            id = "warm_rose",
            name = "玫瑰",
            meaning = "心动、柔软",
            light = Color(0xFFE689B0),
            dark = Color(0xFFFFBCD5),
            temperature = ColorTemperature.WARM,
        ),
        PaletteColor(
            id = "warm_flame",
            name = "朱红",
            meaning = "热烈、激动、生气",
            // 用户 2026-10-03 指定的色值，彩度 C79 —— 有意突破 48 的上限。
            // 理由：「热烈、激动、生气」本来就该是整块色板里最响的那个，
            // 而这一味如果也压到 48，出来是偏暗的砖红，反而比原来更「素」。
            light = Color(0xFFED5126),
            dark = Color(0xFFFF9072),
            temperature = ColorTemperature.WARM,
        ),
        PaletteColor(
            id = "warm_honey",
            name = "蜜橘",
            meaning = "充实、有干劲",
            light = Color(0xFFEB9046),
            dark = Color(0xFFFFC298),
            temperature = ColorTemperature.WARM,
        ),
        PaletteColor(
            id = "warm_amber",
            name = "琥珀",
            meaning = "开心、轻快",
            light = Color(0xFFCFA63C),
            dark = Color(0xFFFCCF61),
            temperature = ColorTemperature.WARM,
        ),
        PaletteColor(
            id = "warm_caramel",
            name = "焦糖",
            meaning = "踏实、沉稳",
            light = Color(0xFFA86E55),
            dark = Color(0xFFD49479),
            temperature = ColorTemperature.WARM,
        ),
        PaletteColor(
            id = "cool_moss",
            name = "苔绿",
            meaning = "满足、缓慢",
            light = Color(0xFF7A9364),
            dark = Color(0xFFA1BB89),
            temperature = ColorTemperature.COOL,
        ),
        PaletteColor(
            id = "cool_mint",
            name = "薄荷",
            meaning = "清爽、放松",
            light = Color(0xFF6FB398),
            dark = Color(0xFF97DDC0),
            temperature = ColorTemperature.COOL,
        ),
        PaletteColor(
            id = "cool_sky",
            name = "晴空",
            meaning = "平静、清醒",
            light = Color(0xFF53B2C7),
            dark = Color(0xFF7FDCF2),
            temperature = ColorTemperature.COOL,
        ),
        PaletteColor(
            id = "cool_deep",
            name = "深海",
            meaning = "低落、沉思",
            light = Color(0xFF4F7FB5),
            dark = Color(0xFF77A6DF),
            temperature = ColorTemperature.COOL,
        ),
        PaletteColor(
            id = "cool_mist",
            name = "雾紫",
            meaning = "忧郁、敏感",
            light = Color(0xFFAE9ED1),
            dark = Color(0xFFD7C7FC),
            temperature = ColorTemperature.COOL,
        ),
        PaletteColor(
            id = "neutral_cloud",
            name = "云灰",
            meaning = "平淡、记不清",
            light = Color(0xFFBFC1C7),
            dark = Color(0xFFDBDDE2),
            temperature = ColorTemperature.NEUTRAL,
        ),
        PaletteColor(
            id = "neutral_ink",
            name = "石墨",
            meaning = "沉重、黑暗",
            light = Color(0xFF636870),
            dark = Color(0xFF8A8E96),
            temperature = ColorTemperature.NEUTRAL,
        ),
    )

    private val byId: Map<String, PaletteColor> = colors.associateBy { it.id }

    /** 按 id 取色；id 不存在时返回 null，调用方要能接受「记录里的颜色没了」这种情况。 */
    fun byId(id: String): PaletteColor? = byId[id]
}

/**
 * 这个颜色的**出厂**快照 —— 给「快照制度之前」涂过的记录补色用。
 *
 * 注意它取的是 `Palette` 里的原始值，不看用户覆盖：补的是"这些记录还能追溯到的、
 * 最接近当时的颜色"，也就是升级那一刻的出厂值。
 */
fun PaletteColor.defaultSnapshot(): ColorSnapshot = ColorSnapshot(
    light = light.toArgb().toLong() and 0xFFFFFFFFL,
    dark = dark.toArgb().toLong() and 0xFFFFFFFFL,
)
