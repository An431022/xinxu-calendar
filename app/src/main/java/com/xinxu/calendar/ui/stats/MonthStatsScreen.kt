package com.xinxu.calendar.ui.stats

import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.xinxu.calendar.data.DayEntry
import com.xinxu.calendar.data.MonthStats
import com.xinxu.calendar.data.Palette
import com.xinxu.calendar.data.StatsMode
import com.xinxu.calendar.data.TemperatureTally
import com.xinxu.calendar.data.Verdict
import com.xinxu.calendar.data.verdict
import com.xinxu.calendar.ui.theme.currentColor
import com.xinxu.calendar.ui.theme.recordColor
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * 月统计页：把一个月摊开看一眼。
 *
 * 这不是"看板"，是**回望**。所以整页没有任何数字指标、没有进度、没有对比上个月 ——
 * 只有颜色本身、冷暖的大致倾向，和一句结论（设计红线 4）。
 *
 * 三块内容，按重要性排：
 * 1. **颜色方阵** —— 本月涂过的日子，一天一格、按日期顺序铺成接近正方形。这是主角。
 * 2. **冷暖比例** —— 一条比例条。放在方阵下面，因为它是"看完了之后的感受"，不是开场的数字。
 * 3. **结论** —— 一句话。刻意模糊，不给百分比。
 */

/** 方阵里小方格之间的缝。 */
private val CELL_GAP = 6.dp

/**
 * 格子的上限边长。
 *
 * 两个方向都试过了才定下来：格子撑满宽度时，只涂了三天的一个月会变成三个巨大的方块，很唬人；
 * 反过来卡得太小（54dp）时，31 天的 6 列只占屏幕左半边，右边空一大片，整页重心偏掉。
 * 64dp 是让 6 列基本铺满、3 列又不会显得孤零零的那一档。
 */
private val CELL_MAX = 64.dp

/**
 * 统计页的入场序列：**方阵逐格铺开 → 比例条长出来 → 结论落下**。
 *
 * 这三步的顺序不是随便排的，它就是这个页面在讲的那件事的顺序：
 * 先看见"我这个月一天天走过的颜色"，再看见从这些颜色里数出来的比例，最后才落到那一句话上。
 * 反过来的话（一打开结论就砸在脸上），这个页面就变成了一句评判，而不是一次回望。
 *
 * 总时长压在 ~0.8s：它每天只被打开一两次，允许比别处的动效慢一点、有一点仪式感；
 * 但再慢就会变成"每次都要等它演完"。
 */
private const val MOSAIC_STAGGER_MS = 14

/** 错峰最多累计到这么久。31 天全涂满时不能再线性排下去，否则最后一格要等大半秒才冒头。 */
private const val MOSAIC_STAGGER_CAP_MS = 300
private const val MOSAIC_SCALE_FROM = 0.72f

private const val BAR_DELAY_MS = 260

private const val VERDICT_DELAY_MS = 520

@Composable
fun MonthStatsScreen(
    stats: MonthStats,
    onClose: () -> Unit,
    onOpenYear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(StatsMode.BY_COLOR_COUNT) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            StatsHeader(stats, onClose, onOpenYear)

            if (stats.isEmpty) {
                EmptyMonthHint()
            } else {
                SectionTitle("颜色方阵")
                ColorMosaic(stats.coloredDays)

                Spacer(Modifier.height(30.dp))
                SectionTitle("冷暖比例", trailing = { ModeToggle(mode) { mode = it } })
                Spacer(Modifier.height(14.dp))
                TemperatureBar(stats.tally(mode))
                Spacer(Modifier.height(12.dp))
                TallyLegend(stats.tally(mode))

                Spacer(Modifier.height(30.dp))
                VerdictLine(stats.verdict(mode))
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ---------------------------------------------------------------- 头部

@Composable
private fun StatsHeader(stats: MonthStats, onClose: () -> Unit, onOpenYear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            CloseCross()
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "${stats.yearMonth.year}年${stats.yearMonth.monthValue}月",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "情绪总览",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        // 年度视图的入口：统计是"看数据"的家，全年是它的另一个视角。
        // 放在这一页而不是月视图顶栏 —— 顶栏已经有统计、设置、今天三个入口了，再加会挤。
        TextButton(onClick = onOpenYear) {
            Text("全年")
        }
    }
}

/** 手绘的叉。不用 "✕" 字符 —— 之前那个 "▾" 就是字体把它渲染成了一个脏点。 */
@Composable
private fun CloseCross() {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(16.dp)) {
        val w = size.minDimension * 0.16f
        drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), w, StrokeCap.Round)
        drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
    }
}

