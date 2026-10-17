package dev.daniil.iskra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.daniil.iskra.data.Logic
import dev.daniil.iskra.data.Task

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskSheet(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (title: String, emoji: String) -> Unit,
    onDelete: () -> Unit,
) {
    val c = LocalColors.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(task?.title ?: "") }
    var emoji by remember { mutableStateOf(task?.emoji ?: "✨") }
    val save: () -> Unit = {
        if (title.isNotBlank()) onSave(title.trim(), emoji)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = c.card,
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
                color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Например, зарядка") },
                leadingIcon = { Text(emoji, fontSize = 22.sp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.track,
                    cursorColor = c.accent,
                ),
            )
            Spacer(Modifier.height(16.dp))
            Text("Иконка", color = c.dim, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Logic.emojis.forEach { e ->
                    val sel = e == emoji
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) c.accent.copy(alpha = 0.25f) else c.card2)
                            .border(2.dp, if (sel) c.accent else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = 20.sp) }
                }
            }
            if (task == null) {
                Spacer(Modifier.height(16.dp))
                Text("Идеи", color = c.dim, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Logic.suggestions.forEach { (e, t) ->
                        Text(
                            "$e $t",
                            color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(c.card2)
                                .clickable {
                                    title = t
                                    emoji = e
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task != null) {
                    TextButton(onClick = onDelete) {
                        Text("Удалить", color = Color(0xFFFF6B6B), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = save,
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                ) {
                    Text(if (task == null) "Добавить" else "Сохранить", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Spacer(Modifier.width(1.dp))
        }
    }
}
