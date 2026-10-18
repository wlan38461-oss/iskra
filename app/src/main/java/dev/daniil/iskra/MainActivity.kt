package dev.daniil.iskra

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.daniil.iskra.data.Demo
import dev.daniil.iskra.data.Repo
import dev.daniil.iskra.data.Task
import dev.daniil.iskra.reminder.Reminders
import dev.daniil.iskra.scene.Sky
import dev.daniil.iskra.ui.AppModel
import dev.daniil.iskra.ui.BannerHost
import dev.daniil.iskra.ui.DemoConfig
import dev.daniil.iskra.ui.EmberBurst
import dev.daniil.iskra.ui.HatchDialog
import dev.daniil.iskra.ui.IskraTheme
import dev.daniil.iskra.ui.LocalClock
import dev.daniil.iskra.ui.LocalUi
import dev.daniil.iskra.ui.PET_HERO
import dev.daniil.iskra.ui.PetScreen
import dev.daniil.iskra.ui.PetSprite
import dev.daniil.iskra.ui.SettingsScreen
import dev.daniil.iskra.ui.TASKS_HERO
import dev.daniil.iskra.ui.TaskSheet
import dev.daniil.iskra.ui.TasksScreen
import dev.daniil.iskra.ui.uiColors
import dev.daniil.iskra.widget.IskraWidgetReceiver
import dev.daniil.iskra.widget.StreakWidgetReceiver
import dev.daniil.iskra.widget.TodayWidgetReceiver
import dev.daniil.iskra.widget.refreshWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Reminders.ensureChannels(this)
        val demo = demoFrom(intent)
        val launch = Launch(
            tab = intent.getIntExtra("demo_tab", 0),
            sheet = intent.getBooleanExtra("demo_sheet", false),
            hatch = intent.getBooleanExtra("demo_hatch", false),
            hatchAt = intent.getFloatExtra("demo_hatch_at", -1f),
        )
        intent.getStringExtra("demo_pin")?.let { pin(it) }
        intent.getStringExtra("demo_notify")?.let { demoNotify(it) }
        setContent { App(demo, launch) }
    }

    private fun demoFrom(i: Intent): DemoConfig? {
        if (!i.getBooleanExtra("demo", false)) return null
        val minute = i.getIntExtra("demo_minute", 12 * 60)
        val st = Demo.state(
            today = LocalDate.now(),
            lit = i.getIntExtra("demo_lit", 12),
            done = i.getIntExtra("demo_done", 2),
            count = i.getIntExtra("demo_tasks", 6),
            palette = i.getIntExtra("demo_palette", 0),
            hatchSeen = true,
            broken = i.getBooleanExtra("demo_broken", false),
        )
        if (i.getBooleanExtra("demo_save", false)) {
            Repo.save(this, st)
            Repo.setDebugMinute(this, minute)
            CoroutineScope(Dispatchers.Default).launch { refreshWidgets(applicationContext) }
            return null
        }
        return DemoConfig(st, minute)
    }

    private fun pin(kind: String) {
        val cls = when (kind) {
            "today" -> TodayWidgetReceiver::class.java
            "strip" -> StreakWidgetReceiver::class.java
            else -> IskraWidgetReceiver::class.java
        }
        val mgr = AppWidgetManager.getInstance(this)
        if (mgr.isRequestPinAppWidgetSupported) mgr.requestPinAppWidget(ComponentName(this, cls), null, null)
    }

    private fun demoNotify(kind: String) {
        if (kind == "task") Reminders.test(this)
    }
}

class Launch(val tab: Int, val sheet: Boolean, val hatch: Boolean, val hatchAt: Float)

private val NEW_TASK = Task(-1, "", "✨")

