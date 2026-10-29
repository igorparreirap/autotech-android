package com.autotech.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Foto opcional do veículo, guardada na memória interna do app
 * (files/vehicle_photos/vehicle_<id>.jpg). Não altera o banco de dados.
 * A foto é reduzida para no máximo ~1600 px no maior lado antes de salvar.
 */
object VehiclePhotoStore {

    private const val MAX_SIDE = 1600

    private val _version = MutableStateFlow(0)

    /** Muda sempre que uma foto é salva ou removida, para as telas recarregarem. */
    val version: StateFlow<Int> = _version

    fun file(context: Context, vehicleId: Long): File =
        File(File(context.filesDir, "vehicle_photos"), "vehicle_$vehicleId.jpg")

    suspend fun save(context: Context, vehicleId: Long, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decode(context, uri) ?: return@runCatching false
            val target = file(context, vehicleId)
            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, "${target.name}.tmp")
            tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            if (target.exists()) target.delete()
            val ok = tmp.renameTo(target)
            if (ok) _version.update { it + 1 }
            ok
        }.getOrDefault(false)
    }

    suspend fun delete(context: Context, vehicleId: Long) = withContext(Dispatchers.IO) {
        if (file(context, vehicleId).delete()) _version.update { it + 1 }
    }

    suspend fun load(context: Context, vehicleId: Long): ImageBitmap? = withContext(Dispatchers.IO) {
        val f = file(context, vehicleId)
        if (!f.exists()) return@withContext null
        runCatching { BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() }.getOrNull()
    }

    private fun sampleFor(width: Int, height: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= MAX_SIDE) sample *= 2
        return sample
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder já aplica a rotação EXIF das fotos da câmera.
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.setTargetSampleSize(sampleFor(info.size.width, info.size.height))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight) }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }
    }
}
