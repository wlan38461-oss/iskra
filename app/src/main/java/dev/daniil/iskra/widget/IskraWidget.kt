package dev.daniil.iskra.widget

import android.content.Context
import android.graphics.Bitmap
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
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.daniil.iskra.MainActivity
import dev.daniil.iskra.data.Logic
import dev.daniil.iskra.data.Store
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.pet.PetRenderer

class IskraWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sm = Logic.summary(Store.load(context))
        val pet = PetRenderer.bitmap(240, sm.stage, sm.mood)
        provideContent { Content(sm, pet) }
    }

    @Composable
    private fun Content(sm: Summary, pet: Bitmap) {
        val progress = when {
            sm.total == 0 -> "добавь дела"
            sm.complete -> "день засчитан ✓"
            else -> "сегодня ${sm.done}/${sm.total}"
        }
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF1F1512))
                .cornerRadius(22.dp)
                .padding(8.dp)
                .clickable(actionStartActivity<MainActivity>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(pet),
                contentDescription = "Искра",
                modifier = GlanceModifier.size(68.dp),
            )
            Text(
                text = "🔥 ${sm.streak}",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFC94A)),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = progress,
                style = TextStyle(
                    color = ColorProvider(if (sm.complete) Color(0xFFFF9A4A) else Color(0xFFCDB8A8)),
                    fontSize = 12.sp,
                ),
            )
        }
    }
}

class IskraWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IskraWidget()
}
