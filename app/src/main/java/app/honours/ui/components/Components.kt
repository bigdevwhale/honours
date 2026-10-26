package app.honours.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.honours.domain.Badge
import app.honours.domain.Metal
import app.honours.domain.Shape
import app.honours.ui.art.Badges.badge
import app.honours.ui.art.Badges.blank
import app.honours.ui.theme.C
import app.honours.ui.theme.T
import kotlinx.coroutines.delay

/** Office paper: a warm sheet with faint ruled lines, like a ledger. */
fun Modifier.paper() = this.background(C.paper).drawBehind {
    val step = 28.dp.toPx()
    var y = step
    while (y < size.height) { drawLine(C.paperDark.copy(alpha = .55f), Offset(0f, y), Offset(size.width, y), 1f); y += step }
}

enum class Kind { RED, GHOST, GOLD, SOFT }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, kind: Kind = Kind.RED, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val (bg, fg) = when (kind) {
        Kind.RED -> C.red to Color(0xFFFFF4DA)
        Kind.GHOST -> Color.Transparent to C.ink
        Kind.GOLD -> C.gold to C.velvet
        Kind.SOFT -> C.card to C.ink
    }
    val under = if (kind == Kind.GOLD) C.goldDeep else C.redDeep
    Box(
        modifier
            .then(if ((kind == Kind.RED || kind == Kind.GOLD) && enabled) Modifier.drawBehind { drawRoundRect(under, Offset(0f, 3.dp.toPx()), size, CornerRadius(10.dp.toPx())) } else Modifier)
            .clip(shape)
            .background(if (enabled) bg else C.line)
            .then(if (kind == Kind.GHOST || kind == Kind.SOFT) Modifier.border(1.5.dp, if (enabled) C.ink.copy(alpha = .7f) else C.muted, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text.uppercase(), style = T.Button.copy(color = if (enabled) fg else C.muted), textAlign = TextAlign.Center) }
}

@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.clip(shape).background(if (on) C.ink else C.card).border(1.5.dp, if (on) C.ink else C.line, shape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
    ) { Text(text, style = T.Body.copy(color = if (on) C.card else C.ink)) }
}

@Composable
fun Card(modifier: Modifier = Modifier, bg: Color = C.card, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg).border(1.dp, C.line, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier) = Text(text.uppercase(), style = T.Label, modifier = modifier.padding(top = 18.dp, bottom = 6.dp))

@Composable
fun Gap(h: Int = 10) = Spacer(Modifier.height(h.dp))

@Composable
fun Flow(modifier: Modifier = Modifier, gap: Dp = 6.dp, content: @Composable () -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) { content() }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, style: TextStyle = T.Body, minLines: Int = 1, max: Int = 160) {
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.fillMaxWidth().clip(shape).background(C.card).border(1.5.dp, C.line, shape).padding(horizontal = 12.dp, vertical = 10.dp)) {
        if (value.isEmpty()) Text(hint, style = style.copy(color = C.muted.copy(alpha = .7f)))
        BasicTextField(
            value, { onChange(it.take(max)) }, Modifier.fillMaxWidth(), textStyle = style, minLines = minLines, cursorBrush = SolidColor(C.red),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
}

/** The title of a tab. */
@Composable
fun TopBar(title: String, sub: String? = null, end: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Title)
            if (sub != null) Text(sub, style = T.Small)
        }
        end()
    }
}

/** A full-screen sheet over the tabs, with a title and a way back. */
@Composable
fun Sheet(title: String, onClose: () -> Unit, closeLabel: String, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(C.night).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(10.dp).clip(RoundedCornerShape(14.dp)).paper()) {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = T.Heading, modifier = Modifier.weight(1f))
                Text(closeLabel, style = T.Button.copy(color = C.muted), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClose).padding(10.dp))
            }
            val inner = Modifier.fillMaxWidth().weight(1f).then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 16.dp, vertical = 8.dp)
            Column(inner, content = content)
        }
    }
}

/** A badge in a box of [size], with its class's metal; or pressed blank into the paper if not yet earned. */
@Composable
fun BadgeIcon(b: Badge, metal: Metal?, size: Dp, modifier: Modifier = Modifier, light: Offset = Offset(-.5f, -.7f)) {
    Canvas(modifier.size(size)) {
        val c = Offset(this.size.width / 2, this.size.height / 2)
        val r = this.size.minDimension * .44f
        if (metal == null) blank(b.look.shape, c, r, C.paperDark, C.muted.copy(alpha = .45f))
        else badge(b.look, metal, c, r, light, shadow = true)
    }
}

/** The empty outline of a shape, for choices in the workshop. */
@Composable
fun ShapeIcon(s: Shape, size: Dp, on: Boolean) {
    Canvas(Modifier.size(size)) { blank(s, Offset(this.size.width / 2, this.size.height / 2), this.size.minDimension * .42f, if (on) C.gold else C.card, if (on) C.ink else C.muted) }
}

/** Short messages at the bottom of the screen. */
class Toaster {
    var text by mutableStateOf<String?>(null)
    var seq by mutableIntStateOf(0)
    fun show(s: String) { text = s; seq++ }
}

val LocalToast = compositionLocalOf { Toaster() }

@Composable
fun BoxScope.ToastHost(t: Toaster) {
    LaunchedEffect(t.seq) { if (t.text != null) { delay(3200); t.text = null } }
    val s = t.text ?: return
    Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 18.dp, end = 18.dp, bottom = 84.dp)) {
        Text(s, style = T.Body.copy(color = C.card, textAlign = TextAlign.Center), modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(C.ink).clickable { t.text = null }.padding(horizontal = 16.dp, vertical = 11.dp))
    }
}
