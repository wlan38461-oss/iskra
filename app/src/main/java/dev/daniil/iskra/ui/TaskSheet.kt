package dev.daniil.iskra.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.daniil.iskra.data.Task
import dev.daniil.iskra.data.Texts
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskSheet(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (title: String, emoji: String, remind: Int) -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(task?.title ?: "") }
    var emoji by remember { mutableStateOf(task?.emoji ?: "✨") }
    var remind by remember { mutableIntStateOf(task?.remind ?: -1) }
    var lastTime by remember { mutableIntStateOf(if ((task?.remind ?: -1) >= 0) task!!.remind else defaultTime()) }
    var cat by remember {
        mutableIntStateOf(Texts.emojiCats.indexOfFirst { emoji in it.items }.coerceAtLeast(0))
    }
    val save: () -> Unit = {
        if (title.isNotBlank()) onSave(title.trim(), emoji, remind)
    }
    fun pickTime() {
        TimePickerDialog(ctx, { _, h, m ->
            lastTime = h * 60 + m
            remind = lastTime
        }, lastTime / 60, lastTime % 60, true).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = ui.card,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                if (task == null) "Новое дело" else "Изменить дело",
                color = ui.text, fontSize = 24.sp, fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Например, зарядка") },
                leadingIcon = { Text(emoji, fontSize = 24.sp) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ui.accent,
                    unfocusedBorderColor = ui.track,
                    cursorColor = ui.accent,
                    focusedTextColor = ui.text,
                    unfocusedTextColor = ui.text,
                ),
            )

            Spacer(Modifier.height(18.dp))
            Text("Иконка", color = ui.dim, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Texts.emojiCats.forEachIndexed { i, c ->
                    val sel = i == cat
                    Text(
                        "${c.icon} ${c.name}",
                        color = if (sel) ui.onAccent else ui.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) ui.accent else ui.card2)
                            .clickable { cat = i }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 8,
            ) {
                Texts.emojiCats[cat].items.forEach { e ->
                    val sel = e == emoji
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) ui.accent.copy(alpha = 0.25f) else ui.card2)
                            .border(2.dp, if (sel) ui.accent else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = 20.sp) }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ui.card2)
                    .clickable { remind = if (remind >= 0) -1 else lastTime }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔔", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Напоминание", color = ui.text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (remind >= 0) "каждый день, если не сделано" else "выключено",
                        color = ui.dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                if (remind >= 0) {
                    Text(
                        Texts.time(remind),
                        color = ui.accentText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ui.accent.copy(alpha = 0.16f))
                            .clickable { pickTime() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Switch(
                    checked = remind >= 0,
                    onCheckedChange = { on -> remind = if (on) lastTime else -1 },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = ui.accent, checkedBorderColor = ui.accent,
                        uncheckedThumbColor = ui.dim, uncheckedTrackColor = ui.track, uncheckedBorderColor = ui.track,
                    ),
                )
            }

            if (task == null) {
                Spacer(Modifier.height(18.dp))
                Text("Идеи", color = ui.dim, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Texts.suggestions.forEach { (e, t) ->
                        Text(
                            "$e $t",
                            color = ui.text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ui.card2)
                                .clickable {
                                    title = t
                                    emoji = e
                                    cat = Texts.emojiCats.indexOfFirst { e in it.items }.coerceAtLeast(cat)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoveChip("↑ Выше") { onMove(-1) }
                    MoveChip("↓ Ниже") { onMove(1) }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task != null) {
                    TextButton(onClick = onDelete) {
                        Text("Удалить", color = ui.danger, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = save,
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ui.accent, contentColor = ui.onAccent),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                ) {
                    Text(if (task == null) "Добавить" else "Сохранить", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MoveChip(text: String, onClick: () -> Unit) {
    val ui = LocalUi.current
    Text(
        text,
        color = ui.text,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ui.card2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

private fun defaultTime(): Int {
    val h = (LocalTime.now().hour + 1).coerceIn(7, 22)
    return h * 60
}
