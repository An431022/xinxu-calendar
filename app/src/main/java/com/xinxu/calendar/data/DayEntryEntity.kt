package com.xinxu.calendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

/**
 * `day_entry` 表的行。
 *
 * 为什么不直接把 [DayEntry] 标成 `@Entity`：模型层不该认识数据库。
 * 表结构将来会为了查询效率调整（加索引、加列、把颜色拆成子表），
 * 而 [DayEntry] 是界面和统计认的形状 —— 让两者之间隔一层显式的映射，
 * 改表的时候就不会顺手把界面也改坏。
 *
 * ## `note` 这一列装的是 JSON（v2 起）
 *
 * 备注从"一天一句"改成"每个颜色各一句"之后，按理该加一列。但这次**故意没加**：
 * `note` 列的类型本来就是 `TEXT`，把内容从纯文本换成 JSON 对象，
 * **表结构一个字都没变** —— Room 察觉不到，我们就不需要写迁移。
 *
 * 代价是旧数据（v1 存的纯文本）和新数据混在同一列里，得靠 [decodeNotes] 兼容。
 * 换来的是：升级路径上**没有任何一步会碰用户的记录**。
 * 一本记录"足迹"的应用，迁移代码本身就是最大的风险源，能不写就不写。
 *
 * ## `colorIds` 这一列装的是"id@浅色@深色"（2026-10-29 起）
 *
 * 色板改成可编辑之后，历史必须留下**涂色当时**的颜色（见 [ColorSnapshot]）。
 * 和 `note` 同一个手法：列名、类型都不变，只是内容多带两段色值：
 *
 * ```
 * warm_rose@FFE689B0@FFFFBCD5,cool_sky@FF53B2C7@FF7FDCF2
 * ```
 *
 * 老数据是纯 `warm_rose,cool_sky`，解析时自动认成"没有快照"，显示回退当前色板。
 * **没有加列，也就没有迁移。**
 */
@Entity(tableName = "day_entry")
data class DayEntryEntity(
    /** ISO-8601，如 `"2026-10-03"`。同时也是主键：一天只有一条记录。 */
    @PrimaryKey val date: String,
    /**
     * 有序的颜色，逗号分隔，顺序＝用户点击先后。
     * 每段是 `id`（老数据）或 `id@lightARGB@darkARGB`（2026-10-29 起）。
     */
    val colorIds: String,
    /**
     * v2 起是 `{"warm_amber":"发工资啦","cool_deep":"雨好大"}` 这样的 JSON；
     * v1 遗留的是**纯文本**，由 [decodeNotes] 兜住。
     */
    val note: String?,
    val updatedAt: Long,
)

/** `,`。颜色 id 都是我们自己起的（`warm_rose`、`cool_sky`…），不含逗号，分割是安全的。 */
private const val SEPARATOR = ","

/** `@`。颜色 id 不含它，色值是十六进制，也不会含。 */
private const val SNAPSHOT_MARK = "@"

fun DayEntryEntity.toModel(): DayEntry {
    val (ids, snapshots) = decodeColors(colorIds)
    return DayEntry(
        // LocalDate.toString() / parse() 用的就是 ISO-8601，跟表里存的格式是同一个，不用额外格式化。
        date = LocalDate.parse(date),
        colorIds = ids,
        colorSnapshots = snapshots,
        notes = decodeNotes(note),
        updatedAt = updatedAt,
    )
}

fun DayEntry.toEntity(): DayEntryEntity = DayEntryEntity(
    date = date.toString(),
    colorIds = encodeColors(colorIds, colorSnapshots),
    note = encodeNotes(notes),
    updatedAt = updatedAt,
)

/**
 * 把列里的字符串读成「有序 id 列表 + id 对应的色值快照」。
 *
 * 一段里没有 `@` 就是老数据：只有 id，没有快照，显示时回退当前色板。
 * 有 `@` 但十六进制解析不出来（文件被手改坏了）也按"没有快照"处理 ——
 * 一条坏数据不该让整个月的记录读不出来。
 */
private fun decodeColors(raw: String): Pair<List<String>, Map<String, ColorSnapshot>> {
    if (raw.isEmpty()) return emptyList<String>() to emptyMap()

    val ids = ArrayList<String>()
    val snapshots = LinkedHashMap<String, ColorSnapshot>()
    raw.split(SEPARATOR).forEach { token ->
        if (token.isEmpty()) return@forEach
        val parts = token.split(SNAPSHOT_MARK)
        val id = parts[0]
        if (id.isEmpty()) return@forEach
        ids += id
        if (parts.size >= 2) {
            val light = parts[1].toLongOrNull(16)
            if (light != null) {
                val dark = parts.getOrNull(2)?.toLongOrNull(16) ?: light
                snapshots[id] = ColorSnapshot(light = light, dark = dark)
            }
        }
    }
    return ids to snapshots
}

/** [decodeColors] 的反向操作。没有快照的颜色只写 id，保持老格式。 */
private fun encodeColors(ids: List<String>, snapshots: Map<String, ColorSnapshot>): String =
    ids.filter { it.isNotEmpty() }.joinToString(SEPARATOR) { id ->
        val snapshot = snapshots[id] ?: return@joinToString id
        "$id$SNAPSHOT_MARK${snapshot.light.toHex()}$SNAPSHOT_MARK${snapshot.dark.toHex()}"
    }

/** 8 位大写十六进制。补零，写进列里长度固定。 */
private fun Long.toHex(): String = "%08X".format(this and 0xFFFFFFFFL)

/**
 * 把列里的字符串读成 `颜色 id -> 备注`。
 *
 * 两条路：能当 JSON 解析就按 JSON 走；解析不了，说明这是 v1 留下的纯文本
 * （那时候还没有"按颜色分组"这回事），整句归到 [NOTE_WITHOUT_COLOR] 下。
 *
 * 这里**必须**兜住异常，不能让它冒出去：升级后第一次启动就会读这张表，
 * 一个 `JSONException` 直接把用户 15 天的记录顶掉。
 */
private fun decodeNotes(raw: String?): Map<String, String> {
    if (raw.isNullOrEmpty()) return emptyMap()

    return try {
        val json = JSONObject(raw)
        buildMap {
            json.keys().forEach { key ->
                val text = json.optString(key)
                // 空串等于没写。存进去的话标签条上会冒出一个空盒子。
                if (text.isNotEmpty()) put(key, text)
            }
        }
    } catch (_: JSONException) {
        mapOf(NOTE_WITHOUT_COLOR to raw)
    }
}

/** [decodeNotes] 的反向操作。全空就写 `null`，别在列里留一个 `"{}"`。 */
private fun encodeNotes(notes: Map<String, String>): String? {
    val kept = notes.filterValues { it.isNotEmpty() }
    if (kept.isEmpty()) return null
    return JSONObject(kept).toString()
}
