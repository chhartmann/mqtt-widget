package com.chn.mqttwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.widget.TextView

/**
 * Draws icons from the bundled Material Icons font
 * (assets/fonts/MaterialIcons-Regular.ttf, Apache 2.0 - see the LICENSE file
 * next to it).
 *
 * A widget cannot render text in a custom font: RemoteViews only knows the
 * framework widgets, so the glyph is rasterized into a tinted bitmap that is
 * handed to the ImageView like any other picture. The config screens render the
 * same glyph as text via [bind].
 */
object MaterialIconFont {

    private const val FONT_ASSET = "fonts/MaterialIcons-Regular.ttf"
    private const val CATALOG_ASSET = "fonts/material_icons.codepoints"

    private val lock = Any()

    @Volatile
    private var typeface: Typeface? = null

    @Volatile
    private var catalog: IconCatalog? = null

    fun typeface(context: Context): Typeface = typeface ?: synchronized(lock) {
        typeface ?: Typeface.createFromAsset(context.applicationContext.assets, FONT_ASSET)
            .also { typeface = it }
    }

    fun catalog(context: Context): IconCatalog = catalog ?: synchronized(lock) {
        catalog ?: context.applicationContext.assets.open(CATALOG_ASSET)
            .bufferedReader()
            .use { IconCatalog.parse(it.readText()) }
            .also { catalog = it }
    }

    /**
     * Tinted [sizePx] x [sizePx] bitmap of the icon glyph. Unknown names fall
     * back to [IconCatalog.DEFAULT_ICON].
     */
    fun bitmap(context: Context, name: String, color: Int, sizePx: Int): Bitmap {
        val glyph = catalog(context).glyphFor(name)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        if (glyph.isEmpty()) return bitmap

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = typeface(context)
            this.color = color
        }

        // Scale by the measured glyph bounds so square icons (home) and wide
        // ones (linear_scale) all end up at the same optical size.
        val bounds = Rect()
        paint.textSize = sizePx.toFloat()
        paint.getTextBounds(glyph, 0, glyph.length, bounds)
        val longest = maxOf(bounds.width(), bounds.height()).coerceAtLeast(1)
        paint.textSize = sizePx * sizePx * 0.92f / longest
        paint.getTextBounds(glyph, 0, glyph.length, bounds)

        Canvas(bitmap).drawText(
            glyph,
            (sizePx - bounds.width()) / 2f - bounds.left,
            (sizePx - bounds.height()) / 2f - bounds.top,
            paint,
        )
        return bitmap
    }

    /** Renders the icon glyph as text in [view] (cheap enough for lists). */
    fun bind(view: TextView, name: String) {
        view.typeface = typeface(view.context)
        view.text = catalog(view.context).glyphFor(name)
    }
}
