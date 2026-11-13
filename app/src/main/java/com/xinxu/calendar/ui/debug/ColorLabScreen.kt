package com.xinxu.calendar.ui.debug

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xinxu.calendar.data.Palette
import com.xinxu.calendar.ui.calendar.DayCellContent
import com.xinxu.calendar.ui.theme.PaletteOverrideStore
import com.xinxu.calendar.ui.theme.currentLight
import com.xinxu.calendar.ui.theme.displayName
import com.kyant.hct.HctUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 十二色要守的参考线。破例是允许的，这里只负责把差距摆出来。 */
private const val SAFE_CHROMA_MAX = 48.0
private const val SAFE_TONE_MIN = 52.0
private const val SAFE_TONE_MAX = 78.0

/** HCT 彩度的实用上限。再高 CAM16 就开始裁剪，拖到底也不会有更多颜色。 */
private const val CHROMA_MAX = 120.0

/**
 * 调色盘 —— 十二色和选中框光晕都可以在这里改。
 *
 * ## 2026-10-29 重做：从四根 RGBA 滑块换成五条渐变条
 *
 * 上一版是 R/G/B/A 四根 0..255 的滑块。技术上够用，但它逼着人**用数字想颜色**：
 * 想调深一点，不知道该拉哪根、拉多少。现在换成图三那种：
 *
 * 1. 当前颜色（棋盘格垫底，一眼看清是不是透明）
 * 2. 色相（彩虹条）
 * 3. 彩度（从灰到艳）
 * 4. 明度（黑 → 本色 → 白）
 * 5. 透明度（棋盘格 + 本色）
 * 6. 最下面直接输色号
 *
 * 内部状态因此也从 ARGB 换成了 **HCT（色相 / 彩度 / 明度）+ alpha**：
 * 那三条滑块各自对应一个分量，拖的时候另外两个纹丝不动。
 *
 * ## 保存 = 以后新涂的颜色用新色板，**历史不变**
 *
 * 2026-10-29 和用户确认的语义：改色板不影响已经涂过的日子（记录里存了涂色当时的快照）。
 * 所以「保存」写的是 `PaletteOverrideStore`（出厂值之上的一层覆盖），
 * 已经存在的记录不受影响。
 *
 * ## HCT 提示是**建议**，不是门槛
 *
 * 越出参考线时标个提醒，但绝不阻止保存 —— 给一套"不刺眼"的默认色是产品的事，
 * 用户想调成什么样是他们的事。兜底是「还原这个颜色」和「全部还原」。
 */
