package com.autotech.android.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

/** Leitura das páginas de um PDF para o visualizador do aplicativo (PdfRenderer do Android). */
class PdfPages private constructor(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer
) : Closeable {

    private val lock = Any()
    private var closed = false

    val count: Int = renderer.pageCount

    /** Altura / largura de cada página, para reservar o espaço antes de desenhar. */
    private val ratios: List<Float> = (0 until count).map { i ->
        val p = renderer.openPage(i)
        try { p.height.toFloat() / p.width } finally { p.close() }
    }

    fun ratio(index: Int): Float = ratios[index]

    suspend fun render(index: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(lock) {
            if (closed) return@synchronized null
            runCatching {
                val p = renderer.openPage(index)
                try {
                    val h = (widthPx * p.height.toFloat() / p.width).toInt().coerceAtLeast(1)
                    Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(Color.WHITE)
                        p.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                } finally {
                    p.close()
                }
            }.getOrNull()
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            runCatching { renderer.close() }
            runCatching { pfd.close() }
        }
    }

    companion object {
        fun open(file: File): PdfPages? = runCatching {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            try {
                PdfPages(pfd, PdfRenderer(pfd))
            } catch (e: Exception) {
                pfd.close(); throw e
            }
        }.getOrNull()
    }
}
