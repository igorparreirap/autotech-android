package com.autotech.android.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.core.content.FileProvider
import com.autotech.android.data.local.entity.FuelRecordEntity
import com.autotech.android.data.local.entity.MaintenanceRecordEntity
import com.autotech.android.data.local.entity.VehicleEntity
import com.autotech.android.data.mapper.toFillUp
import com.autotech.android.domain.FuelConsumption
import com.autotech.android.domain.FuelMix
import com.autotech.android.domain.FuelType
import com.autotech.android.domain.MaintenanceCatalog
import com.autotech.android.domain.VehicleNaming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * RF-10: relatório do veículo em PDF com a identidade visual do aplicativo
 * (fundo cinza-azulado, cartões brancos, faixa azul com logo e nome do AutoTech).
 * Ordem: dados do veículo e odômetro, manutenções, abastecimentos (opcional) e
 * comprovantes anexados (opcional).
 */
object VehicleReportPdf {

    data class Options(val includeFuel: Boolean, val includeReceipts: Boolean)

    private const val PREFIX = "Relatorio_AutoTech_"

    /** Muda quando o desenho do PDF mudar, para que relatórios antigos sejam refeitos. */
    private const val LAYOUT_VERSION = 1

    private fun prefs(context: Context) = context.getSharedPreferences("autotech_reports", Context.MODE_PRIVATE)

    /** Relatório atual do veículo (só existe um por veículo). */
    fun currentReport(context: Context, vehicleId: Long): File? =
        prefs(context).getString("file_$vehicleId", null)?.let(::File)?.takeIf { it.exists() }

    /** Últimas opções escolhidas para o veículo (abastecimentos marcado e comprovantes desmarcado na primeira vez). */
    fun lastOptions(context: Context, vehicleId: Long): Options =
        Options(prefs(context).getBoolean("fuel_$vehicleId", true), prefs(context).getBoolean("receipts_$vehicleId", false))

    /**
     * "Impressão digital" de tudo o que entra no PDF: veículo, registros, opções, foto e comprovantes.
     * Se nada disso mudou, o relatório salvo continua valendo.
     */
    fun signature(
        context: Context,
        vehicle: VehicleEntity,
        maintenance: List<MaintenanceRecordEntity>,
        fuel: List<FuelRecordEntity>,
        options: Options
    ): String {
        val sb = StringBuilder("v$LAYOUT_VERSION|$options|$vehicle|")
        maintenance.forEach { sb.append(it).append('|') }
        if (options.includeFuel) fuel.forEach { sb.append(it).append('|') }
        VehiclePhotoStore.file(context, vehicle.id).takeIf { it.exists() }?.let { sb.append("foto:${it.length()}:${it.lastModified()}|") }
        if (options.includeReceipts) {
            val paths = maintenance.mapNotNull { it.receiptPath } + if (options.includeFuel) fuel.mapNotNull { it.receiptPath } else emptyList()
            paths.forEach { p -> val f = File(p); sb.append("r:$p:${f.length()}:${f.lastModified()}|") }
        }
        val digest = java.security.MessageDigest.getInstance("SHA-1").digest(sb.toString().toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun isUpToDate(context: Context, vehicleId: Long, signature: String): Boolean =
        currentReport(context, vehicleId) != null && prefs(context).getString("sig_$vehicleId", null) == signature

    /**
     * RF-10: devolve o relatório salvo se nada mudou; senão gera um novo e apaga o anterior.
     */
    suspend fun ensure(
        context: Context,
        vehicle: VehicleEntity,
        maintenance: List<MaintenanceRecordEntity>,
        fuel: List<FuelRecordEntity>,
        options: Options
    ): Result<File> {
        val sig = signature(context, vehicle, maintenance, fuel, options)
        if (isUpToDate(context, vehicle.id, sig)) return Result.success(currentReport(context, vehicle.id)!!)
        return export(context, vehicle, maintenance, fuel, options).onSuccess { file ->
            withContext(Dispatchers.IO) {
                listReports(context, vehicle.plate).filter { it != file }.forEach { it.delete() }
                currentReport(context, vehicle.id)?.takeIf { it != file }?.delete()
            }
            prefs(context).edit()
                .putString("file_${vehicle.id}", file.absolutePath)
                .putString("sig_${vehicle.id}", sig)
                .putBoolean("fuel_${vehicle.id}", options.includeFuel)
                .putBoolean("receipts_${vehicle.id}", options.includeReceipts)
                .apply()
        }
    }

    /** Apaga o relatório do veículo (usado quando o veículo é excluído com todo o histórico). */
    fun deleteReports(context: Context, vehicleId: Long, plate: String) {
        listReports(context, plate).forEach { it.delete() }
        currentReport(context, vehicleId)?.delete()
        prefs(context).edit().remove("file_$vehicleId").remove("sig_$vehicleId").apply()
    }

    fun reportsDir(context: Context): File =
        (context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: File(context.filesDir, "relatorios"))
            .apply { mkdirs() }

    /** Relatórios já gerados para a placa, do mais novo para o mais antigo. */
    fun listReports(context: Context, plate: String): List<File> =
        reportsDir(context).listFiles { f -> f.name.startsWith("$PREFIX${plate}_") && f.extension == "pdf" }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

    suspend fun export(
        context: Context,
        vehicle: VehicleEntity,
        maintenance: List<MaintenanceRecordEntity>,
        fuel: List<FuelRecordEntity>,
        options: Options
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val now = Date()
            val name = PREFIX + vehicle.plate + "_" + SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.ROOT).format(now) + ".pdf"
            val out = File(reportsDir(context), name)
            val doc = ReportBuilder(context, vehicle, now, options).build(maintenance, if (options.includeFuel) fuel else emptyList())
            try {
                FileOutputStream(out).use { doc.writeTo(it) }
            } finally {
                doc.close()
            }
            out
        }
    }