@Composable
fun ColorLabScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val overrides = PaletteOverrideStore.overrides
    val names = PaletteOverrideStore.names
    val focusManager = LocalFocusManager.current

    // 现在编辑的是哪个槽。
    var selectedId by remember { mutableStateOf(Palette.colors.first().id) }

    // 还原之后靠它把 HCT 状态重新对齐一次。
    var reload by remember { mutableIntStateOf(0) }

    var hue by remember { mutableFloatStateOf(0f) }
    var chroma by remember { mutableFloatStateOf(0f) }
    var tone by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1f) }

    var hexText by remember { mutableStateOf("") }
    var hexFocused by remember { mutableStateOf(false) }
    // 颜色名字的草稿。只在点「保存」时写盘，跟颜色本身一致。
    var nameDraft by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    // 当前槽现在的颜色和名字。必须在组合上下文里算 —— currentLight()/displayName() 是 @Composable，
    // 挂起 lambda 里调不了。
    val baseColor = Palette.byId(selectedId)?.currentLight() ?: Color.White
    // 名字框里只放**用户真正起过的名字**，不放 displayName 的回退值。
    // 否则出厂名会被预填，"保存"就把它固化成了自定义名字 —— 于是改过色还顶着「雾紫」，
    // 看起来像"自定义"规则失效。回退值只用在标题上。
    val customName = PaletteOverrideStore.names[selectedId].orEmpty()
    val fallbackName = Palette.byId(selectedId)?.displayName().orEmpty()

    // 把当前颜色读成 HCT。
    LaunchedEffect(selectedId, reload) {
        alpha = baseColor.alpha
        nameDraft = customName
        val hct = runCatching { HctUtils.hctFromInt(baseColor.copy(alpha = 1f).toArgb()) }.getOrNull()
        if (hct != null && hct.size >= 3) {
            hue = hct[0].toFloat()
            chroma = hct[1].toFloat()
            tone = hct[2].toFloat()
        }
    }

    // 三条滑块合成颜色。HCT 算不出来（极端彩度/明度会被裁剪，甚至抛异常）时退回到白色，
    // 页面不崩、用户继续拖就是了。
    val argb = remember(hue, chroma, tone) { hctToArgb(hue, chroma, tone) }
    val color = Color(argb).copy(alpha = alpha)

    // 色号输入框：颜色从滑块过来时同步文字；用户正在输入时别去打断他。
    LaunchedEffect(color, hexFocused) {
        if (!hexFocused) hexText = color.toHexString()
    }

    // 「已保存」只亮一会儿，够看见就行。
    LaunchedEffect(saved) {
        if (saved) {
            delay(1600)
            saved = false
        }
    }

    val hueStops = remember { List(13) { i -> hctColor(i * 30f, 60f, 70f) } }
    val chromaStops = remember(hue, tone) {
        List(9) { i -> hctColor(hue, (CHROMA_MAX * i / 8f).toFloat(), tone) }
    }
    val toneStops = remember(hue, chroma) {
        List(11) { i -> hctColor(hue, chroma, i * 10f) }
    }
    val alphaStops = remember(argb) {
        List(9) { i -> Color(argb).copy(alpha = i / 8f) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // Dialog 是 decorFitsSystemWindows = false（要盖住全屏），所以状态栏这一条得自己让出来，
            // 否则"调色盘"这个标题会被时间/电量压住。
            .statusBarsPadding()
            // 键盘弹出时整页跟着上移 —— Dialog 自己不会让位，不加这句色号/名字框会被键盘盖住。
            .imePadding()
            // 点空白处收掉焦点。关掉键盘之后焦点还赖在输入框里，这个页面之前最别扭的就是这点。
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "调色盘",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "改的是以后的色板；已经涂过的日子不会变",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LabCloseCross(onClick = onClose)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "选一个颜色",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))

        // 两行六列，跟底部色盘的排布一致 —— 同一个色在调色盘和真实界面里位置相同。
        Palette.colors.chunked(6).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { paletteColor ->
                    val isPicked = paletteColor.id == selectedId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .background(paletteColor.currentLight())
                                .then(
                                    if (isPicked) {
                                        Modifier.border(
                                            width = 2.5.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedId = paletteColor.id },
                        )
                        // 改过出厂值的槽，右上角点一个小点 —— 一眼看出哪些是自己调过的。
                        if (overrides.containsKey(paletteColor.id)) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(20.dp))

        // 当前颜色：图三里那根最粗的实色条。
        Text(
            text = nameDraft.ifBlank { fallbackName },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        AlphaChecker(
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp)),
        )

        Spacer(Modifier.height(14.dp))

        // 四根渐变条 + 一根透明度。顺序跟着图三：色相 → 彩度 → 明度 → 透明度。
        ColorBar(
            value = hue / 360f,
            stops = hueStops,
            onChange = { hue = (it * 360f).coerceIn(0f, 359f) },
        )
        Spacer(Modifier.height(10.dp))
        ColorBar(
            value = (chroma / CHROMA_MAX).toFloat(),
            stops = chromaStops,
            onChange = { chroma = (it * CHROMA_MAX).coerceIn(0.0, CHROMA_MAX).toFloat() },
        )
        Spacer(Modifier.height(10.dp))
        ColorBar(
            value = tone / 100f,
            stops = toneStops,
            onChange = { tone = (it * 100f).coerceIn(0f, 100f) },
        )
        Spacer(Modifier.height(10.dp))
        ColorBar(
            value = alpha,
            stops = alphaStops,
            onChange = { alpha = it.coerceIn(0f, 1f) },
            checkerboard = true,
        )

        Spacer(Modifier.height(14.dp))

        // 色号：可以直接敲。认 `#RGB` / `#RRGGBB` / `#AARRGGBB`，敲到一半不合法就不动颜色。
        LabTextField(
            text = hexText,
            onTextChange = { text ->
                hexText = text
                parseHexColor(text)?.let { value ->
                    alpha = Color(value).alpha
                    runCatching { HctUtils.hctFromInt(value) }.getOrNull()?.let { hct ->
                        if (hct.size >= 3) {
                            hue = hct[0].toFloat()
                            chroma = hct[1].toFloat()
                            tone = hct[2].toFloat()
                        }
                    }
                }
            },
            onFocusChanged = { hexFocused = it },
            monospace = true,
        )

        // 名字：只影响显示，不影响任何记录。改过色但没起名时这里回退成「自定义」。
        Spacer(Modifier.height(10.dp))
        LabTextField(
            text = nameDraft,
            onTextChange = { nameDraft = it },
            placeholder = "颜色名字（不填就显示「自定义」）",
        )

        // HCT 参考线（C≤48、T 52–78）。只提示，不拦截。
        Spacer(Modifier.height(10.dp))
        HctReadout(chroma.toDouble(), tone.toDouble())

        Spacer(Modifier.height(16.dp))

        // 保存：只影响以后的涂色和新打开的界面，历史记录有快照，不受影响。
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    PaletteOverrideStore.set(context, selectedId, color.toArgb().toLong() and 0xFFFFFFFFL)
                    // 名字和颜色一起保存。空名字 = 删掉自定义名字，显示时按规则回退。
                    PaletteOverrideStore.setName(context, selectedId, nameDraft)
                    saved = true
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (saved) "已保存" else "保存")
            }
            TextButton(
                onClick = {
                    PaletteOverrideStore.clear(context, selectedId)
                    reload++
                },
                enabled = overrides.containsKey(selectedId) || names.containsKey(selectedId),
            ) {
                Text("还原这个颜色")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    PaletteOverrideStore.clearAll(context)
                    reload++
                },
                enabled = overrides.isNotEmpty() || names.isNotEmpty(),
            ) {
                Text("全部还原")
            }
            val clipboard = LocalClipboardManager.current
            TextButton(onClick = { clipboard.setText(AnnotatedString(color.toHexString())) }) {
                Text("复制 ${color.toHexString()}")
            }
        }

        Spacer(Modifier.height(18.dp))

        // 真实尺寸的预览格：只看一个大色块判断不了"它当日期底色时会不会压住数字"。
        Text(
            text = "日历上的样子",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(7) { index ->
                DayCellContent(
                    day = index + 12,
                    colors = if (index == 3) listOf(color) else emptyList(),
                    isToday = false,
                    isSelected = index == 3,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

/**
 * 一条**渐变轨道**：颜色本身就是刻度，滑块圆点只负责告诉你在哪儿。
 *
 * 图三那种样子。四根轨道（色相 / 彩度 / 明度 / 透明度）共用这一个组件 ——
 * 它们唯一的区别是 `stops` 怎么算，交互完全一致。
 *
 * 手势用 `awaitEachGesture` 自己写，而不是 `detectDragGestures`：
 * 后者要等手指移过 touch slop 才算"开始拖"，点一下不动的落点就没反应。
 * 这里按下即定位，才符合"点哪跳哪"的直觉。
 */
@Composable
private fun ColorBar(
    value: Float,
    stops: List<Color>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    checkerboard: Boolean = false,
) {
    val density = LocalDensity.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onChange((down.position.x / size.width).coerceIn(0f, 1f))
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                onChange((change.position.x / size.width).coerceIn(0f, 1f))
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        val radius = CornerRadius(size.height / 2f)
        if (checkerboard) drawCheckerboard()
        drawRoundRect(
            brush = Brush.horizontalGradient(stops),
            size = Size(size.width, size.height),
            cornerRadius = radius,
        )
        // 圆点：白心 + 一圈淡墨边。放在这里而不是用 Material Slider，
        // 是因为 Slider 的轨道只能单色，渐变得自己画。
        val dotRadius = size.height * 0.34f
        val inset = dotRadius + 2.dp.toPx()
        val x = inset + value.coerceIn(0f, 1f) * (size.width - inset * 2)
        val center = Offset(x, size.height / 2f)
        drawCircle(Color.White, dotRadius, center)
        drawCircle(
            Color.Black.copy(alpha = 0.18f),
            dotRadius,
            center,
            style = Stroke(1.dp.toPx()),
        )
    }
}

/**
 * 色号 / 名字的输入框。
 *
 * 用 BasicTextField 而不是 TextField：这里只需要一个能敲字的框，不需要 label、
 * 指示线那一整套表单语汇（跟备注输入框同一个理由）。
 */
@Composable
private fun LabTextField(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String? = null,
    monospace: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    // 聚焦时把自己滚进可视区。键盘弹起来之后页面虽然整体上移了，但靠下的输入框
    //（色号、名字）还是可能被挡住 —— 这个 requester 负责把它们送到眼前。
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoView)
                .onFocusChanged { state ->
                    onFocusChanged(state.isFocused)
                    if (state.isFocused) scope.launch { bringIntoView.bringIntoView() }
                },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

/**
 * HCT 读数 + 越界提示。
 *
 * 只提示，不拦截 —— 这套参考线的意义是"让你知道你离开了多远"，不是"不许走"。
 * 朱红（C79）和石墨（T44）本来就是这个色板里**有意**的两处破例，所以这里也不做特判，
 * 让它们照样显示越界，反而能提醒后来的人"这两个是例外，不是测错了"。
 */
@Composable
private fun HctReadout(chroma: Double, tone: Double) {
    val ok = MaterialTheme.colorScheme.onSurfaceVariant
    val chromaBad = chroma > SAFE_CHROMA_MAX
    val toneBad = tone < SAFE_TONE_MIN || tone > SAFE_TONE_MAX
    val warn = MaterialTheme.colorScheme.error

    Column {
        Row {
            Text(
                text = "HCT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ok,
                modifier = Modifier.width(40.dp),
            )
            Text(
                text = "C %.0f".format(chroma),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = if (chromaBad) warn else ok,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "T %.0f".format(tone),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = if (toneBad) warn else ok,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = when {
                chromaBad && toneBad -> "彩度偏高、明度也出界了。参考范围 C≤48、T 52–78"
                chromaBad -> "彩度偏高。参考上限 48，再高就有点扎眼"
                toneBad && tone < SAFE_TONE_MIN -> "明度偏低。参考下限 52，太暗会压住日期数字"
                toneBad -> "明度偏高。参考上限 78，太亮在白底上会糊"
                else -> "在参考范围内（C≤48、T 52–78）"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (chromaBad || toneBad) warn else ok,
        )
    }
}

/** 棋盘格垫底 + 半透明颜色。没有它，A 拉到底时看起来像"颜色变浅"而不是"变透明"。 */
@Composable
private fun AlphaChecker(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawCheckerboard()
        drawRect(color = color)
    }
}

/** 一块 8dp 的灰白棋盘。色条和色块共用。 */
private fun DrawScope.drawCheckerboard() {
    val cell = 8.dp.toPx()
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = 0f
        var col = 0
        while (x < size.width) {
            drawRect(
                color = if ((row + col) % 2 == 0) Color(0xFFFFFFFF) else Color(0xFFD8D8D8),
                topLeft = Offset(x, y),
                size = Size(cell, cell),
            )
            x += cell
            col++
        }
        y += cell
        row++
    }
}

/** HCT -> ARGB。失败时返回白色，别让一条坏读数把页面搞崩。 */
private fun hctToArgb(hue: Float, chroma: Float, tone: Float): Int =
    runCatching { HctUtils.hctToInt(hue.toDouble(), chroma.toDouble(), tone.toDouble()) }
        .getOrNull() ?: Color.White.toArgb()

private fun hctColor(hue: Float, chroma: Float, tone: Float): Color = Color(hctToArgb(hue, chroma, tone))

/** 认 `#RGB` / `#RRGGBB` / `#AARRGGBB`。敲到一半不合法返回 null（不打断输入）。 */
private fun parseHexColor(text: String): Int? {
    val hex = text.trim().removePrefix("#")
    return when (hex.length) {
        3 -> {
            val r = hex[0].digitToIntOrNull(16) ?: return null
            val g = hex[1].digitToIntOrNull(16) ?: return null
            val b = hex[2].digitToIntOrNull(16) ?: return null
            (0xFF shl 24) or (r * 17 shl 16) or (g * 17 shl 8) or (b * 17)
        }
        6 -> hex.toLongOrNull(16)?.let { (it or 0xFF000000L).toInt() }
        8 -> hex.toLongOrNull(16)?.toInt()
        else -> null
    }
}

/** `#AARRGGBB`。补到 8 位，复制出去贴进 code 里不用再数零。 */
private fun Color.toHexString(): String = "#%08X".format(toArgb())

/** 关闭用的叉，和选中态试验台同款。 */
@Composable
private fun LabCloseCross(onClick: () -> Unit) {
    val crossColor = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val stroke = size.width * 0.13f
            val style = Stroke(width = stroke, cap = StrokeCap.Round)
            drawPath(
                path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height)
                },
                color = crossColor,
                style = style,
            )
            drawPath(
                path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(0f, size.height)
                },
                color = crossColor,
                style = style,
            )
        }
    }
}
