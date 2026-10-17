package dev.daniil.iskra.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.daniil.iskra.pet.Mood
import dev.daniil.iskra.pet.PetRenderer
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Continuously increasing animation phase for flames. */
@Composable
fun rememberPhase(): State<Float> {
    val inf = rememberInfiniteTransition(label = "phase")
    return inf.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI * 12).toFloat(),
        animationSpec = infiniteRepeatable(tween(10_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
}

@Composable
fun PetSprite(
    stage: Int,
    mood: Mood,
    palette: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    silhouette: Boolean = false,
    glow: Boolean = false,
) {
    val phase = if (animated) rememberPhase() else null
    Canvas(modifier.size(size)) {
        val p = phase?.value ?: 0.6f
        drawIntoCanvas {
            PetRenderer.draw(
                it.nativeCanvas, 0f, 0f, this.size.minDimension, stage, mood, p,
                glow = glow, palette = palette, silhouette = silhouette,
            )
        }
    }
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalColors.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(c.card))
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val c = LocalColors.current
    Box(modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
        Text(text, color = c.text, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        if (trailing != null) {
            Text(
                trailing, color = c.dim, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
fun ProgressRing(progress: Float, size: Dp, stroke: Dp, content: @Composable BoxScope.() -> Unit) {
    val c = LocalColors.current
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) { anim.animateTo(progress, tween(700)) }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(c.track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
            drawArc(
                Brush.sweepGradient(listOf(c.accent, c.accent2, c.accent)),
                -90f, 360f * anim.value, false, Offset(inset, inset), arcSize,
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

/** Burst of pixel embers when a day gets completed. */
@Composable
fun EmberBurst(tick: Int, modifier: Modifier = Modifier) {
    if (tick == 0) return
    val c = LocalColors.current
    val t = remember { Animatable(1f) }
    LaunchedEffect(tick) {
        t.snapTo(0f)
        t.animateTo(1f, tween(1800, easing = LinearEasing))
    }
    val parts = remember(tick) {
        val r = Random(tick * 31 + 7)
        List(80) {
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
        val colors = listOf(c.accent, c.accent2, Color.White)
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
    val c = LocalColors.current
    val head = model.banners.firstOrNull()
    val last = remember { androidx.compose.runtime.mutableStateOf("") }
    if (head != null) last.value = head.text
    LaunchedEffect(head?.id) {
        if (head != null) {
            delay(3200)
            model.banners.remove(head)
        }
    }
    AnimatedVisibility(
        visible = head != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier.statusBarsPadding().padding(16.dp),
    ) {
        val text = last.value
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(c.card2, c.card)))
                .border(1.5.dp, c.accent, RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text(text, color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
    }
}
