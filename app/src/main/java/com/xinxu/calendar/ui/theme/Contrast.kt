package com.xinxu.calendar.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** 我们自己的两个"前景色"候选：深墨（跟选中态同色）与纯白。 */
private val INK = Color(0xFF2E2724)
private val WHITE = Color.White

/**
 * 给定一块底色，返回在它**上面更清楚**的那种前景色。
 *
 * 早先的写法是"亮度 > 0.45 就用深色，否则用白色"，那是个拍脑袋的阈值，
 * 对中间调的颜色会选错：玫瑰 `#E689B0` 的感知亮度只有 0.37，按阈值会被判成"深底"而配白字，
 * 可实际算下来白字对比度只有 2.5，深字有 6.1 —— 差了 2.4 倍。
 *
 * 现在直接按 WCAG 的对比度公式把两个候选都算出来，取高的那个。规则虽小，
 * 但色板里有 12 种颜色、每种还有深浅两套，靠人眼一个个拍板是不现实的。
 */
fun contrastOn(background: Color): Color {
    val l = background.luminance()
    val withInk = contrastRatio(l, INK.luminance())
    val withWhite = contrastRatio(l, WHITE.luminance())
    return if (withInk >= withWhite) INK else WHITE
}

private fun contrastRatio(a: Float, b: Float): Float {
    val hi = maxOf(a, b)
    val lo = minOf(a, b)
    return (hi + 0.05f) / (lo + 0.05f)
}
