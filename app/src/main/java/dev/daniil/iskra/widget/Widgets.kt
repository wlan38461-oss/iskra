package dev.daniil.iskra.widget

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.daniil.iskra.MainActivity
import dev.daniil.iskra.R
import dev.daniil.iskra.data.Engine
import dev.daniil.iskra.data.IskraState
import dev.daniil.iskra.data.Repo
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.pet.Palettes
import dev.daniil.iskra.reminder.Reminders
import dev.daniil.iskra.scene.Sky
import dev.daniil.iskra.scene.SkyState
import java.time.LocalDateTime
import kotlin.math.sqrt

internal val STAMP = longPreferencesKey("stamp")

internal class WData(val state: IskraState, val sm: Summary, val sky: SkyState)

internal fun loadData(ctx: Context): WData {
    val st = Repo.load(ctx)
    val now = LocalDateTime.now()
    val dm = Repo.debugMinute(ctx)
    val minute = if (dm >= 0) dm else now.hour * 60 + now.minute
    return WData(st, Engine.summary(st, now), Sky.at(minute))
}

internal fun fonts(ctx: Context): WidgetFonts {
    val black = try {
        ResourcesCompat.getFont(ctx, R.font.nunito_black)
    } catch (_: Exception) {
        null
    } ?: Typeface.DEFAULT_BOLD
    val bold = try {
        ResourcesCompat.getFont(ctx, R.font.nunito_bold)
    } catch (_: Exception) {
        null
    } ?: Typeface.DEFAULT_BOLD
    return WidgetFonts(black, bold)
}

/** Bitmap size for a widget, limited so RemoteViews stay small. */
internal class Px(val w: Int, val h: Int, val d: Float)

internal fun fit(wDp: Float, hDp: Float, density: Float): Px {
    val max = 650_000f
    var d = density
    val area = wDp * hDp * d * d
    if (area > max) d *= sqrt(max / area)
    return Px((wDp * d).toInt().coerceAtLeast(40), (hDp * d).toInt().coerceAtLeast(40), d)
}

/** Redraws every placed widget with fresh data. */
suspend fun refreshWidgets(ctx: Context) {
    try {
        val mgr = GlanceAppWidgetManager(ctx)
        val stamp = System.currentTimeMillis()
        val all: List<GlanceAppWidget> = listOf(IskraWidget(), TodayWidget(), StreakWidget())
        for (w in all) {
            for (id in mgr.getGlanceIds(w.javaClass)) {
                updateAppWidgetState(ctx, id) { it[STAMP] = stamp }
                w.update(ctx, id)
            }
        }
    } catch (_: Throwable) {
    }
}

// ------------------------------------------------------------------ pet 2×2

class IskraWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { PetContent() }
    }
}

@Composable
private fun PetContent() {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val stamp = currentState(STAMP) ?: 0L
    val px = fit(size.width.value, size.height.value, ctx.resources.displayMetrics.density)
    val bmp = remember(stamp, px.w, px.h) {
        val data = loadData(ctx)
        WidgetArt.pet(px.w, px.h, px.d, data.sky, data.sm, fonts(ctx))
    }
    Box(GlanceModifier.fillMaxSize().cornerRadius(22.dp).clickable(actionStartActivity<MainActivity>())) {
        Image(
            provider = ImageProvider(bmp),
            contentDescription = "Искра",
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize(),
        )
    }
}

// ------------------------------------------------------------------ strip 4×1

class StreakWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { StripContent() }
    }
}

@Composable
private fun StripContent() {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val stamp = currentState(STAMP) ?: 0L
    val px = fit(size.width.value, size.height.value, ctx.resources.displayMetrics.density)
    val bmp = remember(stamp, px.w, px.h) {
        val data = loadData(ctx)
        WidgetArt.strip(px.w, px.h, px.d, data.sky, data.sm, fonts(ctx))
    }
    Box(GlanceModifier.fillMaxSize().cornerRadius(22.dp).clickable(actionStartActivity<MainActivity>())) {
        Image(
            provider = ImageProvider(bmp),
            contentDescription = "Серия",
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize(),
        )
    }
}

// ------------------------------------------------------------------ today list 4×2

class TodayWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { TodayContent() }
    }
}

@Composable
private fun TodayContent() {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val stamp = currentState(STAMP) ?: 0L
    val data = remember(stamp) { loadData(ctx) }
    val sm = data.sm
    val light = data.sky.phase.light
    val leftDp = (size.width.value * 0.36f).coerceIn(84f, 150f)
    val px = fit(size.width.value, size.height.value, ctx.resources.displayMetrics.density)
    val bg = remember(stamp, px.w, px.h) {
        WidgetArt.today(px.w, px.h, px.d, data.sky, sm, leftDp * px.d, light)
    }
    val pal = Palettes.get(sm.palette)
    val checkOn = remember(stamp) { WidgetArt.check(54, true, pal.accent.toInt(), light) }
    val checkOff = remember(stamp) { WidgetArt.check(54, false, pal.accent.toInt(), light) }
    val text = if (light) Color(0xFF18222E) else Color(0xFFF1F3FF)
    val dim = if (light) Color(0xFF6A7684) else Color(0xFFA9B1D0)
    val accent = Color(pal.accent)
    val openApp = actionStartActivity<MainActivity>()

    Box(GlanceModifier.fillMaxSize().cornerRadius(22.dp)) {
        Image(
            provider = ImageProvider(bg),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize().clickable(openApp),
        )
        Row(GlanceModifier.fillMaxSize()) {
            Spacer(GlanceModifier.width(leftDp.dp))
            Column(GlanceModifier.fillMaxSize().padding(start = 12.dp, end = 18.dp, top = 16.dp, bottom = 12.dp)) {
                Row(GlanceModifier.fillMaxWidth().clickable(openApp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Сегодня",
                        style = TextStyle(color = ColorProvider(text), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text = when {
                            sm.total == 0 -> ""
                            sm.complete -> "🔥 ${sm.streak}"
                            else -> "${sm.done} из ${sm.total}"
                        },
                        style = TextStyle(color = ColorProvider(accent), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                if (data.state.tasks.isEmpty()) {
                    Text(
                        text = "Добавь дела в приложении — ими питается Искра",
                        style = TextStyle(color = ColorProvider(dim), fontSize = 13.sp),
                        modifier = GlanceModifier.clickable(openApp),
                    )
                } else {
                    LazyColumn(GlanceModifier.fillMaxSize()) {
                        items(data.state.tasks, itemId = { it.id }) { t ->
                            val done = t.id in data.state.doneIds
                            Row(
                                GlanceModifier.fillMaxWidth().padding(vertical = 5.dp)
                                    .clickable(actionRunCallback<ToggleTaskAction>(actionParametersOf(ToggleTaskAction.KEY to t.id))),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(
                                    provider = ImageProvider(if (done) checkOn else checkOff),
                                    contentDescription = if (done) "сделано" else "не сделано",
                                    modifier = GlanceModifier.size(20.dp),
                                )
                                Spacer(GlanceModifier.width(8.dp))
                                Text(
                                    text = "${t.emoji}  ${t.title}",
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(if (done) dim else text),
                                        fontSize = 14.sp,
                                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[KEY] ?: return
        Reminders.toggleOutside(context, id, onlyMarkDone = false)
    }

    companion object {
        val KEY = ActionParameters.Key<Long>("task")
    }
}

// ------------------------------------------------------------------ receivers

class IskraWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IskraWidget()
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}
