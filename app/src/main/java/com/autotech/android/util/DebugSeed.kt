package com.autotech.android.util

import com.autotech.android.AutoTechApplication
import com.autotech.android.BuildConfig
import com.autotech.android.data.local.entity.FuelRecordEntity
import com.autotech.android.data.local.entity.MaintenanceRecordEntity
import com.autotech.android.data.local.entity.PlateLookupCacheEntity
import com.autotech.android.data.local.entity.VehicleEntity
import java.util.Calendar
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random

/**
 * Carga de dados simulada para testes visuais e exploratórios (somente no build de depuração).
 *
 * Cria a conta de teste definida no local.properties (SEED_NAME, SEED_EMAIL e SEED_PASSWORD, que não
 * são versionados) e um veículo fictício comprado zero km em 15/03/2024, com o histórico completo até
 * hoje: odômetro crescente, abastecimentos a cada 380 a 480 km, revisões e manutenções. Alguns ciclos
 * são montados para cair nas etapas de aviso da RN-03 no dia da carga: 30, 15 e 7 dias por tempo e
 * 10% e 50% do intervalo por quilometragem.
 *
 * Todos os registros passam pelos repositórios, em ordem cronológica, e portanto pelas mesmas regras
 * do odômetro (RN-02 e RN-05) de um lançamento feito pela tela.
 */
object DebugSeed {

    const val PLATE = "TCC1A24"
    /** Placa fictícia gravada só no cache local, para o teste de cadastro pela placa sem gastar crédito da API. */
    const val CACHED_PLATE = "TCC5E26"

    private const val DAY = 86_400_000L

    val enabled: Boolean
        get() = BuildConfig.DEBUG && BuildConfig.SEED_EMAIL.isNotBlank() && BuildConfig.SEED_PASSWORD.isNotBlank()

    private data class Ev(val at: Long, val km: Long, val run: suspend (Long) -> Unit)

