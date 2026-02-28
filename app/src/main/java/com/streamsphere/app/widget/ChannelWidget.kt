package com.streamsphere.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import com.streamsphere.app.MainActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider

class ChannelWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(140.dp, 100.dp),
            DpSize(220.dp, 120.dp),
            DpSize(220.dp, 220.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(context)
        }
    }
}

@Composable
private fun WidgetContent(context: Context) {

    val backgroundColor = ColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFF111827)
    )

    val dividerColor = ColorProvider(
        day = Color(0xFFE5E7EB),
        night = Color(0xFF1A2235)
    )

    val primaryTextColor = ColorProvider(
        day = Color(0xFF111827),
        night = Color(0xFFFFFFFF)
    )

    val secondaryTextColor = ColorProvider(
        day = Color(0xFF4B5563),
        night = Color(0xFF6B7A99)
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📺 StreamSphere",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = primaryTextColor
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

            WidgetRow("🇳🇵", "NTV Nepal", Color(0xFFFC8181), primaryTextColor)
            WidgetRow("🇮🇳", "DD National", Color(0xFFFBD38D), primaryTextColor)
            WidgetRow("🔬", "Discovery Science", Color(0xFF90CDF4), primaryTextColor)
            WidgetRow("🎵", "MTV", Color(0xFFD6BCFA), primaryTextColor)

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                "Tap to open →",
                style = TextStyle(
                    fontSize = 9.sp,
                    color = secondaryTextColor
                )
            )
        }
    }
}

@Composable
private fun WidgetRow(
    flag: String,
    name: String,
    dot: Color,
    textColor: ColorProvider
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
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
            text = "$flag $name",
            style = TextStyle(
                fontSize = 11.sp,
                color = textColor
            ),
            maxLines = 1
        )
    }
}

class ChannelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ChannelWidget()
}
