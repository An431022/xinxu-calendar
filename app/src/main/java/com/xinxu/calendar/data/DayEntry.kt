package com.xinxu.calendar.data

import java.time.LocalDate

/**
 * 涂色**那一刻**留下的色值快照。
 *
 * 2026-10-29 定下的语义：**改色板，不影响已经涂过的日子。**
 * 用户原话是「过去的日子永远留下当时的颜色」——所以每次涂色都把当时的色值抄一份进记录，
 * 以后色板怎么改，历史都保持原样。
 *
 * ## 为什么存两个值
 *
 * 深色模式显示的是另一套明度（tone + 15 重新生成）。只存浅色那一个的话，切到深色模式时
 * 历史颜色就得靠"当前色板"再算一遍 —— 那等于又让它跟着色板走，正好违背这条语义。
 */
data class ColorSnapshot(
    /** 涂色时浅色模式下的 ARGB（0..0xFFFFFFFF）。 */
    val light: Long,
    /** 涂色时深色模式下的 ARGB。 */
    val dark: Long,
)

/**
 * 一天的心情记录。
 *
 * 数据库里对应一张 `day_entry` 表：
 * ```
 * date TEXT PRIMARY KEY   -- ISO "2026-10-03"
 * colorIds TEXT           -- 有序、逗号分隔；每段是 `id` 或 `id@lightARGB@darkARGB`
 * note TEXT               -- 可空。v2 起内容是一个 JSON 对象，见 [DayEntryEntity]
 * updatedAt INTEGER
 * ```
 *
 * ## 颜色为什么还要存快照
 *
 * v0.1 的约定是"只存颜色 id，色值在 [Palette] 里，改色值不会动到历史记录"。
 * 但 2026-10-29 色板真的可以改了（调色盘 → 保存），这条约定就不够了：
 * 只存 id 的话，改一次「玫瑰」的色值，**所有**涂过玫瑰的日子会一起变色。
 *
 * 现在的规则：每条记录把涂色当时的 light / dark 各存一份（[colorSnapshots]）。
 * id 仍然是身份的来源（冷暖统计、备注归属都看它），快照只负责"当时长什么样"。
 * 老记录没有快照时回退到当前色板 —— 启动时会尽量补一份（见 `DayEntryStore`）。
 *
 * ## 备注为什么按颜色分组
 *
 * 一天可以有好几个颜色，而它们往往**互相打架** —— 上午「琥珀」是发工资的松快，
 * 晚上「深海」是淋了雨的闷。这种日子用一句话概括不了：随便写哪一句，
 * 另一句就没地方待了；写成一大段，两个颜色又都找不着自己。
 *
 * 所以 v2 把备注改成 [notes]：**颜色 id -> 那句话**。一个颜色最多配一句
 * （想写长就写长，界面上会折叠，见计划 8.6），互不挤占。
 */
data class DayEntry(
    val date: LocalDate,
    val colorIds: List<String> = emptyList(),
    /**
     * 颜色 id -> 涂色当时的色值。没有键表示"这一色是色板快照制度之前涂的"，
     * 显示时回退到当前色板。
     */
    val colorSnapshots: Map<String, ColorSnapshot> = emptyMap(),
    /**
     * 颜色 id -> 备注。键为 [NOTE_WITHOUT_COLOR] 的那条表示**不归属任何颜色**。
     *
     * 为什么允许"没有颜色的备注"：留白本身也是一种表达。一天什么都没涂，
     * 但想留一句话，这是正当的用法，不该被数据结构挡住。
     */
    val notes: Map<String, String> = emptyMap(),
    val updatedAt: Long = 0L,
) {
    /** 没涂色、也没写字的日子。留白是一种正常状态，不是"缺数据"。 */
    val isEmpty: Boolean get() = colorIds.isEmpty() && notes.isEmpty()
}

/**
 * 「不归属任何颜色」的备注用的键。
 *
 * 用空串而不是 `null`：`Map` 的键可以是空串，用起来不用到处判空；
 * 而且它落在 JSON 里就是 `{"": "……"}`，一眼能看出是个特殊槽位。
 */
const val NOTE_WITHOUT_COLOR: String = ""

/**
 * 第二个及以后「不归属任何颜色」的槽用 `_1`、`_2`… 做键。
 *
 * ## 为什么允许不止一个无归属槽
 *
 * 取消一个颜色时，它那句备注得有个去处。只有一个无归属槽时，那个槽一旦被占，
 * 这句就只能**留在已经取消的颜色底下** —— 字是不丢了，可它挂在一个"这天没涂"的
 * 颜色上，读起来别扭。槽位不唯一之后，"颜色撤了、话不想撤"永远有地方落。
 *
 * 你 2026-10-17 那个比喻很准：**这是一层缓存**。
 *
 * ## 不用写 Room 迁移
 *
 * 键仍然只是字符串，老数据里的 `{"": "……"}` 原样读得懂 —— 这正是当初定
 * "换内容不换类型"留的后路。颜色 id 都是 `warm_*` / `cool_*` 这种形状，
 * 不会以 `_` 开头，两类键撞不上。
 */
private const val COLORLESS_SLOT_PREFIX = "_"

/** 这个键是不是「不归属任何颜色」的槽（含 [NOTE_WITHOUT_COLOR] 与 `_1`、`_2`…）。 */
fun isColorlessSlot(id: String): Boolean =
    id == NOTE_WITHOUT_COLOR || (id.length > 1 && id.startsWith(COLORLESS_SLOT_PREFIX))

/**
 * 下一个还空着的无归属槽：[NOTE_WITHOUT_COLOR] 优先，然后是 `_1`、`_2`…
 *
 * 有意**回收空洞**：中间某条被清空之后，下一次会重新用上那个位置，
 * 无归属槽因此总是从前往后排满，不会越用越散。
 */
fun nextFreeColorlessSlot(notes: Map<String, String>): String {
    if (notes[NOTE_WITHOUT_COLOR].isNullOrEmpty()) return NOTE_WITHOUT_COLOR
    var i = 1
    while (!notes["$COLORLESS_SLOT_PREFIX$i"].isNullOrEmpty()) i++
    return "$COLORLESS_SLOT_PREFIX$i"
}

/**
 * 这一天**已经放着字**的无归属槽，按 [NOTE_WITHOUT_COLOR]、`_1`、`_2`… 的顺序。
 */
fun colorlessSlotsInUse(notes: Map<String, String>): List<String> = buildList {
    if (!notes[NOTE_WITHOUT_COLOR].isNullOrEmpty()) add(NOTE_WITHOUT_COLOR)
    var i = 1
    while (!notes["$COLORLESS_SLOT_PREFIX$i"].isNullOrEmpty()) {
        add("$COLORLESS_SLOT_PREFIX$i")
        i++
    }
}
