package com.ahmedmunna.ipcall.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ahmedmunna.ipcall.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.math.max

class Preset(
    val name: String,
    val base: List<Color>,
    val blobs: List<Color>,
    /** true = dark background (white text), false = light background (dark text) */
    val dark: Boolean
)

val Presets = listOf(
    Preset(
        "Frost",
        listOf(Color(0xFFA8BBD0), Color(0xFFC8DCE6), Color(0xFFD8D2EC)),
        listOf(Color(0xFFFFFFFF), Color(0xFF9EC5E8), Color(0xFFD3B8F0)),
        false
    ),
    Preset(
        "Ocean",
        listOf(Color(0xFF7CC4F5), Color(0xFFA5E3F0), Color(0xFFC7F0E8)),
        listOf(Color(0xFFFFFFFF), Color(0xFF4FACFE), Color(0xFF7FE3D0)),
        false
    ),
    Preset(
        "Rose",
        listOf(Color(0xFFF8C8E0), Color(0xFFC8D4F5), Color(0xFFE7D2F7)),
        listOf(Color(0xFFFFFFFF), Color(0xFFFF9ACB), Color(0xFF9AB5FF)),
        false
    ),
    Preset(
        "Aurora",
        listOf(Color(0xFF0B1F2A), Color(0xFF12404B), Color(0xFF1B3A5C)),
        listOf(Color(0xFF00D4AA), Color(0xFF4F7CFF), Color(0xFF8A5CFF)),
        true
    ),
    Preset(
        "Sunset",
        listOf(Color(0xFFFF8A5C), Color(0xFFE5527A), Color(0xFF6A4CC9)),
        listOf(Color(0xFFFFD27A), Color(0xFFFF6B9A), Color(0xFF7B8CFF)),
        true
    ),
    Preset(
        "Midnight",
        listOf(Color(0xFF090C20), Color(0xFF1A1B4B), Color(0xFF3A1F6E)),
        listOf(Color(0xFF5B6CFF), Color(0xFFB44CFF), Color(0xFF00B7FF)),
        true
    ),
    Preset(
        "Forest",
        listOf(Color(0xFF0F3D3E), Color(0xFF1E6B52), Color(0xFF3E8E6B)),
        listOf(Color(0xFFA8E6A3), Color(0xFF2EC4B6), Color(0xFFF4F1BB)),
        true
    ),
    Preset(
        "Graphite",
        listOf(Color(0xFF1B1D22), Color(0xFF2C2F36), Color(0xFF40444D)),
        listOf(Color(0xFF7C8AA5), Color(0xFF4B5563), Color(0xFF9CA3AF)),
        true
    )
)

val LocalThemeState = staticCompositionLocalOf<ThemeState> { error("ThemeState not provided") }

/** Holds the user-changeable look: background (preset or own photo) and glass settings. */
class ThemeState {
    var preset by mutableIntStateOf(Prefs.getInt("bg_preset", 0))
        private set
    var useImage by mutableStateOf(Prefs.getBool("bg_use_image", false))
        private set
    var imageBitmap by mutableStateOf<ImageBitmap?>(null)
        private set
    private var imageLum by mutableFloatStateOf(0.5f)

    var dim by mutableFloatStateOf(Prefs.getFloat("bg_dim", 0.15f))
        private set
    var imageBlur by mutableFloatStateOf(Prefs.getFloat("bg_blur", 0f))
        private set
    var glassBlur by mutableFloatStateOf(Prefs.getFloat("glass_blur", 0.55f))
        private set
    var glassOpacity by mutableFloatStateOf(Prefs.getFloat("glass_opacity", 0.5f))
        private set
    var animated by mutableStateOf(Prefs.getBool("bg_anim", true))
        private set

    val isDark: Boolean
        get() = if (useImage && imageBitmap != null) imageLum * (1f - dim) < 0.5f
        else Presets[preset.coerceIn(0, Presets.lastIndex)].dark

    fun selectPreset(i: Int) {
        preset = i
        useImage = false
        Prefs.putInt("bg_preset", i)
        Prefs.putBool("bg_use_image", false)
    }

    fun selectImage() {
        if (imageBitmap != null) {
            useImage = true
            Prefs.putBool("bg_use_image", true)
        }
    }

    fun updateDim(v: Float) { dim = v; Prefs.putFloat("bg_dim", v) }
    fun updateImageBlur(v: Float) { imageBlur = v; Prefs.putFloat("bg_blur", v) }
    fun updateGlassBlur(v: Float) { glassBlur = v; Prefs.putFloat("glass_blur", v) }
    fun updateGlassOpacity(v: Float) { glassOpacity = v; Prefs.putFloat("glass_opacity", v) }
    fun updateAnimated(v: Boolean) { animated = v; Prefs.putBool("bg_anim", v) }

    private fun file(ctx: Context) = File(ctx.filesDir, "background.jpg")

    /** Copies the picked photo into app storage (downscaled) and uses it as background. */
    suspend fun importImage(ctx: Context, uri: Uri) {
        val result = withContext(Dispatchers.IO) {
            try {
                val bmp = decode({ ctx.contentResolver.openInputStream(uri) }, 1600) ?: return@withContext null
                file(ctx).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                bmp to luminance(bmp)
            } catch (_: Throwable) {
                null
            }
        } ?: return
        imageBitmap = result.first.asImageBitmap()
        imageLum = result.second
        useImage = true
        Prefs.putBool("bg_use_image", true)
    }

    suspend fun loadSaved(ctx: Context) {
        val f = file(ctx)
        if (!f.exists()) {
            if (useImage) { useImage = false }
            return
        }
        val result = withContext(Dispatchers.IO) {
            try {
                val bmp = decode({ f.inputStream() }, 1600) ?: return@withContext null
                bmp to luminance(bmp)
            } catch (_: Throwable) {
                null
            }
        } ?: return
        imageBitmap = result.first.asImageBitmap()
        imageLum = result.second
    }

    private fun luminance(b: Bitmap): Float {
        val s = Bitmap.createScaledBitmap(b, 16, 16, true)
        var sum = 0.0
        for (y in 0 until 16) for (x in 0 until 16) {
            val p = s.getPixel(x, y)
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val bl = p and 0xFF
            sum += (0.299 * r + 0.587 * g + 0.114 * bl) / 255.0
        }
        return (sum / 256.0).toFloat()
    }

    private fun decode(open: () -> InputStream?, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
        var bmp = open()?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val rotation = try {
            open()?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Throwable) {
            0f
        }
        val longest = max(bmp.width, bmp.height)
        val scale = if (longest > maxSide) maxSide.toFloat() / longest else 1f
        if (rotation != 0f || scale != 1f) {
            val m = Matrix().apply {
                postScale(scale, scale)
                postRotate(rotation)
            }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        return bmp
    }
}
