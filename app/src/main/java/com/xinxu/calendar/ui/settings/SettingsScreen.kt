package com.xinxu.calendar.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 设置页。顶部那个齿轮进来的地方。
 *
 * ## 卡片式分组（2026-10-19 重做）
 *
 * 原来的形状是"分组标题 + 一条条独立可点的行"，行与行之间只靠 10dp 间距分开，
 * 整页看起来是一串漂浮的文字。现在改成**一组一张卡片**：卡片底色比背景亮一档，
 * 把"同类的东西"围在一起；每行左边的图标负责快速认脸，右边的箭头负责说明"点进去还有一页"。
 *
 * 这套形状是从系统设置那类界面借的，借的不是某张具体的图，而是它解决的两个问题：
 * 一是**分组有边界**（哪些是同类，一眼看得出来），二是**每行都有图标**（扫视比读字快）。
 *
 * ## 「个性化」这一组放什么
 *
 * 选中态试验台和调色盘原来只能靠长按图标偷偷进入 —— 那对开发者自己够用，
 * 对任何第二个人都不成立（2026-10-19 用户原话：「关掉之后就调不出来了」）。
 * 现在它们有了正式入口，长按仍然保留，当作熟手的快捷方式。
 *
 * ## 页面只负责"摆东西"
 *
 * 它不认识数据库、不认识文件，只知道"用户点了导出 / 点了导入"和"现在忙不忙、
 * 上一步结果是什么"。这样以后往里加别的分组（比如深色模式）时，
 * 这个文件不需要知道那些功能怎么实现。
 */
@Composable
fun SettingsScreen(
    busy: Boolean,
    message: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenColorLab: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // 跟统计页一样铺满屏幕，所以要自己把状态栏那一条让出来。
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
                        .clickable(onClick = onClose)
                        .semantics { contentDescription = "关闭" },
                    contentAlignment = Alignment.Center,
                ) {
                    CloseCross()
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "数据与外观",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SettingsGroup("个性化") {
                SettingsRow(
                    icon = { PaletteIcon() },
                    title = "调色盘",
                    subtitle = "调整色盘颜色",
                    onClick = onOpenColorLab,
                )
            }

            Spacer(Modifier.height(24.dp))

            SettingsGroup("数据与备份") {
                SettingsRow(
                    icon = { ExportIcon() },
                    title = "导出备份",
                    subtitle = "把全部记录写成一个 JSON 文件",
                    enabled = !busy,
                    onClick = onExport,
                )
                SettingsRow(
                    icon = { ImportIcon() },
                    title = "导入备份",
                    subtitle = "导入已有JSON备份文件",
                    enabled = !busy,
                    onClick = onImport,
                )
            }

            if (message != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

/**
 * 一组设置：小标题 + 一张卡片。
 *
 * 卡片用 `surfaceContainerLowest` —— 浅色下是纯白、深色下比背景亮一档。
 * 这两个值在 Theme.kt 里显式写死了，因为 M3 默认的 `surfaceContainerLowest` 在深色模式下
 * 是**最暗**的那一档，直接用会让卡片陷进背景里、整页失去层次。
 */
@Composable
private fun SettingsGroup(
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

/**
 * 一行设置：图标 + 标题/副标题 + 箭头。
 *
 * 整行可点（而不是只有右边一个小箭头可点）—— 设置项的文字本来就是给人点的目标，
 * 把点击区限制在一个图标上，只有开发者自己按得准。
 *
 * 卡片内部**不画行间分隔线**：参考的那种界面也是靠行高和图标把它们分开的，
 * 多一道线反而会把一张完整的卡片切成碎片。
 */
@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            )
        }
        Spacer(Modifier.width(10.dp))
        RowCaret(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.7f else 0.3f))
    }
}

// ---------------------------------------------------------------------------
// 图标：全部手绘。这个工程从头到尾不引图标库（见 AGENTS.md 的构建约束）。
// ---------------------------------------------------------------------------

/** 调色盘：一个圆环 + 中心一点，比四个小方块更不像「统计入口」。 */
@Composable
private fun PaletteIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(20.dp)) {
        val stroke = 1.6.dp.toPx()
        drawCircle(
            color = color,
            radius = size.minDimension / 2f - stroke,
            style = Stroke(width = stroke),
        )
        drawCircle(color = color, radius = 1.9.dp.toPx(), center = center)
    }
}

/** 导出：箭头朝上 + 底托。 */
@Composable
private fun ExportIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(20.dp)) {
        drawArrowIcon(color, up = true)
    }
}

/** 导入：箭头朝下 + 底托。 */
@Composable
private fun ImportIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(20.dp)) {
        drawArrowIcon(color, up = false)
    }
}

/**
 * 上/下箭头 + 一条底托。
 *
 * 底托是必要的：只有箭头的话，它和「返回上一级」那个图标太像了，
 * 而这两个动作方向正好相反，认错一次就点进不该点的地方。
 */
private fun DrawScope.drawArrowIcon(color: Color, up: Boolean) {
    val w = size.width
    val h = size.height
    val stroke = 1.7.dp.toPx()
    val cx = w / 2f

    // 竖干：箭头朝上时从下往上，朝下时从上往下。
    val shaftTop = if (up) h * 0.18f else h * 0.72f
    val shaftBottom = if (up) h * 0.72f else h * 0.18f
    drawLine(
        color = color,
        start = Offset(cx, shaftBottom),
        end = Offset(cx, shaftTop),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    // 两撇
    val tipY = shaftTop
    val wingY = if (up) tipY + h * 0.20f else tipY - h * 0.20f
    drawLine(
        color = color,
        start = Offset(cx, tipY),
        end = Offset(cx - w * 0.20f, wingY),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(cx, tipY),
        end = Offset(cx + w * 0.20f, wingY),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    // 底托
    val baseY = if (up) h * 0.88f else h * 0.88f
    drawLine(
        color = color,
        start = Offset(w * 0.20f, baseY),
        end = Offset(w * 0.80f, baseY),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

/** 行尾的">"。手绘：一个尖角不值得为它多几 MB。 */
@Composable
private fun RowCaret(color: Color) {
    Canvas(Modifier.size(width = 8.dp, height = 14.dp)) {
        drawPath(
            path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(0f, size.height)
            },
            color = color,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/** 手绘的叉。跟统计页那个同款 —— 不用 "✕" 字符，字体渲染出来是个脏点。 */
@Composable
private fun CloseCross() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(17.dp)) {
        val stroke = 1.9.dp.toPx()
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width, 0f),
            end = Offset(0f, size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
