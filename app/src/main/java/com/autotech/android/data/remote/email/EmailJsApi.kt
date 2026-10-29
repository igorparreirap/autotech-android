package com.autotech.android.data.remote.email

import com.squareup.moshi.Json
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** RF-01: envio do código de recuperação de senha pelo EmailJS (plano gratuito, remetente Gmail do app). */
interface EmailJsApi {
    @POST("api/v1.0/email/send")
    suspend fun send(@Body body: EmailJsRequest): Response<ResponseBody>
}

data class EmailJsRequest(
    @Json(name = "service_id") val serviceId: String,
    @Json(name = "template_id") val templateId: String,
    /** Public Key da conta. */
    @Json(name = "user_id") val userId: String,
    /** Private Key: exigida porque a conta só aceita chamadas com chave privada. */
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "template_params") val templateParams: Map<String, String>
)

/** Chaves lidas do local.properties pelo build (BuildConfig). */
data class EmailJsKeys(
    val serviceId: String,
    val templateId: String,
    val publicKey: String,
    val privateKey: String
) {
    val configured: Boolean get() = listOf(serviceId, templateId, publicKey, privateKey).all { it.isNotBlank() }
}
