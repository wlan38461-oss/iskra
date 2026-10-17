package dev.daniil.iskra.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.daniil.iskra.MainActivity
import dev.daniil.iskra.data.Logic
import dev.daniil.iskra.data.Store
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.pet.Palettes
import dev.daniil.iskra.pet.PetRenderer

class IskraWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sm = Logic.summary(Store.load(context))
        val pet = PetRenderer.bitmap(240, sm.stage, sm.mood, sm.palette)
        val bg = background(sm.palette)
        provideContent { Content(sm, pet, bg) }
    }

    private fun background(palette: Int): Bitmap {
        val w = 300
        val h = 300
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        val p = Paint()
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFF1B1428.toInt(), 0xFF2A1812.toInt(), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        val accent = Palettes.get(palette).c1 and 0x00FFFFFF
        p.shader = RadialGradient(w * 0.5f, h * 0.45f, w * 0.55f, 0x55000000 or accent, 0, Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        return b
    }

    @Composable
    private fun Content(sm: Summary, pet: Bitmap, bg: Bitmap) {
        val pal = Palettes.get(sm.palette)
        val accent = Color(pal.accent)
        val accent2 = Color(pal.accent2)
        val progress = when {
            sm.total == 0 -> "добавь дела"
            sm.complete -> "день засчитан ✓"
            else -> "сегодня ${sm.done} из ${sm.total}"
        }
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(26.dp)
                .background(ImageProvider(bg), ContentScale.FillBounds)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = GlanceModifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(pet),
                        contentDescription = sm.petName,
                        modifier = GlanceModifier.size(62.dp),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Column {
                        Text(
                            text = "${sm.streak}",
                            style = TextStyle(
                                color = ColorProvider(accent2),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "${Logic.days(sm.streak)} 🔥",
                            style = TextStyle(color = ColorProvider(Color(0xFFCDB8A8)), fontSize = 12.sp),
                        )
                    }
                }
                Spacer(GlanceModifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    sm.week.forEachIndexed { i, cell ->
                        if (i > 0) Spacer(GlanceModifier.width(4.dp))
                        val col = when (cell.state) {
                            1 -> accent
                            2 -> Color(0xFF7A7580)
                            else -> Color(0x33FFFFFF)
                        }
                        Box(
                            modifier = GlanceModifier
                                .size(if (i == sm.week.lastIndex) 12.dp else 10.dp)
                                .cornerRadius(6.dp)
                                .background(col),
                        ) {}
                    }
                }
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    text = progress,
                    style = TextStyle(
                        color = ColorProvider(if (sm.complete) accent else Color(0xFFE8DAD0)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

class IskraWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IskraWidget()
}