    /** Devolve o id do usuário de teste. Se o veículo já existir, não carrega de novo. */
    suspend fun run(app: AutoTechApplication, now: Long = System.currentTimeMillis()): Result<Long> = runCatching {
        val g = app.graph
        val email = BuildConfig.SEED_EMAIL.trim()
        val password = BuildConfig.SEED_PASSWORD
        val name = BuildConfig.SEED_NAME.ifBlank { "Usuário de teste" }
        val uid = g.authRepository.login(email, password).getOrNull()
            ?: reuseSeedAccount(app, email, password)
            ?: g.authRepository.register(email, password, name).getOrThrow()

        app.database.plateLookupCacheDao().upsert(
            PlateLookupCacheEntity(CACHED_PLATE, "FIAT", "ARGO", "DRIVE 1.0", 2022, 2023, "BRANCA", "FLEX", now)
        )
        if (g.vehicleRepository.listAllVehicles(uid).any { it.plate == PLATE }) return@runCatching uid

        val rnd = Random(2024)
        val start = Calendar.getInstance().apply { set(2024, Calendar.MARCH, 15, 10, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val days = ((now - start) / DAY).toInt()

        // ------------------------------------------------ odômetro dia a dia (cerca de 43 km por dia)
        val kmAt = LongArray(days + 1)
        kmAt[0] = 12
        for (d in 1..days) {
            val wd = Calendar.getInstance().apply { timeInMillis = start + d * DAY }.get(Calendar.DAY_OF_WEEK)
            val base = if (wd == Calendar.SATURDAY || wd == Calendar.SUNDAY) 28 else 49
            kmAt[d] = kmAt[d - 1] + base + rnd.nextInt(-14, 15)
        }
        val kmNow = kmAt[days]
        fun dayAtKm(km: Long) = kmAt.indexOfFirst { it >= km }.coerceAtLeast(1)
        fun dayOf(t: Long) = ((t - start) / DAY).toInt().coerceIn(0, days)
        fun at(day: Int, hour: Int) = start + day * DAY + (hour - 10) * 3_600_000L

        val vid = g.vehicleRepository.insertVehicle(
            VehicleEntity(
                userId = uid, plate = PLATE, brand = "VOLKSWAGEN", model = "POLO", version = "1.0 TSI COMFORTLINE",
                year = 2023, modelYear = 2024, color = "PRATA", fuel = "FLEX", currentOdometerKm = 12
            )
        ).getOrThrow()

        val events = mutableListOf<Ev>()

        // ------------------------------------------------ abastecimentos
        fun gasPrice(m: Int) = 5.79 + 0.016 * m + 0.12 * sin(m / 3.0)
        fun ethPrice(m: Int) = 3.88 + 0.015 * m + 0.26 * sin(m / 2.4 + 1.0)
        var lastKm = kmAt[0]
        var lastEthanol = false
        var next = rnd.nextLong(380, 480)
        var first = true
        for (d in 2 until days - 1) {
            if (!first && kmAt[d] - lastKm < next) continue
            val month = d / 30
            val gp = gasPrice(month); val ep = ethPrice(month)
            val ethanol = ep / gp <= 0.70
            val run = kmAt[d] - lastKm
            val kmpl = if (lastEthanol) 9.4 + rnd.nextDouble(-0.5, 0.5) else 13.3 + rnd.nextDouble(-0.6, 0.6)
            val liters = if (first) 38.0 else (run / kmpl).coerceIn(20.0, 50.0)
            val price = if (ethanol) ep else gp
            val km = kmAt[d]; val t = at(d, 7 + rnd.nextInt(0, 12)); val f = first
            events += Ev(t, km) { _ ->
                g.fuelRepository.addRecord(
                    FuelRecordEntity(
                        vehicleId = vid, dateMillis = t, eventKm = km, vehicleKm = km,
                        liters = round2(liters), amount = round2(liters * price), receiptPath = null, registeredAtMillis = t,
                        ethanolLiters = if (ethanol) round2(liters) else null,
                        gasolineLiters = if (ethanol) null else round2(liters),
                        computeConsumption = !f
                    ),
                    updateVehicleOdometerTo = km
                ).getOrThrow()
            }
            lastKm = km; lastEthanol = ethanol; first = false; next = rnd.nextLong(380, 480)
        }

        // ------------------------------------------------ manutenções
        data class Sub(val key: String, val km: Int?, val months: Int?)
        fun maint(
            day: Int, key: String, amount: Double?, labor: Double?, desc: String,
            intervalKm: Int? = null, months: Int? = null, subs: List<Sub> = emptyList(), exactAt: Long? = null
        ) {
            val t = exactAt ?: at(day, 9 + rnd.nextInt(0, 8))
            val km = kmAt[dayOf(t)]
            events += Ev(t, km) { _ ->
                val rec = MaintenanceRecordEntity(
                    vehicleId = vid, itemKey = key, eventDateMillis = t, eventKm = km, vehicleKmAtRecord = km,
                    amount = amount, description = desc, receiptPath = null, registeredAtMillis = t, laborAmount = labor
                )
                val id = g.maintenanceRepository.addRecord(rec, updateVehicleOdometerTo = km).getOrThrow()
                if (intervalKm != null || months != null) g.maintenanceRepository.addCustomCycle(vid, id, key, intervalKm, months)
                val note = if (key == "review") "Feito na revisão periódica" else "Feito no mesmo atendimento"
                for (s in subs) {
                    val sid = g.maintenanceRepository.addRecord(
                        rec.copy(id = 0, itemKey = s.key, amount = null, laborAmount = null, description = note), updateVehicleOdometerTo = km
                    ).getOrThrow()
                    if (s.km != null || s.months != null) g.maintenanceRepository.addCustomCycle(vid, sid, s.key, s.km, s.months)
                }
            }
        }
        /** Serviço feito há [months] meses menos [dueInDays] dias: vence em exatamente [dueInDays] dias. */
        fun dueIn(dueInDays: Int, months: Int): Long =
            Calendar.getInstance().apply { timeInMillis = now + dueInDays * DAY + 3_600_000L; add(Calendar.MONTH, -months) }.timeInMillis

        val rev1 = dayAtKm(10_150); val rev2 = dayAtKm(21_300)
        val oilKm = kmNow - 9_200; val rev3 = dayAtKm(oilKm)
        maint(158, "alignment", 120.0, null, "Alinhamento e balanceamento depois de cair em um buraco")
        maint(rev1, "review", 489.0, null, "Revisão de 10.000 km na concessionária",
            10_000, 12, listOf(Sub("oil", 10_000, 12), Sub("rotation", 10_000, null)))
        maint(dayAtKm(14_800), "puncture", 40.0, null, "Conserto de furo no pneu traseiro direito")
        maint(rev2, "review", 812.0, 260.0, "Revisão de 20.000 km: óleo, inspeção dos freios e rodízio",
            10_000, 12, listOf(Sub("oil", 10_000, 12), Sub("brake", null, null), Sub("rotation", 10_000, null)))
        maint(dayAtKm(24_600), "wipers", 89.9, null, "Palhetas dianteiras")
        maint(dayOf(dueIn(30, 12)), "brake_fluid", 160.0, 70.0, "Troca do fluido de freio DOT 4", null, 12, exactAt = dueIn(30, 12))
        maint(dayOf(dueIn(15, 12)), "cabin_filter", 95.0, null, "Filtro de cabine e higienização do ar-condicionado", null, 12, exactAt = dueIn(15, 12))
        maint(dayOf(dueIn(7, 12)), "alignment", 140.0, null, "Alinhamento e balanceamento das quatro rodas", null, 12, exactAt = dueIn(7, 12))
        maint(dayAtKm(29_500), "brake_pads_front", 380.0, 120.0, "Pastilhas dianteiras e retífica dos discos", 30_000, null)
        maint(rev3, "review", 1045.0, 330.0, "Revisão de 30.000 km: óleo, filtros de ar e de combustível e velas",
            10_000, 12, listOf(Sub("oil", 10_000, 12), Sub("air_filter", 30_000, 36), Sub("spark_plugs", 40_000, 48), Sub("fuel_filter", 30_000, 36)))
        maint(dayAtKm(oilKm + 2_300), "battery", 529.0, null, "Bateria 60 Ah com 18 meses de garantia", null, 24)
        maint(dayAtKm(kmNow - 5_400), "rotation", 60.0, null, "Rodízio de pneus", 10_000, null)

        for (e in events.sortedBy { it.at }) e.run(vid)
        g.vehicleRepository.updateOdometer(vid, kmNow)
        uid
    }

    /**
     * Se o e-mail da conta de teste mudou no local.properties, a conta que já tem o veículo fictício
     * passa a usar o novo e-mail, com todo o histórico, em vez de uma conta nova e vazia.
     */
    private suspend fun reuseSeedAccount(app: AutoTechApplication, email: String, password: String): Long? {
        val db = app.database
        val owner = db.vehicleDao().findOwnerByPlate(PLATE) ?: return null
        if (db.userDao().getByEmail(email.lowercase()) != null) return null
        db.userDao().updateEmail(owner, email.lowercase())
        return app.graph.authRepository.login(email, password).getOrNull() ?: owner
    }

    private fun round2(v: Double) = (v * 100).roundToLong() / 100.0
}