@Composable
private fun SectionTitle(text: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        trailing?.invoke()
    }
}

// ---------------------------------------------------------------- 颜色方阵

/**
 * 本月涂过的日子，一天一格，**按日期顺序**铺成接近正方形的网格。
 *
 * 为什么只放涂过的日子、不放整月 31 格：那会变成一排空框，跟日历格里被否掉的"复选框"是同一个毛病
 * （见计划 8.1.1）。而这里要的是"我这个月走过的颜色"，留白本来就不该出现在一张**回望**的图上。
 */
@Composable
private fun ColorMosaic(days: List<DayEntry>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cols = ceil(sqrt(days.size.toFloat())).toInt().coerceAtLeast(1)
        val cell = min(
            (maxWidth - CELL_GAP * (cols - 1)) / cols,
            CELL_MAX,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CELL_GAP),
        ) {
            days.withIndex().chunked(cols).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                    row.forEach { (index, day) -> MosaicCell(day, cell, index) }
                    // 最后一行补空格子把整行撑到和别的行一样宽。
                    // 补了之后每行等宽，居中时各列仍然对齐；不补的话最后一行会缩成半宽、居中后往右偏。
                    repeat(cols - row.size) { Spacer(Modifier.size(cell)) }
                }
            }
        }
    }
}

@Composable
private fun MosaicCell(day: DayEntry, size: Dp, index: Int) {
    // 用**涂色当时**的色值：改色板不该让已经过去的这个月换个颜色。
    val colors = day.colorIds.mapNotNull { recordColor(day, it) }

    // 逐格冒出来：每格比前一格晚一点点，整个方阵看着像"一天一天铺开的"。
    // 按日期顺序错峰（而不是按对角线）是有意的 —— 铺开的节奏应该跟时间的节奏一致。
    // 时长与曲线交给 M3E 的 MotionScheme，只有"错峰多少"是我们自己的话。
    val motionScheme = MaterialTheme.motionScheme
    val stagger = (index * MOSAIC_STAGGER_MS).coerceAtMost(MOSAIC_STAGGER_CAP_MS)
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // 错峰用"先等一会儿再开动画"，而不是把 delay 塞进 AnimationSpec ——
        // MotionScheme 给的是弹簧，弹簧本身不含"什么时候开始"。
        if (stagger > 0) delay(stagger.toLong())
        enter.animateTo(
            targetValue = 1f,
            animationSpec = motionScheme.defaultSpatialSpec<Float>(),
        )
    }

    Row(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                val s = MOSAIC_SCALE_FROM + (1f - MOSAIC_SCALE_FROM) * enter.value
                scaleX = s
                scaleY = s
                alpha = enter.value
            }
            .clip(RoundedCornerShape(size * 0.24f)),
    ) {
        if (colors.isEmpty()) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
        } else {
            // 一天多色时在格内**竖着等分**，跟日历格那条小横条的分法一致（从左到右＝点的先后）。
            colors.forEach { color ->
                Box(Modifier.weight(1f).fillMaxHeight().background(color))
            }
        }
    }
}

// ---------------------------------------------------------------- 冷暖比例

