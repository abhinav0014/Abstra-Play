package com.streamsphere.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.streamsphere.app.MainActivity
import com.streamsphere.app.R
import com.streamsphere.app.data.model.ChannelUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ShortcutHelper {

    /**
     * Requests the launcher to pin a shortcut for [channel].
     * On tap, the shortcut launches [MainActivity] directly into the fullscreen player.
     * Returns true if pinned shortcuts are supported on this device.
     */
    suspend fun pinChannelShortcut(context: Context, channel: ChannelUiModel): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

        val manager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!manager.isRequestPinShortcutSupported) return false

        val icon = buildIcon(context, channel)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.EXTRA_CHANNEL_ID, channel.id)
            putExtra(MainActivity.EXTRA_STREAM_URL, channel.streamUrl)
            putExtra(MainActivity.EXTRA_FULLSCREEN, true)
            // Required: the intent must have a non-empty action and the
            // activity must be exported + handle the intent in the manifest.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val shortcut = ShortcutInfo.Builder(context, "channel_${channel.id}")
            .setShortLabel(channel.name.take(10))          // max ~10 chars shown under icon
            .setLongLabel("${channel.name} · ${channel.country}")
            .setIcon(icon)
            .setIntent(launchIntent)
            .build()

        manager.requestPinShortcut(shortcut, null)
        return true
    }

    // ── Icon helpers ──────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun buildIcon(context: Context, channel: ChannelUiModel): Icon {
        // Try to download the channel logo and use it directly.
        val bitmap = channel.logoUrl?.let { downloadBitmap(it) }
        return if (bitmap != null) {
            Icon.createWithAdaptiveBitmap(padToAdaptive(bitmap))
        } else {
            // Fall back to the app launcher icon.
            Icon.createWithResource(context, R.mipmap.ic_launcher)
        }
    }

    /** Downloads a bitmap on IO dispatcher, returns null on any failure. */
    private suspend fun downloadBitmap(urlString: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout    = 5_000
            conn.doInput        = true
            conn.connect()
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { BitmapFactory.decodeStream(it) }
            } else null
        } catch (_: Exception) { null }
    }

    /**
     * Adaptive icons require the foreground layer to be 108×108dp where the
     * safe zone is the central 72×72dp circle. We pad the logo into a square
     * 108×108 canvas so it doesn't get clipped.
     */
    private fun padToAdaptive(src: Bitmap): Bitmap {
        val size   = 108.coerceAtLeast(src.width).coerceAtLeast(src.height)
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val left   = (size - src.width)  / 2f
        val top    = (size - src.height) / 2f
        canvas.drawBitmap(src, left, top, null)
        return result
    }
}
