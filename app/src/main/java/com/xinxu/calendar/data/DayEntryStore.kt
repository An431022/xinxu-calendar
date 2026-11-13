package com.xinxu.calendar.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * 全应用唯一的数据入口。界面只跟它打交道，不认识 Room。
 *
 * ## 为什么是全量载入内存
 *
 * 一天最多一条记录，十年也才三千多行。**全量读进内存**（启动时一次查询）之后，
 * 月视图翻页、统计聚合都变成内存操作，不需要为每种问法写一条 SQL，
 * 也不会出现"这个月读到了、上个月还没读到"这种一半数据的状态。
 *
 * 数据集真的大到不适合全量载入的时候（比如加入搜索全部备注），
 * 再改成按需查询 —— [DayEntryDao] 里已经备好了 `ofMonth` / `between` 两条查询。
 *
 * ## 写入策略
 *
 * 先改内存、再异步落盘（write-behind）。这样点一下颜色，色条**立刻**变，
 * 不用等磁盘；而落盘失败最多丢最后一次点击，不会卡住界面。
 *
 * 用 `mutableStateMapOf` 而不是普通 MutableMap：它是 Compose 的快照状态，
 * 写入会让读了它的 Composable 自动重组 —— 否则涂完颜色色条不会变。
 */
/** 只用来记"一次性迁移做没做过"，不放任何记录数据。 */
private const val STORE_PREFS = "day_entry_store"
private const val KEY_SNAPSHOT_BACKFILLED = "snapshot_backfilled"

class DayEntryStore(context: Context) {

    private val dao = XinxuDatabase.get(context).dayEntryDao()

    /** 只给"快照制度上线时的一次性补全"用（见 backfillSnapshots）。 */
    private val prefs = context.getSharedPreferences(STORE_PREFS, Context.MODE_PRIVATE)

    // **必须是 Main.immediate，不能是 Dispatchers.IO。**
    //
    // 这里踩过一次真坑：原先用 IO 作用域，加载完直接改 `entries`。当加载**快于**首帧组合时
    // 一切正常；一旦加载**慢于**首帧（库大一点、WAL 长一点就会），首帧读到的是空 map，
    // 而那次来自后台线程的写入没能让界面重组 —— 结果是磁盘里明明有 8 条记录，
    // 日历上却一条颜色都不显示，而且再也不会自己好，因为之后没有任何新的写入来触发重组。
    //
    // 快照状态一律在主线程写，IO 只用来跑查询。
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val entries = mutableStateMapOf<LocalDate, DayEntry>()

    /** 首次从磁盘读完之后才为 true。 */
    var ready by mutableStateOf(false)
        private set