@Composable
private fun ModeToggle(mode: StatsMode, onChange: (StatsMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        StatsMode.entries.forEach { candidate ->
            val selected = candidate == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onChange(candidate) }
                    .padding(horizontal = 13.dp, vertical = 5.dp),
            ) {
                Text(
                    text = candidate.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/**
 * 冷暖比例条。
 *
 * 用的颜色**取自十二色本身**（朱红 / 深海），不是另配的"图表色" ——
 * 屏幕上所有的色相都应该来自那套色板，否则这一条会显得跟整个应用不是一套东西。
 *
 * 中性色不画进条里（它不表达冷暖），只在下面的图例里单独列出来。
 */
@Composable
private fun TemperatureBar(tally: TemperatureTally) {
    val warmColor = Palette.byId("warm_flame")?.currentColor() ?: MaterialTheme.colorScheme.primary
    val coolColor = Palette.byId("cool_deep")?.currentColor() ?: MaterialTheme.colorScheme.secondary
    val warm = tally.warm
    val cool = tally.cool

    // 从左往右长出来，而且**比方阵晚一点**才开始动 —— 它讲的是"从方阵里数出来的结果"，
    // 抢在方阵前面出现，顺序就反了。切口径时也重播一次，让"票数变了"这件事被看见。
    val motionScheme = MaterialTheme.motionScheme
    val grow = remember { Animatable(0f) }
    LaunchedEffect(warm, cool) {
        if (warm + cool == 0) return@LaunchedEffect
        grow.snapTo(0f)
        if (BAR_DELAY_MS > 0) delay(BAR_DELAY_MS.toLong())
        grow.animateTo(
            targetValue = 1f,
            animationSpec = motionScheme.defaultSpatialSpec<Float>(),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (warm + cool == 0) return@Box
        // 比例条"从左往右长出来"走**绘制层**（scaleX），而不是 `fillMaxWidth(grow.value)` 那种
        // 布局层动画 —— 后者每帧改一次宽度分数、触发 measure/layout；缩放只重绘、不重排。
        Row(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = grow.value
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
        ) {
            if (warm > 0) Box(Modifier.weight(warm.toFloat()).fillMaxHeight().background(warmColor))
            if (cool > 0) Box(Modifier.weight(cool.toFloat()).fillMaxHeight().background(coolColor))
        }
    }
}

@Composable
private fun TallyLegend(tally: TemperatureTally) {
    Text(
        text = "暖 ${tally.warm}　·　冷 ${tally.cool}　·　中性 ${tally.neutral}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------- 结论

@Composable
private fun VerdictLine(verdict: Verdict) {
    val color = when (verdict) {
        Verdict.WARM -> Palette.byId("warm_flame")?.currentColor() ?: MaterialTheme.colorScheme.onSurface
        Verdict.COOL -> Palette.byId("cool_deep")?.currentColor() ?: MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val motionScheme = MaterialTheme.motionScheme
    val land = remember { Animatable(0f) }
    LaunchedEffect(verdict) {
        land.snapTo(0f)
        if (VERDICT_DELAY_MS > 0) delay(VERDICT_DELAY_MS.toLong())
        land.animateTo(
            targetValue = 1f,
            animationSpec = motionScheme.defaultSpatialSpec<Float>(),
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        // 最后落地：一句结论应该像是"看完前面所有东西之后浮现的"，所以要等方阵和比例条都走完。
        // 位移只有几个 dp —— 它是"落下来"，不是"滑进来"。
        modifier = Modifier.graphicsLayer {
            alpha = land.value
            translationY = (1f - land.value) * 6.dp.toPx()
        },
    ) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(11.dp))
        Text(
            text = verdict.label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 一个月一天都没涂的样子。
 *
 * 这一段是设计红线 2 的直接体现：**留白不是失败**。
 * 所以这里不写"本月未记录"，也不写"快去记录吧" —— 只说一句"没关系"。
 */
@Composable
private fun EmptyMonthHint() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 60.dp)) {
        Text(
            text = "这个月还是空的",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        // 2026-10-29 换的文案（原来的"想涂的时候随时来，不涂也没关系"）。
        // 新版把"空白"说成"还在等",而不是"还没做"—— 主语是时光，不是用户，
        // 所以读起来是一句描述，不是一句提醒。这是红线二在措辞上的延续。
        Text(
            text = "时光正在等待被涂上第一抹色彩",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
    }
}
