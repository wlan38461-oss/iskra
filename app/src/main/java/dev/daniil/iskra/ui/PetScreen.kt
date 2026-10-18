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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.daniil.iskra.data.Achievements
import dev.daniil.iskra.data.Growth
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.data.Texts
import dev.daniil.iskra.pet.Mood
import dev.daniil.iskra.pet.PetLook
import dev.daniil.iskra.scene.SceneRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val PET_HERO: Dp = 450.dp

@Composable
fun PetScreen(model: AppModel, list: LazyListState) {
    val ui = LocalUi.current
    val sm = model.summary
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().background(ui.sheet),
        contentPadding = PaddingValues(bottom = 130.dp),
    ) {
        item(key = "scene") { PetHero(model) }
        item(key = "growth") { Pad { GrowthCard(sm) } }
        item(key = "stats") { Pad { StatsGrid(sm) } }
        item(key = "embers") { Pad { EmbersCard(sm) } }
        item(key = "evo") { Pad { EvolutionCard(sm) } }
        item(key = "ach") { Pad { AchievementsCard(model) } }
    }
}

@Composable
private fun Pad(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { content() }
}

@Composable
private fun PetHero(model: AppModel) {
    val ui = LocalUi.current
    val sm = model.summary
    val sky = model.sky
    val hero = heroColors(sky)
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val bounce = remember { Animatable(1f) }
    var bubble by remember { mutableStateOf(Texts.phrase(sm, model.now.hour, false)) }
    var bubbleSeq by remember { mutableIntStateOf(1) }
    var bubbleVisible by remember { mutableStateOf(true) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(bubbleSeq) {
        bubbleVisible = true
        delay(4200)
        bubbleVisible = false
    }
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
            .height(PET_HERO + top)
            .pointerInput(sm.mood, sm.form) {
                detectTapGestures {
                    if (model.state.settings.vibration) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    bubble = Texts.phrase(sm, model.now.hour, true)
                    bubbleSeq++
                    scope.launch {
                        bounce.snapTo(1f)
                        bounce.animateTo(0.86f, tween(90))
                        bounce.animateTo(1f, spring(dampingRatio = 0.28f, stiffness = Spring.StiffnessMediumLow))
                    }
                }
            },
    ) {
        val look = sm.look
        val logs = sm.done
        SceneCanvas(sky, Modifier.fillMaxSize()) { t, d ->
            SceneRenderer.Opts(
                mode = SceneRenderer.FULL, time = t, density = d, look = look, logs = logs,
                petScale = bounce.value, petPhase = t * PHASE_SPEED,
                horizon = 0.6f, groundY = 0.765f, petSize = 0.33f, arcTop = 0.24f, stars = 80,
            )
        }
        Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
            Text(sm.petName, style = heroTitleStyle(hero, 32))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip(Texts.formName(sm), hero)
                if (sm.hatched) GlassChip("🎂 ${sm.lit} ${Texts.days(sm.lit)}", hero)
                GlassChip("🔥 ${sm.streak}", hero)
            }
        }
        AnimatedVisibility(
            visible = bubbleVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = top + PET_HERO * 0.30f, start = 28.dp, end = 28.dp),
        ) {
            Text(
                bubble,
                color = Color(0xFF2A1A12),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFFF6EC))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        Text(
            "нажми на меня",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(ui.sheet),
        )
    }
}

@Composable
private fun GrowthCard(sm: Summary) {
    val ui = LocalUi.current
    CardBox(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (sm.form == 0) "Яйцо греется" else "${Texts.formName(sm)} растёт",
                    color = ui.text, fontSize = 20.sp, fontWeight = FontWeight.Black,
                )
                Text(Texts.growthText(sm), color = ui.accentText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
            PetSprite(sm.look, 58.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(Texts.moodText(sm), color = ui.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
        Spacer(Modifier.height(14.dp))
        GrowthBar(sm, 10.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            if (sm.form == 0) {
                "Каждый день, когда все дела сделаны, яйцо теплеет. На третий день из него вылупится Огонёк."
            } else {
                "Каждый день, когда все дела сделаны, питомец подрастает. Пропустишь — он не уменьшится, но загрустит и потускнеет."
            },
            color = ui.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp,
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
            Stat("🪵", "${sm.totalDone}", Texts.plural(sm.totalDone, "дело сделано", "дела сделано", "дел сделано"), Modifier.weight(1f))
            Stat("📅", "${sm.lit}", Texts.plural(sm.lit, "день горения", "дня горения", "дней горения"), Modifier.weight(1f))
        }
    }
}

@Composable
private fun EmbersCard(sm: Summary) {
    val ui = LocalUi.current
    CardBox(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Угольки", color = ui.text, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            for (i in 0 until 3) {
                val has = i < sm.freezes
                Box(
                    Modifier
                        .padding(start = 6.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (has) ui.accent.copy(alpha = 0.18f) else ui.track.copy(alpha = 0.6f))
                        .border(2.dp, if (has) ui.accent.copy(alpha = 0.8f) else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🪨", fontSize = 18.sp, modifier = Modifier.alpha(if (has) 1f else 0.25f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Каждые 7 дней серии дают уголёк. Пропустишь день — сгорит уголёк, а не серия. Можно копить до трёх.",
            color = ui.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp,
        )
    }
}

@Composable
private fun EvolutionCard(sm: Summary) {
    val ui = LocalUi.current
    CardBox(padding = 18.dp) {
        Text("Путь огонька", color = ui.text, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (f in 0..3) {
                val reached = sm.lit >= Growth.starts[f]
                val current = f == sm.form
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (current) ui.accent.copy(alpha = 0.16f) else ui.card2)
                            .border(2.dp, if (current) ui.accent else Color.Transparent, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val look = if (current) sm.look else PetLook(f, if (f == 0) 0f else 0.5f, Mood.NORMAL, sm.palette, if (f == 0) 1f else 0f)
                        PetSprite(look, 64.dp, animated = current, silhouette = !reached)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        Growth.names[f],
                        color = if (reached) ui.text else ui.dim, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        if (f == 0) "старт" else "${Growth.starts[f]} ${Texts.days(Growth.starts[f])}",
                        color = ui.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementsCard(model: AppModel) {
    val ui = LocalUi.current
    val got = model.state.achievements
    CardBox(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Достижения", color = ui.text, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text("${got.count { Achievements.byId(it) != null }}/${Achievements.all.size}", color = ui.accentText, fontSize = 15.sp, fontWeight = FontWeight.Black)
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
                            .background(if (has) ui.accent.copy(alpha = 0.14f) else ui.card2)
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(if (has) a.emoji else "🔒", fontSize = 26.sp, modifier = Modifier.alpha(if (has) 1f else 0.45f))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            a.title, color = if (has) ui.text else ui.dim,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 14.sp,
                        )
                        Text(
                            a.desc, color = ui.dim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center, lineHeight = 12.sp,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