    init {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { dao.all() }
            loaded.forEach { entries[LocalDate.parse(it.date)] = it.toModel() }
            backfillSnapshots()
            ready = true
        }
    }

    /**
     * 给"快照制度之前"涂过的日子补一份颜色。
     *
     * 2026-10-29 之前记录只存 id，不存色值 —— 那时候色板不能改，所以"当前色板"就是
     * "当时颜色"，永远对得上。现在色板可以改了，这些老记录如果不补，用户一改色板，
     * 过去的日子就会跟着变色，正好违背他要的语义。
     *
     * 补的是**升级这一刻**的色板值：那是这些记录还能追溯到的、最接近"当时"的颜色。
     * 补完就落盘，幂等 —— 中途失败下次启动会接着补，已经补过的不会重复写。
     *
     * 为什么放在 `ready = true` 之前：界面第一次读到这些记录时就该带着快照，
     * 不然首帧会先按"无快照"渲染一次，等补完再跳一下颜色。
     */
    private fun backfillSnapshots() {
        // **只做一次**：这是"快照制度上线时"的一次性迁移，不是每次启动都跑的补丁。
        //
        // 每次启动都补的话，将来任何一条写路径漏了快照，都会被这里用出厂值悄悄"补好"：
        // 颜色看起来是回退了，而不是暴露出 bug。2026-10-30 那个"写了备注之后颜色回退"
        // 就是这么被掩盖了一整天 —— 根因是 setNote 漏了快照，表现得却像"历史语义错了"。
        if (prefs.getBoolean(KEY_SNAPSHOT_BACKFILLED, false)) return

        entries.forEach { (date, entry) ->
            if (entry.colorIds.isEmpty()) return@forEach
            val missing = entry.colorIds.filter { it !in entry.colorSnapshots }
            if (missing.isEmpty()) return@forEach

            val added = missing.mapNotNull { id ->
                Palette.byId(id)?.let { id to it.defaultSnapshot() }
            }
            if (added.isEmpty()) return@forEach

            val updated = entry.copy(colorSnapshots = entry.colorSnapshots + added)
            entries[date] = updated
            scope.launch { withContext(Dispatchers.IO) { dao.upsert(updated.toEntity()) } }
        }

        prefs.edit().putBoolean(KEY_SNAPSHOT_BACKFILLED, true).apply()
    }

    fun colorIdsOf(date: LocalDate): List<String> = entries[date]?.colorIds.orEmpty()

    /** 这天涂色时留下的色值快照（`id -> light/dark`）。老记录可能是空的。 */
    fun colorSnapshotsOf(date: LocalDate): Map<String, ColorSnapshot> =
        entries[date]?.colorSnapshots.orEmpty()

    fun entryOf(date: LocalDate): DayEntry? = entries[date]

    /** 某个月的全部记录，给统计页用。按日期升序。 */
    fun entriesOf(yearMonth: java.time.YearMonth): List<DayEntry> =
        entries.values
            .filter { java.time.YearMonth.from(it.date) == yearMonth }
            .sortedBy { it.date }

    /** 全部记录，按日期升序。导出备份用。 */
    fun allEntries(): List<DayEntry> = entries.values.sortedBy { it.date }

    /**
     * 把一份备份合并进来，返回**真正动过**的天数。
     *
     * 三条规则都朝"不丢东西"的方向定：
     *
     * - **颜色取并集。** 本地涂了、备份里没有的颜色留着 —— 颜色是轻量的，多点一个不痛；
     *   反过来，"导入一次备份反而少了几个颜色"才让人不敢再用这个功能。
     * - **备注按槽覆盖，但只覆盖备份里真有字的槽。** 一句话只能有一个版本，备份既然是
     *   用户主动拿来恢复的，就以它为准；本地独有的槽原样留着，不会被一次导入抹掉。
     * - **`updatedAt` 取两者较大的那个**，让"这一条最后一次动过是什么时候"仍然说得通。
     *
     * 空记录（没颜色、没备注）直接跳过：备份里不该有，真有也不该凭空造出一行。
     */
    fun merge(imported: List<DayEntry>): Int {
        var changed = 0
        imported.forEach { incoming ->
            if (incoming.isEmpty) return@forEach
            val local = entries[incoming.date]

            val colorIds = if (local == null) {
                incoming.colorIds
            } else {
                local.colorIds + incoming.colorIds.filterNot { it in local.colorIds }
            }

            val notes = LinkedHashMap<String, String>()
            local?.notes?.forEach { (slot, text) -> if (text.isNotEmpty()) notes[slot] = text }
            incoming.notes.forEach { (slot, text) -> if (text.isNotEmpty()) notes[slot] = text }

            // 快照**本地优先**：它记的是"这一天当时涂的是什么颜色"，本地那份更可信。
            // 备份里只有本地没有的颜色才补进来（换手机恢复时才走到这条）。
            val snapshots = LinkedHashMap<String, ColorSnapshot>()
            local?.colorSnapshots?.forEach { (id, snapshot) -> snapshots[id] = snapshot }
            incoming.colorSnapshots.forEach { (id, snapshot) ->
                if (id !in snapshots) snapshots[id] = snapshot
            }

            val merged = DayEntry(
                date = incoming.date,
                colorIds = colorIds,
                colorSnapshots = snapshots,
                notes = notes,
                updatedAt = maxOf(local?.updatedAt ?: 0L, incoming.updatedAt),
            )
            // 一模一样就不写盘 —— 反复导入同一份备份不该每次都产生一次 IO。
            if (merged == local) return@forEach

            entries[merged.date] = merged
            changed++
            scope.launch { withContext(Dispatchers.IO) { dao.upsert(merged.toEntity()) } }
        }
        return changed
    }

    /**
     * 同一天点同一个颜色第二次就取消。
     *
     * 故意**不做**"删除"按钮：取消动作就藏在同一个颜色上，少一个按钮、少一个概念。
     * 顺序按点击先后追加，因为色条是"等分切 N 段按选择顺序"排列的（计划 8.1）。
     *
     * [snapshot] 是**涂色这一刻**的色值，由调用方从当前色板取（含用户改过的颜色）。
     * Store 不自己去读 Palette：它不知道现在是浅色还是深色模式，也不该知道。
     */
    fun toggle(date: LocalDate, colorId: String, snapshot: ColorSnapshot? = null) {
        val current = entries[date]
        val ids = current?.colorIds.orEmpty()
        val removing = colorId in ids
        val next = if (removing) ids - colorId else ids + colorId

        val notes = current?.notes.orEmpty().toMutableMap()
        if (removing) {
            // 颜色被取消了，它那条备注不能跟着人间蒸发 —— 用户只是改主意不涂这个色，
            // 不代表当时写下的那句话就不作数了。挪到「不归属任何颜色」的槽位。
            //
            // ⚠️ 2026-10-17 之前这里只写了"槽位已经有字就原样留着"，却没把 text 写回 map：
            // `remove` 一走那句话就真的没了 —— 一天有两条备注、空槽又占着的时候，
            // 取消一个颜色会**静默吃掉一条备注**（用户报告，模拟器/真机都能复现）。
            // 现在无归属槽**不唯一**（`_1`、`_2`…，见 [nextFreeColorlessSlot]），
            // 所以永远找得到空位：一个字不会丢，也不会赖在已经取消的颜色底下。
            notes.remove(colorId)?.let { text ->
                notes[nextFreeColorlessSlot(notes)] = text
            }
        }

        val snapshots = current?.colorSnapshots.orEmpty().toMutableMap()
        if (removing) {
            // 颜色撤了，它的快照也一起撤 —— 留着的话，以后重新涂同一个色会先短暂显示
            // 上一回的旧色值，等新快照写进去再跳一下。
            snapshots.remove(colorId)
        } else if (snapshot != null) {
            snapshots[colorId] = snapshot
        }

        val updated = DayEntry(
            date = date,
            colorIds = next,
            colorSnapshots = snapshots,
            notes = notes,
            updatedAt = System.currentTimeMillis(),
        )
        entries[date] = updated
        scope.launch { withContext(Dispatchers.IO) { dao.upsert(updated.toEntity()) } }
    }

    /** 这天的全部备注，`颜色 id -> 文字`。键可能是 [NOTE_WITHOUT_COLOR]。 */
    fun notesOf(date: LocalDate): Map<String, String> = entries[date]?.notes.orEmpty()

    /** 这天写没写过字。月视图上那个小点用得到，所以单独给一个不返回字符串的问法。 */
    fun hasNoteOf(date: LocalDate): Boolean = entries[date]?.notes?.isNotEmpty() == true

    /**
     * 写入或清除**某一天、某一个颜色下**的那条备注。
     *
     * 文字和颜色是**两个独立的维度**：写了字不等于涂了色，涂了色也不必有字。
     * 所以这里不碰 `colorIds`，[toggle] 也不碰别的颜色的备注 —— 各改各的那一格。
     *
     * [colorId] 传 [NOTE_WITHOUT_COLOR] 就是"不归属任何颜色"的那条，
     * 给没涂色、但想留一句话的日子用。留白本身也是一种表达，不该被数据结构挡住。
     *
     * 全空（没颜色、备注也清空）就把整行删掉。留着的话表里会攒下一堆
     * "看着有记录、点开什么都没有"的空行，以后做导出、年度视图时还得再筛一遍。
     *
     * 打字过程中这里会被逐字调用，也就是逐字 upsert。**这是有意的**：一句话不过二三十个字，
     * 每次 upsert 都是毫秒级、散在打字节奏里，代价可以忽略；换来的是内存永远实时正确、
     * 而且不必引入防抖那一套"停手多久才落盘"的时序状态（那种状态一旦在切日期时漏掉一次，
     * 丢的就是用户刚写的字）。
     */
    fun setNote(date: LocalDate, colorId: String, text: String) {
        val current = entries[date]
        val trimmed = text.trim()

        val notes = current?.notes.orEmpty().toMutableMap()
        if (trimmed.isEmpty()) notes.remove(colorId) else notes[colorId] = trimmed

        if (notes.isEmpty() && current?.colorIds.isNullOrEmpty()) {
            entries.remove(date)
            scope.launch { withContext(Dispatchers.IO) { dao.delete(date.toString()) } }
            return
        }

        // 用 copy 而不是重新构造 DayEntry：写一句话只该改 notes，
        // **颜色和快照必须原样带过去**。
        //
        // 2026-10-30 的 bug 就是这里重新构造时漏了 colorSnapshots：
        // 涂完色再写备注 → 快照被写成空 → 重启后补快照的逻辑拿**出厂色**补上，
        // 于是"改过色板之后涂的颜色"看起来回退成了初始色。只涂色不写备注不会触发，
        // 因为 toggle 自己带着快照。
        val updated = current?.copy(
            notes = notes,
            updatedAt = System.currentTimeMillis(),
        ) ?: DayEntry(
            date = date,
            notes = notes,
            updatedAt = System.currentTimeMillis(),
        )
        entries[date] = updated
        scope.launch { withContext(Dispatchers.IO) { dao.upsert(updated.toEntity()) } }
    }

    /**
     * 把一条备注从 [from] 这个槽挪到 [to]。长按标签改"这句话属于哪个颜色"用的就是它。
     *
     * **目标槽已经有字时两边交换，绝不覆盖。** 一天涂六种颜色就可能写六句话，
     * 手滑挪错一格太容易发生；而覆盖掉的那句话在别处没有任何副本，丢了就是真丢了。
     * 交换则一个字都不少，用户发现挪错了再挪一次就换回来。
     *
     * 只碰 [notes]，不碰 [colorIds] —— **换归属不等于改涂色**。候选槽位由界面给
     * （当天的颜色 + [NOTE_WITHOUT_COLOR]），所以挪完必然还落在会显示的那几个槽里。
     *
     * [from] 或 [to] 传 [NOTE_WITHOUT_COLOR] 都合法，两个方向都能挪。
     */
    fun moveNote(date: LocalDate, from: String, to: String) {
        if (from == to) return
        val current = entries[date] ?: return
        val moving = current.notes[from]?.takeIf { it.isNotEmpty() } ?: return
        val displaced = current.notes[to]?.takeIf { it.isNotEmpty() }

        val notes = current.notes.toMutableMap()
        notes.remove(from)
        // 原槽被腾空之后，把目标槽原来那句放进去 —— 这一步让"交换"成为原子的两个赋值，
        // 而不是"先清空、再判断"，中间不存在两句话同时消失的一瞬。
        if (displaced != null) notes[from] = displaced
        notes[to] = moving

        // 同样用 copy：换归属只改 notes，颜色 id 和色值快照一个都不许动。
        // （漏字段的教训见 setNote 里那段注释。）
        val updated = current.copy(
            notes = notes,
            updatedAt = System.currentTimeMillis(),
        )
        entries[date] = updated
        scope.launch { withContext(Dispatchers.IO) { dao.upsert(updated.toEntity()) } }
    }
}
