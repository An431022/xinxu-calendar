package com.xinxu.calendar.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.kyant.hct.HctUtils

/**
 * 用户对十二色的**覆盖**（调色盘 → 保存）。
 *
 * ## 为什么单独放一份
 *
 * [com.xinxu.calendar.data.Palette] 里的十二色是**出厂默认**，是数据、也是生成脚本的产物。
 * 用户改色不该去改那份常量（那是编译期的），也不该让"默认值"和"我现在用的值"混在一起。
 * 所以覆盖单独存在 SharedPreferences 里：出厂值永远在，用户的改动是一层可撤掉的覆盖。
 *
 * ## 存什么
 *
 * 只存**浅色值**（`#FFE689B0` 这种 ARGB）。深色值不存，显示时按色板原本的规则算 ——
 * 色相、彩度不变，tone + 15（上限 88）。这样两个模式仍然是"同一个颜色的两种明度"，
 * 而不是两个互不相干的颜色（跟 Palette 的口径一致）。
 *
 * ## 什么时候加载
 *
 * `MainActivity.onCreate` 里调一次 [ensureLoaded]。界面读 [overrides] 是 Compose 状态读，
 * 加载完会自己重组，不需要监听。
 *
 * ## 和老记录的关系
 *
 * **没有关系**：已涂的日子存了涂色当时的快照（见 `ColorSnapshot`），改这里只影响
 * "以后新涂的颜色"和色板本身的样子（取色盘、统计页的示意色）。
 */
object PaletteOverrideStore {

    private const val PREFS = "palette_overrides"
    private const val KEY_PREFIX = "color_"
    private const val NAME_PREFIX = "name_"

    private val _overrides = mutableStateOf<Map<String, Long>>(emptyMap())

    /** 颜色 id -> 覆盖后的浅色 ARGB。Compose 状态：改了会触发重组。 */
    val overrides: Map<String, Long> get() = _overrides.value

    private val _names = mutableStateOf<Map<String, String>>(emptyMap())

    /** 颜色 id -> 用户起的名字。空表示没起过（显示时再按规则回退）。 */
    val names: Map<String, String> get() = _names.value

    private var loaded = false

    /** 从磁盘读一次。重复调用无副作用。 */
    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val colors = LinkedHashMap<String, Long>()
        val names = LinkedHashMap<String, String>()
        sp.all.forEach { (key, value) ->
            when {
                key.startsWith(KEY_PREFIX) -> {
                    val argb = (value as? Long)?.let { it and 0xFFFFFFFFL } ?: return@forEach
                    colors[key.removePrefix(KEY_PREFIX)] = argb
                }
                key.startsWith(NAME_PREFIX) -> {
                    val name = value as? String
                    if (!name.isNullOrBlank()) names[key.removePrefix(NAME_PREFIX)] = name
                }
            }
        }
        _overrides.value = colors
        _names.value = names
    }

    /** 保存一个颜色。只写浅色值，深色值按 [darkOf] 算。 */
    fun set(context: Context, id: String, light: Long) {
        val value = light and 0xFFFFFFFFL
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_PREFIX + id, value)
            .apply()
        _overrides.value = _overrides.value + (id to value)
    }

    /**
     * 给一个颜色起名。传 null 或空串就是删掉名字。
     *
     * 名字**只影响显示**：历史记录、统计、备注归属全都认 id，不认名字。
     */
    fun setName(context: Context, id: String, name: String?) {
        val trimmed = name?.trim().orEmpty()
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (trimmed.isEmpty()) editor.remove(NAME_PREFIX + id) else editor.putString(NAME_PREFIX + id, trimmed)
        editor.apply()
        _names.value = if (trimmed.isEmpty()) _names.value - id else _names.value + (id to trimmed)
    }

    /** 还原这一个颜色槽（删掉覆盖和名字，回到出厂值）。 */
    fun clear(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PREFIX + id)
            .remove(NAME_PREFIX + id)
            .apply()
        _overrides.value = _overrides.value - id
        _names.value = _names.value - id
    }

    /** 全部还原。 */
    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        _overrides.value = emptyMap()
        _names.value = emptyMap()
    }

    /**
     * 浅色值 -> 深色值：色相、彩度不变，tone + 15，上限 88；**alpha 原样保留**。
     *
     * 这条规则跟 Palette 的深色色板是同一套（"同一个颜色的两种明度"）。
     * HCT 库不可用、或者算不出来时**原样返回浅色值** —— 宁可深色模式下暗一点，
     * 也不要因为一个 JNI 调用失败让色板崩掉。
     */
    fun darkOf(light: Long): Long {
        val alpha = light and 0xFF000000L
        val opaque = light or 0xFF000000L
        val hct = runCatching { HctUtils.hctFromInt(opaque.toInt()) }.getOrNull() ?: return light
        if (hct.size < 3) return light
        val tone = (hct[2] + 15.0).coerceAtMost(88.0)
        val argb = runCatching { HctUtils.hctToInt(hct[0], hct[1], tone) }.getOrNull() ?: return light
        return alpha or (argb.toLong() and 0xFFFFFFL)
    }
}
