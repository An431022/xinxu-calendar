package com.xinxu.calendar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xinxu.calendar.data.Palette
import com.xinxu.calendar.data.PaletteColor
import com.xinxu.calendar.ui.theme.contrastOn
import com.xinxu.calendar.ui.theme.currentColor

/** 一行放几个。6 × 2 = 12，正好一整块色板，不需要滚动 —— 滚动就违背"三步内记完"了。 */
private const val DOTS_PER_ROW = 6

private val DOT_SIZE = 40.dp

/**
 * 十二色常驻取色面板。
 *
 * **它不是弹窗、不是抽屉，就一直摊在这里。** 计划里那条设计红线写的是
 * "打开 App → 点今天 → 点颜色，没有保存按钮，目标 3 秒"。要是每次涂色都得先点开一个面板，
 * 就成了四步，而且每天都要重复一次"打开-选择-关闭"这种纯仪式感的动作。
 *
 * 不做文字标签：颜色是给人**认**的，不是给人读的。选了之后下方卡片会写出颜色的名字，
 * 第一次不认识、第二次就认识了。真写 12 个标签，这块面板会高一倍、也更像一张表单。
 */
@Composable
fun ColorPicker(
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = Palette.colors.chunked(DOTS_PER_ROW)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { color ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        ColorDot(
                            color = color,
                            selected = color.id in selectedIds,
                            onClick = { onToggle(color.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorDot(
    color: PaletteColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val fill = color.currentColor()
    val ringColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(DOT_SIZE)
            .clip(CircleShape)
            .background(fill)
            .then(
                // 选中的颜色套一圈深色环。环画在圆内边缘，所以圆本身不变大、不会把整行挤歪。
                if (selected) Modifier.border(2.5.dp, ringColor, CircleShape) else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            CheckMark(color = contrastOn(fill))        }
    }
}

@Composable
private fun CheckMark(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        drawPath(
            path = Path().apply {
                moveTo(w * 0.20f, h * 0.52f)
                lineTo(w * 0.42f, h * 0.74f)
                lineTo(w * 0.80f, h * 0.26f)
            },
            color = color,
            style = Stroke(
                width = w * 0.13f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

