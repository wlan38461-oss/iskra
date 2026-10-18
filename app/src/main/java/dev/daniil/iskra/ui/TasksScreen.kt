package dev.daniil.iskra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.data.Task
import dev.daniil.iskra.data.Texts
import dev.daniil.iskra.scene.SceneRenderer
import java.time.format.DateTimeFormatter
import java.util.Locale

val TASKS_HERO: Dp = 250.dp

@Composable
fun TasksScreen(model: AppModel, list: LazyListState, onEdit: (Task) -> Unit, onAdd: () -> Unit) {
    val ui = LocalUi.current
    val sm = model.summary
    val state = model.state
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().background(ui.sheet),
        contentPadding = PaddingValues(bottom = 130.dp),
    ) {
        item(key = "hero") { TasksHero(model) }
        item(key = "today") { Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { TodayCard(sm) } }
        item(key = "week") { Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { WeekCard(sm) } }
        item(key = "title") {
            SectionTitle(
                "Дела на сегодня",
                Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                trailing = if (sm.total > 0) "${sm.done} из ${sm.total}" else null,
            )
        }
        if (state.tasks.isEmpty()) {
            item(key = "empty") {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    EmptyState(sm, onAdd) { e, t -> model.addTask(t, e, -1) }
                }
            }
        } else {
            items(state.tasks, key = { it.id }) { t ->
                TaskRow(
                    task = t,
                    done = t.id in state.doneIds,
                    onToggle = { model.toggle(t.id) },
                    onEdit = { onEdit(t) },
                    modifier = Modifier.animateItem().padding(horizontal = 16.dp, vertical = 5.dp),
                )
            }
            item(key = "hint") {
                Text(
                    "Нажми — выполнить · удерживай — изменить",
                    color = ui.dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                )
            }
        }
        item(key = "quote") {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                CardBox(padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiTile("💬", 40.dp, ui.accent.copy(alpha = 0.12f))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Мысль дня", color = ui.dim, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                Texts.quoteOfDay(model.now.toLocalDate()),
                                color = ui.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksHero(model: AppModel) {
    val ui = LocalUi.current
    val sky = model.sky
    val hero = heroColors(sky)
    val sm = model.summary
    val sheet = ui.sheet.toArgb()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val name = model.state.settings.userName.trim()
    val date = model.now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru"))).replaceFirstChar { it.uppercase() }
    Box(Modifier.fillMaxWidth().height(TASKS_HERO + top)) {
        SceneCanvas(sky, Modifier.fillMaxSize()) { t, d ->
            SceneRenderer.Opts(
                mode = SceneRenderer.HERO, time = t, density = d, sheet = sheet,
                horizon = 0.8f, arcTop = 0.2f, arcFrom = 0.56f, arcTo = 0.98f, stars = 55, celestialSize = 0.85f,
            )
        }
        Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
            Text(date, color = hero.dim, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, style = heroTitleStyle(hero, 14).copy(color = hero.dim))
            Spacer(Modifier.height(2.dp))
            Text(
                Texts.greeting(model.now.hour) + if (name.isNotEmpty()) ",\n$name" else "",
                style = heroTitleStyle(hero, 30),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip("🔥 ${sm.streak} ${Texts.days(sm.streak)}", hero)
                if (sm.freezes > 0) GlassChip("🪨 ${sm.freezes}", hero)
                GlassChip("${sky.phase.emoji} ${sky.phase.title}", hero)
            }
        }
    }
}

@Composable
private fun TodayCard(sm: Summary) {
    val ui = LocalUi.current
    CardBox(
        brush = Brush.linearGradient(listOf(lerp(ui.card, ui.accent, if (ui.light) 0.1f else 0.16f), ui.card)),
        padding = 18.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = if (sm.total == 0) 0f else sm.done.toFloat() / sm.total,
                size = 108.dp,
                stroke = 12.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (sm.total == 0) "—" else "${sm.done}/${sm.total}",
                        color = ui.text, fontSize = 26.sp, fontWeight = FontWeight.Black,
                    )
                    Text("сегодня", color = ui.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PetSprite(sm.look, 60.dp)
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(sm.petName, color = ui.accentText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(
                            Texts.formName(sm) + if (sm.hatched) " · ${sm.lit} ${Texts.days(sm.lit)}" else "",
                            color = ui.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(Texts.moodText(sm), color = ui.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Texts.growthText(sm), color = ui.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        GrowthBar(sm, 8.dp)
    }
}

/** Segmented bar for the egg (3 days), smooth bar for growth after hatching. */
@Composable
fun GrowthBar(sm: Summary, height: Dp) {
    val ui = LocalUi.current
    if (sm.form == 0) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until 3) {
                val fill = (sm.warmth - i).coerceIn(0f, 1f)
                Box(
                    Modifier.weight(1f).height(height).clip(RoundedCornerShape(height / 2)).background(ui.track),
                ) {
                    if (fill > 0f) {
                        Box(
                            Modifier.fillMaxWidth(fill).height(height).clip(RoundedCornerShape(height / 2))
                                .background(Brush.horizontalGradient(listOf(ui.accent, ui.accent2))),
                        )
                    }
                }
            }
        }
    } else {
        LinearProgressIndicator(
            progress = { sm.growth },
            modifier = Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)),
            color = ui.accent,
            trackColor = ui.track,
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
    }
}

