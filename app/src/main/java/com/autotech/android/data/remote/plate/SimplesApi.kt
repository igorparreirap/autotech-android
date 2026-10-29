package com.autotech.android.data.remote.plate

import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * RF02: consulta de dados do veículo pela placa na SimplesAPI.
 * POST https://api.simplesapi.com.br/v1/vehicle/plate
 * Cobrança: R$ 0,10 por consulta com sucesso (200) ou placa não encontrada (404).
 * Placa inválida (422) e serviço indisponível (503) não são cobrados.
 * @see <a href="https://simplesapi.com.br/consulta-placa/">SimplesAPI</a>
 */
interface SimplesApi {

    @POST("v1/vehicle/plate")
    suspend fun lookupPlate(
        @Header("Authorization") authorization: String,
        @Body body: PlateLookupRequest
    ): Response<PlateLookupResponse>
}

data class PlateLookupRequest(
    val plate: String
)

data class PlateLookupResponse(
    val plate: String?,
    @Json(name = "vehicle_info") val vehicleInfo: VehicleInfoDto?
)

data class VehicleInfoDto(
    val brand: String?,
    val model: String?,
    val version: String?,
    val year: Int?,
    @Json(name = "model_year") val modelYear: Int?,
    val color: String?,
    val fuel: String?
)

/** Corpo padrão de erro da SimplesAPI: {"error": true, "code": "...", "message": "..."} */
data class SimplesApiErrorDto(
    val error: Boolean?,
    val code: String?,
    val message: String?
)
