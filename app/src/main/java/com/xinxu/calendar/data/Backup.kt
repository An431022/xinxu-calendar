package com.xinxu.calendar.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * 备份文件的读写。
 *
 * ## 为什么是 JSON、为什么长这样
 *
 * 备份要能**跨版本活下去**，所以文件里写的是**语义**（日期、颜色 id、那句话、颜色值），
 * 而不是 Room 的行结构 —— 表结构以后怎么改（加列、拆表），这个文件都不用动。
 *
 * ```json
 * {
 *   "app": "xinxu-calendar",
 *   "format": 2,
 *   "exportedAt": "2026-10-29T21:45:12+08:00",
 *   "days": [
 *     {
 *       "date": "2026-10-15",
 *       "colors": [
 *         { "id": "warm_rose", "light": "#FFE689B0", "dark": "#FFFFBCD5" }
 *       ],
 *       "notes": { "warm_rose": "发工资啦" }
 *     }
 *   ]
 * }
 * ```
 *
 * ## 四条刻意的决定
 *
 * - **颜色 id 一定在，色值另外写一份（format 2 起）。** id 是身份：冷暖统计看它、
 *   备注归属看它、色板的语义看它。而色值是谁也替代不了的"当时长什么样"——
 *   2026-10-29 起色板可以改，历史必须留下涂色那一刻的颜色（见 [ColorSnapshot]）。
 *   用户当时问的"能不能直接记录颜色编码"，答案就是 `light` / `dark` 两个 hex 字段。
 * - **没有快照的老记录只写 id。** 不硬编一个"当时"出来 —— 那是编造。
 *   导入后这些颜色会回退到当时的色板，跟没备份时一致。
 * - **`notes` 的键就是颜色 id**，包括 [NOTE_WITHOUT_COLOR]（空串）和 `_1`、`_2` 这些
 *   "不归属"槽 —— 备份要能原样还原，就得把内部结构如实写下。
 * - **`format` 是给以后留的门**：读到不认识的数字时明确拒绝，而不是猜着解析、
 *   把用户十年的记录读成半截。format 1（colors 是字符串数组）现在仍然读得懂。
 */
object Backup {

    /** 当前写出的格式。2 = 颜色带 light/dark 快照。 */
    const val FORMAT = 2
    private const val APP_TAG = "xinxu-calendar"

    /**
     * 文件名短到能在系统文件选择器里一眼看完：`xinxu.back-261017.json`。
     *
     * 年月日各留两位（`yyMMdd`）—— 这是个自用的小工具，不会用到 2100 年，
     * 换来的是一眼就知道"这是哪天导的"。
     *
     * 刻意不写时分秒：绝大多数日子一天只导一次，多出来的六位数字只会让文件名更长、
     * 长到在手机那一行里被截断（原来那个 `xinxu-calendar-20261017-034848.json` 就是）。
     * 真在同一天导了第二次，系统会自己加 `(1)` —— 把这个极少数情况交给系统处理，
     * 比让每一天的文件名都为它变长划算。
     */
    fun suggestedFileName(now: OffsetDateTime = OffsetDateTime.now()): String =
        "xinxu.back-" + now.format(DateTimeFormatter.ofPattern("yyMMdd")) + ".json"

