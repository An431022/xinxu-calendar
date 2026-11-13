package com.xinxu.calendar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 没涂色的日子：只留一小节，比涂了色的短得多。 */
private const val EMPTY_WIDTH = 0.16f

/** 涂了色、但所在格子没被选中。 */
private const val COLORED_WIDTH = 0.56f

/** 涂了色、并且这个格子正被选中 —— 色条加宽，跟面板里的颜色一一对得上。 */
private const val COLORED_WIDTH_SELECTED = 0.84f

/**
 * 日期底下那条小横条 —— 整个 App 的主角。
 *
 * 它是「这一天是什么颜色」的唯一常驻显示位（对应你的图二）。四种状态：
 *
 * | 情况 | 画成什么 |
 * |---|---|
 * | 没涂色 | **极短的一小节**，颜色只比背景深一点点 —— 有位置感，但没有存在感 |
 * | 涂了 1 色 | 整条单色 |
 * | 涂了 N 色 | 等分切成 N 段，按用户选择顺序排列 |
 * | 所在格子被选中 | 变宽（0.56 → 0.84 倍格宽），和面板里的颜色一一对得上 |
 *
 * 「没涂色」这一态在 2026-10-03 改过一版：最早是给整条描一圈淡色线框，真机上三十几个
 * 空线框铺在数字下面看着很吵，而且像一排待勾选的复选框 —— 正好是我们最不想要的那种
 * 「你还有事没做」的暗示。改成"更短 + 更淡"之后，它退成了一个位置提示。
 *
 * ## 为什么是 drawWithCache 而不是 Box + clip + background（2026-10-19 改）
 *
 * 原实现是 `Box(...).clip(RoundedCornerShape(percent = 50)).background(...)`，多色时
 * 里面再套一个 `Row` + N 个 `weight(1f)` 的 `Box`。这有几个代价：
 *
 * 1. `Modifier.clip(shape)` 是**图形层**（RenderNode），不是画布裁剪 —— 每一条色条一个层。
 *    一屏同时可见两页、每页三十几格，就是几十个层叠在一起。
 * 2. 空色条也有这一层，而它画的只是一小块淡色圆角。
 * 3. 多色时的 `Row` + `weight` 要参与测量分配。
 *
 * 现在整条只用一次 `drawBehind`：圆角靠 `clipPath`（`canvas.save/clipPath/restore`，
 * 纯画布状态，不建层），各色段是等宽矩形。视觉与原来一致（圆头、等分、无接缝），
 * 但每格的绘制节点与图层都少了一大截。
 *
 * **这不是"翻页卡顿"的解决方案。** 那次卡顿的根因是 debug 包的 `debuggable=true` 让 ART
 * 禁止 AOT（见开发计划 8.9、踩坑 13.16）。这里是顺带做的绘制简化：在 debug 包上实测它
 * 把每帧从约 25ms 降到 13ms，但真正让翻页顺畅的是改用 `daily` 包。
 *
 * @param colors 这一天已涂的颜色，按选择顺序。空列表表示没涂。
 * @param selected 所在日期格是否处于选中态（决定宽度和空条颜色）。
 */
@Composable
fun DayStrip(
    colors: List<Color>,
    selected: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
) {
    val widthRatio = when {
        colors.isEmpty() -> EMPTY_WIDTH
        selected -> COLORED_WIDTH_SELECTED
        else -> COLORED_WIDTH
    }
    val emptyColor = emptyMarkColor(selected)
    // 让 lambda 捕获一个稳定的小对象，避免 drawWithCache 的 cache 键抖动。
    val palette = colors

    Box(
        modifier = modifier
            .fillMaxWidth(widthRatio)
            .height(height)
            .drawWithCache {
                val radius = size.height / 2f
                val corner = CornerRadius(radius)
                val rect = Rect(Offset.Zero, Size(size.width, size.height))
                // 圆头形状只算一次，滚动时反复用到。
                val roundPath = Path().apply {
                    addRoundRect(RoundRect(rect, corner))
                }
                onDrawBehind {
                    if (palette.isEmpty()) {
                        drawRoundRect(color = emptyColor, cornerRadius = corner)
                    } else {
                        clipPath(roundPath) {
                            val segment = size.width / palette.size
                            palette.forEachIndexed { index, color ->
                                drawRect(
                                    color = color,
                                    topLeft = Offset(index * segment, 0f),
                                    size = Size(segment, size.height),
                                )
                            }
                        }
                    }
                }
            },
    )
}

/**
 * 「什么都没涂」那一小节的颜色。
 *
 * 不写死色值，而是用**背景色掺一点点前景色**算出来：这样以后一旦调整整个 App 的背景色，
 * 这条小横条会自己跟着变深变浅，不需要回来逐个手动改。深色模式下前景比背景亮，
 * 同一个公式会自动变成"比背景亮一点点"，不用写两套。
 */
@Composable
private fun emptyMarkColor(selected: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    return if (selected) {
        // 选中态整个格子是深墨色，所以这里要往亮的方向掺。
        lerp(scheme.primary, scheme.onPrimary, 0.38f)
    } else {
        lerp(scheme.surface, scheme.onSurface, 0.10f)
    }
}