@Composable
fun WeekCard(sm: Summary) {
    val ui = LocalUi.current
    val names = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    CardBox(padding = 14.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            sm.week.forEachIndexed { i, cell ->
                val isToday = i == sm.week.lastIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        names[cell.date.dayOfWeek.value - 1],
                        color = if (isToday) ui.accentText else ui.dim, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (cell.state) {
                                    1 -> Brush.verticalGradient(listOf(ui.accent2, ui.accent))
                                    2 -> Brush.verticalGradient(listOf(Color(0xFF8C8794), Color(0xFF5D5866)))
                                    else -> Brush.verticalGradient(listOf(ui.track, ui.track))
                                },
                            )
                            .then(if (isToday) Modifier.border(2.dp, ui.accentText, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (cell.state) {
                            1 -> Text("🔥", fontSize = 16.sp)
                            2 -> Text("🪨", fontSize = 15.sp)
                            else -> Text("${cell.date.dayOfMonth}", color = ui.dim, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(task: Task, done: Boolean, onToggle: () -> Unit, onEdit: () -> Unit, modifier: Modifier) {
    val ui = LocalUi.current
    val bg by animateColorAsState(if (done) lerp(ui.card, ui.accent, if (ui.light) 0.08f else 0.12f) else ui.card, label = "bg")
    val check by animateColorAsState(if (done) ui.accent else Color.Transparent, label = "check")
    val pop by animateFloatAsState(
        if (done) 1f else 0.86f,
        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "pop",
    )
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .fillMaxWidth()
            .then(if (ui.light) Modifier.shadow(6.dp, shape, ambientColor = Color(0x1F334455), spotColor = Color(0x1F334455)) else Modifier)
            .clip(shape)
            .background(bg)
            .combinedClickable(onClick = onToggle, onLongClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiTile(task.emoji, 48.dp, ui.accent.copy(alpha = if (done) 0.24f else 0.12f))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                color = if (done) ui.dim else ui.text,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            if (task.remind >= 0) {
                Text("🔔 ${Texts.time(task.remind)}", color = ui.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(30.dp)
                .scale(pop)
                .clip(CircleShape)
                .background(check)
                .border(2.5.dp, if (done) ui.accent else ui.track, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Text("✓", color = ui.onAccent, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyState(sm: Summary, onAdd: () -> Unit, onQuick: (String, String) -> Unit) {
    val ui = LocalUi.current
    CardBox(padding = 20.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PetSprite(sm.look, 104.dp, glow = 0.8f)
            Text(
                if (sm.form == 0) "Яйцо ждёт тепла" else "${sm.petName} проголодалась",
                color = ui.text, fontSize = 19.sp, fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Добавь дела, которые хочешь делать каждый день. Каждое сделанное — полено в костёр. Вот идеи:",
                color = ui.dim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Texts.suggestions.take(8).forEach { (e, t) ->
                    Text(
                        "$e $t",
                        color = ui.text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ui.card2)
                            .clickable { onQuick(e, t) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
                Text(
                    "＋ Своё дело",
                    color = ui.onAccent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ui.accent)
                        .clickable(onClick = onAdd)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
    }
}
