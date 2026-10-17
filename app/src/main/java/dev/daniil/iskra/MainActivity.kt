package dev.daniil.iskra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.daniil.iskra.data.IskraState
import dev.daniil.iskra.data.Logic
import dev.daniil.iskra.data.Store
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.data.Task
import dev.daniil.iskra.pet.PetRenderer
import dev.daniil.iskra.widget.IskraWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.PI

private val BgColor = Color(0xFF1A1210)
private val CardColor = Color(0xFF261A16)
private val FireColor = Color(0xFFFF8C2A)
private val GoldColor = Color(0xFFFFD23F)
private val TrackColor = Color(0xFF3A2A24)
private val TextMain = Color(0xFFF3E6DC)
private val TextDim = Color(0xFFA8948A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IskraTheme { IskraApp() } }
    }
}

@Composable
fun IskraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = FireColor,
            onPrimary = Color(0xFF2B0F06),
            background = BgColor,
            surface = CardColor,
            onBackground = TextMain,
            onSurface = TextMain,
            surfaceVariant = Color(0xFF33231D),
            onSurfaceVariant = TextDim,
            outline = Color(0xFF5A4038),
        ),
        content = content,
    )
}

@Composable
fun IskraApp() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(Store.load(ctx)) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var askDelete by remember { mutableStateOf<Task?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }
    val bounce = remember { Animatable(1f) }
    val feed = remember { Animatable(0f) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                state = Store.load(ctx)
                now = LocalDateTime.now()
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val sm = Logic.summary(state, now)

    fun refreshWidget() {
        scope.launch { runCatching { IskraWidget().updateAll(ctx) } }
    }

    fun commit(newState: IskraState) {
        state = newState
        now = LocalDateTime.now()
        refreshWidget()
    }

    fun onToggle(id: Long) {
        val wasDone = id in state.doneIds
        val before = Logic.summary(state, LocalDateTime.now())
        commit(Store.toggle(ctx, state, id))
        val after = Logic.summary(state, now)
        if (!wasDone) {
            scope.launch {
                bounce.snapTo(1f)
                bounce.animateTo(1.2f, tween(110))
                bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 400f))
            }
            scope.launch {
                feed.snapTo(0f)
                feed.animateTo(1f, tween(900))
            }
            if (after.complete && !before.complete) {
                banner = if (after.stage > before.stage) {
                    "✨ Эволюция! Теперь я — ${Logic.stageNames[after.stage]}"
                } else {
                    "🔥 День засчитан! Серия: ${after.streak}"
                }
                scope.launch {
                    delay(3500)
                    banner = null
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(BgColor)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(sm) }
            item { PetCard(sm, bounce.value, feed.value) }
            item { WeekStrip(sm) }
            item {
                Text(
                    "Дела на каждый день",
                    color = TextMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.tasks.isEmpty()) {
                item {
                    Text(
                        "Пока пусто. Добавь то, что хочешь делать каждый день: зарядка, 10 страниц книги, стакан воды…",
                        color = TextDim,
                    )
                }
            }
            items(state.tasks, key = { it.id }) { t ->
                TaskRow(
                    task = t,
                    done = t.id in state.doneIds,
                    onToggle = { onToggle(t.id) },
                    onDelete = { askDelete = t },
                )
            }
            item { AddRow { title -> commit(Store.addTask(ctx, state, title)) } }
            item { Spacer(Modifier.height(48.dp)) }
        }

        banner?.let { Banner(it, Modifier.align(Alignment.BottomCenter)) }
    }

    askDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { askDelete = null },
            title = { Text("Удалить дело?") },
            text = { Text(t.title) },
            confirmButton = {
                TextButton(onClick = {
                    commit(Store.removeTask(ctx, state, t.id))
                    askDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { askDelete = null }) { Text("Отмена") }
            },
            containerColor = CardColor,
        )
    }
}

@Composable
private fun Header(sm: Summary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Искра", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("лучшая серия: ${sm.best}", color = TextDim, fontSize = 13.sp)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF3A2016))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("🔥 ${sm.streak}", color = GoldColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PetCard(sm: Summary, scale: Float, feed: Float) {
    val inf = rememberInfiniteTransition(label = "fire")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI * 12).toFloat(),
        animationSpec = infiniteRepeatable(tween(10_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.5f, 0.9f)
                    },
            ) {
                drawIntoCanvas {
                    PetRenderer.draw(it.nativeCanvas, 0f, 0f, size.minDimension, sm.stage, sm.mood, phase, glow = true)
                }
            }
            if (feed > 0f && feed < 1f) {
                Text(
                    "+🪵",
                    fontSize = 26.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (40f - 60f * feed).dp)
                        .alpha(1f - feed),
                )
            }
        }
        Text(Logic.stageNames[sm.stage], color = FireColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            Logic.moodText(sm),
            color = TextMain,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (sm.total > 0) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("Сытость", color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${sm.done}/${sm.total}", color = TextDim, fontSize = 13.sp)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { sm.done.toFloat() / sm.total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = FireColor,
                trackColor = TrackColor,
            )
        }
        Text(
            Logic.nextStageText(sm.streak),
            color = TextDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun WeekStrip(sm: Summary) {
    val names = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardColor)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        sm.week.forEachIndexed { i, (date, ok) ->
            val isToday = i == sm.week.lastIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(names[date.dayOfWeek.value - 1], color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (ok) FireColor else TrackColor)
                        .then(if (isToday) Modifier.border(2.dp, GoldColor, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (ok) "🔥" else date.dayOfMonth.toString(),
                        color = TextMain,
                        fontSize = if (ok) 14.sp else 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, done: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (done) Color(0xFF3A2216) else CardColor)
            .clickable(onClick = onToggle)
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (done) FireColor else Color.Transparent)
                .border(2.dp, if (done) FireColor else Color(0xFF6A5048), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Text("✓", color = Color(0xFF2B0F06), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            task.title,
            modifier = Modifier.weight(1f),
            color = if (done) TextDim else TextMain,
            textDecoration = if (done) TextDecoration.LineThrough else null,
            fontSize = 16.sp,
        )
        Text(
            "✕",
            color = TextDim,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onDelete)
                .padding(12.dp),
        )
    }
}

@Composable
private fun AddRow(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val submit: () -> Unit = {
        if (text.isNotBlank()) {
            onAdd(text.trim())
            text = ""
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(60) },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Новое дело…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = submit,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Banner(text: String, modifier: Modifier) {
    Box(
        modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF4A2410))
            .border(1.dp, FireColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(text, color = GoldColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