    /** Envia o PDF para o app que o usuário escolher (WhatsApp, e-mail, Drive...). */
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "Relatório do veículo — AutoTech")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(file.name, uri)
        context.startActivity(Intent.createChooser(send, "Compartilhar relatório").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Abre o PDF em outro leitor instalado no aparelho. */
    fun openWith(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching {
            context.startActivity(Intent.createChooser(view, "Abrir relatório com").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            Toast.makeText(context, "Nenhum leitor de PDF encontrado.", Toast.LENGTH_SHORT).show()
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────────

private class ReportBuilder(
    private val context: Context,
    private val vehicle: VehicleEntity,
    private val generatedAt: Date,
    private val options: VehicleReportPdf.Options
) {
    // Mesmas cores do tema do aplicativo (AtColors)
    private val primary = 0xFF1569CC.toInt()
    private val deep = 0xFF0E2A57.toInt()
    private val accentSoft = 0xFFE6F1FD.toInt()
    private val ink = 0xFF0F1B2D.toInt()
    private val muted = 0xFF8A94A6.toInt()
    private val background = 0xFFEEF2F8.toInt()
    private val field = 0xFFF3F6FB.toInt()
    private val line = 0xFFDCE3EE.toInt()
    private val zebra = 0xFFF8FAFD.toInt()
    private val white = Color.WHITE
    private val whiteSoft = 0xD9FFFFFF.toInt()

    private val pageW = 595
    private val pageH = 842
    private val m = 32f
    private val right = pageW - m
    private val cw = pageW - 2 * m
    private val bottom = pageH - 46f

    private val ptBr = Locale("pt", "BR")
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", ptBr)
    private val stampFmt = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", ptBr)
    private val money = NumberFormat.getCurrencyInstance(ptBr)
    private val number = NumberFormat.getIntegerInstance(ptBr)
    private val decimal = NumberFormat.getNumberInstance(ptBr).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }

    private val vehicleName = VehicleNaming.display(vehicle.nickname, vehicle.brand, vehicle.model)

    private val doc = PdfDocument()
    private lateinit var page: PdfDocument.Page
    private lateinit var c: Canvas
    private var y = m
    private var pageNumber = 0
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val logo: Bitmap? = runCatching {
        context.assets.open("branding/logo.png").use { BitmapFactory.decodeStream(it) }
    }.getOrNull()

    private data class Attachment(
        val number: Int,
        val kind: String,
        val title: String,
        val dateMillis: Long,
        val km: Long,
        val path: String
    )

    fun build(maintenance: List<MaintenanceRecordEntity>, fuel: List<FuelRecordEntity>): PdfDocument {
        // Anexos numerados na ordem em que aparecem no relatório
        val attachments = mutableListOf<Attachment>()
        val maintenanceAttach = mutableMapOf<Long, Int>()
        val fuelAttach = mutableMapOf<Long, Int>()
        if (options.includeReceipts) {
            maintenance.filter { !it.receiptPath.isNullOrBlank() }.forEach { r ->
                val n = attachments.size + 1
                maintenanceAttach[r.id] = n
                attachments += Attachment(n, "Manutenção", MaintenanceCatalog.label(r.itemKey), r.eventDateMillis, r.eventKm, r.receiptPath!!)
            }
            fuel.filter { !it.receiptPath.isNullOrBlank() }.forEach { f ->
                val n = attachments.size + 1
                fuelAttach[f.id] = n
                attachments += Attachment(n, "Abastecimento", "${decimal.format(f.liters)} L", f.dateMillis, f.eventKm, f.receiptPath!!)
            }
        }

        newPage(first = true)
        titleBand()
        vehicleCard(maintenance.size)
        summary(maintenance, fuel)

        // ── Manutenções
        section("Manutenções", "${maintenance.size} ${if (maintenance.size == 1) "registro" else "registros"}")
        if (maintenance.isEmpty()) notice("Nenhuma manutenção registrada para este veículo.")
        maintenance.forEach { maintenanceRow(it, maintenanceAttach[it.id]) }

        // ── Abastecimentos (opcional)
        if (options.includeFuel) {
            y += 6f
            section("Abastecimentos", "${fuel.size} ${if (fuel.size == 1) "registro" else "registros"}")
            if (fuel.isEmpty()) notice("Nenhum abastecimento registrado para este veículo.")
            else fuelTable(fuel, fuelAttach)
        }

        // ── Comprovantes (opcional)
        if (options.includeReceipts) {
            if (attachments.isEmpty()) {
                y += 6f
                section("Comprovantes", "nenhum anexo")
                notice("Nenhum comprovante foi anexado aos registros incluídos neste relatório.")
            } else {
                attachments.forEachIndexed { i, a -> attachmentPages(a, first = i == 0, total = attachments.size) }
            }
        }

        doc.finishPage(page)
        logo?.recycle()
        return doc
    }

    // ─── Páginas ─────────────────────────────────────────────────────────────

    private fun newPage(first: Boolean = false) {
        if (pageNumber > 0) doc.finishPage(page)
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create())
        c = page.canvas
        c.drawColor(background)
        footer()
        y = m
        if (!first) miniHeader()
    }

    private fun ensure(h: Float) {
        if (y + h > bottom) newPage()
    }

    private fun footer() {
        fill.shader = null
        fill.color = line
        c.drawRect(m, pageH - 36f, right, pageH - 35.4f, fill)
        c.drawText(
            "AutoTech · Relatório do veículo ${vehicle.plate} · gerado em ${stampFmt.format(generatedAt)}",
            m, pageH - 22f, text(7.5f, muted)
        )
        c.drawText("Página $pageNumber", right, pageH - 22f, text(7.5f, muted, align = Paint.Align.RIGHT))
    }

    /** Cabeçalho reduzido das páginas seguintes: a identidade aparece em todas. */
    private fun miniHeader() {
        drawLogo(m, y, 18f)
        c.drawText("AutoTech", m + 24f, y + 13f, text(11f, primary, bold = true))
        c.drawText(
            ellipsize("$vehicleName · ${vehicle.plate}", text(8.5f, muted), cw - 120f),
            right, y + 13f, text(8.5f, muted, align = Paint.Align.RIGHT)
        )
        y += 30f
    }

    private fun titleBand() {
        val t = y
        val h = 92f
        fill.shader = LinearGradient(m, t, right, t + h, primary, deep, Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(m, t, right, t + h), 18f, 18f, fill)
        fill.shader = null
        fill.color = white
        c.drawCircle(m + 46f, t + h / 2, 28f, fill)
        drawLogo(m + 26f, t + h / 2 - 20f, 40f)
        c.drawText("AutoTech", m + 86f, t + 42f, text(22f, white, bold = true))
        c.drawText("Relatório do veículo", m + 86f, t + 60f, text(10.5f, whiteSoft))
        val tags = buildList {
            add("// manutenção")
            if (options.includeFuel) add("// combustível")
            if (options.includeReceipts) add("// comprovantes")
        }
        val mono = text(8f, whiteSoft, align = Paint.Align.RIGHT).apply { typeface = Typeface.MONOSPACE }
        tags.forEachIndexed { i, s -> c.drawText(s, right - 16f, t + 22f + i * 11f, mono) }
        c.drawText("Gerado em ${stampFmt.format(generatedAt)}", right - 16f, t + h - 14f, text(8.5f, white, align = Paint.Align.RIGHT))
        y += h + 12f
    }

    private fun vehicleCard(maintenanceCount: Int) {
        val t = y
        val pad = 16f
        val photoW = 200f
        val photoH = 140f
        val statsH = 46f
        val h = pad + photoH + 12f + statsH + pad
        card(t, h, 18f)

        // Foto do veículo (RF-12) ou a logo como ilustração padrão
        val pr = RectF(m + pad, t + pad, m + pad + photoW, t + pad + photoH)
        val photo = decodeImage(VehiclePhotoStore.file(context, vehicle.id), 640)
        if (photo != null) {
            drawCropped(photo, pr, 14f)
            photo.recycle()
        } else {
            round(pr, field, 14f)
            drawLogo(pr.centerX() - 28f, pr.centerY() - 36f, 56f, alpha = 110)
            c.drawText("Sem foto cadastrada", pr.centerX(), pr.centerY() + 38f, text(8f, muted, align = Paint.Align.CENTER))
        }

        // Coluna da direita: nome, placa e odômetro
        val x = pr.right + 16f
        val colW = right - pad - x
        var ty = t + pad + 16f
        wrap(vehicleName, text(17f, ink, bold = true), colW, 2).forEach {
            c.drawText(it, x, ty, text(17f, ink, bold = true)); ty += 20f
        }
        val subs = listOfNotNull(
            vehicle.nickname?.let { "Nome original: ${VehicleNaming.original(vehicle.brand, vehicle.model)}" },
            vehicle.version
        )
        subs.forEach {
            c.drawText(ellipsize(it, text(9f, muted), colW), x, ty, text(9f, muted)); ty += 13f
        }
        // Base da coluna, alinhada ao pé da foto: placa e odômetro lado a lado
        // (o texto acima vai no máximo até a linha 98, e a base começa na 112).
        val oTop = t + pad + photoH - 44f
        plate(x, oTop + 4f, vehicle.plate)
        val ox = x + 130f
        round(RectF(ox, oTop, x + colW, oTop + 44f), primary, 12f)
        c.drawText("ODÔMETRO", ox + 12f, oTop + 15f, text(7f, whiteSoft, bold = true))
        c.drawText(
            ellipsize("${number.format(vehicle.currentOdometerKm)} km", text(16f, white, bold = true), x + colW - ox - 24f),
            ox + 12f, oTop + 35f, text(16f, white, bold = true)
        )

        // Linha de dados
        val sy = t + pad + photoH + 12f
        round(RectF(m + pad, sy, right - pad, sy + statsH), field, 12f)
        val stats = listOf(
            "Ano" to listOfNotNull(vehicle.year, vehicle.modelYear).distinct().joinToString("/").ifEmpty { "—" },
            "Cor" to (vehicle.color ?: "—"),
            "Combustível" to (FuelType.from(vehicle.fuel)?.label ?: vehicle.fuel ?: "Não informado"),
            "Manutenções" to number.format(maintenanceCount.toLong())
        )
        val colStat = (cw - 2 * pad) / stats.size
        stats.forEachIndexed { i, (label, value) ->
            val sx = m + pad + 14f + i * colStat
            c.drawText(label.uppercase(ptBr), sx, sy + 18f, text(7f, muted, bold = true))
            c.drawText(ellipsize(value, text(10.5f, ink, bold = true), colStat - 20f), sx, sy + 35f, text(10.5f, ink, bold = true))
        }
        y = t + h + 12f
    }

    private fun summary(maintenance: List<MaintenanceRecordEntity>, fuel: List<FuelRecordEntity>) {
        val maintTotal = maintenance.sumOf { it.amount ?: 0.0 }
        val items = if (options.includeFuel) {
            val fuelTotal = fuel.sumOf { it.amount ?: 0.0 }
            listOf(
                Triple("Gasto com manutenção", money.format(maintTotal), "${maintenance.size} serviços"),
                Triple("Gasto com combustível", money.format(fuelTotal), "${decimal.format(fuel.sumOf { it.liters })} litros"),
                Triple("Total gasto", money.format(maintTotal + fuelTotal), "manutenção + combustível")
            )
        } else {
            listOf(
                Triple("Gasto com manutenção", money.format(maintTotal), "total registrado"),
                Triple("Serviços registrados", number.format(maintenance.size.toLong()), "no histórico"),
                Triple("Último serviço", maintenance.maxOfOrNull { it.eventDateMillis }?.let { dateFmt.format(Date(it)) } ?: "—",
                    maintenance.maxByOrNull { it.eventDateMillis }?.let { "${number.format(it.eventKm)} km" } ?: "")
            )
        }
        val gap = 10f
        val w = (cw - gap * (items.size - 1)) / items.size
        val h = 62f
        items.forEachIndexed { i, (label, value, note) ->
            val x = m + i * (w + gap)
            round(RectF(x, y, x + w, y + h), white, 14f)
            c.drawText(label.uppercase(ptBr), x + 12f, y + 18f, text(7f, muted, bold = true))
            c.drawText(ellipsize(value, text(14f, ink, bold = true), w - 24f), x + 12f, y + 39f, text(14f, ink, bold = true))
            c.drawText(ellipsize(note, text(8f, muted), w - 24f), x + 12f, y + 53f, text(8f, muted))
        }
        y += h + 16f
    }

    private fun section(title: String, info: String?) {
        ensure(80f)
        round(RectF(m, y + 2f, m + 4f, y + 18f), primary, 2f)
        c.drawText(title, m + 12f, y + 15f, text(13f, ink, bold = true))
        info?.let { c.drawText(it, right, y + 15f, text(9f, muted, align = Paint.Align.RIGHT)) }
        y += 28f
    }

    private fun notice(message: String) {
        val p = text(9.5f, muted)
        val lines = wrap(message, p, cw - 28f)
        val h = 24f + lines.size * 13f
        ensure(h + 8f)
        card(y, h, 14f)
        lines.forEachIndexed { i, l -> c.drawText(l, m + 14f, y + 20f + i * 13f, p) }
        y += h + 8f
    }

    private fun maintenanceRow(r: MaintenanceRecordEntity, attachment: Int?) {
        val pad = 12f
        val amountW = 96f
        val leftX = m + pad + 34f + 10f
        val textW = right - pad - amountW - leftX
        val descPaint = text(9f, ink)
        // RF-04: quando a mão de obra foi separada, o PDF mostra peças + mão de obra antes da descrição
        val laborLine = r.laborAmount?.let { l ->
            "Peças ${money.format(com.autotech.android.domain.MaintenanceCost.parts(r.amount, l) ?: 0.0)} + mão de obra ${money.format(l)}"
        }
        val desc = listOfNotNull(laborLine, r.description?.takeIf { it.isNotBlank() })
            .joinToString("\n")
            .takeIf { it.isNotBlank() }
            ?.let { wrap(it, descPaint, textW, 5) }.orEmpty()
        val h = max(58f, pad + 30f + desc.size * 12.5f + (if (desc.isEmpty()) 0f else 6f) + pad - 4f)
        ensure(h + 8f)
        card(y, h, 14f)

        val label = MaintenanceCatalog.label(r.itemKey)
        fill.color = accentSoft
        c.drawCircle(m + pad + 17f, y + pad + 17f, 17f, fill)
        c.drawText(label.take(1).uppercase(ptBr), m + pad + 17f, y + pad + 22f, text(13f, primary, bold = true, align = Paint.Align.CENTER))

        c.drawText(ellipsize(label, text(11f, ink, bold = true), textW), leftX, y + pad + 12f, text(11f, ink, bold = true))
        c.drawText("${dateFmt.format(Date(r.eventDateMillis))}  ·  ${number.format(r.eventKm)} km", leftX, y + pad + 26f, text(8.5f, muted))
        desc.forEachIndexed { i, l -> c.drawText(l, leftX, y + pad + 41f + i * 12.5f, descPaint) }

        c.drawText(
            r.amount?.let { money.format(it) } ?: "—",
            right - pad, y + pad + 12f, text(11.5f, if (r.amount == null) muted else ink, bold = true, align = Paint.Align.RIGHT)
        )
        attachment?.let { chip("Anexo $it", right - pad, y + pad + 20f) }
        y += h + 8f
    }

    private fun fuelTable(fuel: List<FuelRecordEntity>, attachments: Map<Long, Int>) {
        val withAttach = options.includeReceipts && attachments.isNotEmpty()
        // RF-05: km/L só na linha do abastecimento em que o cálculo foi pedido,
        // referente ao trecho desde o abastecimento imediatamente anterior
        val periods = FuelConsumption.periods(fuel.map { it.toFillUp() }).associateBy { it.fillId }
        val headers = buildList {
            add("Data"); add("KM"); add("Litros"); add("Preço / litro"); add("Total"); add("km/L")
            if (withAttach) add("Anexo")
        }
        val weights = if (withAttach) listOf(0.14f, 0.15f, 0.11f, 0.14f, 0.15f, 0.19f, 0.12f)
        else listOf(0.15f, 0.17f, 0.13f, 0.16f, 0.17f, 0.22f)
        val xs = weights.runningFold(m) { acc, w -> acc + w * cw }
        val rowH = 22f

        fun header() {
            round(RectF(m, y, right, y + 24f), primary, 10f)
            headers.forEachIndexed { i, h -> c.drawText(h, xs[i] + 10f, y + 16f, text(8f, white, bold = true)) }
            y += 26f
        }

        ensure(24f + rowH * 2)
        header()
        fuel.forEachIndexed { i, f ->
            if (y + rowH > bottom) { newPage(); header() }
            fill.color = if (i % 2 == 0) white else zebra
            c.drawRect(m, y, right, y + rowH, fill)
            val values = buildList {
                add(dateFmt.format(Date(f.dateMillis)))
                add("${number.format(f.eventKm)} km")
                add(decimal.format(f.liters))
                add(f.amount?.takeIf { f.liters > 0 }?.let { money.format(it / f.liters) } ?: "—")
                add(f.amount?.let { money.format(it) } ?: "—")
                add(periods[f.id]?.let { p ->
                    decimal.format(p.kmPerLiter) + when (p.burned) {
                        FuelMix.ETHANOL -> " (álcool)"
                        FuelMix.GASOLINE -> " (gasolina)"
                        FuelMix.MIXED -> " (mistura)"
                        FuelMix.UNKNOWN -> ""
                    }
                } ?: "—")
                if (withAttach) add(attachments[f.id]?.let { "Anexo $it" } ?: "—")
            }
            values.forEachIndexed { k, v ->
                val p = if (k == 4) text(9f, ink, bold = true) else text(9f, ink)
                c.drawText(ellipsize(v, p, xs[k + 1] - xs[k] - 14f), xs[k] + 10f, y + 15f, p)
            }
            y += rowH
        }
        // Linha de total
        if (y + 26f > bottom) newPage()
        round(RectF(m, y + 2f, right, y + 26f), field, 10f)
        c.drawText("Total", xs[0] + 10f, y + 18f, text(9f, ink, bold = true))
        c.drawText(decimal.format(fuel.sumOf { it.liters }), xs[2] + 10f, y + 18f, text(9f, ink, bold = true))
        c.drawText(money.format(fuel.sumOf { it.amount ?: 0.0 }), xs[4] + 10f, y + 18f, text(9f, ink, bold = true))
        y += 34f
    }

    // ─── Comprovantes ────────────────────────────────────────────────────────

    private fun attachmentPages(a: Attachment, first: Boolean, total: Int) {
        newPage()
        if (first) section("Comprovantes", "$total ${if (total == 1) "anexo" else "anexos"}")
        attachmentHeader(a, null)
        val file = File(a.path)
        val ext = file.extension.lowercase(Locale.ROOT)
        when {
            !file.exists() -> notice("O arquivo deste comprovante não foi encontrado no aparelho.")
            ext == "pdf" -> drawPdfReceipt(a, file)
            ext in IMAGE_EXT -> {
                val bmp = decodeImage(file, 1400)
                if (bmp == null) notice("Não foi possível ler a imagem deste comprovante.")
                else { drawFit(bmp); bmp.recycle() }
            }
            else -> notice(
                "Arquivos do tipo .${ext.uppercase(Locale.ROOT).ifEmpty { "?" }} não podem ser exibidos dentro do PDF. " +
                    "O comprovante continua guardado no aplicativo com o nome ${file.name}."
            )
        }
    }

    private fun attachmentHeader(a: Attachment, pageInfo: String?) {
        val h = 54f
        card(y, h, 14f)
        val chipW = 64f
        round(RectF(m + 12f, y + 13f, m + 12f + chipW, y + 41f), primary, 10f)
        c.drawText("Anexo ${a.number}", m + 12f + chipW / 2, y + 31f, text(9.5f, white, bold = true, align = Paint.Align.CENTER))
        val tx = m + 12f + chipW + 12f
        val tw = right - 12f - tx
        c.drawText(ellipsize("${a.kind} · ${a.title}" + (pageInfo?.let { " · $it" } ?: ""), text(11f, ink, bold = true), tw), tx, y + 24f, text(11f, ink, bold = true))
        c.drawText(
            ellipsize("${dateFmt.format(Date(a.dateMillis))} · ${number.format(a.km)} km · ${File(a.path).name}", text(8.5f, muted), tw),
            tx, y + 39f, text(8.5f, muted)
        )
        y += h + 10f
    }

    private fun drawPdfReceipt(a: Attachment, file: File) {
        runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                val renderer = PdfRenderer(pfd)
                try {
                    val count = min(renderer.pageCount, MAX_RECEIPT_PAGES)
                    for (i in 0 until count) {
                        if (i > 0) {
                            newPage()
                            attachmentHeader(a, "página ${i + 1} de ${renderer.pageCount}")
                        }
                        val p = renderer.openPage(i)
                        val w = 1100
                        val bmp = Bitmap.createBitmap(w, max(1, (w.toFloat() * p.height / p.width).toInt()), Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(white)
                        p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        p.close()
                        drawFit(bmp)
                        bmp.recycle()
                    }
                    if (renderer.pageCount > MAX_RECEIPT_PAGES) {
                        notice("Este comprovante tem ${renderer.pageCount} páginas; as $MAX_RECEIPT_PAGES primeiras foram incluídas.")
                    }
                } finally {
                    renderer.close()
                }
            }
        }.onFailure {
            notice("Não foi possível ler este PDF (ele pode estar protegido por senha).")
        }
    }

    /** Imagem inteira, sem cortes, no espaço que sobra da página, dentro de um cartão branco. */
    private fun drawFit(bmp: Bitmap) {
        val pad = 10f
        val availW = cw - 2 * pad
        val availH = bottom - y - 2 * pad
        if (availH < 120f) { newPage(); return drawFit(bmp) }
        val scale = min(availW / bmp.width, availH / bmp.height)
        val w = bmp.width * scale
        val h = bmp.height * scale
        card(y, h + 2 * pad, 14f)
        val left = m + pad + (availW - w) / 2
        c.drawBitmap(bmp, null, RectF(left, y + pad, left + w, y + pad + h), bitmapPaint)
        y += h + 2 * pad + 10f
    }

    // ─── Desenho ─────────────────────────────────────────────────────────────

    private fun text(size: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
            textAlign = align
        }

    private fun round(r: RectF, color: Int, radius: Float) {
        fill.shader = null
        fill.color = color
        c.drawRoundRect(r, radius, radius, fill)
    }

    private fun card(top: Float, h: Float, radius: Float) = round(RectF(m, top, right, top + h), white, radius)

    private fun chip(label: String, rightX: Float, top: Float) {
        val p = text(7.5f, primary, bold = true, align = Paint.Align.CENTER)
        val w = p.measureText(label) + 16f
        round(RectF(rightX - w, top, rightX, top + 16f), accentSoft, 8f)
        c.drawText(label, rightX - w / 2, top + 11f, p)
    }

    private fun plate(x: Float, top: Float, plateText: String) {
        val w = 120f
        val h = 36f
        val r = RectF(x, top, x + w, top + h)
        round(r, white, 6f)
        c.save()
        c.clipPath(Path().apply { addRoundRect(r, 6f, 6f, Path.Direction.CW) })
        fill.color = primary
        c.drawRect(x, top, x + w, top + 9f, fill)
        c.restore()
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f; color = ink }
        c.drawRoundRect(r, 6f, 6f, stroke)
        c.drawText("BRASIL", x + w / 2, top + 7f, text(5.5f, white, bold = true, align = Paint.Align.CENTER))
        val p = text(15f, ink, bold = true, align = Paint.Align.CENTER).apply { letterSpacing = 0.08f }
        c.drawText(plateText, x + w / 2, top + 29f, p)
    }

    private fun drawLogo(x: Float, top: Float, size: Float, alpha: Int = 255) {
        val b = logo ?: return
        val p = Paint(bitmapPaint).apply { this.alpha = alpha }
        c.drawBitmap(b, null, RectF(x, top, x + size, top + size), p)
    }

    /** Preenche o retângulo cortando as sobras (como a foto no aplicativo). */
    private fun drawCropped(b: Bitmap, dst: RectF, radius: Float) {
        val scale = max(dst.width() / b.width, dst.height() / b.height)
        val sw = dst.width() / scale
        val sh = dst.height() / scale
        val src = Rect(
            ((b.width - sw) / 2).toInt(), ((b.height - sh) / 2).toInt(),
            ((b.width + sw) / 2).toInt(), ((b.height + sh) / 2).toInt()
        )
        c.save()
        c.clipPath(Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) })
        c.drawBitmap(b, src, dst, bitmapPaint)
        c.restore()
    }

    private fun wrap(text: String, p: Paint, width: Float, maxLines: Int = Int.MAX_VALUE): List<String> {
        val out = mutableListOf<String>()
        text.split('\n').forEach { para ->
            var cur = ""
            para.split(' ').filter { it.isNotEmpty() }.forEach { word ->
                val cand = if (cur.isEmpty()) word else "$cur $word"
                if (p.measureText(cand) <= width) {
                    cur = cand
                } else {
                    if (cur.isNotEmpty()) out += cur
                    cur = word
                    while (p.measureText(cur) > width && cur.length > 1) {
                        val k = p.breakText(cur, true, width, null).coerceAtLeast(1)
                        out += cur.substring(0, k)
                        cur = cur.substring(k)
                    }
                }
            }
            if (cur.isNotEmpty()) out += cur
        }
        if (out.size <= maxLines) return out
        val kept = out.take(maxLines).toMutableList()
        kept[maxLines - 1] = ellipsize(kept[maxLines - 1] + "…", p, width)
        return kept
    }

    private fun ellipsize(s: String, p: Paint, width: Float): String {
        if (p.measureText(s) <= width) return s
        var t = s
        while (t.isNotEmpty() && p.measureText("$t…") > width) t = t.dropLast(1)
        return "${t.trimEnd()}…"
    }

    /** Lê a imagem já reduzida e na orientação correta (fotos de câmera vêm giradas pelo EXIF). */
    private fun decodeImage(file: File, maxSide: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val bmp = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            if (bmp != null) return rotateByExif(file, bmp)
        }
        if (Build.VERSION.SDK_INT >= 28) {
            return runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val longest = max(info.size.width, info.size.height)
                    if (longest > maxSide) {
                        val f = maxSide.toFloat() / longest
                        decoder.setTargetSize((info.size.width * f).toInt().coerceAtLeast(1), (info.size.height * f).toInt().coerceAtLeast(1))
                    }
                }
            }.getOrNull()
        }
        return null
    }

    private fun rotateByExif(file: File, bmp: Bitmap): Bitmap {
        val degrees = runCatching {
            when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (degrees == 0f) return bmp
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
        if (rotated !== bmp) bmp.recycle()
        return rotated
    }

    private companion object {
        val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
        const val MAX_RECEIPT_PAGES = 20
    }
}
