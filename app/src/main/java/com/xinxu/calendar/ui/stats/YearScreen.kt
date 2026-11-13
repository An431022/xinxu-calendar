package com.xinxu.calendar.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xinxu.calendar.data.DayEntry
import com.xinxu.calendar.data.Palette
import com.xinxu.calendar.data.StatsMode
import com.xinxu.calendar.data.TemperatureTally
import com.xinxu.calendar.data.computeYearTally
import com.xinxu.calendar.ui.theme.currentColor
import com.xinxu.calendar.ui.theme.recordColor
import java.time.LocalDate

/**
 * 一排放几个。8 是用户 2026-11-13 定的：既能铺满手机宽度，又不会把格子压得太小。
 */
private const val COLUMNS = 8

/**
 * 年度视图：一整年的颜色，一格一天，像 GitHub 的绿墙，但**竖着放**。
 *
 * 用户 2026-11-13 的两个硬要求：
 * 1. **一排 8 个色块**，按日期连续往下排；
 * 2. **月份穿插在排列之间**，不另起一排打断连续性 ——
 *    所以这里是"每 8 天一行、行尾标月份"，月份只是行尾的一行小字，不占行、不换行。
 *
 * 与月视图的「颜色是数据」保持一致：格子用的是**涂色当时的快照色**，
 * 改色板不会让这一年变样。一天多色就把格子横向等分，和日历格里的色条同一个语义。
 */
@Composable
fun YearScreen(
    year: Int,
    entries: List<DayEntry>,
    onClose: () -> Unit,
    onYearChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byDate = remember(entries) { entries.associateBy { it.date } }
    val rows = remember(year) { yearRows(year) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            YearHeader(year = year, onClose = onClose, onYearChange = onYearChange)

            // 一整年 365+ 格，**只组合视口内看得见的行**（开发计划 8.15 定的对策：
            // "用 Canvas 一次性绘制或 LazyGrid 虚拟化，不做 365 个独立 View"）。之前是
            // 非 lazy 的 Column + verticalScroll，378 个带 clip 图层的格子全量组合，
            // 滚动时每帧都在画不在屏幕上的格子。
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(rows) { row -> YearFlowRow(row, byDate) }
                item {
                    // 滚到墙底下，才看到这一年"整体偏哪边"。
                    YearTrendFooter(
                        entries = entries,
                        year = year,
                        modifier = Modifier.padding(top = 28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun YearHeader(year: Int, onClose: () -> Unit, onYearChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp, bottom = 20.dp),
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
                text = "${year}年",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "全年 · 一格一天",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        YearArrow(left = true, onClick = { onYearChange(year - 1) })
        Spacer(Modifier.width(4.dp))
        YearArrow(left = false, onClick = { onYearChange(year + 1) })
    }
}

/** 一格：8 列里的一格。有颜色就铺满，多色横向等分；没涂就是比背景深一点点的底。 */
@Composable
private fun YearCell(date: LocalDate, entry: DayEntry?, modifier: Modifier = Modifier) {
    val colors = entry?.colorIds.orEmpty().mapNotNull { recordColor(entry, it) }
    // 未涂色的底色跟日历格里那条"极短的一小节"用同一个思路：只比背景深一点点，
    // 不制造"这里还空着"的催促感（设计红线 2）。
    val emptyColor = lerp(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.onSurface,
        0.06f,
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp)),
    ) {
        if (colors.isEmpty()) {
            Box(Modifier.fillMaxSize().background(emptyColor))
        } else {
            Row(Modifier.fillMaxSize()) {
                colors.forEach { color ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(color),
                    )
                }
            }
        }
    }
}

