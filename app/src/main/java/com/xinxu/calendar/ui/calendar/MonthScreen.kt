package com.xinxu.calendar.ui.calendar

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xinxu.calendar.data.Backup
import com.xinxu.calendar.data.ColorSnapshot
import com.xinxu.calendar.data.DayEntry
import com.xinxu.calendar.data.DayEntryStore
import com.xinxu.calendar.data.NOTE_WITHOUT_COLOR
import com.xinxu.calendar.data.Palette
import com.xinxu.calendar.data.PaletteColor
import com.xinxu.calendar.data.almanac.Almanac
import com.xinxu.calendar.data.colorlessSlotsInUse
import com.xinxu.calendar.data.computeMonthStats
import com.xinxu.calendar.data.isColorlessSlot
import com.xinxu.calendar.data.nextFreeColorlessSlot
import com.xinxu.calendar.ui.components.ColorPicker
import com.xinxu.calendar.ui.components.DayStrip
import com.xinxu.calendar.ui.debug.ColorLabScreen
import com.xinxu.calendar.ui.settings.SettingsScreen
import com.xinxu.calendar.ui.stats.MonthStatsScreen
import com.xinxu.calendar.ui.stats.YearScreen
import com.xinxu.calendar.ui.theme.currentColor
import com.xinxu.calendar.ui.theme.currentSnapshot
import com.xinxu.calendar.ui.theme.displayName
import com.xinxu.calendar.ui.theme.recordColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 表头顺序跟魅族蓝本一致：一周从周一算起。 */
private val WEEKDAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

private val WEEKDAY_NAMES = listOf(
    "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日",
)

/**
 * 一格日期的高宽比。魅族蓝本因为有农历那一行，格子比方格更高一些；
 * 我们 v0.1 只有"数字 + 色条"两样，所以比蓝本再高一点，免得整屏全靠上顶、
 * 下半屏空一大片。
 */
private const val CELL_ASPECT = 0.75f

/**
 * 索引态格子的高宽比。**6 行必须一屏放下**（2026-11-13 定）：
 * 0.43 在真机（MEIZU 21 Pro，391×912dp）和模拟器（411×914dp）上都量过，
 * 6 行 + 头部 + 底部 Spacer 恰好落在屏内，最后一行不用滚就能看到格底的「…」。
 *
 * 之前是 0.42，只差几个 dp 就放不下：30 号这种落在最后一行的格子，不滚就看不到「…」，
 * "省略号示意还有备注"就白做了。再矮（<0.42）倒是更稳，但 3 条索引 + 「…」会压到农历那行。
 */
private const val EXPANDED_CELL_ASPECT = 0.43f

/**
 * 进入 / 退出备注索引态的过渡时长。格子长高、底部让位、索引淡入都按这一档走 ——
 * 以前是布尔开关，一下"闪现"（2026-11-13 反馈）。280ms 在真机上试出来偏慢，
 * 收到 200ms：够看清"日历长高、底部让位"，又不会挡手。
 */
private const val INDEX_TRANSITION_MS = 200

/** 左右各留 1200 个月（100 年）的翻页余地，足够用了，也让"今天"总在正中间。 */
private const val PAGE_COUNT = 2400
private const val INITIAL_PAGE = PAGE_COUNT / 2

/**
 * 动画规格统一走 Material 3 Expressive 的 [androidx.compose.material3.MotionScheme]：
 * 空间类动作（位移、缩放）用 `*SpatialSpec`，透明度类用 `*EffectsSpec`，
 * 「重要且慢」用 `default*`、「次要且快」用 `fast*`。具体时长和曲线由 M3 定，这里不再自己拍数字。
 *
 * 定这个量级的原则没变：**动效只用来交代"什么东西从哪儿来、去了哪儿"，不用来表演。**
 * 这是每天都要开一次的日历，任何一处多余的花哨动作，都会在第 100 次使用时变成噪音。
 * 所以取的都是 `fast*` / `default*` 里最靠前的那几档，不主动去够"慢而重"的档位。
 */
/**
 * 等统计页退场动画**播完**的兜底时长 —— **这不是动画规格**，别把它当成时长档位。
 *
 * 规格本身走 `motionScheme.fastEffectsSpec()`；弹簧没有"确定的毫秒数"，所以这里只能取一个
 * 比它实际收尾略长的值，好让 Dialog 被摘掉之前那几帧确实播完了。故意不叫 `_MS` 结尾的
 * 动效常量名，免得以后有人以为这是该往 `MotionScheme` 里换的东西。
 */
private const val STATS_EXIT_SETTLE_MS = 200

/**
 * 最后一个输入框失焦之后，等多久才真的结束编辑。
 *
 * 从一行点到另一行时，Compus 的焦点会先清再设，中间有个极短的空档。200ms
 * 足够覆盖它，又不至于让人在"点空白 → 面板收起"之间等出迟疑感。
 */
private const val FOCUS_LOST_SETTLE_MS = 200

/**
 * 换天之后，多久以内不把"没焦点"当用户离开。
 *
 * 换天时旧行先失焦、新行随后才 attach，真机实测这中间能差 200ms 以上 ——
 * 正好压过上面那 200ms 的防抖。宽限期要盖住这段空档，同时给 NoteEditor 的重试
 * （60+120+180+240ms）留出把焦点接回来的机会。
 */
private const val FOCUS_GRACE_MS = 700L

private const val SELECTION_SCALE_FROM = 0.86f

/**
 * M1 + M2 + M3：月视图 + 涂色闭环 + 本地落盘。
 *
 * 记录已经存进 Room（见 [DayEntryStore]），杀掉进程再打开，颜色还在。
 *
 * 底部那块面板是这一屏的重心：它同时回答"我选的是哪一天""这天涂了什么"，
 * 并且**把十二色一直摊在外面**，让涂色不需要先打开任何东西。
 */
