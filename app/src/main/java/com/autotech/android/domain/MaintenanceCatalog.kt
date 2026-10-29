package com.autotech.android.domain

import java.text.Normalizer
import java.util.Locale

/** Parte do carro que agrupa os itens de manutenção (RF-04). */
data class MaintenanceCategory(val key: String, val label: String)

/**
 * Item de manutenção. [main] = aparece entre os principais da categoria.
 * [fuels] = só aparece para esses combustíveis (null = para todos).
 */
data class MaintenanceItem(
    val key: String,
    val label: String,
    val category: String,
    val main: Boolean = false,
    val fuels: Set<FuelType>? = null
)

/**
 * RF-04: catálogo de manutenção organizado por parte do carro. Em cada parte, os itens principais vêm
 * primeiro e depois a lista completa dos itens comuns de troca. O usuário ainda pode criar um item
 * próprio em qualquer parte ("Outro"); ele é guardado com o nome digitado e passa a ter ciclo,
 * avisos e histórico separados, como os itens da lista.
 * As chaves antigas (oil, brake, tire, battery, timing, review, other) continuam válidas.
 */
object MaintenanceCatalog {

    const val CUSTOM_PREFIX = "custom:"
    private const val LEGACY_OTHER = "other"
    private const val MAX_NAME = 40

    private val COMBUSTION = setOf(FuelType.FLEX, FuelType.GASOLINE, FuelType.HYBRID)
    private val DIESEL = setOf(FuelType.DIESEL)

    val categories: List<MaintenanceCategory> = listOf(
        MaintenanceCategory("motor", "Motor"),
        MaintenanceCategory("cambio", "Câmbio e embreagem"),
        MaintenanceCategory("freios", "Freios"),
        MaintenanceCategory("suspensao", "Suspensão e direção"),
        MaintenanceCategory("pneus", "Pneus e rodas"),
        MaintenanceCategory("eletrica", "Elétrica e bateria"),
        MaintenanceCategory("ar", "Ar-condicionado"),
        MaintenanceCategory("funilaria", "Funilaria e pintura"),
        MaintenanceCategory("vidros", "Vidros e acessórios"),
        MaintenanceCategory("revisao", "Revisão e inspeção")
    )

