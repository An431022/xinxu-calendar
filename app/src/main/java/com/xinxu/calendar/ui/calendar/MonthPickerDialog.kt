package com.xinxu.calendar.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.YearMonth

private const val MONTHS_PER_ROW = 4

/**
 * 年月跳转。
 *
 * 为什么必须有：左右滑动翻月只适合"隔一两个月"。要看去年这个月得划 12 次，
 * 一年之后这个 App 才算真正开始有用，那时候划不动就是硬伤。
 *
 * 形状是个"年份步进器 + 12 个月方格"，因为它跟日历本身长得像 —— 不用学。
 * 打开时从**当前正在看的那个月**开始，而不是从今天开始：用户点这个入口八成是
 * 想从眼前这个月往别处去。
 */
@Composable
fun MonthPickerDialog(
    visible: YearMonth,
    onPick: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { YearMonth.now() }
    val scheme = MaterialTheme.colorScheme

    // 只允许在左右各 100 年内走，跟 HorizontalPager 的翻页余地对齐，
    // 免得跳到一个翻不回来的年份。
    val yearFloor = today.year - 100
    val yearCeiling = today.year + 100
    var year by remember { mutableStateOf(visible.year) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = scheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    StepArrow(
                        direction = -1,
                        enabled = year > yearFloor,
                        onClick = { year -= 1 },
                    )
                    Text(
                        text = "$year",
                        modifier = Modifier.padding(horizontal = 18.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                    )
                    StepArrow(
                        direction = 1,
                        enabled = year < yearCeiling,
                        onClick = { year += 1 },
                    )
                }

                Spacer(Modifier.height(14.dp))

                (1..12).chunked(MONTHS_PER_ROW).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { month ->
                            MonthCell(
                                label = "${month}月",
                                isViewing = year == visible.year && month == visible.monthValue,
                                isTodayMonth = year == today.year && month == today.monthValue,
                                onClick = { onPick(YearMonth.of(year, month)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCell(
    label: String,
    isViewing: Boolean,
    isTodayMonth: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .padding(3.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(if (isViewing) Modifier.background(scheme.primary) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isViewing || isTodayMonth) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                color = when {
                    isViewing -> scheme.onPrimary
                    isTodayMonth -> scheme.primary
                    else -> scheme.onSurface
                },
            )
            // 今天所在的月份另外点一个小点：否则翻到别的年份时，
            // 会出现"我记得今天在这个月，可它看起来毫无标记"的困惑。
            if (isTodayMonth && !isViewing) {
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(scheme.primary),
                )
            }
        }
    }
}

/**
 * 年份步进器的左右箭头。跟表头那颗三角一样手绘 —— 引一套图标库只为两个箭头，
 * 要让 APK 大好几 MB，不划算。
 */
@Composable
private fun StepArrow(
    direction: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 11.dp, height = 16.dp)) {
            drawPath(
                path = Path().apply {
                    if (direction < 0) {
                        moveTo(size.width, 0f)
                        lineTo(0f, size.height / 2f)
                        lineTo(size.width, size.height)
                    } else {
                        moveTo(0f, 0f)
                        lineTo(size.width, size.height / 2f)
                        lineTo(0f, size.height)
                    }
                    close()
                },
                color = color,
            )
        }
    }
}
