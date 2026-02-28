package com.streamsphere.app.widget

import android.content.Context
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.*
import androidx.glance.layout.*
import androidx.glance.material3.ColorProviders
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.streamsphere.app.ui.theme.DarkColorScheme
import com.streamsphere.app.ui.theme.LightColorScheme

class ChannelWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(120.dp, 120.dp), DpSize(240.dp, 120.dp), DpSize(240.dp, 240.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(
                colors = ColorProviders(
                    light = LightColorScheme,
                    dark  = DarkColorScheme
                )
            ) {
                WidgetContent(context)
            }
        }
    }
}

@Composable
private fun WidgetContent(context: Context) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "📺",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text  = "StreamSphere",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp,
                        color      = GlanceTheme.colors.onSurface
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))
            Divider()
            Spacer(modifier = GlanceModifier.height(8.dp))

            // Placeholder channel list (real data would come from DataStore/Room)
            Text(
                text  = "🇳🇵 NTV Nepal",
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.padding(bottom = 4.dp)
            )
            Text(
                text  = "🇮🇳 DD National",
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.padding(bottom = 4.dp)
            )
            Text(
                text  = "🔬 Discovery Science",
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.padding(bottom = 4.dp)
            )
            Text(
                text  = "🎵 MTV",
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface)
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text  = "Tap to open →",
                style = TextStyle(
                    fontSize = 10.sp,
                    color    = GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = GlanceModifier.clickable(
                    actionStartActivity<android.app.Activity>(context)
                )
            )
        }
    }
}

class ChannelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ChannelWidget()
}
