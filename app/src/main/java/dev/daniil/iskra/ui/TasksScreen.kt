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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.daniil.iskra.data.Logic
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.data.Task
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TasksScreen(model: AppModel, onEdit: (Task?) -> Unit) {
    val c = LocalColors.current
    val sm = model.summary
    val state = model.state

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header(model) }
        item { TodayCard(sm) }
        item { WeekCard(sm) }
        item {
            CardBox(padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💬", fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        Logic.quoteOfDay(model.now.toLocalDate()),
                        color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        item {
            SectionTitle(
                "Сегодня",
                trailing = if (sm.total > 0) "${sm.done} из ${sm.total}" else null,
            )
        }
        if (state.tasks.isEmpty()) {
            item { EmptyState(sm) { e, t -> model.addTask(t, e) } }
        } else {
            items(state.tasks, key = { it.id }) { t ->
                TaskRow(
                    task = t,
                    done = t.id in state.doneIds,
                    onToggle = { model.toggle(t.id) },
                    onEdit = { onEdit(t) },
                    modifier = Modifier.animateItem(),
                )
            }
            item {
                Text(
                    "Нажми — выполнить · удерживай — изменить",
                    color = c.dim, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Header(model: AppModel) {
    val c = LocalColors.current
    val sm = model.summary
    val now = model.now
    val name = model.state.settings.userName.trim()
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru")))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                date.replaceFirstChar { it.uppercase() },
                color = c.dim, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                Logic.greeting(now.hour) + if (name.isNotEmpty()) ", $name" else "",
                color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Black, lineHeight = 30.sp,
            )
        }
        StreakPill(sm.streak)
    }
}

@Composable
fun StreakPill(streak: Int) {
    val c = LocalColors.current
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(c.accent.copy(alpha = 0.28f), c.accent2.copy(alpha = 0.18f))))
            .border(1.dp, c.accent.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔥", fontSize = 18.sp)
        Spacer(Modifier.width(6.dp))
        Text("$streak", color = c.accent2, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun TodayCard(sm: Summary) {
    val c = LocalColors.current
    CardBox(brush = Brush.linearGradient(listOf(lerp(c.card, c.accent, 0.16f), c.card)), padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = if (sm.total == 0) 0f else sm.done.toFloat() / sm.total,
                size = 104.dp,
                stroke = 11.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (sm.total == 0) "—" else "${sm.done}/${sm.total}",
                        color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Black,
                    )
                    Text("сегодня", color = c.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    PetSprite(sm.stage, sm.mood, sm.palette, 58.dp)
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(sm.petName, color = c.accent, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Text(Logic.stageNames[sm.stage], color = c.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(Logic.moodText(sm), color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun WeekCard(sm: Summary) {
    val c = LocalColors.current
    val names = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    CardBox(padding = 14.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            sm.week.forEachIndexed { i, cell ->
                val isToday = i == sm.week.lastIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        names[cell.date.dayOfWeek.value - 1],
                        color = if (isToday) c.accent2 else c.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (cell.state) {
                                    1 -> Brush.verticalGradient(listOf(c.accent2, c.accent))
                                    2 -> Brush.verticalGradient(listOf(Color(0xFF6E6A73), Color(0xFF45414A)))
                                    else -> Brush.verticalGradient(listOf(c.track, c.track))
                                },
                            )
                            .then(if (isToday) Modifier.border(2.dp, c.accent2, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (cell.state) {
                            1 -> Text("🔥", fontSize = 16.sp)
                            2 -> Text("🪨", fontSize = 15.sp)
                            else -> Text("${cell.date.dayOfMonth}", color = c.dim, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
    val c = LocalColors.current
    val bg by animateColorAsState(if (done) lerp(c.card, c.accent, 0.12f) else c.card, label = "bg")
    val check by animateColorAsState(if (done) c.accent else Color.Transparent, label = "check")
    val pop by animateFloatAsState(
        if (done) 1f else 0.85f,
        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "pop",
    )
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .combinedClickable(onClick = onToggle, onLongClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.accent.copy(alpha = if (done) 0.25f else 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(task.emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            task.title,
            modifier = Modifier.weight(1f),
            color = if (done) c.dim else c.text,
            textDecoration = if (done) TextDecoration.LineThrough else null,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(30.dp)
                .scale(pop)
                .clip(CircleShape)
                .background(check)
                .border(2.5.dp, if (done) c.accent else c.track, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Text("✓", color = c.onAccent, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyState(sm: Summary, onAdd: (String, String) -> Unit) {
    val c = LocalColors.current
    CardBox(padding = 20.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PetSprite(0, sm.mood, sm.palette, 96.dp, glow = true)
            Text("${sm.petName} проголодалась", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Добавь дела, которые хочешь делать каждый день. Вот идеи — нажми, чтобы добавить:",
                color = c.dim, fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Logic.suggestions.take(6).forEach { (e, t) ->
                    Text(
                        "$e $t",
                        color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(c.card2)
                            .clickable { onAdd(e, t) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}
