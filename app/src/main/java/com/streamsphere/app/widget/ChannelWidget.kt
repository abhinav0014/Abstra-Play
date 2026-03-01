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
import androidx.glance.ImageProvider
import com.streamsphere.app.MainActivity
import com.streamsphere.app.R
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

/**
 * 1×1 square widget showing:
 *  - The channel logo image (if available via remote URL loaded as bitmap)
 *  - OR the channel name initial + flag emoji as large text fallback
 * Tapping opens the app directly to play that channel.
 */
class ChannelWidget : GlanceAppWidget() {

    // Single 1x1 cell size
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(73.dp, 73.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        // Pick the first widget-pinned channel
        val channel: FavouriteChannel? = try {
            entryPoint.appDatabase().favouritesDao().getWidgetChannels().first().firstOrNull()
        } catch (e: Exception) {
            null
        }

        provideContent {
            SquareWidgetContent(context = context, channel = channel)
        }
    }
}

@Composable
private fun SquareWidgetContent(context: Context, channel: FavouriteChannel?) {
    val bgColor      = ColorProvider(Color(0xFF111827), Color(0xFF111827))
    val textColor    = ColorProvider(Color(0xFFE8EDF5), Color(0xFFE8EDF5))
    val accentColor  = ColorProvider(Color(0xFF4F8EF7), Color(0xFF4F8EF7))
    val overlayColor = ColorProvider(Color(0x99000000), Color(0x99000000))

    val playIntent = if (channel != null) {
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.EXTRA_CHANNEL_ID, channel.id)
            putExtra(MainActivity.EXTRA_STREAM_URL, channel.streamUrl)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    } else null

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .cornerRadius(16.dp)
            .clickable(
                if (playIntent != null) actionStartActivity(playIntent)
                else actionStartActivity<MainActivity>()
            ),
        contentAlignment = Alignment.Center
    ) {
        if (channel == null) {
            // Empty state: show app icon placeholder + hint text
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text  = "📺",
                    style = TextStyle(fontSize = 28.sp, color = textColor)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text  = "Add channel",
                    style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color(0xFF6B7A99), Color(0xFF6B7A99))),
                    maxLines = 2
                )
            }
        } else {
            // Channel content
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Background: try to load logo via Glance Image (bitmap loaded externally)
                // Glance does not support async network images directly, so we use the
                // channel initial + flag as the primary visual, styled to look like a logo tile
                val initial = channel.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                val flag    = if (channel.country.length <= 4) channel.country else ""

                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Big initial letter styled as logo
                    Text(
                        text  = initial,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize   = 32.sp,
                            color      = accentColor
                        )
                    )
                    if (flag.isNotBlank()) {
                        Text(
                            text  = flag,
                            style = TextStyle(fontSize = 11.sp, color = textColor)
                        )
                    }
                }

                // Bottom bar with truncated channel name
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(overlayColor)
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text     = channel.name,
                            style    = TextStyle(
                                fontSize = 8.sp,
                                color    = textColor,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }

                // LIVE indicator dot top-right
                if (channel.streamUrl != null) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize().padding(5.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalAlignment = Alignment.End
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(7.dp)
                                .height(7.dp)
                                .background(ColorProvider(Color(0xFFE53E3E), Color(0xFFE53E3E)))
                                .cornerRadius(4.dp)
                        ) {}
                    }
                }
            }
        }
    }
}

class ChannelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ChannelWidget()
}
