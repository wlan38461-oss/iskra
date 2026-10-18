package dev.daniil.iskra.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.daniil.iskra.pet.Mood
import dev.daniil.iskra.pet.PetDraw
import dev.daniil.iskra.pet.PetLook
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The egg shakes, cracks and a little flame pops out. Then you name it. */
@Composable
fun HatchDialog(palette: Int, startName: String, autoPlay: Boolean = true, onDone: (String) -> Unit) {
    val ui = LocalUi.current
    val clock = LocalClock.current
    val p = remember { Animatable(if (autoPlay) 0f else 1f) }
    var name by remember { mutableStateOf(startName) }
    LaunchedEffect(Unit) {
        if (autoPlay) p.animateTo(1f, tween(3600, easing = LinearEasing))
    }
    val shards = remember {
        val r = Random(5)
        List(16) {
            val a = -PI.toFloat() * (0.1f + 0.8f * r.nextFloat())
            val sp = 0.5f + r.nextFloat() * 0.8f
            floatArrayOf(cos(a) * sp, sin(a) * sp, 8f + r.nextFloat() * 10f, r.nextFloat() * 360f)
        }
    }
    val egg = PetLook(0, 0f, Mood.NORMAL, palette, 2.98f)
    val baby = PetLook(1, 0f, Mood.HAPPY, palette)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(lerp(Color(0xFF0B0A16), ui.accent, 0.4f), Color(0xFF0B0A16)), radius = 1400f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Canvas(Modifier.size(240.dp)) {
                    val v = p.value
                    val t = clock.value
                    val s = size.minDimension
                    if (v < 0.6f) {
                        val k = v / 0.6f
                        val amp = 4f + 22f * k * k
                        val ang = sin(t * (18f + 30f * k)) * amp
                        val big = s * 1.15f
                        rotate(ang, Offset(s / 2f, s * 0.95f)) {
                            drawIntoCanvas { c ->
                                PetDraw.draw(c.nativeCanvas, (s - big) / 2f, s * 0.95f - big * 0.922f, big, egg, t * PHASE_SPEED, 1f)
                            }
                        }
                    } else {
                        val k = ((v - 0.6f) / 0.25f).coerceIn(0f, 1f)
                        val pop = if (k < 1f) 0.3f + 0.9f * k - 0.2f * k * k else 1f
                        val big = s * 1.75f
                        scale(pop, Offset(s / 2f, s * 0.95f)) {
                            drawIntoCanvas { c ->
                                PetDraw.draw(c.nativeCanvas, (s - big) / 2f, s * 0.95f - big * 0.922f, big, baby, t * PHASE_SPEED, 1f)
                            }
                        }
                    }
                    if (v >= 0.6f && v < 1f) {
                        val k = (v - 0.6f) / 0.4f
                        shards.forEach { sh ->
                            val x = s / 2f + sh[0] * s * 0.6f * k
                            val y = s * 0.6f + sh[1] * s * 0.6f * k + s * 0.5f * k * k
                            val sz = sh[2]
                            drawRect(
                                Color(0xFF4E333A).copy(alpha = (1f - k).coerceIn(0f, 1f)),
                                topLeft = Offset(x - sz / 2, y - sz / 2),
                                size = Size(sz, sz),
                            )
                        }
                    }
                    if (v in 0.56f..0.7f) {
                        val f = 1f - kotlin.math.abs(v - 0.6f) / 0.1f
                        drawRect(Color.White.copy(alpha = (f * 0.9f).coerceIn(0f, 1f)), size = Size(size.width, size.height))
                    }
                }
                Spacer(Modifier.height(18.dp))
                AnimatedVisibility(visible = p.value < 0.6f) {
                    Text(
                        "Кажется, что-то происходит…",
                        color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                    )
                }
                AnimatedVisibility(
                    visible = p.value >= 0.85f,
                    enter = fadeIn() + slideInVertically { it / 3 },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "🐣 Огонёк вылупился!",
                            color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                        )
                        Text(
                            "Три дня ты грел яйцо делами. Теперь у тебя есть маленький огонёк — он будет расти каждый день, когда ты всё успеваешь.",
                            color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center, lineHeight = 20.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Как его назовёшь?", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(20) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onDone(name) }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ui.accent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                cursorColor = ui.accent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { onDone(name) },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ui.accent, contentColor = ui.onAccent),
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                        ) { Text("Привет, малыш!", fontWeight = FontWeight.Black, fontSize = 17.sp) }
                    }
                }
            }
        }
    }
}
