package dev.daniil.iskra.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.daniil.iskra.pet.PetDraw
import dev.daniil.iskra.pet.PetLook
import dev.daniil.iskra.scene.SceneRenderer
import dev.daniil.iskra.scene.Sky
import dev.daniil.iskra.scene.SkyState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Pixel-art pet. */
@Composable
fun PetSprite(
    look: PetLook,
    size: Dp,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    silhouette: Boolean = false,
    glow: Float = 0f,
) {
    val clock = LocalClock.current
    Canvas(modifier.size(size)) {
        val ph = if (animated) clock.value * PHASE_SPEED else 0.6f
        drawIntoCanvas {
            PetDraw.draw(it.nativeCanvas, 0f, 0f, this.size.minDimension, look, ph, glow, silhouette)
        }
    }
}

/** The landscape, redrawn every frame from the shared clock. */
@Composable
fun SceneCanvas(sky: SkyState, modifier: Modifier, opts: (time: Float, density: Float) -> SceneRenderer.Opts) {
    val clock = LocalClock.current
    Canvas(modifier) {
        val o = opts(clock.value, density)
        drawIntoCanvas { SceneRenderer.draw(it.nativeCanvas, size.width, size.height, sky, o) }
    }
}

/** Text colours that read on top of the sky. */
class HeroColors(val light: Boolean, val text: Color, val dim: Color, val shadow: Shadow, val glass: Color, val glassBorder: Color)

fun heroColors(sky: SkyState): HeroColors {
    val light = Sky.luminance(Sky.lerpColor(sky.top, sky.mid, 0.5f)) > 0.5f
    return if (light) {
        HeroColors(
            true, Color(0xFF13253A), Color(0xFF34495F),
            Shadow(Color(0x66FFFFFF), Offset(0f, 1f), 8f),
            Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.7f),
        )
    } else {
        HeroColors(
            false, Color.White, Color.White.copy(alpha = 0.78f),
            Shadow(Color(0x73000000), Offset(0f, 2f), 10f),
            Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.22f),
        )
    }
}

@Composable
fun GlassChip(text: String, hero: HeroColors, modifier: Modifier = Modifier) {
    Text(
        text,
        color = hero.text,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(hero.glass)
            .border(1.dp, hero.glassBorder, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ui = LocalUi.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .then(if (ui.light) Modifier.shadow(10.dp, shape, ambientColor = Color(0x22334455), spotColor = Color(0x22334455)) else Modifier)
            .clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(ui.card))
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val ui = LocalUi.current
    Row(modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, color = ui.text, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, color = ui.dim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProgressRing(progress: Float, size: Dp, stroke: Dp, content: @Composable BoxScope.() -> Unit) {
    val ui = LocalUi.current
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) { anim.animateTo(progress, tween(700)) }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arc = Size(this.size.width - sw, this.size.height - sw)
            drawArc(ui.track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(sw))
            if (anim.value > 0.001f) {
                drawArc(
                    Brush.sweepGradient(listOf(ui.accent, ui.accent2, ui.accent)),
                    -90f, 360f * anim.value, false, Offset(inset, inset), arc,
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Burst of pixel embers when a day gets completed. */
@Composable
fun EmberBurst(tick: Int, modifier: Modifier = Modifier) {
    if (tick == 0) return
    val ui = LocalUi.current
    val t = remember { Animatable(1f) }
    LaunchedEffect(tick) {
        t.snapTo(0f)
        t.animateTo(1f, tween(1900, easing = LinearEasing))
    }
    val parts = remember(tick) {
        val r = Random(tick * 31 + 7)
        List(90) {
            val a = r.nextFloat() * 2f * PI.toFloat()
            val sp = 0.35f + r.nextFloat() * 0.85f
            floatArrayOf(cos(a) * sp, sin(a) * sp - 0.6f, 6f + r.nextFloat() * 10f, r.nextInt(3).toFloat())
        }
    }
    if (t.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val v = t.value
        val cx = size.width / 2f
        val cy = size.height * 0.42f
        val unit = size.width * 0.8f
        val colors = listOf(ui.accent, ui.accent2, Color.White)
        parts.forEach { p ->
            val x = cx + p[0] * unit * v
            val y = cy + p[1] * unit * v + 1.1f * unit * v * v
            val s = p[2] * (1f - v * 0.5f)
            drawRect(
                colors[p[3].toInt()].copy(alpha = (1f - v).coerceIn(0f, 1f)),
                topLeft = Offset(x - s / 2, y - s / 2),
                size = Size(s, s),
            )
        }
    }
}

@Composable
fun BannerHost(model: AppModel, modifier: Modifier = Modifier) {
    val ui = LocalUi.current
    val head = model.banners.firstOrNull()
    val last = remember { mutableStateOf("") }
    if (head != null) last.value = head.text
    LaunchedEffect(head?.id) {
        if (head != null) {
            delay(3300)
            model.banners.remove(head)
        }
    }
    AnimatedVisibility(
        visible = head != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier.statusBarsPadding().padding(16.dp),
    ) {
        Box(
            Modifier
                .shadow(16.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(ui.card)
                .border(1.5.dp, ui.accent, RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text(last.value, color = ui.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun EmojiTile(emoji: String, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(tint),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = (size.value * 0.46f).sp) }
}

@Composable
fun Stat(emoji: String, value: String, label: String, modifier: Modifier) {
    val ui = LocalUi.current
    Row(
        modifier
            .then(if (ui.light) Modifier.shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x22334455), spotColor = Color(0x22334455)) else Modifier)
            .clip(RoundedCornerShape(22.dp))
            .background(ui.card)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiTile(emoji, 42.dp, ui.accent.copy(alpha = 0.14f))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(value, color = ui.text, fontSize = 22.sp, fontWeight = FontWeight.Black, lineHeight = 24.sp)
            Text(label, color = ui.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
        }
    }
}

fun heroTitleStyle(hero: HeroColors, size: Int): TextStyle = TextStyle(
    color = hero.text,
    fontSize = size.sp,
    fontWeight = FontWeight.Black,
    lineHeight = (size + 4).sp,
    shadow = hero.shadow,
    fontFamily = Nunito,
)
