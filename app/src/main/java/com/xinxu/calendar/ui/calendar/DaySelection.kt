package com.xinxu.calendar.ui.calendar

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 「选中的那一天长什么样」的**唯一**定义处。
 *
 * ## 2026-10-29：收敛成一种，不能再配
 *
 * 之前这里是 7 种形状 × 6 种填充的试验台，后来收敛到「圆角方块 / 光晕」两种，
 * 现在连光晕也删了 —— 用户用下来觉得它"和软件相性不太合"。
 *
 * 于是 `SelectionStyle`、`SelectionFill`、`SelectionStyleStore` 整套都删掉了：
 * 只剩一个样式时，再保留"可配置"的结构只会让每个消费点都多背一层间接。
 * 选中态现在是一条写死的视觉规则，改它就改这一个文件。
 */

/** 选中格的形状：16dp 圆角方块。 */
val SELECTION_SHAPE: Shape = RoundedCornerShape(16.dp)

/** 选中格描边宽度。 */
val SELECTION_BORDER_WIDTH: Dp = 1.5.dp

/**
 * 选中格的底色：跟底部取色盘那个方框同一套配色。
 *
 * 2026-10-04 在真机试验台上挑定的 —— 它最接近参照的魅族日历气质：
 * 格子里始终只有那个日期数字，选中只是一圈安静的浅底 + 描边，不跟下面色带里的颜色抢戏。
 */
@Composable
fun selectionBackground(): Color = MaterialTheme.colorScheme.surfaceVariant

/** 选中格里日期数字的颜色：浅底配重一档的墨色，保证对比度。 */
@Composable
fun selectionContentColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

/** 选中格的描边色。 */
@Composable
fun selectionBorderColor(): Color = MaterialTheme.colorScheme.primary

/** 选中时日期数字是否加粗。 */
const val SELECTION_BOLD_NUMBER: Boolean = true