    val items: List<MaintenanceItem> = listOf(
        // ── Motor
        MaintenanceItem("oil", "Óleo do motor e filtro", "motor", main = true),
        MaintenanceItem("air_filter", "Filtro de ar do motor", "motor", main = true),
        MaintenanceItem("fuel_filter", "Filtro de combustível", "motor", main = true),
        MaintenanceItem("spark_plugs", "Velas de ignição", "motor", main = true, fuels = COMBUSTION),
        MaintenanceItem("water_separator", "Filtro separador de água", "motor", main = true, fuels = DIESEL),
        MaintenanceItem("timing", "Correia dentada / comando", "motor", main = true),
        MaintenanceItem("coolant", "Líquido de arrefecimento", "motor", main = true),
        MaintenanceItem("spark_cables", "Cabos de vela", "motor", fuels = COMBUSTION),
        MaintenanceItem("ignition_coil", "Bobina de ignição", "motor", fuels = COMBUSTION),
        MaintenanceItem("glow_plugs", "Velas aquecedoras", "motor", fuels = DIESEL),
        MaintenanceItem("arla", "ARLA 32", "motor", fuels = DIESEL),
        MaintenanceItem("aux_belt", "Correia auxiliar (alternador)", "motor"),
        MaintenanceItem("tensioner", "Tensor e polias", "motor"),
        MaintenanceItem("timing_chain", "Corrente de comando", "motor"),
        MaintenanceItem("water_pump", "Bomba d'água", "motor"),
        MaintenanceItem("thermostat", "Válvula termostática", "motor"),
        MaintenanceItem("radiator", "Radiador", "motor"),
        MaintenanceItem("radiator_hoses", "Mangueiras do radiador", "motor"),
        MaintenanceItem("injectors", "Limpeza dos bicos injetores", "motor"),
        MaintenanceItem("throttle_body", "Limpeza do corpo de borboleta (TBI)", "motor", fuels = COMBUSTION),
        MaintenanceItem("head_gasket", "Junta do cabeçote", "motor"),
        MaintenanceItem("lambda", "Sonda lambda", "motor"),
        MaintenanceItem("engine_mounts", "Coxins do motor", "motor"),
        // ── Câmbio e embreagem
        MaintenanceItem("gearbox_oil", "Óleo do câmbio", "cambio", main = true),
        MaintenanceItem("clutch_kit", "Kit de embreagem", "cambio", main = true),
        MaintenanceItem("clutch_fluid", "Fluido da embreagem", "cambio", main = true),
        MaintenanceItem("atf", "Fluido do câmbio automático (ATF)", "cambio"),
        MaintenanceItem("clutch_cable", "Cabo de embreagem", "cambio"),
        MaintenanceItem("clutch_actuator", "Atuador / cilindro da embreagem", "cambio"),
        MaintenanceItem("gearbox_mount", "Coxim do câmbio", "cambio"),
        MaintenanceItem("cv_joint", "Junta homocinética", "cambio"),
        MaintenanceItem("cv_boot", "Coifa da homocinética", "cambio"),
        MaintenanceItem("diff_oil", "Óleo do diferencial", "cambio"),
        // ── Freios
        MaintenanceItem("brake", "Revisão dos freios", "freios", main = true),
        MaintenanceItem("brake_pads_front", "Pastilhas de freio dianteiras", "freios", main = true),
        MaintenanceItem("brake_fluid", "Fluido de freio", "freios", main = true),
        MaintenanceItem("brake_discs", "Discos de freio", "freios", main = true),
        MaintenanceItem("brake_pads_rear", "Pastilhas de freio traseiras", "freios"),
        MaintenanceItem("brake_shoes", "Lonas / sapatas de freio", "freios"),
        MaintenanceItem("brake_drums", "Tambores de freio", "freios"),
        MaintenanceItem("caliper", "Pinça de freio", "freios"),
        MaintenanceItem("wheel_cylinder", "Cilindro de roda", "freios"),
        MaintenanceItem("brake_hoses", "Flexíveis de freio", "freios"),
        MaintenanceItem("handbrake", "Freio de mão (cabo / regulagem)", "freios"),
        MaintenanceItem("abs_sensor", "Sensor do ABS", "freios"),
        // ── Suspensão e direção
        MaintenanceItem("shocks_front", "Amortecedores dianteiros", "suspensao", main = true),
        MaintenanceItem("shocks_rear", "Amortecedores traseiros", "suspensao", main = true),
        MaintenanceItem("shock_kit", "Kit batente e coxim do amortecedor", "suspensao", main = true),
        MaintenanceItem("ball_joint", "Pivô da suspensão", "suspensao", main = true),
        MaintenanceItem("springs", "Molas", "suspensao"),
        MaintenanceItem("control_arm", "Bandeja", "suspensao"),
        MaintenanceItem("control_arm_bushings", "Buchas da bandeja", "suspensao"),
        MaintenanceItem("stabilizer_links", "Bieletas da barra estabilizadora", "suspensao"),
        MaintenanceItem("tie_rod_end", "Terminal de direção", "suspensao"),
        MaintenanceItem("axial_rod", "Barra axial", "suspensao"),
        MaintenanceItem("steering_rack", "Caixa de direção", "suspensao"),
        MaintenanceItem("power_steering_fluid", "Fluido da direção hidráulica", "suspensao"),
        MaintenanceItem("wheel_bearing", "Rolamento de roda", "suspensao"),
        // ── Pneus e rodas
        MaintenanceItem("tire", "Troca de pneus", "pneus", main = true),
        MaintenanceItem("rotation", "Rodízio de pneus", "pneus", main = true),
        MaintenanceItem("alignment", "Alinhamento", "pneus", main = true),
        MaintenanceItem("balancing", "Balanceamento", "pneus", main = true),
        MaintenanceItem("puncture", "Conserto de furo", "pneus"),
        MaintenanceItem("valves", "Válvulas dos pneus", "pneus"),
        MaintenanceItem("spare", "Estepe", "pneus"),
        MaintenanceItem("wheel_repair", "Reparo / desempeno de rodas", "pneus"),
        MaintenanceItem("tpms", "Sensor de pressão dos pneus", "pneus"),
        // ── Elétrica e bateria
        MaintenanceItem("battery", "Bateria", "eletrica", main = true),
        MaintenanceItem("headlight_bulbs", "Lâmpadas dos faróis", "eletrica", main = true),
        MaintenanceItem("alternator", "Alternador", "eletrica"),
        MaintenanceItem("starter", "Motor de arranque", "eletrica"),
        MaintenanceItem("fuses", "Fusíveis e relés", "eletrica"),
        MaintenanceItem("rear_bulbs", "Lâmpadas das lanternas e de freio", "eletrica"),
        MaintenanceItem("headlight_aim", "Regulagem dos faróis", "eletrica"),
        MaintenanceItem("key_battery", "Bateria da chave / controle", "eletrica"),
        MaintenanceItem("horn", "Buzina", "eletrica"),
        // ── Ar-condicionado
        MaintenanceItem("cabin_filter", "Filtro de cabine (ar-condicionado)", "ar", main = true),
        MaintenanceItem("ac_cleaning", "Higienização do ar-condicionado", "ar", main = true),
        MaintenanceItem("ac_gas", "Recarga de gás do ar-condicionado", "ar", main = true),
        MaintenanceItem("ac_compressor", "Compressor do ar-condicionado", "ar"),
        MaintenanceItem("ac_condenser", "Condensador", "ar"),
        MaintenanceItem("ac_belt", "Correia do compressor", "ar"),
        MaintenanceItem("blower", "Ventilador interno", "ar"),
        // ── Funilaria e pintura
        MaintenanceItem("body_repair", "Reparo de lataria", "funilaria", main = true),
        MaintenanceItem("paint", "Pintura", "funilaria", main = true),
        MaintenanceItem("polish", "Polimento e cristalização", "funilaria", main = true),
        MaintenanceItem("pdr", "Martelinho de ouro", "funilaria"),
        MaintenanceItem("bumper", "Para-choque", "funilaria"),
        MaintenanceItem("mirror", "Retrovisor", "funilaria"),
        MaintenanceItem("rust", "Tratamento de ferrugem", "funilaria"),
        MaintenanceItem("headlight_polish", "Polimento dos faróis", "funilaria"),
        // ── Vidros e acessórios
        MaintenanceItem("wipers", "Palhetas do limpador", "vidros", main = true),
        MaintenanceItem("windshield", "Para-brisa", "vidros", main = true),
        MaintenanceItem("side_glass", "Vidro lateral / traseiro", "vidros"),
        MaintenanceItem("window_regulator", "Máquina de vidro", "vidros"),
        MaintenanceItem("washer", "Reservatório e esguicho do lavador", "vidros"),
        MaintenanceItem("locks", "Fechaduras e travas", "vidros"),
        MaintenanceItem("film", "Película", "vidros"),
        // ── Revisão e inspeção
        MaintenanceItem("review", "Revisão periódica", "revisao", main = true),
        MaintenanceItem("checkup", "Check-up antes de viagem", "revisao", main = true),
        MaintenanceItem("inspection", "Vistoria / inspeção veicular", "revisao"),
        MaintenanceItem("scanner", "Diagnóstico eletrônico (scanner)", "revisao")
    )

