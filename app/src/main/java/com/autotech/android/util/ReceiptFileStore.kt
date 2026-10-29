package com.autotech.android.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Arquivo escolhido pelo usuário, copiado para a área de espera até o registro ser salvo. */
data class PendingReceipt(
    val tempPath: String,
    val originalName: String,
    val extension: String
)

/**
 * Comprovantes (RF-04 / RF-05): aceita QUALQUER tipo de arquivo (foto, PDF, XML da nota
 * fiscal...). Os arquivos ficam na área interna do app, em files/comprovantes, com nome
 * padronizado: Tipo_Veiculo-Placa_Item_AAAA-MM-DD.ext
 * Ex.: Manutencao_Gol-ABC1D23_Oleo-filtro_2026-10-21.pdf
 */
object ReceiptFileStore {

    private const val DIR = "comprovantes"
    private const val PENDING = ".pendentes"

    fun dir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    private fun pendingDir(context: Context): File = File(dir(context), PENDING).apply { mkdirs() }

    /** Copia o arquivo escolhido para a área de espera, mantendo a extensão original. */
    suspend fun stage(context: Context, uri: Uri): PendingReceipt? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: "arquivo"
            val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT).takeIf { it.isNotBlank() && it.length <= 8 }
                ?: resolver.getType(uri)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                ?: "bin"
            val out = File(pendingDir(context), "${UUID.randomUUID()}.$ext")
            resolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
                ?: return@runCatching null
            PendingReceipt(out.absolutePath, name, ext)
        }.getOrNull()
    }

    /** "Óleo / filtro" -> "Oleo-filtro" */
    private fun clean(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').ifBlank { "x" }

    /** Nome padronizado, sem extensão. */
    fun baseName(kind: String, vehicleModel: String?, plate: String?, item: String?, dateMillis: Long): String {
        val vehicle = listOfNotNull(vehicleModel, plate).joinToString("-").ifBlank { "Veiculo" }
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(dateMillis))
        return listOfNotNull(clean(kind), clean(vehicle), item?.let { clean(it) }, date).joinToString("_")
    }

    /** Move o arquivo da área de espera para a pasta de comprovantes com o nome padronizado. */
    fun finalize(context: Context, pending: PendingReceipt, baseName: String): String? {
        val src = File(pending.tempPath)
        if (!src.exists()) return null
        var target = File(dir(context), "$baseName.${pending.extension}")
        var n = 2
        while (target.exists()) {
            target = File(dir(context), "${baseName}_$n.${pending.extension}")
            n++
        }
        return if (src.renameTo(target)) target.absolutePath else null
    }

    /** Desfaz o finalize (usado se o registro não puder ser salvo). */
    fun revert(finalPath: String, pending: PendingReceipt) {
        runCatching { File(finalPath).renameTo(File(pending.tempPath)) }
    }

    fun discard(pending: PendingReceipt?) {
        pending?.let { runCatching { File(it.tempPath).delete() } }
    }

    /** Apaga arquivos esquecidos na área de espera há mais de 1 dia. */
    fun cleanupPending(context: Context) {
        val limit = System.currentTimeMillis() - 86_400_000L
        pendingDir(context).listFiles()?.filter { it.lastModified() < limit }?.forEach { it.delete() }
    }

    /** Abre o comprovante no app adequado do aparelho (visualizador de PDF, imagens, etc.). */
    fun open(context: Context, path: String) {
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "Arquivo do comprovante não encontrado.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase(Locale.ROOT)) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(Intent.createChooser(intent, "Abrir comprovante").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum aplicativo para abrir este tipo de arquivo.", Toast.LENGTH_SHORT).show()
        }
    }
}
