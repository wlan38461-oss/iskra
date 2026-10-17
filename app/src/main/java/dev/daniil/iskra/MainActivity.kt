package dev.daniil.iskra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.daniil.iskra.data.Task
import dev.daniil.iskra.ui.AppModel
import dev.daniil.iskra.ui.BannerHost
import dev.daniil.iskra.ui.EmberBurst
import dev.daniil.iskra.ui.IskraTheme
import dev.daniil.iskra.ui.LocalColors
import dev.daniil.iskra.ui.PetScreen
import dev.daniil.iskra.ui.PetSprite
import dev.daniil.iskra.ui.SettingsScreen
import dev.daniil.iskra.ui.TaskSheet
import dev.daniil.iskra.ui.TasksScreen
import dev.daniil.iskra.ui.colorsFor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

/** null = closed, NEW = add sheet, otherwise the task being edited */
private val NEW = Task(-1, "", "✨")

@Composable
fun App() {
    val ctx = LocalContext.current
    val model = remember { AppModel(ctx.applicationContext) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) model.reload() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    IskraTheme(colorsFor(model.state.settings.palette)) {
        val c = LocalColors.current
        val haptic = LocalHapticFeedback.current
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var sheet by remember { mutableStateOf<Task?>(null) }

        LaunchedEffect(model.feedTick) {
            if (model.feedTick > 0 && model.state.settings.vibration) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = c.bg,
                bottomBar = {
                    NavigationBar(containerColor = c.card, tonalElevation = 0.dp) {
                        val colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = c.accent,
                            selectedTextColor = c.accent,
                            unselectedIconColor = c.dim,
                            unselectedTextColor = c.dim,
                            indicatorColor = c.accent.copy(alpha = 0.16f),
                        )
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                            label = { Text("Дела", fontWeight = FontWeight.ExtraBold) },
                            colors = colors,
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = {
                                val sm = model.summary
                                PetSprite(sm.stage, sm.mood, sm.palette, 30.dp, animated = tab == 1)
                            },
                            label = { Text(model.summary.petName, fontWeight = FontWeight.ExtraBold) },
                            colors = colors,
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                            label = { Text("Настройки", fontWeight = FontWeight.ExtraBold) },
                            colors = colors,
                        )
                    }
                },
                floatingActionButton = {
                    if (tab == 0) {
                        ExtendedFloatingActionButton(
                            onClick = { sheet = NEW },
                            containerColor = c.accent,
                            contentColor = c.onAccent,
                            shape = RoundedCornerShape(20.dp),
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(24.dp)) },
                            text = { Text("Добавить дело", fontWeight = FontWeight.ExtraBold) },
                        )
                    }
                },
            ) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tabs",
                    ) { t ->
                        when (t) {
                            0 -> TasksScreen(model) { task -> sheet = task }
                            1 -> PetScreen(model)
                            else -> SettingsScreen(model)
                        }
                    }
                }
            }

            EmberBurst(model.burstTick)
            BannerHost(model, Modifier.align(Alignment.TopCenter))
        }

        sheet?.let { t ->
            val isNew = t === NEW
            TaskSheet(
                task = if (isNew) null else t,
                onDismiss = { sheet = null },
                onSave = { title, emoji ->
                    if (isNew) model.addTask(title, emoji) else model.updateTask(t.id, title, emoji)
                    sheet = null
                },
                onDelete = {
                    model.removeTask(t.id)
                    sheet = null
                },
            )
        }
    }
}
