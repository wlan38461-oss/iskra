package dev.daniil.iskra.ui

import android.Manifest
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.daniil.iskra.R
import dev.daniil.iskra.data.Settings
import dev.daniil.iskra.data.Texts
import dev.daniil.iskra.pet.Mood
import dev.daniil.iskra.pet.Palettes
import dev.daniil.iskra.pet.PetLook
import dev.daniil.iskra.reminder.Reminders
import dev.daniil.iskra.scene.DayPhase
import dev.daniil.iskra.scene.SceneRenderer
import dev.daniil.iskra.scene.Sky
import dev.daniil.iskra.widget.IskraWidgetReceiver
import dev.daniil.iskra.widget.StreakWidgetReceiver
import dev.daniil.iskra.widget.TodayWidgetReceiver
import android.provider.Settings as SysSettings

@Composable
fun SettingsScreen(model: AppModel, list: LazyListState) {
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val st = model.state.settings
    val sm = model.summary
    var editName by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val allowed = remember(model.now, model.state) { model.isDemo || Reminders.allowed(ctx) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) openNotificationSettings(ctx)
        model.reload()
    }
    val askNotifications = {
        if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else openNotificationSettings(ctx)
    }

    fun pickTime(current: Int, set: (Int) -> Unit) {
        TimePickerDialog(ctx, { _, h, m -> set(h * 60 + m) }, current / 60, current % 60, true).show()
    }

    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().background(ui.sheet),
        contentPadding = PaddingValues(bottom = 130.dp),
    ) {
        item(key = "hero") { SettingsHero(model) }

        item { Title("Профиль") }
        item {
            Pad {
                CardBox(padding = 4.dp) {
                    NavRow("👤", "Твоё имя", st.userName.ifBlank { "не указано" }) { editName = "user" }
                    Divider()
                    NavRow("🔥", "Имя питомца", st.petName) { editName = "pet" }
                }
            }
        }

        item { Title("Напоминания") }
        item {
            Pad {
                CardBox(padding = 4.dp) {
                    if (!allowed) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ui.danger.copy(alpha = 0.12f))
                                .clickable { askNotifications() }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("🔕", fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Уведомления выключены", color = ui.text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Нажми, чтобы включить — иначе напоминания не придут", color = ui.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    ReminderRow("🌅", "Утренний план", "что сегодня по делам", st.morningOn, st.morningAt,
                        onToggle = { on -> model.updateSettings { it.copy(morningOn = on) } },
                        onTime = { pickTime(st.morningAt) { m -> model.updateSettings { it.copy(morningAt = m) } } })
                    Divider()
                    ReminderRow("🌙", "Вечером", "если сделано не всё", st.eveningOn, st.eveningAt,
                        onToggle = { on -> model.updateSettings { it.copy(eveningOn = on) } },
                        onTime = { pickTime(st.eveningAt) { m -> model.updateSettings { it.copy(eveningAt = m) } } })
                    Divider()
                    ReminderRow("⏳", "Последний шанс", "когда серия под угрозой", st.lastOn, st.lastAt,
                        onToggle = { on -> model.updateSettings { it.copy(lastOn = on) } },
                        onTime = { pickTime(st.lastAt) { m -> model.updateSettings { it.copy(lastAt = m) } } })
                    Divider()
                    Text(
                        "Для отдельного дела время ставится в самом деле: удерживай его в списке → «Напоминание».",
                        color = ui.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    Row(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                        TextButton(onClick = {
                            if (!allowed) askNotifications() else {
                                Reminders.test(ctx)
                                Toast.makeText(ctx, "Отправила пробное уведомление", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("🔔 Проверить уведомление", color = ui.accentText, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
        }

        item { Title("Виджеты") }
        item {
            Pad {
                CardBox(padding = 14.dp) {
                    WidgetRow(R.drawable.preview_pet, "Питомец", "костёр, серия и прогресс дня · 2×2") {
                        pinWidget(ctx, IskraWidgetReceiver::class.java)
                    }
                    Spacer(Modifier.height(12.dp))
                    WidgetRow(R.drawable.preview_today, "Дела на сегодня", "отмечай дела прямо с экрана · 4×2") {
                        pinWidget(ctx, TodayWidgetReceiver::class.java)
                    }
                    Spacer(Modifier.height(12.dp))
                    WidgetRow(R.drawable.preview_strip, "Серия", "огонёк, неделя и прогресс · 4×1") {
                        pinWidget(ctx, StreakWidgetReceiver::class.java)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Если кнопка не сработала: зажми пустое место на рабочем столе → Виджеты → Искра.",
                        color = ui.dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp,
                    )
                }
            }
        }

        item { Title("Оформление") }
        item {
            Pad {
                CardBox(padding = 4.dp) {
                    SwitchRow("🌗", "По времени суток", "рассвет, день, закат и ночь в цветах", st.autoTheme) { on ->
                        model.updateSettings { it.copy(autoTheme = on) }
                    }
                    PhaseStrip(model.sky.phase)
                }
            }
        }

        item { Title("Цвет пламени") }
        item {
            Pad {
                CardBox(padding = 12.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Palettes.all.forEachIndexed { i, p ->
                            val unlocked = sm.best >= p.unlockAt
                            val sel = st.palette == i
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (sel) Color(p.accent).copy(alpha = 0.18f) else ui.card2)
                                    .border(2.dp, if (sel) Color(p.accent) else Color.Transparent, RoundedCornerShape(18.dp))
                                    .clickable {
                                        if (unlocked) {
                                            model.updateSettings { it.copy(palette = i) }
                                        } else {
                                            Toast.makeText(ctx, "Откроется при рекорде ${p.unlockAt} ${Texts.days(p.unlockAt)}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                PetSprite(PetLook(1, 0.7f, Mood.NORMAL, i), 54.dp, animated = sel, silhouette = !unlocked)
                                Text(
                                    p.name, color = if (unlocked) ui.text else ui.dim, fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 13.sp,
                                )
                                if (!unlocked) Text("🔒 ${p.unlockAt} дн.", color = ui.dim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item { Title("Отклик") }
        item {
            Pad {
                CardBox(padding = 4.dp) {
                    SwitchRow("📳", "Вибрация", "лёгкий отклик при выполнении", st.vibration) { on ->
                        model.updateSettings { it.copy(vibration = on) }
                    }
                }
            }
        }

        item { Title("Как это работает") }
        item {
            Pad {
                CardBox(padding = 18.dp) {
                    Rule("🥚", "Сначала у тебя яйцо. Три дня, когда все дела сделаны, — и из него вылупится Огонёк.")
                    Rule("🪵", "Каждое выполненное дело — полено в костёр.")
                    Rule("🌱", "Каждый засчитанный день питомец подрастает: Огонёк → Пламя (10 дней) → Феникс (21).")
                    Rule("🔥", "Дни подряд — это серия. Она открывает новые цвета пламени.")
                    Rule("🪨", "Каждые 7 дней серии дают уголёк — он спасёт серию при пропуске.")
                    Rule("💨", "Пропустил день без угольков — серия сгорает, питомец грустит и тлеет, но не уменьшается.")
                }
            }
        }

        item { Title("Данные") }
        item {
            Pad {
                CardBox(padding = 4.dp) {
                    NavRow("🗑️", "Удалить все данные", "дела, серия, питомец", danger = true) { confirmReset = true }
                }
            }
        }

        item {
            Text(
                "Искра 2.0 · сделано вместе с Claude 🔥",
                color = ui.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
        }
    }

    editName?.let { which ->
        val isPet = which == "pet"
        var value by remember(which) { mutableStateOf(if (isPet) st.petName else st.userName) }
        AlertDialog(
            onDismissRequest = { editName = null },
            containerColor = ui.card,
            title = { Text(if (isPet) "Имя питомца" else "Как тебя зовут?", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(20) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ui.accent, unfocusedBorderColor = ui.track, cursorColor = ui.accent,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = value.trim()
                    model.updateSettings { s: Settings -> if (isPet) s.copy(petName = v.ifBlank { "Искра" }) else s.copy(userName = v) }
                    editName = null
                }) { Text("Сохранить", fontWeight = FontWeight.ExtraBold, color = ui.accentText) }
            },
            dismissButton = { TextButton(onClick = { editName = null }) { Text("Отмена", color = ui.dim) } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = ui.card,
            title = { Text("Удалить всё?", fontWeight = FontWeight.Black) },
            text = { Text("Дела, серия, рекорды, достижения и питомец пропадут. Начнёшь с нового яйца.") },
            confirmButton = {
                TextButton(onClick = {
                    model.resetAll()
                    confirmReset = false
                }) { Text("Удалить", color = ui.danger, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена", color = ui.dim) } },
        )
    }
}

private fun openNotificationSettings(ctx: Context) {
    try {
        ctx.startActivity(
            Intent(SysSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(SysSettings.EXTRA_APP_PACKAGE, ctx.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: Exception) {
    }
}

private fun pinWidget(ctx: Context, cls: Class<*>) {
    val mgr = AppWidgetManager.getInstance(ctx)
    val ok = try {
        mgr.isRequestPinAppWidgetSupported && mgr.requestPinAppWidget(ComponentName(ctx, cls), null, null)
    } catch (_: Exception) {
        false
    }
    if (!ok) Toast.makeText(ctx, "Добавь вручную: зажми рабочий стол → Виджеты → Искра", Toast.LENGTH_LONG).show()
}

@Composable
private fun SettingsHero(model: AppModel) {
    val ui = LocalUi.current
    val sky = model.sky
    val hero = heroColors(sky)
    val sheet = ui.sheet.toArgb()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxWidth().height(190.dp + top)) {
        SceneCanvas(sky, Modifier.fillMaxSize()) { t, d ->
            SceneRenderer.Opts(
                mode = SceneRenderer.HERO, time = t, density = d, sheet = sheet,
                horizon = 0.78f, arcTop = 0.22f, arcFrom = 0.6f, arcTo = 0.98f, stars = 40, celestialSize = 0.75f,
            )
        }
        Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
            Text("Настройки", style = heroTitleStyle(hero, 32))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip("${sky.phase.emoji} ${sky.phase.title} · ${Texts.time(model.minute)}", hero)
            }
        }
    }
}

@Composable
private fun Pad(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) { content() }
}

@Composable
private fun Title(text: String) {
    SectionTitle(text, Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp))
}

@Composable
private fun Divider() {
    HorizontalDivider(color = LocalUi.current.track.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun RowIcon(emoji: String) {
    EmojiTile(emoji, 40.dp, LocalUi.current.accent.copy(alpha = 0.12f))
}

@Composable
private fun NavRow(emoji: String, title: String, value: String, danger: Boolean = false, onClick: () -> Unit) {
    val ui = LocalUi.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(emoji)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) ui.danger else ui.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(value, color = ui.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("›", color = ui.dim, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AppSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val ui = LocalUi.current
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = ui.accent,
            checkedBorderColor = ui.accent,
            uncheckedThumbColor = ui.dim,
            uncheckedTrackColor = ui.track,
            uncheckedBorderColor = ui.track,
        ),
    )
}

@Composable
private fun SwitchRow(emoji: String, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val ui = LocalUi.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(emoji)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ui.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(sub, color = ui.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        AppSwitch(checked, onChange)
    }
}

@Composable
private fun ReminderRow(
    emoji: String,
    title: String,
    sub: String,
    on: Boolean,
    at: Int,
    onToggle: (Boolean) -> Unit,
    onTime: () -> Unit,
) {
    val ui = LocalUi.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onToggle(!on) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(emoji)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ui.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(sub, color = ui.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            Texts.time(at),
            color = if (on) ui.accentText else ui.dim,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (on) ui.accent.copy(alpha = 0.14f) else ui.card2)
                .clickable(onClick = onTime)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        Spacer(Modifier.width(8.dp))
        AppSwitch(on, onToggle)
    }
}

@Composable
private fun WidgetRow(res: Int, title: String, sub: String, onAdd: () -> Unit) {
    val ui = LocalUi.current
    Column {
        Image(
            painter = painterResource(res),
            contentDescription = title,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = ui.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(sub, color = ui.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Добавить",
                color = ui.onAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(ui.accent2, ui.accent)))
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun PhaseStrip(current: DayPhase) {
    val ui = LocalUi.current
    val phases = listOf(
        DayPhase.DAWN to 375, DayPhase.MORNING to 555, DayPhase.DAY to 840,
        DayPhase.SUNSET to 1095, DayPhase.DUSK to 1230, DayPhase.NIGHT to 120,
    )
    val ranges = mapOf(
        DayPhase.DAWN to "5:00", DayPhase.MORNING to "7:30", DayPhase.DAY to "11:00",
        DayPhase.SUNSET to "17:00", DayPhase.DUSK to "19:30", DayPhase.NIGHT to "21:30",
    )
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        phases.forEach { (ph, m) ->
            val sky = Sky.at(m)
            val sel = ph == current
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(sky.top), Color(sky.mid), Color(sky.bottom))))
                        .border(2.dp, if (sel) ui.accent else Color.Transparent, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text(ph.emoji, fontSize = 18.sp) }
                Spacer(Modifier.height(4.dp))
                Text(ph.title, color = if (sel) ui.accentText else ui.text, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(ranges[ph] ?: "", color = ui.dim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Rule(emoji: String, text: String) {
    val ui = LocalUi.current
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, color = ui.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp)
    }
}

