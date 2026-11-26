package com.ardalorn.arcanvas

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * Holds the drawing and every tool setting. It lives in a ViewModel so a rotation or any other
 * configuration change keeps both the picture and Focus mode exactly as they were.
 */
class CanvasViewModel(app: Application) : AndroidViewModel(app) {
    enum class Tool { BRUSH, ERASER, STAMP }

    var sizeIndex by mutableIntStateOf(DEFAULT_SIZE_INDEX)
        private set
    var colorIndex by mutableIntStateOf(0)
        private set
    var stampIndex by mutableIntStateOf(0)
        private set
    var tool by mutableStateOf(Tool.BRUSH)
        private set

    var focusMode by mutableStateOf(false)
        private set
    var islandVisible by mutableStateOf(false)
        private set
    var sessionPulse by mutableStateOf(false)
        private set

    /** The drawing surface. Transparent where nothing has been drawn; the paper shows through. */
    var bitmap by mutableStateOf<Bitmap?>(null)
        private set

    /** Bumped after every change to [bitmap]'s pixels so the screen redraws. */
    var version by mutableIntStateOf(0)
        private set

    /** Plain (non-snapshot) timestamp, so touches do not trigger recomposition. */
    var lastPing = 0L
        private set

    private var surface: Canvas? = null
    private var density = 1f