@Composable
fun MonthScreen(modifier: Modifier = Modifier) {
    val motionScheme = MaterialTheme.motionScheme
    val today = remember { LocalDate.now() }
    val anchorMonth = remember { YearMonth.from(today) }

    val context = LocalContext.current

    // 调色盘（2026-10-19 加，2026-10-29 重做、收成唯一的个性化入口）。
    // 它改的是**用户自己的色板**（PaletteOverrideStore），保存后写盘；
    // 已经涂过的日子不受影响 —— 记录里存着涂色当时的颜色快照。
    var colorLabOpen by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(initialPage = INITIAL_PAGE) { PAGE_COUNT }
    val scope = rememberCoroutineScope()

    val store = remember { DayEntryStore(context.applicationContext) }

    var selectedDate by remember { mutableStateOf(today) }

    // 只服务于"翻月时跳到哪一天"。单独存一个"日"是为了让跨月来回翻能原样回来：
    // 10/31 翻到 11 月只有 30 天 → 11/30，如果直接改 selectedDate，再翻回 10 月就变成 10/30 了。
    var anchorDay by remember { mutableStateOf(today.dayOfMonth) }

    // 非空时打开统计页。用 Dialog 做全屏覆盖，而不是换 Activity 或切换 Composable 树 ——
    // 这样月视图一直留在组合里，关掉统计页之后翻到哪个月还是哪个月，不会跳回今天。
    var statsMonth by remember { mutableStateOf<YearMonth?>(null) }

    // 跟 statsMonth 刻意拆成两个状态：statsMonth 管"这个全屏 Dialog 还在不在"，
    // statsVisible 管"里面的内容露没露出来"。合成一个的话，关闭时 Dialog 会被立刻摘掉，
    // 退场动画连一帧都没机会播。
    var statsVisible by remember { mutableStateOf(false) }

    // 年度视图：跟统计页同一套"打开标记 + 可见标记"。
    // 它**叠在统计页之上**（同一套 MonthPageOverlay），所以关掉它回到统计页，而不是回月视图。
    var yearShown by remember { mutableStateOf(today.year) }
    var yearOpen by remember { mutableStateOf(false) }
    var yearVisible by remember { mutableStateOf(false) }

    // 年月跳转弹窗。跟统计页同样是 Dialog，同样不重建月视图。
    var pickerOpen by remember { mutableStateOf(false) }

    // 备注输入框露没露出来。**刻意不跟着 selectedDate 走** ——
    // 换天时必须退出去，否则输入框里会留着上一天的草稿，看着像在改今天这一天。
    var noteEditing by remember { mutableStateOf(false) }

    // 备注区"展开态"（2026-10-30 加）：日历收成周视图，所有备注都摊开。
    //
    // 触发有两条：**单击任意一条备注**、或者在备注区**继续上滑**（滚到底之后还往上）。
    // 退出则是反过来：在备注区继续下滑。换天不退出 —— 用户还在看这一天的字，只是换了一天。
    //
    // 当天没有备注时**不自动退回月视图**（2026-10-30 晚改）：那样动静太大、太突然。
    // 现在展开态下即使一条备注都没有，也会给一个空输入框（见 NoteTagList），
    // 周视图保持不动。
    var notesFocus by remember { mutableStateOf(false) }

    // 备注**索引态**（2026-11-13 加）：下滑展开，格子拉高，每格挂 4 条备注索引。
    //
    // 它和上面的 notesFocus 是**两个页面，不是同一个**：
    // - notesFocus：日历收成**周视图**，底部备注全文摊开 —— 上滑 / 单击备注进入；
    // - noteIndexOpen：日历保持**整月**、格子拉高，只看每格的索引 —— 下滑进入。
    // 2026-11-13 的索引态一度直接复用了 notesFocus，结果上滑也进了索引页（2026-11-13 修）。
    // 两者互斥，进入任一个都要把另一个清掉。
    var noteIndexOpen by remember { mutableStateOf(false) }

    // 底部区域（备注区 + 面板）的让位动画。进索引态时先淡出、淡完再把布局摘掉；
    // 退出时布局立刻回来、从透明淡入。直接 if 切换会"啪"地消失，
    // 那是"进入/退出索引闪现"的主要来源（2026-11-13 反馈）。
    var bottomShown by remember { mutableStateOf(!noteIndexOpen) }
    LaunchedEffect(noteIndexOpen) {
        if (noteIndexOpen) {
            // 等淡出走完再摘布局，不然最后几帧会跳一下。
            delay(INDEX_TRANSITION_MS.toLong())
            bottomShown = false
        } else {
            bottomShown = true
        }
    }
    val bottomAlpha by animateFloatAsState(
        targetValue = if (noteIndexOpen) 0f else 1f,
        animationSpec = tween(durationMillis = INDEX_TRANSITION_MS),
        label = "bottom-alpha",
    )

    // 换天瞬间的焦点宽限（见下面 LaunchedEffect(selectedDate) 的说明）。
    var focusGrace by remember { mutableStateOf(false) }

    // 长按网格下方某条备注 → 打开"改这一句"的弹窗；null 表示没开着。
    //
    // 存的是**颜色 id** 而不是那句文本本身：文本会在弹窗里逐字变，只有"这句话挂在哪个
    // 颜色上"才是这条备注的身份 —— 换归属时身份跟着搬，文本留在原地不动。
    var editingNoteId by remember { mutableStateOf<String?>(null) }

    // 「铺满屏幕去写这一条」开着的时候存的是**那个槽**；null 表示没开。
    //
    // 和 `editingNoteId` 刻意分成两个状态：那个是"改这一句"的小卡片（改字 + 换归属），
    // 这个是纯写字的全屏页。两者语义不同，同时开也没有意义。
    var fullScreenNoteId by remember { mutableStateOf<String?>(null) }

    // 设置页。跟统计页一样是页面内覆盖层 —— 关掉之后月视图还停在原来的位置。
    //
    // 两个状态，和统计页同一套分工（见下面两个 LaunchedEffect）：
    // `settingsOpen` 管"覆盖层在不在"，`settingsVisible` 管"里面的内容露没露出来"。
    // 合成一个的话关闭时覆盖层会被立刻摘掉，退场动画根本没机会播 ——
    // 2026-10-17 真机反馈"退出统计页、设置页时没有动画"，说的就是这个。
    var settingsOpen by remember { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(false) }

    // 备份：busy 挡住重复点击，message 是这一步的结果（成功，或者哪里不对劲），
    // pendingExport 是"等用户挑完保存位置之后要写出去的那份文本"。
    var backupBusy by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    // 导出：先让用户挑保存位置（系统文件选择器），拿到 uri 再真正写。
    // 用 SAF 而不是自己往某个目录里塞文件 —— 那样要么得申请存储权限，要么写在一个
    // 用户根本找不到的私有目录里，"导出备份"就等于白导。
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val text = pendingExport
        pendingExport = null
        if (uri == null || text == null) {
            backupBusy = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(text.toByteArray())
                    } ?: error("打不开这个位置")
                }.isSuccess
            }
            backupBusy = false
            backupMessage = if (ok) "导出完成" else "没能写到那个位置，换一个再试"
        }
    }

    // 导入：挑文件 → 读字节 → 解析 → 合并。解析抛出的那句话直接念给用户听
    // （Backup.decode 里那些话本来就是写给用户看的），这里不再自己编一套。
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            backupBusy = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().decodeToString()
                    } ?: error("打不开这个文件")
                    Backup.decode(text)
                }
            }
            backupBusy = false
            result.fold(
                onSuccess = { days ->
                    val changed = store.merge(days)
                    backupMessage = "读回 ${days.size} 天，其中 $changed 天有更新"
                },
                onFailure = { backupMessage = it.message ?: "导入失败" },
            )
        }
    }

    // 表头跟着当前页走，而不是跟着 selectedDate —— 用户翻到 11 月但没点任何一天时，
    // 标题必须显示"11月"，否则会以为没翻动。
    val visibleMonth = anchorMonth.plusMonths((pagerState.currentPage - INITIAL_PAGE).toLong())

    // 翻月时把选中日挪到目标月的同一天。底部面板必须始终在讲"你眼前这个月"的事 ——
    // 不挪的话，跳到 3 月后面板还在说"10月3日"，而屏幕上根本没有哪天被选中，两头对不上。
    //
    // **翻月仍然退出编辑态**（2026-10-30 保留这条）：编辑态只显示选中那一周，用户翻到
    // 别的月份时，面前已经没有"正在写的那一天"了，再留着输入框会像在写一个看不见的日子。
    // 同一个月内换天则**不退出**（见下一个 effect）。
    LaunchedEffect(visibleMonth) {
        if (visibleMonth != YearMonth.from(selectedDate)) {
            noteEditing = false
        }
        selectedDate = visibleMonth.atDay(anchorDay.coerceAtMost(visibleMonth.lengthOfMonth()))
    }

    // 换天只收掉"绑定在某一天上"的弹窗（改这一句 / 全屏写），**不退出编辑态**。
    // 编辑态本来就是一排输入框，换一天就换成那一天的框，接着写就是了（2026-10-30 改）——
    // 用户原话："切换到其他日期就会自动退出添加备注了，我希望它不要退出"。
    //
    // 换天同时给焦点一段**宽限期**：换天的一瞬间旧行失焦、新行还没 attach，
    // 中间的空档能到两百多毫秒（比"用户离开"的 200ms 防抖还长）。不放过这段空档的话，
    // 编辑器会在新行出现之前就被拆掉，焦点永远回不来（2026-10-30 复现并修）。
    LaunchedEffect(selectedDate) {
        editingNoteId = null
        fullScreenNoteId = null
        focusGrace = true
        delay(FOCUS_GRACE_MS)
        focusGrace = false
    }

    // 备注展开态按返回，先退回月视图，而不是直接退出 app。
    // 2026-10-30 用户反馈：周视图下侧滑返回会直接回桌面，"这里要加一个缓冲"。
    // BackHandler 是**后注册优先**，所以这条放在编辑态之前 —— 编辑态该先退。
    BackHandler(enabled = notesFocus && !noteEditing) { notesFocus = false }

    // 索引态（下滑展开的那一页）按返回也先收回月视图 —— 跟展开态是同一个道理：
    // 它是页面内状态，不在任何返回栈里。
    BackHandler(enabled = noteIndexOpen) { noteIndexOpen = false }

    // 编辑中按返回键，先收输入框，而不是直接把应用退到后台。
    BackHandler(enabled = noteEditing) { noteEditing = false }

    // 全屏写完按返回，先收回全屏页，别直接退到桌面。
    BackHandler(enabled = fullScreenNoteId != null) { fullScreenNoteId = null }

    // 编辑态什么时候结束：**看焦点，不看键盘**（2026-10-29 改）。    //
    // 原来监听键盘高度：一归零就结束编辑。问题是从第一行点到第二行时，输入法会重排，
    // 真机上表现为"键盘下沉又弹起"，这段时间里 `WindowInsets.ime` 会持续报 0 ——
    // 旧逻辑在这段时间里就把编辑态关了，输入框消失、整个页面跟着沉下去。
    //
    // 现在只认一件事：**最后一个输入框也失去焦点了**，才结束编辑。
    // 点第二行只是焦点从 A 挪到 B，不会有"谁都没焦点"的持续状态（短暂空档由防抖吸收）。
    //
    // 代价：键盘收起本身不再结束编辑 —— 想收干净要么再按一次返回（第一次会被输入法吃掉，
    // 这是系统行为，下面的 BackHandler 兜第二次），要么点一下铅笔。
    // 这个交易值得：误关比多按一次难查得多。
    var noteFocused by remember { mutableStateOf(false) }
    var noteFocusSeen by remember { mutableStateOf(false) }

    // 退出编辑时把焦点记忆清掉，下次进编辑从零开始。
    // 不清的话会残留上一次的 true，而这会影响"这一轮到底有没有人聚焦过"的判断。
    LaunchedEffect(noteEditing) {
        if (!noteEditing) {
            noteFocused = false
            noteFocusSeen = false
        }
    }

    LaunchedEffect(noteFocused) {
        if (noteFocused) {
            noteFocusSeen = true
            return@LaunchedEffect
        }
        // 刚进编辑态时第一行还没抢到焦点，别在第一帧就把它关掉。
        if (!noteEditing || !noteFocusSeen) return@LaunchedEffect
        // 换天宽限期内不判"用户离开"：这一下失焦是换行造成的，不是用户走了。
        if (focusGrace) return@LaunchedEffect
        delay(FOCUS_LOST_SETTLE_MS.toLong())
        // 防抖之后再读一次状态：这期间焦点回到某一行的话，本协程已被重启，走不到这里。
        if (!noteFocused) noteEditing = false
    }

    // 统计页进场：这里只把 statsMonth 立起来，让 Dialog 先进场（此刻 statsVisible 还是 false，
    // 内容处于收起态），下一帧再翻 statsVisible —— 有"收起 → 展开"这一前一后，动画才播得出来。
    LaunchedEffect(statsMonth) {
        if (statsMonth != null) statsVisible = true
    }

    // 统计页退场：先把内容藏起来，等退场播完再摘掉 Dialog。
    // key 用 statsVisible，所以退场途中又打开统计页时这个协程会被取消，不会把新开的那个一起关掉。
    LaunchedEffect(statsVisible) {
        if (!statsVisible && statsMonth != null) {
            delay(STATS_EXIT_SETTLE_MS.toLong())
            statsMonth = null
        }
    }

    // 年度视图的进场 / 退场：跟统计页同一套。退场时**不**动 yearShown，
    // 这样再打开还是停在刚才看的那一年。
    LaunchedEffect(yearOpen) {
        if (yearOpen) yearVisible = true
    }

    LaunchedEffect(yearVisible) {
        if (!yearVisible && yearOpen) {
            delay(STATS_EXIT_SETTLE_MS.toLong())
            yearOpen = false
        }
    }

    // 设置页进场 / 退场：与统计页逐字对应的一套。
    // 分开写而不是抽个函数，是因为两边跟的状态名不同（statsMonth / settingsVisible），
    // 抽出去反而要在调用处传一堆读写 lambda，读起来更绕。
    LaunchedEffect(settingsOpen) {
        if (settingsOpen) settingsVisible = true
    }

    LaunchedEffect(settingsVisible) {
        if (!settingsVisible && settingsOpen) {
            delay(STATS_EXIT_SETTLE_MS.toLong())
            settingsOpen = false
        }
    }

    // 关于键盘：曾经把面板**叠**在 Box 底部、自己吃 imePadding，结果是"面板浮起来了，
    // 但它盖住了下面的东西"—— 网格下方的备注标签整条被压在面板底下看不见（2026-10-16 实测）。
    // 根子在于叠加不参与流式布局，Column 根本不知道底下还有一块。
    // 现在回到一件事：整页一起吃 ime inset，谁不够谁自己收缩。
    //
    // 关于"谁收缩"：网格曾经吃 `weight(1f)` —— 也就是"最后分配、不够就裁它"。
    // 2026-10-17 实测，四条长备注一展开，六行日历被压成一行数字、色盘直接被推出屏幕。
    // 现在反过来 —— 网格**先量先占**（自然高度，不参与压缩），能屈能伸的是下方那些文字：
    // 空间够就自然铺开，不够就吃剩余空间、内部滚动（`weight(1f, fill = false)`）。
    // 一句话：**上面的日历不许动，下面的文字自己滚。**
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        MonthHeader(
            month = visibleMonth,
            onOpenPicker = { pickerOpen = true },
            onJumpToToday = {
                selectedDate = today
                anchorDay = today.dayOfMonth
                scope.launch { pagerState.animateScrollToPage(INITIAL_PAGE) }
            },
            onOpenStats = { statsMonth = visibleMonth },
            onOpenSettings = { settingsOpen = true },
            onOpenColorLab = { colorLabOpen = true },
        )

        WeekdayHeader()

        // 网格按**自然高度**摆着，不参与压缩 —— 六行格子永远完整。
        //
        // 但写字的时候反过来 —— 只留选中那一排（onlyRow），pager 改成按内容取高，
        // 上面这层 Box 负责把它顶到顶部。腾出来的空间全给输入框和键盘。
        val selectedRowIndex = remember(visibleMonth, selectedDate) {
            MonthGrid.of(visibleMonth).weeks.indexOfFirst { selectedDate in it }
        }
        // 这个月有没有备注 —— 空月份下滑不进入索引态（用户 2026-11-13："空状态直接不显示"）。
        // **不能包 remember(visibleMonth, store.ready)**：备注是在组合之外改的，这两个 key 不变
        // 就不会重算，于是"本月写下第一条备注后立刻下滑"会没反应，得翻月或重启才恢复
        //（2026-11-13 验证时发现，旧包同样存在）。entriesOf 读的是 mutableStateMapOf（快照状态），
        // 直接算就能订阅到增删；每次重组扫一遍本月那几十条，代价可以忽略。
        val hasNotesThisMonth = store.entriesOf(visibleMonth).any { it.notes.isNotEmpty() }
        // 索引态过渡进度（0 = 月视图，1 = 索引态）。**放在这里而不是 MonthPage 里**：
        // pager 的 animateContentSize 也要看它 —— 过渡期间必须让 pager 高度直接跟随内容，
        // 否则弹簧追不上每帧变高的格子，会把最后一行裁掉一帧（2026-11-13 真机反馈"30 号闪没"）。
        val indexProgress by animateFloatAsState(
            targetValue = if (noteIndexOpen) 1f else 0f,
            animationSpec = tween(durationMillis = INDEX_TRANSITION_MS),
            label = "index-progress",
        )
        // 过渡进行中（不含两端的稳定态）：这期间不要给 pager 挂 animateContentSize。
        val indexTransitioning = indexProgress > 0.001f && indexProgress < 0.999f
        val expandThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 索引态**不再让网格吃 weight**（2026-11-13 修）：
                // 过渡期间底部区域还在淡出、仍然占着 `weight(1f)`，两个 weight 会平分
                // 剩余空间，把淡出中的面板瞬间挤到屏幕中间 —— 真机上看到的就是
                // "动画中间被一个白色的东西遮住一块"。现在网格始终按内容取高，
                // 底部区域被逐步压缩 → 淡出 → 移除，没有布局突变。
                // 展开态（周视图）同样不用撑满，剩下的高度留给备注全文。
                // 折叠态：在月视图上**下滑**展开备注索引（2026-11-13）。
                // 索引态、展开态、编辑态都不接这个手势 —— 索引态整页要能滚，
                // 另外两个网格只剩一周，垂直拖动该归滚动容器。
                .pointerInput(notesFocus, noteIndexOpen, noteEditing, hasNotesThisMonth) {
                    if (notesFocus || noteIndexOpen || noteEditing) return@pointerInput
                    var dragDown = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragDown = 0f },
                        onDragCancel = { dragDown = 0f },
                        onDragEnd = {
                            if (dragDown > expandThresholdPx && hasNotesThisMonth) {
                                noteIndexOpen = true
                            }
                            dragDown = 0f
                        },
                    ) { change, dragAmount ->
                        dragDown += dragAmount
                        change.consume()
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            // 页高不再一致了（4~6 行），而 pager 取的是"可见页里最高的那个"：
            // 从 5 行的月份往 6 行的月份滑，容器在滑动过程中就涨成了 6 行高。
            //
            // `verticalAlignment` 默认是 `CenterVertically` —— 矮的那页会在高容器里**垂直居中**，
            // 于是日历整体往下沉半行，松手回弹时又弹回去（2026-10-17 真机反馈的"抽搐"）。
            // 顶对齐之后，页内内容在滑动全程都不挪位置。
            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                // 过渡期间格子的高宽比在 0.75 ↔ 0.43 之间连续变化，所以 pager 始终按内容取高。
                // 以前索引态切成 fillMaxSize —— 那一下会把高度跳变直接露出来（2026-11-13 修）。
                // 5 行 ↔ 6 行、以及格子长高都由 animateContentSize 平滑。
                //
                // 规格是"临界阻尼 + 高刚度"，刻意不用 MotionScheme 的三档：
                // ① 默认 spring 收尾很长 —— 从 6 行的 11 月翻回 5 行的 10 月时，
                //    备注区要等约 1 秒才落到最终位置（2026-10-17 真机反馈）。
                // ② 换成 `defaultSpatialSpec` 仍然偏慢，而且**幅度越大越慢**（弹簧按物理走，
                //    位移大就要更久）—— 用户注意到"只滑一点点就松手时便签上移很快，
                //    整页翻过去就慢"，说的就是这个。
                // ③ `fastSpatialSpec` 还是弹簧，尾巴仍拖。这里有三个具体要求同时成立：
                //    要快、要不回弹、要跟翻页动作同时结束 —— 那就是临界阻尼 + 大刚度。
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    // 过渡进行中：让 pager 高度**直接跟随内容**，不要挂 animateContentSize ——
                    // 弹簧追不上每帧变高的格子，会把最后一行裁掉一帧（2026-11-13 真机反馈
                    // "动画中间 30 号闪没"）。稳定的折叠态才用它抹平"5 行 ↔ 6 行"的翻月跳变。
                    .then(
                        if (indexTransitioning) {
                            Modifier.clipToBounds()
                        } else {
                            Modifier
                                .animateContentSize(
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = 1f,
                                        stiffness = 1600f,
                                    ),
                                )
                                .clipToBounds()
                        },
                    ),
            ) { page ->
                MonthPage(
                    month = anchorMonth.plusMonths((page - INITIAL_PAGE).toLong()),
                    today = today,
                    selectedDate = selectedDate,
                    entryOf = store::entryOf,
                    onSelectDate = {
                        selectedDate = it
                        anchorDay = it.dayOfMonth
                        // 索引态里点日期 = "找到了"：收起索引、回到正常月视图（2026-11-13）。
                        // 展开态里点日期**不退出** —— 用户还在看这一天的字，只是换了一天
                        //（2026-10-30 定的语义，别跟索引态混）。
                        if (noteIndexOpen) noteIndexOpen = false
                    },
                    // 编辑态和展开态都把网格收成周视图 —— 同一套 onlyRow 机制（8.6.18）。
                    // 索引态保持整月，靠拉高格子来腾地方。
                    onlyRow = if (noteEditing || notesFocus) {
                        selectedRowIndex.takeIf { it >= 0 }
                    } else {
                        null
                    },
                    notesExpanded = noteIndexOpen,
                    indexProgress = indexProgress,
                    // 索引态里在网格上**上滑** → 收回索引、回月视图（2026-11-13 加）。
                    onIndexCollapse = { noteIndexOpen = false },
                )
            }
        }

        // 网格下方、面板上方：把选中这天写过的话摊在这里。
        // 这是你自己在 2026-10-16 画的（"这些文字备注或许可以直接排列在日期下方展示"），
        // 也是决策记录第 17 条说的"面板右上角留给这一天级的操作"的延续 ——
        // 写字的入口在面板，读字的地方在网格下面，各占各的位。
        //
        // 这一块吃 `weight(1f)` —— **占满网格与面板之间的所有剩余空间**，两个作用：
        // 内容多时自己内部滚（不挤压日历），内容少时把面板**顶到屏幕底部**。
        //
        // 前半程这里写的是 `weight(1f, fill = false)`（按内容取高），结果是没涂色、
        // 没写字的日子面板跟着内容往上跑，和上方标签贴在一起（2026-10-17 真机反馈）。
        // 面板的位置该由屏幕决定，不该由"这天写没写字"决定。
        //
        // **编辑态整块收起**：进入编辑时每一行输入框里已经摆着那句话，再列一遍纯属重复，
        // 而且它会把键盘让出来的那点空间又吃掉一半。这跟面板里"写字时色盘让位给输入框"
        // 是同一条分工 —— 上面那句"面板只负责写，网格下方只负责读"落到行为上就是：
        // **写的时候不读。**
        // 备注索引态整块收起：索引已经在格子下面了，再列一遍是重复，而且会占掉索引态的高度。
        // 让位过程是淡出的（bottomAlpha），淡完才摘掉布局（bottomShown）。
        if (bottomShown && !noteEditing) {
            NoteTagList(
                entry = store.entryOf(selectedDate),
                expanded = notesFocus,
                onRequestExpand = { notesFocus = true },
                onRequestCollapse = { notesFocus = false },
                onEditNote = { editingNoteId = it },
                onNoteChange = { colorId, text -> store.setNote(selectedDate, colorId, text) },
                onExpandNote = { fullScreenNoteId = it },
                modifier = Modifier
                    .weight(1f)
                    .alpha(bottomAlpha),
            )
        }

        // 索引态整块收起（日历撑满全屏，点日期才回到正常月视图）。
        // 展开态**不收起**：8.6.18 的截图里面板和色盘都还在，展开只让日历收成一周、
        // 把高度让给备注全文 —— 这两件事别混。
        if (bottomShown) {
            // 不写字时按内容取高（色盘就那两行）；写字时同样"补上剩下的"，
            // 好让一天涂了六七种颜色时输入框整体留在屏内、由它自己滚 ——
            // 而不是像 2026-10-17 那样，最后一个框永远滑不上来。
            DayPanel(
                date = selectedDate,
                entry = store.entryOf(selectedDate),
                noteEditing = noteEditing,
                // 快照由 DayPanel 在组合时算好（currentLight/currentDark 是 @Composable，
                // 不能在点击回调里调用），这里只管把它写进记录。
                onToggleColor = { id, snapshot -> store.toggle(selectedDate, id, snapshot) },
                onNoteChange = { colorId, text -> store.setNote(selectedDate, colorId, text) },
                onToggleNoteEditing = { noteEditing = !noteEditing },
                onExpandNote = { fullScreenNoteId = it },
                // 编辑态的真实结束信号：所有输入框都失焦（见上面 FOCUS_LOST_SETTLE_MS 那段）。
                onNoteFocusChanged = { noteFocused = it },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .alpha(bottomAlpha)
                    .then(if (noteEditing) Modifier.weight(1f) else Modifier),
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    // 统计页与设置页都走 `MonthPageOverlay` —— **刻意不用 Dialog**。
    //
    // 2026-10-17 真机反馈："退出这俩界面时基本就是屏幕闪了一下，还是灰色的。"
    // 根子在 Dialog 的灰色遮罩是**系统窗口**的一部分，不受这里 `AnimatedVisibility` 的控制：
    // 内容滑走了，那层灰还留在原地，最后跟窗口一起"啪"地消失 —— 看上去就是闪一下。
    // 换成页面内覆盖层之后，遮罩和内容都在我们自己手里，可以一起淡出。
    statsMonth?.let { month ->
        MonthPageOverlay(visible = statsVisible, onDismiss = { statsVisible = false }) {
            MonthStatsScreen(
                // 把 store.ready 也当成 key：数据还没从磁盘读完时打开统计页，
                // ready 一变就会重算，不会停在一个假的空月份上。
                stats = remember(month, store.ready) {
                    computeMonthStats(store.entriesOf(month), month)
                },
                onClose = { statsVisible = false },
                onOpenYear = {
                    // 从统计页进去时，先停在"正在看的这个月"那一年；进去之后可以左右翻年。
                    yearShown = month.year
                    yearOpen = true
                },
            )
        }
    }

    // 年度视图：叠在统计页之上（同一套覆盖层）。数据只给它这一年的 ——
    // 全年视图不需要知道别的年份，别把整库都传进去。
    if (yearOpen) {
        MonthPageOverlay(visible = yearVisible, onDismiss = { yearVisible = false }) {
            YearScreen(
                year = yearShown,
                entries = remember(yearShown, store.ready) {
                    store.allEntries().filter { it.date.year == yearShown }
                },
                onYearChange = { yearShown = it },
                onClose = { yearVisible = false },
            )
        }
    }

    // 设置页。跟统计页同样走 `MonthPageOverlay`（页面内覆盖层，不用 Dialog 的原因见那边）。
    // 关掉之后月视图还停在原来的月份，不会像换 Activity 那样把"翻到哪儿了"给忘掉。
    if (settingsOpen) {
        MonthPageOverlay(visible = settingsVisible, onDismiss = { settingsVisible = false }) {
            SettingsScreen(
                busy = backupBusy,
                message = backupMessage,
                onExport = {
                    backupBusy = true
                    backupMessage = null
                    pendingExport = Backup.encode(store.allEntries())
                    exportLauncher.launch(Backup.suggestedFileName())
                },
                onImport = {
                    backupBusy = true
                    backupMessage = null
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                // **不关设置页**：试验台和调色盘都是 Dialog，会盖在覆盖层之上；
                // 关掉它们之后设置页原样还在，用户不会被丢回月视图。
                onOpenColorLab = { colorLabOpen = true },
                onClose = { settingsVisible = false },
            )
        }
    }

    if (pickerOpen) {
        MonthPickerDialog(
            visible = visibleMonth,
            onPick = { target ->
                pickerOpen = false
                val delta = ChronoUnit.MONTHS.between(anchorMonth, target).toInt()
                val page = (INITIAL_PAGE + delta).coerceIn(0, PAGE_COUNT - 1)
                scope.launch {
                    // 隔一两个月就用动画（能看出"翻过去了"），跨年就直接落位 ——
                    // 让 pager 真去滚几十上百页会变成一段漫长的飞页，看着像卡住了。
                    if (abs(delta) <= 2) {
                        pagerState.animateScrollToPage(page)
                    } else {
                        pagerState.scrollToPage(page)
                    }
                }
            },
            onDismiss = { pickerOpen = false },
        )
    }

    // 长按网格下方某条备注 → 就地把这一句改掉：改文字，或者改它挂在哪一个颜色上。
    //
    // 为什么不做成"跳进面板的编辑态"：面板是"给这一天写点什么"，按颜色分行、一行一个框；
    // 而长按进来的用户心里想的是"改**这一句**"。让人先在一堆框里找到那一行，等于把已经
    // 指明白的对象又让他重找一遍。所以这里只摆这一条，顺手把候选颜色一起摊出来。
    editingNoteId?.let { colorId ->
        NoteEditDialog(
            date = selectedDate,
            entry = store.entryOf(selectedDate),
            initialColorId = colorId,
            onNoteChange = { slot, text -> store.setNote(selectedDate, slot, text) },
            onMoveNote = { from, to -> store.moveNote(selectedDate, from, to) },
            onDismiss = { editingNoteId = null },
        )
    }

    // 铺满屏幕写这一条。入口是面板里某个输入框右下角的小图标 ——
    // 四行装不下的长文（引文、长日记）在这儿一次写完。
    fullScreenNoteId?.let { slot ->
        NoteFullScreenEditor(
            date = selectedDate,
            colorId = slot,
            dotColor = recordColor(store.entryOf(selectedDate), slot),
            value = store.notesOf(selectedDate)[slot].orEmpty(),
            onValueChange = { store.setNote(selectedDate, slot, it) },
            onDismiss = { fullScreenNoteId = null },
        )
    }

    // 调色盘。入口是设置页的「个性化」或长按齿轮。
    // 保存会写盘（PaletteOverrideStore），只影响以后新涂的颜色 —— 历史有快照，不变。
    if (colorLabOpen) {
        Dialog(
            onDismissRequest = { colorLabOpen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            ColorLabScreen(onClose = { colorLabOpen = false })
        }
    }
}

/** 覆盖层的遮罩浓度。和 Dialog 默认的 `dimAmount` 对齐，换过来之后观感不变。 */
private const val SCRIM_ALPHA = 0.32f

/**
 * 统计页与设置页共用的、盖在月视图上面的一层。
 *
 * **为什么不用 Dialog**：Dialog 的灰色遮罩（scrim）是**系统窗口**的一部分，
 * 不受这里 [AnimatedVisibility] 的控制 —— 内容滑走了，那层灰还留在原地，
 * 最后跟窗口一起"啪"地消失。真机上看上去就是**闪一下**
 * （2026-10-17 用户原话：「退出基本就是屏幕闪了一下，还是灰色的」）。
 * 换成页面内覆盖层之后，遮罩和内容都在我们自己手里，可以一起淡出。
 *
 * 和 Dialog 相比少了一件系统给的东西：返回键不再自动关闭，所以这里自己接 [BackHandler]。
 *
 * @param visible 内容露没露出来。关闭时先置 false 播退场动画，播完再由外面把覆盖层摘掉
 *   ——所以调用方要有两个状态（`xxxOpen` 管覆盖层在不在、`xxxVisible` 管内容露没露）。
 * @param onDismiss 返回键 / 点遮罩时的关闭请求。
 */
@Composable
private fun MonthPageOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme

    // 遮罩跟着内容一起呼吸。用 animateFloatAsState 而不是挂在 AnimatedVisibility 的
    // transition 上，是因为遮罩要在内容之外、而且要比内容"活得久"一点：
    // 内容已经滑走时，它还留着一层正在变淡的灰，最后两边一起归零。
    val scrim by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = motionScheme.fastEffectsSpec<Float>(),
        label = "pageScrim",
    )

    BackHandler(enabled = visible) { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA * scrim)),
        )
        // 进场从屏幕底部整屏推上来；退场顺着原路往下滑回去（用户 2026-10-17 的建议：
        // 「既然是从下面往上滑入的，不如就让它们向下滑出退场」），但只滑 1/6 屏 ——
        // 退场该比进场利落，直接滑一整屏会显得"恋恋不舍"。
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(motionScheme.defaultEffectsSpec<Float>()) +
                slideInVertically(motionScheme.defaultSpatialSpec<IntOffset>()) { it },
            exit = fadeOut(motionScheme.fastEffectsSpec<Float>()) +
                slideOutVertically(motionScheme.fastSpatialSpec<IntOffset>()) { it / 6 },
            modifier = Modifier.fillMaxSize(),
        ) {
            content()
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onOpenPicker: () -> Unit,
    onJumpToToday: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenColorLab: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 标题 + 三角整块可点，打开年月跳转。
        // 点击区靠 8dp 内边距往外撑：只按一行文字的高度，拇指按不准。
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenPicker)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 翻月时标题跟着 pager 的方向滑进来。
            // 日期格本来就是 pager 在推，标题原先却是一帧硬切 —— 那是翻月时唯一一处"跳"的地方，
            // 整片都在动、只有这一行字啪地换掉，反而更扎眼。
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    // 往后的月份从右边来，往前的从左边来，跟手指滑的方向一致。
                    val sign = if (targetState > initialState) 1 else -1
                    (
                        fadeIn(motionScheme.defaultEffectsSpec<Float>()) +
                            slideInHorizontally(
                                motionScheme.defaultSpatialSpec<IntOffset>(),
                            ) { width -> sign * width / 3 }
                        ).togetherWith(
                        fadeOut(motionScheme.fastEffectsSpec<Float>()) +
                            slideOutHorizontally(
                                motionScheme.fastSpatialSpec<IntOffset>(),
                            ) { width -> -sign * width / 4 },
                    )
                },
                label = "monthTitle",
            ) { shownMonth ->
                Text(
                    text = "${shownMonth.year}年${shownMonth.monthValue}月",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(8.dp))
            // 原本这里是个 "▾" 字符，在小字号下糊成一个脏点；手绘三角形才控制得住大小和位置。
            // 它是"这里能点"的暗示，所以必须真的能点 —— M4 之前它是个死装饰。
            DropdownTriangle(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        // 统计入口放在这里而不是底部面板的右上角：统计是**月份级**的事，
        // 该跟同样是月份级的「今天」待在一起（计划 3 里给面板右上角留的位子
        // 留给"这一天"级别的操作，比如以后的备注）。
        StatsButton(onClick = onOpenStats)
        // 齿轮紧挨着统计：两个都是"月视图之外的一页"，摆在一起比一左一右好找。
        // 长按齿轮仍保留为调色盘的快捷方式（选中态试验台已经删了，短按去正事这条不变）。
        SettingsButton(onClick = onOpenSettings, onLongClick = onOpenColorLab)
        TextButton(onClick = onJumpToToday) {
            Text("今天")
        }
    }
}