    fun encode(entries: List<DayEntry>, now: OffsetDateTime = OffsetDateTime.now()): String {
        val days = JSONArray()
        entries.sortedBy { it.date }.forEach { entry ->
            val notes = JSONObject()
            entry.notes.forEach { (colorId, text) -> if (text.isNotEmpty()) notes.put(colorId, text) }

            val colors = JSONArray()
            entry.colorIds.forEach { id ->
                val snapshot = entry.colorSnapshots[id]
                val item = JSONObject().put("id", id)
                if (snapshot != null) {
                    item.put("light", snapshot.light.toHexColor())
                    item.put("dark", snapshot.dark.toHexColor())
                }
                colors.put(item)
            }

            days.put(
                JSONObject()
                    .put("date", entry.date.toString())
                    .put("colors", colors)
                    .put("notes", notes)
                    // 记下"这一条最后一次被改动是什么时候"（UTC 毫秒）。
                    // 合并时用不到也没关系，留着是为了以后做"从上次备份以来改了什么"。
                    .put("updatedAt", entry.updatedAt),
            )
        }
        return JSONObject()
            .put("app", APP_TAG)
            .put("format", FORMAT)
            .put("exportedAt", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .put("days", days)
            .toString(2)
    }

    /**
     * 读回来。
     *
     * 格式不认识、或者根本不是这个 app 的文件，抛 [IllegalArgumentException] ——
     * 调用方把这句话原样念给用户听就够了，不必自己再编一套措辞。
     *
     * **一行坏数据不该毁掉整份备份**：认不出的日期就跳过那一行，其余照收。
     */
    fun decode(text: String): List<DayEntry> {
        val root = try {
            JSONObject(text)
        } catch (_: Exception) {
            throw IllegalArgumentException("这不是一个 JSON 文件")
        }
        if (root.optString("app") != APP_TAG) {
            throw IllegalArgumentException("这不是心绪日历的备份文件")
        }
        val format = root.optInt("format", 0)
        if (format <= 0 || format > FORMAT) {
            throw IllegalArgumentException("备份格式是 $format，这个版本读不了")
        }

        val days = root.optJSONArray("days") ?: JSONArray()
        val out = ArrayList<DayEntry>(days.length())
        for (i in 0 until days.length()) {
            val day = days.optJSONObject(i) ?: continue
            val date = try {
                LocalDate.parse(day.optString("date"))
            } catch (_: Exception) {
                continue
            }

            val colorIds = ArrayList<String>()
            val snapshots = LinkedHashMap<String, ColorSnapshot>()
            day.optJSONArray("colors")?.let { arr ->
                for (j in 0 until arr.length()) {
                    when (val element = arr.opt(j)) {
                        // format 1：纯 id 字符串。没有快照，导入后回退当时色板。
                        is String -> if (element.isNotEmpty()) colorIds += element
                        // format 2：{ id, light, dark }。
                        is JSONObject -> {
                            val id = element.optString("id")
                            if (id.isEmpty()) continue
                            colorIds += id
                            val light = parseHexColor(element.optString("light"))
                            if (light != null) {
                                snapshots[id] = ColorSnapshot(
                                    light = light,
                                    dark = parseHexColor(element.optString("dark")) ?: light,
                                )
                            }
                        }
                    }
                }
            }

            val notes = LinkedHashMap<String, String>()
            day.optJSONObject("notes")?.let { obj ->
                obj.keys().forEach { key ->
                    val value = obj.optString(key)
                    if (value.isNotEmpty()) notes[key] = value
                }
            }

            val entry = DayEntry(
                date = date,
                colorIds = colorIds,
                colorSnapshots = snapshots,
                notes = notes,
                updatedAt = day.optLong("updatedAt", 0L),
            )
            if (!entry.isEmpty) out += entry
        }
        return out
    }

    /** `#AARRGGBB`。补到 8 位，方便人直接看懂这是哪个颜色。 */
    private fun Long.toHexColor(): String = "#%08X".format(this and 0xFFFFFFFFL)

    /**
     * 读 `#RRGGBB` / `#AARRGGBB`（`#` 可有可无）。读不懂返回 null。
     *
     * 6 位时补上不透明的 alpha —— 手写备份文件的人经常只写 RGB。
     */
    private fun parseHexColor(raw: String): Long? {
        val hex = raw.trim().removePrefix("#")
        if (hex.length != 6 && hex.length != 8) return null
        val value = hex.toLongOrNull(16) ?: return null
        return if (hex.length == 6) value or 0xFF000000L else value
    }
}
