package com.xinxu.calendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 应用主题。
 *
 * ## 为什么是 `MaterialExpressiveTheme`（2026-10-17）
 *
 * material3 1.4.0 起，Expressive 的入口就是这个。它替掉 `MaterialTheme` 之后带来三件事：
 *
 * 1. **默认形状规格换成 expressive 那一套**（圆角更大更圆）—— 只影响没显式给 shape 的地方。
 * 2. **material3 组件内部走 expressive 分支**（`LocalUsingExpressiveTheme` 变成 true）。
 * 3. **动效规格交给 [MotionScheme.expressive]**：弹簧而不是 `tween` 的线性缓动，
 *    位移/尺寸走 spatial 那一档，颜色/透明度走 effects 那一档。
 *
 * 颜色**一个字都没改** —— 色盘是数据不是主题（见下面那段），expressive 也不许动它。
 *
 * 注意 `material3:material3:1.4.0` **没有** `MaterialShapes`（那 30 种花瓣形状），
 * 也没有 `LoadingIndicator` / `ButtonGroup` / `FloatingToolbar` 这些新组件 ——
 * 那些还在 alpha 通道里。所以形状上的 expressive 只能靠手改圆角。
 *
 * ## 不用动态取色
 *
 * 刻意**不用** Material You 动态取色（`dynamicLightColorScheme`）：那会从壁纸里抽色，
 * 同一份记录在不同手机上显示成不同色调，「蜜橘代表充实」这套颜色语言就散了。
 * 界面配色可以跟系统走，但**十二色色板必须跨设备稳定**，所以主题色写死。
 *
 * `primary` 特意压成接近墨色的深灰（不是品牌色，也不在十二色里）：
 * 它专门用来画「选中的那一天」。如果拿调色板里的某个颜色去当选中色，
 * 用户就分不清"这天被选中"和"这天被标成朱红"—— 而选中的格子上恰好要显示色条，
 * 同色相撞上就彻底看不见了。
 *
 * ## 背景色（2026-10-04 定）
 *
 * 浅色 `#F8F9FA`（冷白）、深色 `#1C1A18`（暖深灰）。对照图见 `tools/palette/backgrounds.png`。
 *
 * 选冷白的连带后果是：**外壳（底部面板、描边、选中块）必须跟着去掉暖调**。
 * 它们原来是一套暖米色（`#EFE2DC` / `#D8C2BB` / `#2E2724`），那是照着旧背景 `#FDF8F6` 配的；
 * 直接搬到冷白上会显出一层黄气，像在蓝白纸上贴了一张奶油色的纸。
 * 所以下面浅色这套外壳全部改成了中性灰。
 *
 * ## 「色相只来自十二色」这条约束已经解除（2026-10-19）
 *
 * 这里原本写着一句硬约束：**屏幕上的色相只应该来自十二色**。
 * 现在取消，因为它是从上面那个"外壳去掉暖调"的局部结论外推出来的，
 * 管得太宽了 —— 后来连"选中框能不能用一层淡蓝光晕"都要先搬出这条规矩来挡，
 * 而那句话真正要防的是**主题色跟十二色撞脸**，不是"UI 一律不准有颜色"。
 *
 * **真正保留的只有一条：十二色本身必须固定。** 它们是数据 ——
 * 历史记录里存的是颜色 id，"蜜橘代表充实"这套语言要跨设备、跨时间稳定，
 * 所以仍然不用 Material You 动态取色（见上一节）。
 * 反过来，UI 外壳用蓝、用绿、用渐变，都是审美决定，不再是约束问题。
 *
 * 这一条和"以后可能允许用户自定义十二色"并不冲突：允许自定义，意味着
 * **色板的定义权交给用户**，但同一台设备上手挑出来的那十二个色值，仍然要稳定存下来。
 *
 * 深色那套仍然保持暖调，因为它的背景 `#1C1A18` 本身就是暖的。
 */
private val LightColors = lightColorScheme(
    // 选中态：中性深灰，微微偏冷，跟冷白背景同温。
    primary = Color(0xFF26292D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3E6EA),
    onPrimaryContainer = Color(0xFF1A1D21),
    secondary = Color(0xFF5C6166),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1B1E22),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF1B1E22),
    // 底部面板。只比背景深一点点 —— 它是操作区，不该抢色块的戏。
    surfaceVariant = Color(0xFFEBEEF1),
    onSurfaceVariant = Color(0xFF4C5157),
    // 设置页那种「卡片」的底色：**比背景亮**一档，靠它把一组设置从背景里拎出来。
    // 不能直接用 surfaceVariant —— 那个比背景深，卡片会看起来像凹进去的槽。
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Color(0xFF85898F),
    // 面板里那根空竖色标的描边色（计划 8.1.1）。必须显式给：
    // Material 的默认 outlineVariant 偏紫，混进来会显脏。
    outlineVariant = Color(0xFFC6CBD0),
)

private val DarkColors = darkColorScheme(
    // 深色模式下把选中色反过来用：浅底 + 深色字。仍然不掺任何色相，
    // 保证不会和十二色里的任何一个撞脸。
    primary = Color(0xFFE9E3E0),
    onPrimary = Color(0xFF23211F),
    primaryContainer = Color(0xFF4A403B),
    onPrimaryContainer = Color(0xFFF3E3DC),
    secondary = Color(0xFFC9C0BB),
    onSecondary = Color(0xFF33302D),
    background = Color(0xFF1C1A18),
    onBackground = Color(0xFFEAE4E1),
    surface = Color(0xFF1C1A18),
    onSurface = Color(0xFFEAE4E1),
    surfaceVariant = Color(0xFF38322F),
    onSurfaceVariant = Color(0xFFC4BBB6),
    // 深色下卡片要比背景**亮**才浮得起来；M3 默认的 surfaceContainerLowest 在深色里是最暗的，
    // 直接用会让卡片陷进背景里。这里给一个介于 surface(#1C1A18) 和 surfaceVariant(#38322F) 之间的暖深灰。
    surfaceContainerLowest = Color(0xFF262321),
    outline = Color(0xFF8E8580),
    outlineVariant = Color(0xFF4E4744),
)

/**
 * expressive 的形状规格。
 *
 * `MaterialExpressiveTheme` 自带一套默认值，这里明写出来，是想让**「圆角长什么样」只有一个出处** ——
 * 以后微调只改这一处，不用去翻每个 `RoundedCornerShape`。
 *
 * 比 M3 那一套整体大一圈：expressive 的调性就是「更圆、更软、更像个实体块」。
 * 面板和各类弹窗仍然各自显式写着 24dp / 28dp，那些跟这里的档位无关，不受影响。
 */
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XinxuCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = ExpressiveShapes,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
