package com.autotech.android.data.repository

import com.autotech.android.data.local.dao.PlateLookupCacheDao
import com.autotech.android.data.local.entity.PlateLookupCacheEntity
import com.autotech.android.data.remote.NetworkModule
import com.autotech.android.data.remote.plate.PlateLookupRequest
import com.autotech.android.data.remote.plate.SimplesApi
import com.autotech.android.data.remote.plate.SimplesApiErrorDto
import com.autotech.android.domain.PlateRules
import com.autotech.android.domain.PlateVehicleData
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.io.InterruptedIOException

/**
 * RF02: obtém marca, modelo, versão e ano a partir da placa.
 * 1) valida a placa localmente (não gasta crédito com placa mal digitada);
 * 2) procura no cache local;
 * 3) só então consulta a SimplesAPI e guarda o resultado no cache.
 * Em qualquer falha, a tela mantém o preenchimento manual disponível.
 */
class PlateLookupRepository(
    private val api: SimplesApi,
    private val cacheDao: PlateLookupCacheDao,
    private val apiKey: String
) {

    class PlateLookupException(message: String) : Exception(message)

    suspend fun lookup(rawPlate: String): Result<PlateVehicleData> {
        val plate = PlateRules.normalize(rawPlate)
        if (!PlateRules.isValid(plate)) {
            return fail("Placa inválida. Use o formato ABC1D23 (Mercosul) ou ABC1234.")
        }

        cacheDao.get(plate)?.let { return Result.success(it.toDomain()) }

        if (apiKey.isBlank()) {
            return fail("Chave da SimplesAPI não configurada (SIMPLES_API_KEY no local.properties).")
        }

        val response = try {
            api.lookupPlate("Bearer $apiKey", PlateLookupRequest(plate))
        } catch (e: CancellationException) {
            throw e
        } catch (e: InterruptedIOException) {
            return fail("A consulta passou de ${NetworkModule.TIMEOUT_SEC} segundos.")
        } catch (e: IOException) {
            return fail("Sem conexão com a internet.")
        } catch (e: Exception) {
            return fail("Resposta inesperada do serviço de placas.")
        }

        if (response.isSuccessful) {
            val info = response.body()?.vehicleInfo
            val brand = info?.brand?.trim().orEmpty()
            val model = info?.model?.trim().orEmpty()
            if (info == null || brand.isEmpty() || model.isEmpty()) {
                return fail("O serviço não retornou marca e modelo para esta placa.")
            }
            val data = PlateVehicleData(
                plate = plate,
                brand = brand,
                model = model,
                version = info.version?.trim()?.ifEmpty { null },
                year = info.year,
                modelYear = info.modelYear,
                color = info.color?.trim()?.ifEmpty { null },
                fuel = info.fuel?.trim()?.ifEmpty { null }
            )
            cacheDao.upsert(data.toCache())
            return Result.success(data)
        }

        val serverMessage = parseError(response.errorBody()?.string())?.message
        val message = when (response.code()) {
            401 -> "Chave da SimplesAPI inválida ou expirada."
            402 -> serverMessage ?: "Saldo da SimplesAPI insuficiente."
            403 -> "Conta da SimplesAPI sem termos aceitos ou e-mail não verificado."
            404 -> "Nenhum veículo encontrado para esta placa."
            422 -> "A SimplesAPI recusou o formato da placa."
            429 -> "Muitas consultas seguidas. Aguarde alguns segundos."
            503 -> "Consulta de placa temporariamente indisponível."
            else -> serverMessage ?: "Erro na consulta da placa (HTTP ${response.code()})."
        }
        return fail(message)
    }

    private fun fail(message: String): Result<PlateVehicleData> =
        Result.failure(PlateLookupException(message))

    private fun parseError(body: String?): SimplesApiErrorDto? =
        body?.let {
            runCatching { NetworkModule.moshi.adapter(SimplesApiErrorDto::class.java).fromJson(it) }
                .getOrNull()
        }

    private fun PlateLookupCacheEntity.toDomain() = PlateVehicleData(
        plate = plate, brand = brand, model = model, version = version,
        year = year, modelYear = modelYear, color = color, fuel = fuel
    )

    private fun PlateVehicleData.toCache() = PlateLookupCacheEntity(
        plate = plate, brand = brand, model = model, version = version,
        year = year, modelYear = modelYear, color = color, fuel = fuel,
        fetchedAtMillis = System.currentTimeMillis()
    )
}
