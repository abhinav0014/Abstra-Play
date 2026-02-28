package com.streamsphere.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.*
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
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

// EntryPoint to access Room DB from Glance widget (outside Hilt graph)
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
        // Fetch widget channels from Room DB
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
private fun WidgetContent(
    context: Context,
    channels: List<com.streamsphere.app.data.model.FavouriteChannel>
) {
    val backgroundColor = ColorProvider(Color(0xFFFFFFFF), Color(0xFF111827))
    val dividerColor    = ColorProvider(Color(0xFFE5E7EB), Color(0xFF1A2235))
    val primaryText     = ColorProvider(Color(0xFF111827), Color(0xFFFFFFFF))
    val secondaryText   = ColorProvider(Color(0xFF4B5563), Color(0xFF6B7A99))

    // Dot colors cycling
    val dotColors = listOf(
        Color(0xFFFC8181),
        Color(0xFFFBD38D),
        Color(0xFF90CDF4),
        Color(0xFFD6BCFA)
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .padding(12.dp)
            // Tapping the widget header opens the app
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "📺 StreamSphere",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize   = 12.sp,
                        color      = primaryText
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(dividerColor)
            ) {}

            Spacer(modifier = GlanceModifier.height(6.dp))

            if (channels.isEmpty()) {
                Text(
                    text  = "No widget channels yet.\nFavourite a channel and\ntap 📱 to add it here.",
                    style = TextStyle(fontSize = 10.sp, color = secondaryText)
                )
            } else {
                channels.take(4).forEachIndexed { index, channel ->
                    val dot = dotColors[index % dotColors.size]

                    // Each channel row is individually clickable to play that channel
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
                        Box(
                            modifier = GlanceModifier
                                .width(5.dp)
                                .height(5.dp)
                                .background(dot)
                                .cornerRadius(3.dp)
                        ) {}

                        Spacer(modifier = GlanceModifier.width(6.dp))

                        Text(
                            text  = channel.name,
                            style = TextStyle(
                                fontSize = 11.sp,
                                color    = primaryText
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text  = if (channels.isEmpty()) "Open app →" else "Tap channel to watch →",
                style = TextStyle(fontSize = 9.sp, color = secondaryText)
            )
        }
    }
}

class ChannelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ChannelWidget()
}