/**
 * 统计入口图标：一个 2×2 的小方阵，四个格子直接用色板里的颜色。
 *
 * 不引图标依赖（`material-icons-extended` 会让包大好几 MB），而且这个形状本身就是
 * 统计页里那个"颜色方阵"的缩影 —— 用四个真实的色板颜色，比一个抽象图标更贴题。
 * 尺寸只有 18dp，四个格子各约 8dp，颜色只需彼此分得开，不需要准确传达是哪一色。
 */
@Composable
private fun StatsButton(onClick: () -> Unit) {
    // 先在 remember 里拿到 PaletteColor（普通数据），再在组合里调 currentColor()。
    // currentColor() 是 @Composable，不能塞进 remember 的 lambda —— 那里不是组合上下文。
    val swatches = remember {
        listOf("warm_rose", "warm_amber", "cool_mint", "cool_sky")
            .mapNotNull { id -> Palette.byId(id) }
    }.map { it.currentColor() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            if (swatches.isEmpty()) return@Canvas
            val gap = size.width * 0.16f
            val cell = (size.width - gap) / 2f
            val radius = CornerRadius(cell * 0.3f, cell * 0.3f)
            swatches.take(4).forEachIndexed { index, color ->
                val x = if (index % 2 == 0) 0f else cell + gap
                val y = if (index < 2) 0f else cell + gap
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell),
                    cornerRadius = radius,
                )
            }
        }
    }
}