    private val byKey = items.associateBy { it.key }

    /** Itens de revisão: quando o item principal é uma revisão, o usuário marca o que foi feito nela. */
    val REVIEW_KEYS = setOf("review", "checkup")

    /** Itens sugeridos para marcar numa revisão (os que não valem para o combustível ficam de fora). */
    private val REVIEW_CHECKLIST = listOf(
        "oil", "air_filter", "fuel_filter", "cabin_filter", "spark_plugs", "water_separator",
        "coolant", "brake_fluid", "brake_pads_front", "rotation", "alignment", "balancing", "wipers"
    )

    fun reviewChecklist(fuel: FuelType?): List<MaintenanceItem> =
        REVIEW_CHECKLIST.mapNotNull { byKey[it] }.filter { fuel == null || it.fuels == null || fuel in it.fuels }

    /** Itens da categoria visíveis para o combustível do veículo (combustível desconhecido = todos). */
    fun visibleItems(category: String, fuel: FuelType?): List<MaintenanceItem> =
        items.filter { it.category == category && (fuel == null || it.fuels == null || fuel in it.fuels) }

    /** Principais da categoria, na ordem de prioridade do catálogo. */
    fun mainItems(category: String, fuel: FuelType?): List<MaintenanceItem> =
        visibleItems(category, fuel).filter { it.main }

    /** Demais itens comuns de troca da categoria, em ordem alfabética. */
    fun otherItems(category: String, fuel: FuelType?): List<MaintenanceItem> =
        visibleItems(category, fuel).filterNot { it.main }.sortedBy { plain(it.label) }

    /** Busca pelo nome em todas as partes do carro (sem diferenciar acentos e maiúsculas). */
    fun search(query: String, fuel: FuelType?): List<MaintenanceItem> {
        val q = plain(query).trim()
        if (q.isEmpty()) return emptyList()
        return items.filter { (fuel == null || it.fuels == null || fuel in it.fuels) && q in plain(it.label) }
    }

    fun isCustom(key: String): Boolean = key.startsWith(CUSTOM_PREFIX)

    /** Chave de um item criado pelo usuário: custom:<categoria>:<nome>. Null se o nome estiver vazio. */
    fun customKey(category: String, name: String): String? {
        val clean = name.trim().replace(Regex("\\s+"), " ").take(MAX_NAME)
        if (clean.isEmpty()) return null
        return "$CUSTOM_PREFIX$category:" + clean.replaceFirstChar { it.titlecase(Locale("pt", "BR")) }
    }

    /** Reaproveita um item próprio já usado com o mesmo nome (sem diferenciar maiúsculas e acentos). */
    fun customKey(category: String, name: String, existing: Collection<String>): String? {
        val key = customKey(category, name) ?: return null
        return existing.firstOrNull { isCustom(it) && categoryOf(it) == category && plain(label(it)) == plain(label(key)) } ?: key
    }

    fun categoryOf(key: String): String? = when {
        isCustom(key) -> key.removePrefix(CUSTOM_PREFIX).substringBefore(':')
        else -> byKey[key]?.category
    }

    /** Nome da parte do carro ("Motor", "Freios"...); "Outros" para registros antigos sem categoria. */
    fun categoryLabel(key: String): String =
        categoryOf(key)?.let { c -> categories.firstOrNull { it.key == c }?.label } ?: "Outros"

    fun label(key: String): String = when {
        isCustom(key) -> key.removePrefix(CUSTOM_PREFIX).substringAfter(':')
        key == LEGACY_OTHER -> "Outro"
        else -> byKey[key]?.label ?: key
    }

    private fun plain(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
}