@Composable
fun App(demo: DemoConfig?, launch: Launch) {
    val ctx = LocalContext.current
    val activity = ctx as ComponentActivity
    val model = remember { AppModel(ctx.applicationContext, demo) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) model.reload() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            model.tick()
        }
    }

    // one clock for all animations
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { clock.floatValue = (it - start) / 1_000_000_000f }
    }

    // ask for notifications once
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { model.reload() }
    LaunchedEffect(Unit) {
        if (demo == null && Build.VERSION.SDK_INT >= 33 && !Repo.flag(ctx, "askedNotif") &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Repo.setFlag(ctx, "askedNotif", true)
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val sky = model.sky
    val ui = uiColors(sky.phase, model.state.settings.palette, model.state.settings.autoTheme)
    var tab by remember { mutableIntStateOf(launch.tab.coerceIn(0, 2)) }
    var sheet by remember { mutableStateOf<Task?>(if (launch.sheet) NEW_TASK else null) }
    if (launch.hatch) {
        LaunchedEffect(Unit) { model.showHatch = true }
    }
    val lists = listOf(rememberLazyListState(), rememberLazyListState(), rememberLazyListState())
    val heroes = listOf(TASKS_HERO, PET_HERO, 190.dp)

    // status bar icons follow the sky while the hero is on screen, then the sheet
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val current = lists[tab]
    val heroPx = with(density) { (heroes[tab] + topInset - 24.dp).toPx() }
    val overSheet by remember(tab, heroPx) {
        derivedStateOf { current.firstVisibleItemIndex > 0 || current.firstVisibleItemScrollOffset > heroPx }
    }
    val skyLight = Sky.luminance(sky.top) > 0.5f
    val lightBars = if (overSheet) ui.light else skyLight
    LaunchedEffect(lightBars, ui.light) {
        activity.enableEdgeToEdge(
            statusBarStyle = if (lightBars) SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT)
            else SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = if (ui.light) SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT)
            else SystemBarStyle.dark(AColor.TRANSPARENT),
        )
    }

    CompositionLocalProvider(LocalClock provides clock) {
        IskraTheme(ui) {
            val haptic = LocalHapticFeedback.current
            LaunchedEffect(model.feedTick) {
                if (model.feedTick > 0 && model.state.settings.vibration) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            Box(Modifier.fillMaxSize().background(ui.sheet)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tabs",
                ) { t ->
                    when (t) {
                        0 -> TasksScreen(model, lists[0], onEdit = { sheet = it }, onAdd = { sheet = NEW_TASK })
                        1 -> PetScreen(model, lists[1])
                        else -> SettingsScreen(model, lists[2])
                    }
                }
                if (overSheet) {
                    Box(Modifier.fillMaxWidth().height(topInset).background(ui.sheet.copy(alpha = 0.94f)))
                }
                NavBar(
                    tab = tab,
                    onTab = { tab = it },
                    onAdd = { sheet = NEW_TASK },
                    model = model,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                EmberBurst(model.burstTick)
                BannerHost(model, Modifier.align(Alignment.TopCenter))
            }

            sheet?.let { t ->
                val isNew = t === NEW_TASK
                TaskSheet(
                    task = if (isNew) null else model.state.tasks.firstOrNull { it.id == t.id } ?: t,
                    onDismiss = { sheet = null },
                    onSave = { title, emoji, remind ->
                        if (isNew) model.addTask(title, emoji, remind) else model.updateTask(t.copy(title = title, emoji = emoji, remind = remind))
                        sheet = null
                    },
                    onDelete = {
                        model.removeTask(t.id)
                        sheet = null
                    },
                    onMove = { d -> model.moveTask(t.id, d) },
                )
            }

            if (model.showHatch) {
                HatchDialog(
                    palette = model.state.settings.palette,
                    startName = model.state.settings.petName,
                    autoPlay = launch.hatchAt < 0f,
                ) { name -> model.finishHatch(name) }
            }
        }
    }
}

@Composable
private fun NavBar(tab: Int, onTab: (Int) -> Unit, onAdd: () -> Unit, model: AppModel, modifier: Modifier) {
    val ui = LocalUi.current
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier.navigationBarsPadding().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .shadow(18.dp, shape, ambientColor = ui.accent.copy(alpha = 0.25f), spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f))
                .clip(shape)
                .background(ui.nav)
                .border(1.dp, ui.track.copy(alpha = 0.6f), shape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(tab == 0, "Дела", { onTab(0) }) { tint ->
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            NavItem(tab == 1, model.summary.petName, { onTab(1) }) { _ ->
                PetSprite(model.summary.look, 30.dp, animated = tab == 1)
            }
            NavItem(tab == 2, "Настройки", { onTab(2) }) { tint ->
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(58.dp)
                .shadow(16.dp, CircleShape, ambientColor = ui.accent, spotColor = ui.accent)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(ui.accent2, ui.accent)))
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Новое дело", tint = ui.onAccent, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun NavItem(selected: Boolean, label: String, onClick: () -> Unit, icon: @Composable (androidx.compose.ui.graphics.Color) -> Unit) {
    val ui = LocalUi.current
    val tint = if (selected) ui.accentText else ui.dim
    Row(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) ui.accent.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .animateContentSize()
            .height(46.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(tint)
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