/** 一行：8 个格子位（日期格 + 月份格），月份直接占其中一格。月份**不另起一行**。 */
@Composable
private fun YearFlowRow(row: YearRow, byDate: Map<LocalDate, DayEntry>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        row.slots.forEach { slot ->
            when (slot) {
                is YearSlot.Day -> YearCell(
                    date = slot.date,
                    entry = byDate[slot.date],
                    modifier = Modifier.weight(1f),
                )

                // 月份格就是"格子流里的一格"：和日期格同宽同高，只是里面写字。
                // 这样右侧不需要另留一条标签区，整面墙能在屏幕里居中（用户 2026-11-13 要求）。
                is YearSlot.Month -> Box(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = slot.label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // 最后一行不够 8 个时补空位，否则剩下的格子会被拉宽、和上面的对不齐。
        repeat(row.fill) {
            Spacer(Modifier.weight(1f))
        }
    }
}

/** 格子流里的一项：要么是某一天，要么是某个月的标签格。 */
private sealed interface YearSlot {
    data class Day(val date: LocalDate) : YearSlot
    data class Month(val label: String) : YearSlot
}

private data class YearRow(val slots: List<YearSlot>, val fill: Int)

/**
 * 把一年切成每行 8 个格子位。
 *
 * 月份格插在**该月 1 日之前**：读起来就是「3月」接着 3/1、3/2……
 * 它占掉的也是 8 列里的一格，所以行永远是满的、右边不会空出一条。
 */
private fun yearRows(year: Int): List<YearRow> {
    val slots = mutableListOf<YearSlot>()
    var cursor = LocalDate.of(year, 1, 1)
    while (cursor.year == year) {
        if (cursor.dayOfMonth == 1) {
            slots += YearSlot.Month("${cursor.monthValue}月")
        }
        slots += YearSlot.Day(cursor)
        cursor = cursor.plusDays(1)
    }
    return slots.chunked(COLUMNS).map { chunk ->
        YearRow(slots = chunk, fill = COLUMNS - chunk.size)
    }
}

/** 手绘的叉（同统计页），不用 "✕" 字符 —— 字体自己会把它渲成一个脏点。 */
@Composable
private fun CloseCross() {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(16.dp)) {
        val w = size.minDimension * 0.16f
        drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), w, StrokeCap.Round)
        drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
    }
}

/** 年份左右切换：手绘的尖括号，40dp 点击区。 */
@Composable
private fun YearArrow(left: Boolean, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(10.dp)) {
            val w = size.minDimension * 0.18f
            val inset = size.width * 0.18f
            // left = "<"：竖线在右边、尖在左边；right = ">"：反过来。
            val lineX = if (left) size.width - inset else inset
            val tipX = if (left) inset else size.width - inset
            val top = Offset(lineX, 0f)
            val bottom = Offset(lineX, size.height)
            val tip = Offset(tipX, size.height / 2f)
            // 用两条线拼成 "<" / ">"，避免字体把箭头字符渲染成别的东西。
            drawLine(color, top, tip, w, StrokeCap.Round)
            drawLine(color, tip, bottom, w, StrokeCap.Round)
        }
    }
}

/**
 * 墙底下的冷暖趋势。
 *
 * 口径跟统计页一致（见 [computeYearTally]），但**按天算**：年度视图本来就是"一格一天"，
 * 一天一票、取当天第一个颜色，跟墙上看到的密度对得上。
 * 结论沿用统计页那种模糊说法，不写百分比 —— 一旦写成"暖 62%"，它就从感受变成了指标。
 */
@Composable
private fun YearTrendFooter(entries: List<DayEntry>, year: Int, modifier: Modifier = Modifier) {
    val trend = remember(entries, year) { computeYearTally(entries, year) }
    val tally = trend.tally(StatsMode.BY_DAY_LEAD)

    val warmColor = Palette.byId("warm_flame")?.currentColor()
        ?: MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "冷暖趋势",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))

        if (trend.coloredDays == 0) {
            Text(
                text = "今年还没有颜色",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            YearTallyBar(tally)
            Spacer(Modifier.height(10.dp))
            Text(
                // 计数不是百分比：它只描述"出现过多少"，不构成完成度。
                text = "暖 ${tally.warm} · 冷 ${tally.cool} · 中性 ${tally.neutral}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            val verdict = when {
                tally.warm > tally.cool -> "今年偏暖"
                tally.cool > tally.warm -> "今年偏冷"
                else -> "冷暖相当"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(warmColor, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = verdict,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** 一年的冷暖比例条：暖色取自色板（朱红），冷色取自深海，不另配"图表色"。 */
@Composable
private fun YearTallyBar(tally: TemperatureTally) {
    val warmColor = Palette.byId("warm_flame")?.currentColor()
        ?: MaterialTheme.colorScheme.primary
    val coolColor = Palette.byId("cool_deep")?.currentColor()
        ?: MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (tally.warm + tally.cool > 0) {
            Row(Modifier.fillMaxSize()) {
                if (tally.warm > 0) {
                    Box(
                        Modifier
                            .weight(tally.warm.toFloat())
                            .fillMaxHeight()
                            .background(warmColor),
                    )
                }
                if (tally.cool > 0) {
                    Box(
                        Modifier
                            .weight(tally.cool.toFloat())
                            .fillMaxHeight()
                            .background(coolColor),
                    )
                }
            }
        }
    }
}