/**
 * 设置入口：一个手绘的齿轮。
 *
 * 跟 [StatsButton] 同样尺寸、同样位置逻辑（都是 40dp 的圆），但**不引图标库** ——
 * 这个工程从头到尾手绘图标（铅笔、折角、对角箭头、叉），一个齿轮不值得为它多几 MB。
 *
 * 画法是"一个粗描边的圆 + 八根圆头短齿"：比按多边形一笔一笔算齿形稳得多，
 * 在 18dp 这个尺寸下也认得出是齿轮而不是太阳。
 */
@OptIn(ExperimentalFoundationApi::class) // 长按打开调色盘，跟 [StatsButton] 一个写法
@Composable
private fun SettingsButton(onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = "设置" },
        contentAlignment = Alignment.Center,
    ) {
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(Modifier.size(18.dp)) { drawGear(color) }
    }
}

/** 齿轮：粗描边的圆环 + 八根圆头齿。 */
private fun DrawScope.drawGear(color: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f
    val stroke = radius * 0.28f
    val ringRadius = radius * 0.52f
    val teeth = 8

    repeat(teeth) { index ->
        val angle = index * 2f * PI.toFloat() / teeth
        val cosA = cos(angle)
        val sinA = sin(angle)
        drawLine(
            color = color,
            start = Offset(center.x + cosA * ringRadius, center.y + sinA * ringRadius),
            end = Offset(center.x + cosA * (radius - stroke / 2f), center.y + sinA * (radius - stroke / 2f)),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(
        color = color,
        radius = ringRadius,
        center = center,
        style = Stroke(width = stroke),
    )
}

@Composable
private fun DropdownTriangle(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 12.dp, height = 7.dp)) {
        drawPath(
            path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            },
            color = color,
        )
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        WEEKDAY_LABELS.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthPage(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    entryOf: (LocalDate) -> DayEntry?,
    onSelectDate: (LocalDate) -> Unit,
    onlyRow: Int? = null,
    notesExpanded: Boolean = false,
    /** 索引态过渡进度（0 = 月视图，1 = 索引态）；由外层统一算，pager 的高度过渡也要用它。 */
    indexProgress: Float = 0f,
    /** 索引态：在网格上**上滑** → 收回索引、回月视图（2026-11-13 加）。 */
    onIndexCollapse: () -> Unit = {},
) {
    val grid = remember(month) { MonthGrid.of(month) }
    // 写字的时候只留选中那一排（你自己 2026-10-16 提的："隐去其他只剩下这一排并一起上浮，
    // 这样就可以留出大量编辑空间了"）。键盘一收 onlyRow 变回 null，六行原样回来。
    val weeks = onlyRow?.let { row -> grid.weeks.getOrNull(row)?.let { listOf(it) } } ?: grid.weeks

    // 索引态 6 行现在一屏放得下，正常没有可滚的内容 —— **上滑就是收回索引**。
    // 用 nestedScroll 的 postScroll 而不是自己拦垂直手势：万一以后行高或字体让内容又变高，
    // 滚动仍然优先，滚到底继续上滑才收起（和 NoteTagList 的下滑收起是同一套）。
    // 手势方向别记反：手指往上推、内容往上走时，未消费的 available.y 是**负**的。
    val scroll = rememberScrollState()
    val collapseIndex = rememberUpdatedState(onIndexCollapse)
    val collapseThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    // 斜向手势只走一个方向（2026-11-13 反馈：向右上斜滑时，界面先收起、又翻到前一月）。
    // 在手势开头判定一次**主导方向**：垂直主导才允许"上滑收起"，水平主导整段交给 pager 翻月。
    // 只观察、不消费事件，所以不会跟 DayCell 的点击或 pager 的拖动抢手势；
    // 判定用的是 touchSlop，和 Compose 自己给 scrollable 做方向锁同一套标准。
    val verticalGesture = remember { mutableStateOf(false) }

    // 上滑收起触发的那一刻，手指通常还按在屏幕上。收起后界面变回月视图，但 pager 仍在读
    // 同一串指针事件 —— 接下来往左/右一带就成了翻页，看起来像"上滑完顺手翻了一页"
    //（2026-11-13 反馈）。所以收起后把**本次手势剩余的事件全部吃掉**，直到抬手为止。
    //
    // 消费必须放在一个**不随 notesExpanded 被摘掉**的 pointerInput 里：如果它还挂在
    // `if (notesExpanded)` 分支里，收起重组时这层处理先被移除，就没机会消费后面的动作了。
    val collapsing = remember { mutableStateOf(false) }
    val indexActive = rememberUpdatedState(notesExpanded)

    val indexScrollConnection = remember {
        object : NestedScrollConnection {
            private var upward = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                // 方向锁：水平（或斜向偏水平）的手势交给 pager 翻月，这里不累积、不收起。
                if (!verticalGesture.value) {
                    upward = 0f
                    return Offset.Zero
                }
                if (available.y < 0f) {
                    upward += -available.y
                    if (upward > collapseThresholdPx) {
                        upward = 0f
                        collapsing.value = true
                        collapseIndex.value()
                    }
                } else if (available.y > 0f) {
                    upward = 0f
                }
                return Offset.Zero
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    var dx = 0f
                    var dy = 0f
                    var decided = false
                    verticalGesture.value = false
                    collapsing.value = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (collapsing.value) {
                            // 已经在这次手势里收起了索引：剩下的动作全部消费掉，
                            // 别让 pager 把手还按在屏幕上的左右移动读成翻页。
                            event.changes.forEach { it.consume() }
                            if (change.changedToUpIgnoreConsumed()) break
                            continue
                        }
                        if (change.changedToUpIgnoreConsumed()) break
                        // 折叠态（或收起之后）不需要方向判定 —— 只等抬手，不消费。
                        if (!indexActive.value) continue
                        // 用 position 的差值取位移，不调 positionChange()：
                        // 那个扩展函数和 PointerInputChange 上的同名属性容易撞。
                        val delta = change.position - change.previousPosition
                        dx += delta.x
                        dy += delta.y
                        if (!decided) {
                            val slop = viewConfiguration.touchSlop
                            if (abs(dx) + abs(dy) > slop) {
                                decided = true
                                verticalGesture.value = abs(dy) > abs(dx)
                            }
                        }
                    }
                    verticalGesture.value = false
                    collapsing.value = false
                }
            }
            .then(
                if (notesExpanded) {
                    Modifier
                        .nestedScroll(indexScrollConnection)
                        .verticalScroll(scroll)
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        entry = entryOf(date),
                        belongsToMonth = YearMonth.from(date) == month,
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        onSelect = onSelectDate,
                        notesExpanded = notesExpanded,
                        indexProgress = indexProgress,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    entry: DayEntry?,
    belongsToMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    notesExpanded: Boolean = false,
    /** 索引态过渡进度（0 = 月视图，1 = 索引态），格子高宽比跟着它连续变化。 */
    indexProgress: Float = 0f,
) {
    // 上/下个月补进来的格子：留空，不显示数字。
    // 不画成灰色数字 —— 那会让人以为"这天也能点"，而魅族蓝本也是留白的。
    if (!belongsToMonth) {
        Box(
            modifier = modifier.aspectRatio(
                CELL_ASPECT + (EXPANDED_CELL_ASPECT - CELL_ASPECT) * indexProgress,
            ),
        )
        return
    }

    // 颜色值优先取**涂色当时的快照**（见 recordColor）：改色板不会让历史跟着变。
    // 未知 id 直接丢掉：万一以后色板删过颜色，历史记录也不会把界面搞崩。
    val colors = entry?.colorIds.orEmpty().mapNotNull { recordColor(entry, it) }
    val hasNote = entry?.notes?.isNotEmpty() == true

    // 展开态才准备的索引：备注原文按槽的顺序取出，截断交给渲染层（Text ellipsis）。
    val notePreviews = if (notesExpanded) {
        val notes = entry?.notes.orEmpty()
        noteSlots(entry?.colorIds.orEmpty(), notes)
            .mapNotNull { notes[it] }
            .filter { it.isNotBlank() }
    } else {
        emptyList()
    }
    // 索引态只显示**第一个颜色**（2026-11-14 改）：它和折叠态的"有备注"小点锚在数字
    // 右上角同一个位置 —— 一个点才不会和三个点挤在一起，过渡时也不会跳位。
    val colorDots = if (notesExpanded) {
        entry?.colorIds.orEmpty().take(1).mapNotNull { recordColor(entry, it) }
    } else {
        emptyList()
    }

    // 农历 / 节气 / 节日。一次纯计算，按日期 remember —— 选中态变化时不必重算。
    val almanac = remember(date) { Almanac.of(date) }

    // 默认的 Material 指示器是**按矩形边界**画的，而选中框是圆角。
    // 鼠标悬停或按下时，那个灰块的四角会从圆角外面探出来（2026-10-19 反馈）——
    // 短短一瞬，但正因为它在动、又盖在选中框上，特别扎眼。
    //
    // 所以关掉默认 indication，改用 interactionSource 自己画一层**跟着形状走**的高亮。
    // 顺带把鼠标悬停也管了：触屏上 hover 不会触发，接鼠标/手柄时才出现，正好是需要的。
    val interactionSource = remember { MutableInteractionSource() }

    DayCellContent(
        day = date.dayOfMonth,
        colors = colors,
        hasNote = hasNote,
        subtitle = almanac.cellText,
        notesExpanded = notesExpanded,
        indexProgress = indexProgress,
        notePreviews = notePreviews,
        colorDots = colorDots,
        isToday = isToday,
        isSelected = isSelected,
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
        ) { onSelect(date) },
        interactionSource = interactionSource,
    )
}

/**
 * 一格日期的**画面本身**，跟数据从哪来无关。
 *
 * 之所以是 `internal` 而不是 `private`：调色盘要用它按**真实尺寸**渲染
 * "这个颜色当日期底色时是什么样"—— 只在电脑上画示意图是不够的，得放在真机屏幕上、
 * 跟相邻格子一起看才算数。
 *
 * 选中态是写死的（见 `DaySelection.kt`），2026-10-29 起不再可配置。
 */
@Composable
internal fun DayCellContent(
    day: Int,
    colors: List<Color>,
    isToday: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    hasNote: Boolean = false,
    /** 数字下面那行小字：节日 / 节气 / 农历。传 null 就只留原来的间距。 */
    subtitle: String? = null,
    /** 展开态：格子拉高，显示备注索引和颜色点（2026-11-13）。 */
    notesExpanded: Boolean = false,
    /** 索引态过渡进度（0 = 月视图，1 = 索引态）：格子高宽比、索引淡入、描边淡出都跟着它。 */
    indexProgress: Float = 0f,
    /** 展开态每格的备注索引；只显示开头几个字，超过 3 条折叠成一行「…」。 */
    notePreviews: List<String> = emptyList(),
    /** 索引态当天第一个颜色（只用一个点，见 dateDotColor 的说明）。 */
    colorDots: List<Color> = emptyList(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val motionScheme = MaterialTheme.motionScheme
    val shape = SELECTION_SHAPE
    val fill = selectionBackground()
    val border = selectionBorderColor()

    // 按压 / 悬停反馈。默认的 Material 指示器按矩形边界画，和圆角选中框对不齐，
    // 所以由这里自己画一层形状一致的高亮（见下面 Box 末尾）。
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()

    val numberColor = when {
        isSelected -> selectionContentColor()
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    // 「这天写过字」那个小点的颜色。选中态下跟着前景色走、再压暗，
    // 而不是写死一个灰 —— 写死的话在深底上会直接消失。
    val noteDotColor = if (isSelected) {
        selectionContentColor().copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // 日期右上角的小点：折叠态表示"这天有备注"，索引态显示当天**第一个颜色**
    //（2026-11-14 用户要求：索引态只留一个颜色点，而且和折叠态的点锚在同一个位置 ——
    // 锚在格子上角的话，两种格高差一大截，过渡时点会跳位）。索引态没有颜色但写了字时，
    // 仍退回灰点，别让"有备注"这件事消失。
    val firstColor = colorDots.firstOrNull()
    val dateDotColor = when {
        indexProgress >= 0.5f -> firstColor ?: noteDotColor.takeIf { hasNote }
        else -> noteDotColor.takeIf { hasNote }
    }

    // 选中块的进出由 0→1 的进度驱动：换天时旧格子的进度往 0 走、新格子的往 1 走，
    // 看起来就是选中块"从这一格挪到了那一格"。没被选中的格子进度恒为 0，不会每帧重组。
    val selection by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = motionScheme.fastSpatialSpec<Float>(),
        label = "selection",
    )

    Box(
        modifier = modifier
            .padding(2.dp)
            // 展开态把格子拉高，才能放下 3 条索引 + 一行「…」。
            // 高宽比跟着过渡进度连续变 —— 不能写成布尔二选一，否则进/出索引会"闪"。
            .aspectRatio(CELL_ASPECT + (EXPANDED_CELL_ASPECT - CELL_ASPECT) * indexProgress),
        // 展开态内容从**顶部**排：数字贴着格子顶部，右上角的色点才会和它同一水平线；
        // 折叠态仍然整块居中（原来的样子）。
        contentAlignment = if (notesExpanded) Alignment.TopCenter else Alignment.Center,
    ) {
        // 选中块单独占一层，**只让它自己缩放淡入，绝不牵动日期数字**。
        // 数字要是跟着一起缩，换天时会有一下"弹"，比不做动画还刺眼。
        // 裁剪跟着这一层走：平时不加 clip，否则换异形形状时会连数字一起裁掉。
        if (selection > 0.001f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val s = SELECTION_SCALE_FROM + (1f - SELECTION_SCALE_FROM) * selection
                        scaleX = s
                        scaleY = s
                        alpha = selection
                    },
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                        .background(fill)
                        // 备注索引态**不画描边**（用户 2026-11-13 选定的方案）：这一格里已经
                        // 挤着数字、农历、最多 3 条索引和右上角色点，1.5dp 的框线会把它们整个
                        // 围住、贴着字边，看着像互相压着。浅底 + 数字加粗已足够表达选中。
                        // 折叠态保持描边框，过渡时用透明度淡出（不能直接 if，会跳）。
                        .border(
                            SELECTION_BORDER_WIDTH,
                            border.copy(alpha = 1f - indexProgress),
                            shape,
                        ),
                )
            }
        }

        Column(
            // 必须吃满格宽：否则标签的 fillMaxWidth 只能撑到"数字那么宽"，
            // 每条标签就会跟着文字长度忽长忽短（2026-11-13 真机看到的就是这个）。
            modifier = Modifier
                .fillMaxWidth()
                .then(if (notesExpanded) Modifier.padding(top = 6.dp * indexProgress) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 数字 + 右上角的小点。**点必须锚在数字上**，不能锚在格子角上：
            // 折叠态和索引态的格子高度差一大截，锚格子角的话过渡时点会跳位置（2026-11-14 反馈）。
            // 折叠态有备注 → 灰点；索引态 → 当天第一个颜色；没有就不画。
            Box {
                // 数字 18sp、历法 11sp（2026-11-08 用户反馈"农历太小看不清"）：
                // 原来 22 / 9，落差太大，农历像附注；缩小到 18 并把历法提到 11 之后，
                // 两者的分量接近了，格子也不会被数字一个人占满。
                Text(
                    text = day.toString(),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = if (isToday || (isSelected && SELECTION_BOLD_NUMBER)) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    color = numberColor,
                )
                if (dateDotColor != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                            .size(4.dp)
                            .background(dateDotColor, CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(1.dp))
            // 农历 / 节气 / 节日。
            // **空串也占这一行**：否则有节日的格子会比没节日的高一截，整月看下来数字忽高忽低。
            Text(
                text = subtitle ?: "",
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) {
                    selectionContentColor().copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            // 色条和索引在过渡中间点交接：前半程色条淡出，后半程索引淡入。
            // 不用布尔二选一 —— 那一下就是"闪现"。
            if (indexProgress < 0.5f) {
                Spacer(Modifier.height(3.dp))
                DayStrip(
                    colors = colors,
                    selected = isSelected,
                    modifier = Modifier.alpha((1f - indexProgress * 2f).coerceIn(0f, 1f)),
                )
            } else {
                Spacer(Modifier.height(4.dp))
                NoteIndexChips(
                    previews = notePreviews,
                    modifier = Modifier.alpha(((indexProgress - 0.5f) * 2f).coerceIn(0f, 1f)),
                )
            }
        }



        // 自己画的按压 / 悬停高亮。形状跟着选中框走，所以不会再从圆角四角露出去。
        //
        // 放在最上层，跟 Material 指示器的层级一致 —— 它说的是"手指确实按到了"，
        // 是即时回应，不该被选中块盖在底下。透明度压得很低，因为它只负责"有反应"，
        // 真正的视觉落点是下面那块选中态。
        val highlightAlpha = when {
            pressed -> 0.10f
            hovered -> 0.05f
            else -> 0f
        }
        if (highlightAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = highlightAlpha)),
            )
        }
    }
}

/**
 * 展开态每格下面的备注索引。
 *
 * **只显示开头几个字 + 省略号**，起索引作用，不展开全文 —— 备注可能很长，
 * 全量显示会把格子撑歪、也会挤压相邻格子（用户 2026-11-13 明确否掉了全量方案）。
 * **最多 3 条**，还有更多时第 4 行固定让给「…」（用户 2026-11-13 定的）：
 * 格子高度只放得下数字 + 农历 + 4 行索引，6 条备注时显示 3 条 + 省略号，
 * 比硬塞 4 条再被格子边缘裁掉好读。
 */
@Composable
private fun NoteIndexChips(previews: List<String>, modifier: Modifier = Modifier) {
    // 用户指定的固定灰（#DFE0E2）：和界面里"这一天有备注"的小圆点同色系。
    // 它不跟主题走是故意的 —— 这一格像纸上的铅笔标注，不是主题里的强调色。
    val chipColor = Color(0xFFDFE0E2)
    val chipText = Color(0xFF1C1B1F)
    val shown = previews.take(3)
    val overflow = previews.size > 3

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        shown.forEach { text ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(chipColor)
                    .padding(horizontal = 3.dp, vertical = 1.dp),
            ) {
                Text(
                    text = text,
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = chipText,
                )
            }
        }
        if (overflow) {
            Text(
                text = "…",
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 底部那块面板：把"选中的是哪一天、这天涂了什么颜色"说清楚，并且把十二色摊在下面。
 *
 * 它对应魅族蓝本里「黄金周（国庆节）」那张日程卡片的位置和体量 —— 蓝本那里放的是日程，
 * 我们 MVP 阶段没有日程功能，正好用来放"今天的颜色"。v0.2 补回日程与宜忌时会做成可切换的。
 */
@Composable
private fun DayPanel(
    date: LocalDate,
    entry: DayEntry?,
    noteEditing: Boolean,
    onToggleColor: (String, ColorSnapshot?) -> Unit,
    onNoteChange: (String, String) -> Unit,
    onToggleNoteEditing: () -> Unit,
    onExpandNote: (String) -> Unit,
    onNoteFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.motionScheme
    val colorIds = entry?.colorIds.orEmpty()
    val notes = entry?.notes.orEmpty()
    val colors = remember(colorIds) { colorIds.mapNotNull { Palette.byId(it) } }

    // 点颜色时要把"这一刻的颜色"一起写进记录（历史不跟着色板变）。
    // 点击回调不是组合上下文，所以快照得在这里先算好。
    val snapshots = buildMap {
        Palette.colors.forEach { put(it.id, it.currentSnapshot()) }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                // 编辑态时面板被压在一个受限高度里（调用方给的是 `weight(1f)`），
                // 这里必须**吃满**它，下面的输入区才有"剩下的空间"可分。
                .then(if (noteEditing) Modifier.fillMaxSize() else Modifier)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // **整块**（色标 + 两行字）一起淡着换过去。
                // 一开始只把文字包进来、色标留在外面，换天那一瞬间就会露馅：色标已经跳到新颜色，
                // 字还写着旧日期 —— 抓动画中间帧时真拍到了这 160ms 的错位。
                // key 里带上描述文案是有意的：点颜色的那一刻"还没涂色 → 朱红"同样值得交代，跟换天一样。
                AnimatedContent(
                    targetState = Triple(date, colorIds, describeColors(colors)),
                    transitionSpec = {
                        (
                            fadeIn(motionScheme.fastEffectsSpec<Float>()) +
                                slideInVertically(
                                    motionScheme.fastSpatialSpec<IntOffset>(),
                                ) { it / 3 }
                            ).togetherWith(
                            fadeOut(motionScheme.fastEffectsSpec<Float>()),
                        )
                    },
                    label = "dayPanel",
                    modifier = Modifier.weight(1f),
                ) { (shownDate, shownIds, shownColors) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DayColorMarker(colors = shownIds.mapNotNull { recordColor(entry, it) })
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${shownDate.monthValue}月${shownDate.dayOfMonth}日 " +
                                    WEEKDAY_NAMES[shownDate.dayOfWeek.value - 1],
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            // 第二行：先说你涂了什么，再补农历 / 节日 —— 记录永远是主角，
                            // 历法是"顺带告诉你今天是什么日子"。
                            val almanac = remember(shownDate) { Almanac.of(shownDate) }
                            val almanacLine = buildString {
                                append("农历")
                                append(almanac.lunarText)
                                val extras = almanac.festivals
                                    .ifEmpty { listOfNotNull(almanac.jieQi) }
                                if (extras.isNotEmpty()) {
                                    append(" · ")
                                    append(extras.joinToString("、"))
                                }
                            }
                            Text(
                                text = "$shownColors · $almanacLine",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // 这一角就是 2026-10-03 说好留给"这一天"级别操作的位置（决策记录第 17 条）。
                // 统计当初放去了表头，因为它是月份级的事；而写备注是彻头彻尾的"这一天"，所以放这儿。
                NoteButton(
                    hasNote = notes.isNotEmpty(),
                    editing = noteEditing,
                    onClick = onToggleNoteEditing,
                )
            }

            Spacer(Modifier.height(14.dp))

            // 编辑时**色盘让位**给输入框，而不是叠在它上面。这样面板总高度基本不变：
            // 不然键盘一弹、面板再长高，日历会被挤掉下面几行，看着像整个页面被撞了一下。
            // 反正写字的时候也不需要色盘 —— 颜色早就点完了。
            //
            // 但只"让位"还不够：一天涂的色越多行数越多，七行（六色 + 不归属任何颜色）
            // 一起摆上来，最后一个框照样掉到键盘底下够不到（2026-10-17 实测）。
            // 所以输入区**吃满面板剩下的高度**（`weight(1f)`）并且自己带滚动。
            //
            // 写完的话**不在这里显示** —— 它们排在网格下方（见 NoteTagList）。
            // 面板只负责"写"，网格下方只负责"读"；想改某一句，长按它（见 NoteEditDialog）。
            if (noteEditing) {
                NoteEditor(
                    entry = entry,
                    onNoteChange = onNoteChange,
                    onExpandNote = onExpandNote,
                    onFocusChanged = onNoteFocusChanged,
                    modifier = Modifier.weight(1f),
                )
            } else {
                ColorPicker(
                    selectedIds = colorIds,
                    onToggle = { id -> onToggleColor(id, snapshots[id]) },
                )
            }
        }
    }
}

/**
 * **备注区（只读那一栏）**要摆出来的槽，按显示顺序：当天涂的、并且写了字的颜色 →
 * 那些"颜色已取消、备注却还挂着"的槽 → 无归属槽。
 *
 * 中间那一类就是取消颜色留下的。**只要它还列在这份清单里，用户就不会以为字丢了**；
 * 漏掉它，界面上那条备注会凭空消失、字却还躺在数据库里 —— 那是"我明明写过"里
 * 最难查的一类（2026-10-17 用户报的就是这个）。
 *
 * 这里只列**有字的**：这一栏是"读"，空白不该占地方。编辑态要的是另一份清单，
 * 见 [editorSlots]。
 */
private fun noteSlots(colorIds: List<String>, notes: Map<String, String>): List<String> = buildList {
    addAll(colorIds.filter { !notes[it].isNullOrEmpty() })
    notes.keys.forEach { id ->
        if (id !in colorIds && !isColorlessSlot(id) && !notes[id].isNullOrEmpty()) add(id)
    }
    addAll(colorlessSlotsInUse(notes))
}

/**
 * **编辑态**要摆出来的输入行：当天涂的每个颜色（哪怕还没写）→ "颜色已取消但有字"的槽
 * → 每个已经写着字的无归属槽 → **末尾永远多留一行空的无归属槽**。
 *
 * 末尾那一行是刻意的：想再写一句不挂颜色的，永远当场可做，不需要任何"添加"按钮
 * （设计红线 1：记一次不超过三步）。你 2026-10-17 说无归属槽不唯一"也算一层缓存"——
 * 这一行就是那个缓存的入口。
 */
private fun editorSlots(colorIds: List<String>, notes: Map<String, String>): List<String> = buildList {
    addAll(colorIds)
    notes.keys.forEach { id ->
        if (id !in colorIds && !isColorlessSlot(id) && !notes[id].isNullOrEmpty()) add(id)
    }
    addAll(colorlessSlotsInUse(notes))
    add(nextFreeColorlessSlot(notes))
}

/**
 * 编辑态：**一个颜色一行**。
 *
 * 为什么要分行，而不是像以前那样给整天一个输入框：一天里的颜色经常是对立的
 * （你 2026-10-16 说的"一日里有两种颜色，而且是对立的，说明是两种截然不同的心情"）。
 * 揉在一个框里，写"发工资啦"就没地方写"雨好大"，两句话会互相打架。
 *
 * 最后一行是 [NOTE_WITHOUT_COLOR] —— 没涂色也能写字。你自己那句说得比我好：
 * "在颜色上留白本身也是一种表达"。
 */
@Composable
private fun NoteEditor(
    entry: DayEntry?,
    onNoteChange: (String, String) -> Unit,
    onExpandNote: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 整块自己滚。一天涂的色越多，行数越多 —— 六七行一起摆上来，最后一个框必然掉到
    // 屏幕外（2026-10-17 实测：既看不见，也滑不上来，因为当时没有任何滚动容器）。
    // 调用方给的是 `weight(1f)`，也就是"面板里剩下的都归你，超了自己滚"。
    val scroll = rememberScrollState()
    val colorIds = entry?.colorIds.orEmpty()
    val notes = entry?.notes.orEmpty()

    // 哪一行拿着焦点。编辑态该不该结束由外层看这个 —— 不是看键盘（见 MonthScreen 里的说明）。
    // 只记一个槽而不是计数：焦点是**互斥**的，同一时刻只有一行有它。
    var focusedSlot by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(focusedSlot != null) { onFocusChanged(focusedSlot != null) }

    // 换天时主动把焦点交还第一行（2026-10-30）。
    //
    // 同月换天会换掉编辑器里的槽：比如从"没备注的日子"（只有一个无归属空槽）跳到
    // "有备注 / 只涂了色的日子"（第一行变成某个颜色的槽）。输入框的组合因此被重建，
    // 焦点没了；而 `autoFocus` 只在**首次**组合时请求一次，不会补。
    // 结果就是"从 30 号跳到 29 号"会瞬间无焦点，200ms 防抖一过就被判成"用户离开了"，
    // 编辑态自己关掉（用户 2026-10-30 报的就是这个）。
    //
    // 这里按日期重新请求一次，把焦点接回来。`runCatching` 兜住"节点还没 attach"的情况。
    val firstField = remember { FocusRequester() }
    LaunchedEffect(entry?.date) {
        // 新一行的输入框 **attach 得比这个 effect 晚**（真机上实测差了 200ms 左右）：
        // 换天时先失焦旧行、再组合新行，第一帧请求焦点会打在还没被移除的旧节点上。
        // 所以这里等它出来再请求，试几次；一旦某一行已经拿到焦点就停。
        repeat(4) { attempt ->
            if (focusedSlot != null) return@LaunchedEffect
            delay(60L * (attempt + 1))
            runCatching { firstField.requestFocus() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        editorSlots(colorIds, notes).forEachIndexed { index, colorId ->
            NoteSlot(
                value = notes[colorId].orEmpty(),
                // 色点跟着**涂色当时**的颜色走，不是跟着现在改过的色板走。
                dotColor = recordColor(entry, colorId),
                autoFocus = index == 0,
                onValueChange = { onNoteChange(colorId, it) },
                onExpand = { onExpandNote(colorId) },
                onFocusChanged = { focused ->
                    // 失焦时确认自己仍是"当前聚焦的那一行"再清 —— A→B 切换时，
                    // 可能先收到 A 的失焦、再收到 B 的获焦，直接无条件清空会让中间出现假空档。
                    if (focused) {
                        focusedSlot = colorId
                    } else if (focusedSlot == colorId) {
                        focusedSlot = null
                    }
                },
                // 第一行由 NoteEditor 共享同一个 requester，换天时用于把焦点接回来。
                focusRequester = if (index == 0) firstField else null,
            )
        }
    }
}

/** 一行：左边的色点（跟着这行归属的颜色走）+ 右边的输入框。 */
@Composable
private fun NoteSlot(
    value: String,
    dotColor: Color?,
    autoFocus: Boolean,
    onValueChange: (String) -> Unit,
    onExpand: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    focusRequester: FocusRequester? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .then(
                    if (dotColor != null) {
                        Modifier.background(dotColor, CircleShape)
                    } else {
                        // 不归属任何颜色 —— 画个描边空点，跟 8.1.1 那条"留白一律描边"的规矩一致。
                        Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        )
                    },
                ),
        )
        Spacer(Modifier.width(12.dp))
        NoteField(
            value = value,
            autoFocus = autoFocus,
            onValueChange = onValueChange,
            onExpand = onExpand,
            onFocusChanged = onFocusChanged,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 网格下方的「色点 + 一句话」。
 *
 * 顺序是有讲究的：先按这天的**颜色顺序**排，最后才是那条不归属任何颜色的 ——
 * 跟面板里 [NoteEditor] 的行序一致，看的时候不用在两处之间重新找位置。
 */
@Composable
private fun NoteTagList(
    entry: DayEntry?,
    expanded: Boolean,
    onRequestExpand: () -> Unit,
    onRequestCollapse: () -> Unit,
    onEditNote: (String) -> Unit,
    onNoteChange: (String, String) -> Unit,
    onExpandNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorIds = entry?.colorIds.orEmpty()
    val notes = entry?.notes.orEmpty()

    // 有字才摆出来。谁先谁后由 `noteSlots` 定：当天涂的色在前，然后是"颜色已取消、
    // 备注还挂着"的旧槽，最后是无归属槽 —— 漏掉中间那类，字就在界面上凭空消失。
    val ordered = noteSlots(colorIds, notes).map { it to notes.getValue(it) }

    // 整块自己滚。调用方给的是 `weight(1f)` —— 它**总是**占满网格与面板之间的剩余空间，
    // 所以 `ordered` 为空时这层 Column 依然要在（只是里面没东西）：面板因此被顶到屏幕底部，
    // 不会因为"这天没写字"就往上跑。**这也是这里不能提前 return 的原因。**
    val scroll = rememberScrollState()

    // 「滚到底再继续滑」才切换备注区形态。用 nestedScroll 的 postScroll 而不是自己拦手势：
    // 拦了就没法滚长备注了。available 里是**没被滚动容器消费掉**的那部分距离，
    // 只有已经滚到底（或内容根本不够滚）时才会累积起来。
    val thresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    val expandedState = rememberUpdatedState(expanded)
    val requestExpand = rememberUpdatedState(onRequestExpand)
    val requestCollapse = rememberUpdatedState(onRequestCollapse)
    val hasNotes = rememberUpdatedState(ordered.isNotEmpty())

    val connection = remember {
        object : NestedScrollConnection {
            private var upward = 0f
            private var downward = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                // 手指往上推、内容往上走时，未消费的滚动量是**负**的。
                if (available.y < 0f) {
                    upward += -available.y
                    downward = 0f
                    if (upward > thresholdPx && !expandedState.value && hasNotes.value) {
                        upward = 0f
                        requestExpand.value()
                    }
                } else if (available.y > 0f) {
                    downward += available.y
                    upward = 0f
                    if (downward > thresholdPx && expandedState.value) {
                        downward = 0f
                        requestCollapse.value()
                    }
                }
                return Offset.Zero
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(connection)
            .verticalScroll(scroll)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ordered.forEach { (colorId, text) ->
            key(colorId) {
                NoteTag(
                    text = text,
                    dotColor = recordColor(entry, colorId),
                    expanded = expanded,
                    // 单击 = 把备注区整体展开成周视图（不是单条折叠）。
                    // 已经展开时点击不做事 —— 收起是"往下滑"这个反向手势。
                    onClick = { if (!expanded) onRequestExpand() },
                    onEdit = { onEditNote(colorId) },
                )
            }
        }

        // 展开态下**当天没有备注**时，给一个空输入框（用户 2026-10-30 的图一）：
        // 这样"换到没备注的日子"不会突然退回月视图，而是原地变成"可以直接写"的样子。
        // 写完第一条，输入框让位给标签 —— 那一下本身就是"提交"的反馈。
        //
        // 绑的是"下一个空的无归属槽"，跟编辑态 NoteEditor 末尾那一行是同一个规则
        //（无归属槽不唯一，见 DayEntry.kt）。
        if (expanded && ordered.isEmpty()) {
            val freeSlot = nextFreeColorlessSlot(notes)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    // 备注区底色是页面背景，白输入框直接放上去会看不见 —— 垫一层面板色。
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp),
            ) {
                NoteSlot(
                    value = "",
                    dotColor = null,
                    autoFocus = false,
                    onValueChange = { onNoteChange(freeSlot, it) },
                    onExpand = { onExpandNote(freeSlot) },
                    onFocusChanged = {},
                )
            }
        }

        // 标签把这块占满时，最后一条会和面板贴在一起 —— 留一口气。
        if (ordered.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * 一条标签。**折叠时两行；单击 = 把备注区整个展开（见 [NoteTagList]）；长按 = 改这一句。**
 *
 * 2026-10-30 改了交互：以前单击是"展开 / 收起这一条"，现在单击是把**整块备注区**推上去、
 * 日历收成周视图、所有备注一起展开。单条折叠 / 展开不再是一个维度 —— 展开是"看字"的档位，
 * 该由整个区域一起决定，而不是每条各自为政（那会变成一堆参差不齐的半截文字）。
 */
@OptIn(ExperimentalFoundationApi::class) // 长按要用 combinedClickable，跟 [StatsButton] 一个写法
@Composable
private fun NoteTag(
    text: String,
    dotColor: Color?,
    expanded: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 11.dp)
                .size(10.dp)
                .then(
                    if (dotColor != null) {
                        Modifier.background(dotColor, CircleShape)
                    } else {
                        Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        )
                    },
                ),
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onEdit,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else NOTE_TAG_COLLAPSED_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 收起时留几行。两行大致是一句半，够"想起那天发生了什么"。
 *
 * **展开态不设高度上限**（2026-10-17 之后）：原来这里还有个 `NOTE_TAG_MAX_HEIGHT = 132.dp`，
 * 展开也只给 132dp、超出的部分在标签**内部**滚。可网格下方那块地是**一整块**留给文字的，
 * 给它定一个 132dp 的小格子，等于把地空着不用。现在展开就完整铺开，由**整个列表**去滚。
 */
private const val NOTE_TAG_COLLAPSED_LINES = 2

/**
 * 面板右上角那个"写点什么"的入口。
 *
 * 三档颜色是有区别的，别当装饰看：
 * - 写过字 → 用 `onSurface`，比旁边的灰色重一点，**不点开也知道这天有东西**；
 * - 正在写 → 用 `primary`，跟别处的强调色一致；
 * - 什么都没写 → `onSurfaceVariant`，安静地待着。
 *
 * 44dp 的点击区是照着最小触控目标留的，里头的铅笔只有 19dp —— 手绘的。
 */
@Composable
private fun NoteButton(
    hasNote: Boolean,
    editing: Boolean,
    onClick: () -> Unit,
) {
    val tint = when {
        editing -> MaterialTheme.colorScheme.primary
        hasNote -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            // 图标是手绘的，读屏软件看不见，得自己报个名字。
            .semantics { contentDescription = if (hasNote) "备注，已填写" else "备注" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(19.dp)) { drawPencil(tint) }
    }
}

/**
 * 手绘一支铅笔。
 *
 * 为了这几个图标拖一整个图标库进来不值当（见计划 §7）。几何很简单：
 * 笔身是沿 45° 方向的一根长条，笔尖是接在左下的一个三角形，一笔闭合的轮廓。
 *
 * 归一化到 0..1 再乘实际边长，这样换尺寸不用重算坐标。
 */
private fun DrawScope.drawPencil(color: Color) {
    val w = size.width
    val h = size.height

    // 笔身半宽。乘以 1/√2 得到沿坐标轴的偏移量 —— 笔轴是 45° 的，
    // 垂直方向的单位向量是 (1/√2, 1/√2)。
    val d = 0.115f * 0.7071f

    val path = Path().apply {
        moveTo((0.86f + d) * w, (0.14f + d) * h) // 笔尾一侧
        lineTo((0.86f - d) * w, (0.14f - d) * h) // 笔尾另一侧
        lineTo((0.34f - d) * w, (0.66f - d) * h) // 笔身收到笔尖根部
        lineTo(0.14f * w, 0.86f * h)             // 笔尖端
        lineTo((0.34f + d) * w, (0.66f + d) * h) // 笔尖另一侧
        close()
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

/**
 * 备注输入框。
 *
 * 用 [BasicTextField] 而不是 M3 的 `TextField`：后者自带 label、指示线、固定容器高度，
 * 是一整套"填表单"的语汇，跟这块面板不是一个语气。
 *
 * **没有保存按钮** —— 字是逐字写进 store 的（见 `DayEntryStore.setNote`），
 * 退出方式就是按返回键、或者再点一下铅笔。少一个按钮，也少一次"忘了点保存"的机会。
 */
@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    onExpand: (() -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
) {
    // 第一行可以由外面传一个共享的 requester —— 换天时 NoteEditor 要靠它把焦点接回来。
    // 其余行用自己的。internalRequester 总是 remember，保证 slot 表稳定。
    val internalRequester = remember { FocusRequester() }
    val requester = focusRequester ?: internalRequester

    // 用 TextFieldValue 而不是裸 String：光标要落在**文字末尾**，不是开头。
    // 裸 String 重载时 BasicTextField 自己内部维护 selection、初值是 TextRange(0) ——
    // 于是"点铅笔进来"这类**程序聚焦**，光标停在第一个字前面，想接着往下写还得先点一下末尾。
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }

    // 这一格的内容被外面换掉时（换天、换槽、别处写了）跟着走，光标仍落在末尾。
    LaunchedEffect(value) {
        if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    }

    LaunchedEffect(Unit) {
        // 点铅笔进来就是要打字的，直接聚焦把键盘弹出来，省掉"还得再点一下输入框"这一步。
        // 编辑态是"一个颜色一行"，所以只有第一行抢焦点 —— 每行都抢的话，
        // 最后落到哪一行全看组合顺序，那不成体统。
        if (autoFocus) runCatching { requester.requestFocus() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp, max = 120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        BasicTextField(
            value = field,
            onValueChange = {
                field = it
                onValueChange(it.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                // 右边**留出 34dp** 给全屏图标：它钉在右下角，文字因此永远不会跑到它底下。
                // 用户 2026-10-17 特意提醒过 —— "图标遮住已经输入的文字"比没有图标更糟。
                .padding(start = 14.dp, end = 34.dp, top = 12.dp, bottom = 12.dp)
                .focusRequester(requester)
                // 焦点是编辑态唯一的"还在这里写"信号（2026-10-29 起不再看键盘高度）。
                .onFocusChanged { onFocusChanged?.invoke(it.isFocused) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box {
                    // 这句提示只在**你已经点开铅笔之后**才出现，不是 App 主动来催你写。
                    // 措辞刻意留白、不用"今天还没记录哦"那种口气（见计划 §2 红线二）。
                    if (field.text.isEmpty()) {
                        Text(
                            text = "写点什么…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (onExpand != null) {
            NoteExpandButton(
                onClick = onExpand,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp),
            )
        }
    }
}

/**
 * 输入框右下角那个「铺满屏幕去写」的小图标。
 *
 * 它的存在理由很具体：一个框最多显示四行，长的引文写不下 —— 但**不是所有人都需要全屏**，
 * 所以这里给的是一个"抬手就能点"的入口，而不是把每个人强行拽进全屏。
 *
 * 触摸区 30dp 比图形（16dp）大一圈：图标本身小，手指得够得着。
 */
@Composable
private fun NoteExpandButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "全屏写这条备注" },
        contentAlignment = Alignment.Center,
    ) {
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(Modifier.size(16.dp)) { drawExpandGlyph(color) }
    }
}

/**
 * 四个角（⛶）—— "铺满屏幕"最通用的画法。
 *
 * 一开始画的是两个对角折线（↖ ↘），真机上根本看不出是什么：图标只有 16dp，
 * 两个孤立的角容易被读成排版错位。四个角合起来才是一个明确的"全屏"符号。
 */
private fun DrawScope.drawExpandGlyph(color: Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val arm = 0.32f // 每个角的两条边各画多长（占整个图标的比例）

    /** 一个角：从竖边的一点折到顶点、再折到横边的一点。(dx, dy) 决定它朝向哪个角。 */
    fun corner(x: Float, y: Float, dx: Float, dy: Float) = Path().apply {
        moveTo(x + dx * arm * w, y)
        lineTo(x, y)
        lineTo(x, y + dy * arm * h)
    }

    drawPath(corner(0.06f * w, 0.06f * h, 1f, 1f), color, style = stroke)
    drawPath(corner(0.94f * w, 0.06f * h, -1f, 1f), color, style = stroke)
    drawPath(corner(0.06f * w, 0.94f * h, 1f, -1f), color, style = stroke)
    drawPath(corner(0.94f * w, 0.94f * h, -1f, -1f), color, style = stroke)
}

/**
 * 铺满屏幕写一条备注。
 *
 * ## 为什么要它
 *
 * 面板里的输入框最高 120dp（大约四行）。写引文、写长日记时四行不够用 —— 用户 2026-10-17 说得很直白：
 * "我们的输入框可以放下四行文字，如果这还不够用，那么我觉得应该能够打开一个更纯粹的文本编辑界面"。
 *
 * ## 为什么是"抬手点图标进"，不是"写满了自动跳"
 *
 * 自动跳会把正在打字的人从当前排版里拽走（光标位置、周围上下文全变），而短备注根本不需要它。
 * 所以入口做成输入框右下角一个 30dp 的图标 —— 进不进由写字的人自己定。
 * 图标会遮字这一点是用户特意提的，所以输入框右侧**永久留出 34dp**，文字不会跑到它底下。
 *
 * ## 为什么底部只有一个「完成」
 *
 * 这个页面只负责**写字**。换颜色归属、删掉这条这类动作仍然留在长按进来的那张小卡片里。
 * `完成` 也不是"提交"—— 字早就逐字落库了（`DayEntryStore.setNote`），它只是"把屏幕还给我"。
 */
@Composable
private fun NoteFullScreenEditor(
    date: LocalDate,
    colorId: String,
    dotColor: Color?,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val paletteColor = remember(colorId) { Palette.byId(colorId) }
    val focusRequester = remember { FocusRequester() }

    // 编辑区自己的滚动状态（2026-10-30 晚加）。
    val editorScroll = rememberScrollState()

    // 同 NoteField：光标落在文字末尾，不是开头（程序聚焦时 selection 初值是 0）。
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    }

    // 进来就是要写字的：直接把焦点给到文本框、键盘跟着弹起来，不用再点一下。
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // 键盘弹起时，把**光标**带进视口 —— 不是无脑滚到底（2026-10-30 第三次修）。
    //
    // 前两版无脑 `scrollTo(maxValue)`：光标在末尾时没问题，但用户把光标放到开头
    //（「自|定义」）再唤出键盘，整段会被推到底、开头那几行直接顶出屏幕。
    // 现在按光标的实际位置算：让它落在视口靠上三分之一处，上下都留余量。
    // 光标在哪儿就滚到哪儿，不再假设"总在末尾"。
    //
    // `maxValue` 仍然作为 key：键盘动画期间容器连续变矮、视口高度在变，要跟着重算。
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // 编辑区滚动视口的高度。`ScrollState.viewportSize` 在这个 Compose 版本上取不到，
    // 就在容器上自己记一个 —— 反正我们只需要知道"能看见多高"。
    var viewportHeight by remember { mutableStateOf(0) }
    val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeHeight, viewportHeight, editorScroll.maxValue) {
        if (imeHeight <= 0) return@LaunchedEffect
        val layout = textLayout ?: return@LaunchedEffect
        if (viewportHeight <= 0) return@LaunchedEffect
        delay(50)
        val cursorTop = layout.getCursorRect(field.selection.end).top
        val target = (cursorTop - viewportHeight / 3f).roundToInt()
            .coerceIn(0, editorScroll.maxValue)
        editorScroll.scrollTo(target)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // decorFitsSystemWindows = false 让内容顶到系统栏底下（跟主界面一致），
                    // 所以这里要自己把状态栏那一条让出来，否则"收起"会被时间/电量压住。
                    .statusBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                // 顶栏：收起 + 这条挂在谁身上（**只读**）。写字的人得知道自己在写哪一条，
                // 但改归属是另一件事，留在长按那张卡片里 —— 一个页面只做一件事。
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                            .semantics { contentDescription = "收起" },
                        contentAlignment = Alignment.Center,
                    ) {
                        val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        Canvas(Modifier.size(18.dp)) { drawChevronDown(iconColor) }
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .then(
                                if (dotColor != null) {
                                    Modifier.background(dotColor, CircleShape)
                                } else {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = CircleShape,
                                    )
                                },
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${date.monthValue}月${date.dayOfMonth}日 · " +
                            (paletteColor?.displayName() ?: "不归属任何颜色"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 输入框按内容长高，滚动交给外面这一层（见函数开头 editorScroll 的说明）。
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onSizeChanged { viewportHeight = it.height }
                        .verticalScroll(editorScroll),
                ) {
                    BasicTextField(
                        value = field,
                        onValueChange = {
                            field = it
                            onValueChange(it.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            // 行距松一点：全屏写长文时，行挤在一起读着累。
                            lineHeight = 26.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        // 记住排版结果，键盘弹起时按光标在文本里的实际 y 算滚动目标。
                        onTextLayout = { textLayout = it },
                        decorationBox = { innerTextField ->
                            Box(Modifier.fillMaxWidth()) {
                                if (field.text.isEmpty()) {
                                    Text(
                                        text = "写点什么…",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("完成") }
                }
            }
        }
    }
}

/** 顶栏那个"收起来"的折角（∨）。 */
private fun DrawScope.drawChevronDown(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(0.16f * w, 0.34f * h)
        lineTo(0.50f * w, 0.68f * h)
        lineTo(0.84f * w, 0.34f * h)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/**
 * 长按网格下方一条备注之后弹出来的「改这一句」卡片。
 *
 * 一次做完两件事：改文字、改这句话挂在哪一个颜色上。
 *
 * 为什么不做成"跳进面板的编辑态"：面板说的是"给这一天写点什么"—— 按颜色分行、一行一个框；
 * 而长按进来的用户心里想的是"改**这一句**"。让人再在一堆框里把那一行找出来，等于把已经
 * 指明白的对象又让他重找一遍。所以这里只摆这一条，顺手把候选颜色一并摊开。
 *
 * 候选颜色**给全部十二色 + 一个「不归属」**（十三个位置，6 + 7 分两排摆）。
 *
 * 这里改过一次：早先只给"当天涂了的颜色 + 不归属"，理由是挪到一个没涂的颜色上，那句话就会从
 * 网格下方消失。2026-10-17 用户认可了另一条语义 —— **备注挂在哪个颜色上，和"这天涂了什么色"
 * 是两件事**，于是干脆全给。挂到一个没涂的颜色上不会让日历色条多出一段，那句话也照样列在网格
 * 下方，因为 [NoteTagList] 认的是"这个槽里有字"（[noteSlots]），不认"这天涂过"。
 *
 * 文字照旧**逐字落库**（见 `DayEntryStore.setNote`），所以这里同样没有"保存"。
 */
@Composable
private fun NoteEditDialog(
    date: LocalDate,
    entry: DayEntry?,
    initialColorId: String,
    onNoteChange: (String, String) -> Unit,
    onMoveNote: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val notes = entry?.notes.orEmpty()

    // 这句话现在挂在哪个槽，跟着用户点的色点走。
    var slot by remember(initialColorId) { mutableStateOf(initialColorId) }

    // 弹窗里再叠一层全屏编辑器（图四）。四行装不下的时候，从这里抬手就能进去写。
    var showingFullScreen by remember(initialColorId) { mutableStateOf(false) }

    // 「清除」是两段式的：第一下只把文字换成「确认清除」，第二下才真的清空。
    // 这是这个弹窗里唯一"删了就没了"的动作，不该一次误触就生效。
    var confirmClear by remember(initialColorId) { mutableStateOf(false) }

    // 文本另存一份草稿，而不是每次都去读 notes[slot]。
    // 原因出在换归属那一步：DayEntryStore.moveNote 会把两个槽的内容**互换**
    // （一个槽就是一句备注，不能出现两句并存），互换之后目标槽里装的正是手里这句 ——
    // 所以草稿原样留着即可。要是直接读 notes[slot]，换归属的一瞬间会闪出另一句话。
    // 同样用 TextFieldValue 而不是裸 String：长按进来是**程序聚焦**，
    // 裸 String 时 selection 初值是 0，光标会停在第一个字前面。
    val initialText = notes[initialColorId].orEmpty()
    var draft by remember(initialColorId) {
        mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "改这条备注",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(12.dp))

                // 输入框 + 右下角的全屏图标（图二 / 图三）：四行装不下时，在这里就能进全屏，
                // 不用先收回面板、再在那一行里找图标。
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp, max = 200.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = {
                            draft = it
                            onNoteChange(slot, it.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            // 右边**留出 34dp** 给全屏图标：同 NoteField，文字永不跑到图标底下。
                            .padding(start = 14.dp, end = 34.dp, top = 12.dp, bottom = 12.dp)
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box {
                                if (draft.text.isEmpty()) {
                                    Text(
                                        text = "写点什么…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                            .copy(alpha = 0.55f),
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    NoteExpandButton(
                        onClick = { showingFullScreen = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp, bottom = 4.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "挂在哪个颜色上",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(10.dp))

                // 候选 = **十二色全给** + 一个「不归属」。分两排摆（6 + 7），跟底部色盘的
                // 排法一致 —— 十三个挤一行既放不下、也不好看。
                //
                // 放开到全部十二色是有意的：备注挂在哪个颜色上，和"这天涂了什么色"是
                // 两件事（你 2026-10-17 认可了这条语义）。挂到一个没涂的颜色上**不会**
                // 让日历色条多出一段；那句话照样在下面列着，因为 `noteSlots` 认"有字"，
                // 不认"这天涂过"。
                val candidates = Palette.colors.map { it.id } + NOTE_WITHOUT_COLOR

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // 硬分两排：前六个一排、后七个一排。写成 `chunked(6)` 会得到
                    // 6 + 6 + 1 ——「不归属」自己占一排、右边拖出一条空白，看着像漏了一个
                    // （用户 2026-10-17 指出）。注释说"6 + 7"而代码在按 6 切，是同一个老毛病。
                    listOf(candidates.take(6), candidates.drop(6)).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            // **居中**，不是左对齐：6 个那排宽 236dp、7 个那排宽 276dp，
                            // 差的 40dp 正好是一个间距 —— 各自居中之后，下排天然错开半个
                            // 间距，每个圆落在上排两个圆的缝里，读起来是个梯形。
                            // 左对齐会变成上下两排整整齐齐对着，呆板（用户 2026-10-17 指出）。
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            row.forEach { id ->
                                NoteSlotDot(
                                    colorId = id,
                                    dotColor = recordColor(entry, id),
                                    // 只要当前挂在任何一个无归属槽上，「不归属」那个圆就是选中态
                                    selected = id == slot ||
                                        (id == NOTE_WITHOUT_COLOR && isColorlessSlot(slot)),
                                    onClick = {
                                        // 「不归属」点下去不是挪到那个固定的空串槽，而是挪到
                                        // **当下第一个还空着的无归属槽** —— 无归属槽不唯一，
                                        // 这样它才真的是一层"缓存"（见 `DayEntry.kt`）。
                                        val target = if (id == NOTE_WITHOUT_COLOR) {
                                            nextFreeColorlessSlot(notes - slot)
                                        } else {
                                            id
                                        }
                                        if (target != slot) {
                                            onMoveNote(slot, target)
                                            slot = target
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = {
                            if (confirmClear) {
                                // 清空这个槽的备注。颜色归属不动 —— 删的是一句话，不是那次心情。
                                onNoteChange(slot, "")
                                onDismiss()
                            } else {
                                confirmClear = true
                            }
                        },
                    ) {
                        Text(
                            text = if (confirmClear) "确认清除" else "清除",
                            // error 色是 Material 语义里的"危险动作"。这里只给这一颗按钮用。
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("完成") }
                }
            }
        }
    }

    // 全屏编辑器叠在这一层上面（图四）。它和弹窗共用同一份草稿：
    // 全屏里打的字，退回弹窗时原样在那儿。
    if (showingFullScreen) {
        NoteFullScreenEditor(
            date = date,
            colorId = slot,
            dotColor = recordColor(entry, slot),
            value = draft.text,
            onValueChange = { text ->
                draft = TextFieldValue(text, TextRange(text.length))
                onNoteChange(slot, text)
            },
            onDismiss = { showingFullScreen = false },
        )
    }
}

/**
 * 「改这一句」弹窗里的一个候选归属：实心色点，选中时外面套一圈墨色环。
 *
 * 触摸区 36dp 套 20dp 的点 —— 一天最多十三个候选挤在一行里，点本身必须小，
 * 但手指落点不能小（跟 [NoteButton] 那个 44dp 是同一个道理）。
 * 环用透明色占位，选不选中都不改变尺寸，点不会跳。
 */
@Composable
private fun NoteSlotDot(colorId: String, dotColor: Color?, selected: Boolean, onClick: () -> Unit) {
    val paletteColor = remember(colorId) { Palette.byId(colorId) }
    // 读屏名字要在组合上下文里先算好 —— semantics 的 lambda 不是 @Composable。
    val spokenName = paletteColor?.displayName() ?: "不归属任何颜色"

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            // 色点读屏软件念不出来，得自己报个名字。
            .semantics { contentDescription = spokenName },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .then(
                    if (dotColor != null) {
                        Modifier.background(dotColor, CircleShape)
                    } else {
                        Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        )
                    },
                ),
        )
    }
}

/**
 * 卡片左侧那条竖直色标。
 *
 * 没涂色时是**描边空条**，不是一条浅灰实心 —— 跟 8.1.1 定下的"空白色块一律描边"是同一条规矩：
 * 用户得一眼分清"这天什么都没涂"和"这天涂了云灰"。
 */
@Composable
private fun DayColorMarker(colors: List<Color>) {
    val shape = RoundedCornerShape(3.dp)
    val modifier = Modifier
        .width(5.dp)
        .height(42.dp)
        .clip(shape)

    if (colors.isEmpty()) {
        Box(
            modifier = modifier.border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            ),
        )
        return
    }

    Column(modifier = modifier) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(color),
            )
        }
    }
}

/** 一天涂了 4 种以上时只列前三种，剩下的用"等 N 种"收尾，免得卡片被撑成两行。 */
private const val MAX_NAMES_IN_CARD = 3

@Composable
private fun describeColors(colors: List<PaletteColor>): String {
    if (colors.isEmpty()) return "还没涂色"
    // 名字可能是用户起的、也可能因为改过色而显示「自定义」—— 统一走 displayName。
    // 先在组合上下文里把名字解出来，再交给普通字符串函数（joinToString 的 lambda 不是 @Composable）。
    val names = colors.map { it.displayName() }
    if (names.size <= MAX_NAMES_IN_CARD) return names.joinToString(" · ")
    val head = names.take(MAX_NAMES_IN_CARD).joinToString(" · ")
    return "$head 等 ${names.size} 种"
}