    private val strokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val outlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 4f
            color = android.graphics.Color.argb(89, 61, 64, 74) // rgba(61,64,74,0.35)
        }
    private val detailFill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = DETAIL_COLOR
        }
    private val detailStroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 2.5f
            color = DETAIL_COLOR
        }
    private val clearMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    init {
        viewModelScope.launch {
            delay(SESSION_REMINDER_MS)
            sessionPulse = true
        }
    }

    // ───────────── Surface ─────────────

    /** Called with the drawing area's size in pixels. Resizing stretches the old picture, as the web version did. */
    fun ensureSize(
        width: Int,
        height: Int,
        density: Float,
    ) {
        if (width <= 0 || height <= 0) return
        this.density = density
        val old = bitmap
        if (old != null && old.width == width && old.height == height) return

        val fresh = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val freshCanvas = Canvas(fresh)
        if (old != null) {
            freshCanvas.drawBitmap(
                old,
                null,
                Rect(0, 0, width, height),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
        }
        surface = freshCanvas
        bitmap = fresh
        version++
    }

    // ───────────── Tool state ─────────────

    fun cycleSize() {
        sizeIndex = (sizeIndex + 1) % BrushSizes.size
    }

    fun cycleColor() {
        colorIndex = (colorIndex + 1) % Swatches.size
        if (tool == Tool.ERASER) tool = Tool.BRUSH
    }

    fun toggleEraser() {
        tool = if (tool == Tool.ERASER) Tool.BRUSH else Tool.ERASER
    }

    /** First tap selects the stamp tool; further taps cycle the shape. */
    fun tapStamp() {
        if (tool != Tool.STAMP) {
            tool = Tool.STAMP
        } else {
            stampIndex = (stampIndex + 1) % Stamps.all.size
        }
    }

    // ───────────── Focus mode ─────────────

    fun toggleFocus() {
        focusMode = !focusMode
        if (focusMode) ping() else islandVisible = false
    }

    /** Any touch, or a swallowed Back press, wakes the Focus-mode island. */
    fun ping() {
        lastPing = SystemClock.uptimeMillis()
        if (focusMode && !islandVisible) islandVisible = true
    }

    fun hideIsland() {
        islandVisible = false
    }

    // ───────────── Drawing ─────────────

    fun onDown(
        x: Float,
        y: Float,
    ) {
        val c = surface ?: return
        ping()
        if (tool == Tool.STAMP) {
            placeStamp(c, x, y)
        } else {
            configureStroke()
            c.drawPoint(x, y, strokePaint)
        }
        version++
    }

    fun onMove(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
    ) {
        if (tool == Tool.STAMP) return
        val c = surface ?: return
        ping()
        configureStroke()
        c.drawLine(fromX, fromY, toX, toY, strokePaint)
        version++
    }

    fun clearAll() {
        bitmap?.eraseColor(android.graphics.Color.TRANSPARENT)
        version++
    }

    private fun configureStroke() {
        strokePaint.strokeWidth = BrushSizes[sizeIndex].strokeDp * density
        if (tool == Tool.ERASER) {
            strokePaint.xfermode = clearMode
            strokePaint.color = android.graphics.Color.BLACK
        } else {
            strokePaint.xfermode = null
            strokePaint.color = Swatches[colorIndex].color.toArgb()
        }
    }

    private fun placeStamp(
        c: Canvas,
        x: Float,
        y: Float,
    ) {
        val def = Stamps.all[stampIndex]
        val scale = BrushSizes[sizeIndex].stampDp * density / 100f
        val jitterDegrees = Math.toDegrees(((Random.nextFloat() - 0.5f) * 0.5f).toDouble()).toFloat()

        c.save()
        c.translate(x, y)
        c.rotate(jitterDegrees)
        c.scale(scale, scale)
        c.translate(-50f, -50f)
        fillPaint.color = Swatches[colorIndex].color.toArgb()
        c.drawPath(def.path, fillPaint)
        c.drawPath(def.path, outlinePaint)
        drawStampDetail(c, def.name)
        c.restore()
    }

    private fun drawStampDetail(
        c: Canvas,
        name: String,
    ) {
        when (name) {
            "happy face" -> {
                oval(c, 36f, 42f, 5f, 6f, 0f)
                oval(c, 63f, 39f, 4.5f, 5.5f, 0f)
                val smile =
                    Path().apply {
                        moveTo(34f, 58f)
                        quadTo(50f, 74f, 66f, 54f)
                    }
                c.drawPath(smile, detailStroke)
            }

            "cat" -> {
                oval(c, 37f, 50f, 4f, 5.5f, -0.15f)
                oval(c, 62f, 49f, 4f, 5.5f, 0.15f)
                val nose =
                    Path().apply {
                        moveTo(50f, 60f)
                        lineTo(46f, 66f)
                        lineTo(54f, 66f)
                        close()
                    }
                c.drawPath(nose, detailFill)
                c.drawLine(20f, 58f, 2f, 54f, detailStroke)
                c.drawLine(18f, 66f, 0f, 68f, detailStroke)
                c.drawLine(80f, 58f, 98f, 54f, detailStroke)
                c.drawLine(82f, 66f, 100f, 68f, detailStroke)
            }
        }
    }

    private fun oval(
        c: Canvas,
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
        rotationRadians: Float,
    ) {
        c.save()
        c.translate(cx, cy)
        c.rotate(Math.toDegrees(rotationRadians.toDouble()).toFloat())
        c.drawOval(-rx, -ry, rx, ry, detailFill)
        c.restore()
    }

    // ───────────── Saving ─────────────

    /** Flattens the drawing onto the paper colour and saves a PNG to Pictures/ArCanvas. */
    fun saveDrawing() {
        val source = bitmap ?: return
        val flat = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply {
            drawColor(Tokens.Paper.toArgb())
            drawBitmap(source, 0f, 0f, null)
        }
        val app = getApplication<Application>()
        val name =
            "calm-canvas-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")) + ".png"

        viewModelScope.launch(Dispatchers.IO) {
            val saved = writePng(app, flat, name)
            withContext(Dispatchers.Main) {
                val message = if (saved) "Saved to Pictures/ArCanvas" else "Could not save the drawing"
                Toast.makeText(app, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writePng(
        app: Application,
        image: Bitmap,
        name: String,
    ): Boolean {
        val resolver = app.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ArCanvas")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            val written =
                resolver.openOutputStream(uri)?.use { stream ->
                    image.compress(Bitmap.CompressFormat.PNG, 100, stream)
                } ?: false
            if (!written) {
                resolver.delete(uri, null, null)
                false
            } else {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            }
        } catch (e: java.io.IOException) {
            resolver.delete(uri, null, null)
            false
        }
    }

    private companion object {
        /** rgba(61,64,74,0.85) */
        val DETAIL_COLOR = android.graphics.Color.argb(217, 61, 64, 74)
    }
}
