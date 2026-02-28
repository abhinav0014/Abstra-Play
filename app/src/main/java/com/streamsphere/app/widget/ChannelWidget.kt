package com.streamsphere.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.*
import com.streamsphere.app.MainActivity
import com.streamsphere.app.data.api.AppDatabase
import com.streamsphere.app.data.model.FavouriteChannel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun appDatabase(): AppDatabase
}

class ChannelWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(140.dp, 100.dp),
            DpSize(220.dp, 120.dp),
            DpSize(220.dp, 220.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val widgetChannels = try {
            entryPoint.appDatabase().favouritesDao().getWidgetChannels().first()
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            WidgetContent(context = context, channels = widgetChannels)
        }
    }
}

@Composable
private fun WidgetContent(context: Context, channels: List<FavouriteChannel>) {
    val backgroundColor = ColorProvider(Color(0xFFFFFFFF), Color(0xFF111827))
    val dividerColor    = ColorProvider(Color(0xFFE5E7EB), Color(0xFF1A2235))
    val primaryText     = ColorProvider(Color(0xFF111827), Color(0xFFFFFFFF))
    val secondaryText   = ColorProvider(Color(0xFF4B5563), Color(0xFF6B7A99))
    val dotColors = listOf(Color(0xFFFC8181), Color(0xFFFBD38D), Color(0xFF90CDF4), Color(0xFFD6BCFA))
    
    val intent = Intent(context, MainActivity::class.java)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(intent))
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("📺 StreamSphere", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = primaryText))
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(dividerColor)) {}
            Spacer(modifier = GlanceModifier.height(6.dp))

            if (channels.isEmpty()) {
                Text(
                    text  = "Favourite channels & tap 📱 to add them here.",
                    style = TextStyle(fontSize = 10.sp, color = secondaryText)
                )
            } else {
                channels.take(4).forEachIndexed { index, channel ->
                    val dot = dotColors[index % dotColors.size]
                    val playIntent = Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(MainActivity.EXTRA_CHANNEL_ID, channel.id)
                        putExtra(MainActivity.EXTRA_STREAM_URL, channel.streamUrl)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .clickable(actionStartActivity(playIntent)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = GlanceModifier.width(5.dp).height(5.dp).background(dot).cornerRadius(3.dp)) {}
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(channel.name, style = TextStyle(fontSize = 11.sp, color = primaryText), maxLines = 1)
                    }
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text  = if (channels.isEmpty()) "Open app →" else "Tap to watch →",
                style = TextStyle(fontSize = 9.sp, color = secondaryText)
            )
        }
    }
}

class ChannelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ChannelWidget()
}
