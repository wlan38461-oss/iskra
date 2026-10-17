package dev.daniil.iskra.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.daniil.iskra.data.Achievements
import dev.daniil.iskra.data.Logic
import dev.daniil.iskra.data.Store
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.pet.Mood
import dev.daniil.iskra.pet.PetRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PetScreen(model: AppModel) {
    val sm = model.summary
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Scene(model) }
        item { NameCard(sm) }
        item { StatsGrid(sm) }
        item { EmbersCard(sm) }
        item { EvolutionCard(sm) }
        item { AchievementsCard(model) }
    }
}

@Composable
private fun Scene(model: AppModel) {
    val c = LocalColors.current
    val sm = model.summary
    val phase = rememberPhase()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val bounce = remember { Animatable(1f) }
    var bubble by remember { mutableStateOf(Logic.phrase(sm, model.now.hour, false)) }
    var bubbleSeq by remember { mutableIntStateOf(1) }
    var bubbleVisible by remember { mutableStateOf(true) }

    LaunchedEffect(bubbleSeq) {
        bubbleVisible = true
        delay(4200)
        bubbleVisible = false
    }
    // react when fed from the other tab
    LaunchedEffect(model.feedTick) {
        if (model.feedTick > 0) {
            bounce.snapTo(1f)
            bounce.animateTo(1.18f, tween(110))
            bounce.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(28.dp))
            .pointerInput(sm.mood, sm.stage) {
                detectTapGestures {
                    if (model.state.settings.vibration) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    bubble = Logic.phrase(sm, model.now.hour, true)
                    bubbleSeq++
                    scope.launch {
                        bounce.snapTo(1f)
                        bounce.animateTo(0.86f, tween(90))
                        bounce.animateTo(1f, spring(dampingRatio = 0.28f, stiffness = Spring.StiffnessMediumLow))
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawScene(sm, phase, bounce.value, c)
        }
        AnimatedVisibility(
            visible = bubbleVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp, start = 24.dp, end = 24.dp),
        ) {
            Text(
                bubble,
                color = Color(0xFF2A1A12),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFFF4E8))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        Text(
            "нажми на меня",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
        )
    }
}

private class Star(val x: Float, val y: Float, val s: Float, val p: Float)

private val stars = Random(42).let { r ->
    List(46) { Star(r.nextFloat(), r.nextFloat() * 0.6f, 1.5f + r.nextFloat() * 2.5f, r.nextFloat() * 6.28f) }
}

private fun DrawScope.drawScene(sm: Summary, phaseState: State<Float>, bounce: Float, c: IskraColors) {
    val phase = phaseState.value
    val w = size.width
    val h = size.height
    val d = density

    // sky
    drawRect(
        Brush.verticalGradient(
            0f to Color(0xFF0B0A1C),
            0.55f to Color(0xFF1C1430),
            0.85f to Color(0xFF2E1A16),
            1f to Color(0xFF2E1A16),
        ),
    )
    // stars
    stars.forEach { s ->
        val a = 0.25f + 0.75f * ((sin(phase * 0.5f + s.p) + 1f) / 2f)
        val px = s.s * d * 0.8f
        drawRect(Color.White.copy(alpha = a), Offset(s.x * w, s.y * h), Size(px, px))
    }
    // moon
    val mx = w * 0.84f
    val my = h * 0.15f
    val mr = 18f * d
    drawCircle(Color(0xFFFFF1CF).copy(alpha = 0.12f), mr * 2.2f, Offset(mx, my))
    drawCircle(Color(0xFFF6EBD2), mr, Offset(mx, my))
    drawCircle(Color(0xFFDCCFB2), mr * 0.22f, Offset(mx - mr * 0.3f, my - mr * 0.2f))
    drawCircle(Color(0xFFDCCFB2), mr * 0.14f, Offset(mx + mr * 0.35f, my + mr * 0.3f))

    // ground
    val groundY = h * 0.80f
    drawRect(Color(0xFF1A110E), Offset(0f, groundY), Size(w, h - groundY))
    drawRect(Color(0xFF2B1B14), Offset(0f, groundY), Size(w, 3f * d))

    val petSize = min(w * 0.56f, h * 0.62f)
    val logs = min(sm.done, 6)
    val logW = petSize * 0.40f
    val logH = petSize * 0.10f
    val rows = when {
        logs == 0 -> 0
        logs <= 3 -> 1
        logs <= 5 -> 2
        else -> 3
    }
    val stackTop = groundY + 4f * d - rows * logH * 0.9f
    val glowColor = if (sm.mood == Mood.SMOLDER) Color(0xFF8A6A5A) else c.accent

    // warm glow on the ground
    drawCircle(
        Brush.radialGradient(
            listOf(glowColor.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(w / 2f, stackTop),
            radius = petSize * 1.0f,
        ),
        radius = petSize * 1.0f,
        center = Offset(w / 2f, stackTop),
    )

    // log pile (one log per task done today)
    if (logs == 0) {
        drawOval(Color(0xFF3A302C), Offset(w / 2f - petSize * 0.25f, groundY - 4f * d), Size(petSize * 0.5f, 10f * d))
    } else {
        val layout = intArrayOf(0, 0, 0, 1, 1, 2)
        for (row in 0 until rows) {
            val inRow = (0 until logs).count { layout[it] == row }
            val y = groundY + 4f * d - (row + 1) * logH * 0.9f
            val total = inRow * logW * 0.86f
            for (k in 0 until inRow) {
                val x = w / 2f - total / 2f + k * logW * 0.86f
                drawLog(x, y, logW * 0.82f, logH)
            }
        }
    }

    // sparks
    if (sm.mood != Mood.SMOLDER) {
        for (i in 0 until 14) {
            val t = ((phase * 0.12f + i * 0.0713f) % 1f)
            val sx = w / 2f + sin(i * 12.9f + phase * 0.4f) * petSize * 0.38f * (1f - t * 0.4f)
            val sy = stackTop - petSize * 0.25f - t * h * 0.55f
            val sz = (2.5f + (i % 3)) * d
            drawRect(
                (if (i % 2 == 0) c.accent2 else c.accent).copy(alpha = (1f - t) * 0.9f),
                Offset(sx, sy),
                Size(sz, sz),
            )
        }
    }

    // pet
    val left = (w - petSize) / 2f
    val top = stackTop - petSize * 0.96f
    scale(bounce, pivot = Offset(w / 2f, stackTop)) {
        drawIntoCanvas {
            PetRenderer.draw(it.nativeCanvas, left, top, petSize, sm.stage, sm.mood, phase, palette = sm.palette)
        }
    }
}

private fun DrawScope.drawLog(x: Float, y: Float, lw: Float, lh: Float) {
    drawRoundRect(Color(0xFF6B3D24), Offset(x, y), Size(lw, lh), CornerRadius(lh / 2f))
    drawRoundRect(Color(0xFF8A5332), Offset(x + lh * 0.3f, y + lh * 0.18f), Size(lw - lh, lh * 0.22f), CornerRadius(lh * 0.1f))
    drawCircle(Color(0xFFD9A066), lh / 2f, Offset(x + lw - lh / 2f, y + lh / 2f))
    drawCircle(Color(0xFFA9703F), lh * 0.28f, Offset(x + lw - lh / 2f, y + lh / 2f))
    drawCircle(Color(0xFFD9A066), lh * 0.12f, Offset(x + lw - lh / 2f, y + lh / 2f))
}

@Composable
private fun NameCard(sm: Summary) {
    val c = LocalColors.current
    val (text, progress) = Logic.nextStage(sm.streak)
    CardBox(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sm.petName, color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(
                    "${Logic.stageNames[sm.stage]} · стадия ${sm.stage + 1} из 4",
                    color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                )
            }
            StreakPill(sm.streak)
        }
        Spacer(Modifier.height(8.dp))
        Text(Logic.moodText(sm), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Row {
            Text(text, color = c.dim, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = c.accent,
            trackColor = c.track,
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
    }
}

@Composable
private fun StatsGrid(sm: Summary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("🔥", "${sm.streak}", "серия", Modifier.weight(1f))
            Stat("🏆", "${sm.best}", "рекорд", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("🪵", "${sm.totalDone}", "дел сделано", Modifier.weight(1f))
            Stat("📅", "${sm.daysDone}", Logic.plural(sm.daysDone, "день закрыт", "дня закрыто", "дней закрыто"), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(emoji: String, value: String, label: String, modifier: Modifier) {
    val c = LocalColors.current
    Row(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(c.card)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(c.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(value, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Black, lineHeight = 24.sp)
            Text(label, color = c.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmbersCard(sm: Summary) {
    val c = LocalColors.current
    CardBox(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Угольки", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            for (i in 0 until Store.MAX_FREEZES) {
                val has = i < sm.freezes
                Box(
                    Modifier
                        .padding(start = 6.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (has) Color(0xFF4A3F45) else c.track.copy(alpha = 0.5f))
                        .border(2.dp, if (has) c.accent.copy(alpha = 0.7f) else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🪨", fontSize = 17.sp, modifier = Modifier.alpha(if (has) 1f else 0.25f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Каждые 7 дней серии дают уголёк. Если пропустишь день, он сгорит вместо твоей серии. Можно хранить до 3.",
            color = c.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp,
        )
    }
}

@Composable
private fun EvolutionCard(sm: Summary) {
    val c = LocalColors.current
    CardBox(padding = 18.dp) {
        Text("Путь эволюции", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (st in 0..3) {
                val reached = sm.best >= Logic.thresholds[st]
                val current = st == sm.stage
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (current) c.accent.copy(alpha = 0.18f) else c.card2)
                            .border(2.dp, if (current) c.accent else Color.Transparent, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        PetSprite(
                            st, Mood.NORMAL, sm.palette, 58.dp,
                            animated = current, silhouette = !reached,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        Logic.stageNames[st],
                        color = if (reached) c.text else c.dim, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        if (st == 0) "старт" else "${Logic.thresholds[st]} ${Logic.days(Logic.thresholds[st])}",
                        color = c.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementsCard(model: AppModel) {
    val c = LocalColors.current
    val got = model.state.achievements
    CardBox(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Достижения", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text("${got.size}/${Achievements.all.size}", color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        Achievements.all.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { a ->
                    val has = a.id in got
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (has) c.accent.copy(alpha = 0.14f) else c.card2)
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(if (has) a.emoji else "🔒", fontSize = 26.sp, modifier = Modifier.alpha(if (has) 1f else 0.45f))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            a.title, color = if (has) c.text else c.dim,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 14.sp,
                        )
                        Text(
                            a.desc, color = c.dim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center, lineHeight = 12.sp,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
