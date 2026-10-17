package dev.daniil.iskra.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.daniil.iskra.data.Logic
import dev.daniil.iskra.pet.Mood
import dev.daniil.iskra.pet.Palettes

@Composable
fun SettingsScreen(model: AppModel) {
    val c = LocalColors.current
    val ctx = LocalContext.current
    val st = model.state.settings
    val sm = model.summary
    var editName by remember { mutableStateOf<String?>(null) } // "user" or "pet"
    var confirmReset by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        model.updateSettings { it.copy(reminderOn = ok) }
        if (!ok) Toast.makeText(ctx, "Без разрешения уведомления не придут", Toast.LENGTH_LONG).show()
    }

    fun setReminder(on: Boolean) {
        if (on && Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            model.updateSettings { it.copy(reminderOn = on) }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Настройки", color = c.text, fontSize = 30.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        item { SectionTitle("Профиль") }
        item {
            CardBox(padding = 4.dp) {
                SettingRow("👤", "Твоё имя", st.userName.ifBlank { "не указано" }) { editName = "user" }
                Divider()
                SettingRow("🔥", "Имя питомца", st.petName) { editName = "pet" }
            }
        }

        item { SectionTitle("Цвет пламени") }
        item {
            CardBox(padding = 14.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palettes.all.forEachIndexed { i, p ->
                        val unlocked = sm.best >= p.unlockAt
                        val sel = st.palette == i
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (sel) Color(p.accent).copy(alpha = 0.18f) else c.card2)
                                .border(2.dp, if (sel) Color(p.accent) else Color.Transparent, RoundedCornerShape(18.dp))
                                .clickable {
                                    if (unlocked) {
                                        model.updateSettings { it.copy(palette = i) }
                                    } else {
                                        Toast.makeText(ctx, "Откроется при рекорде ${p.unlockAt} ${Logic.days(p.unlockAt)}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            PetSprite(1, Mood.NORMAL, i, 54.dp, animated = sel, silhouette = !unlocked)
                            Text(
                                p.name, color = if (unlocked) c.text else c.dim, fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 13.sp,
                            )
                            if (!unlocked) {
                                Text("🔒 ${p.unlockAt} дн.", color = c.dim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item { SectionTitle("Напоминание") }
        item {
            CardBox(padding = 4.dp) {
                SwitchRow("🔔", "Напоминать вечером", "если дела ещё не сделаны", st.reminderOn) { setReminder(it) }
                Divider()
                SettingRow(
                    "⏰", "Время",
                    "%02d:%02d".format(st.reminderHour, st.reminderMinute),
                    enabled = st.reminderOn,
                ) {
                    TimePickerDialog(ctx, { _, h, m ->
                        model.updateSettings { it.copy(reminderHour = h, reminderMinute = m) }
                    }, st.reminderHour, st.reminderMinute, true).show()
                }
            }
        }

        item { SectionTitle("Отклик") }
        item {
            CardBox(padding = 4.dp) {
                SwitchRow("📳", "Вибрация", "лёгкий отклик при выполнении дел", st.vibration) { on ->
                    model.updateSettings { it.copy(vibration = on) }
                }
            }
        }

        item { SectionTitle("Как это работает") }
        item {
            CardBox(padding = 18.dp) {
                Rule("🪵", "Каждое выполненное дело — полено для питомца.")
                Rule("🔥", "Сделал все дела за день — день засчитан, серия растёт.")
                Rule("🌟", "Серия 3, 7 и 14 дней — новые формы: Огонёк, Пламя, Феникс.")
                Rule("🪨", "Каждые 7 дней дают уголёк — он спасёт серию при пропуске.")
                Rule("💨", "Пропустил день без угольков — серия сгорает, питомец тлеет.")
                Rule("🎨", "Рекорды открывают новые цвета пламени.")
            }
        }

        item { SectionTitle("Данные") }
        item {
            CardBox(padding = 4.dp) {
                SettingRow("🗑️", "Удалить все данные", "дела, серия, достижения", danger = true) { confirmReset = true }
            }
        }

        item {
            Text(
                "Искра 1.1 · сделано вместе с Claude 🔥",
                color = c.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }

    editName?.let { which ->
        val isPet = which == "pet"
        var value by remember(which) { mutableStateOf(if (isPet) st.petName else st.userName) }
        AlertDialog(
            onDismissRequest = { editName = null },
            containerColor = c.card,
            title = { Text(if (isPet) "Имя питомца" else "Как тебя зовут?", fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(20) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent,
                        unfocusedBorderColor = c.track,
                        cursorColor = c.accent,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = value.trim()
                    model.updateSettings {
                        if (isPet) it.copy(petName = v.ifBlank { "Искра" }) else it.copy(userName = v)
                    }
                    editName = null
                }) { Text("Сохранить", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { editName = null }) { Text("Отмена") } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = c.card,
            title = { Text("Удалить всё?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Дела, серия, рекорды и достижения пропадут навсегда.") },
            confirmButton = {
                TextButton(onClick = {
                    model.resetAll()
                    confirmReset = false
                }) { Text("Удалить", color = Color(0xFFFF6B6B), fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = LocalColors.current.track.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun RowIcon(emoji: String) {
    val c = LocalColors.current
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = 18.sp) }
}

@Composable
private fun SettingRow(
    emoji: String,
    title: String,
    value: String,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val c = LocalColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(emoji)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (danger) Color(0xFFFF6B6B) else if (enabled) c.text else c.dim,
                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
            )
            Text(value, color = c.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("›", color = c.dim, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SwitchRow(emoji: String, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalColors.current
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
            Text(title, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(sub, color = c.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent,
                checkedTrackColor = c.accent,
                uncheckedThumbColor = c.dim,
                uncheckedTrackColor = c.track,
                uncheckedBorderColor = c.track,
            ),
        )
    }
}

@Composable
private fun Rule(emoji: String, text: String) {
    val c = LocalColors.current
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp)
    }
    Spacer(Modifier.height(0.dp))
}
