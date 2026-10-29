package com.autotech.android.data.remote

import com.autotech.android.data.remote.plate.SimplesApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    /** RNF03: tempo máximo de espera das chamadas externas. */
    const val TIMEOUT_SEC = 5L

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun client(log: Boolean): OkHttpClient {
        val b = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .callTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        if (log) {
            val logging = HttpLoggingInterceptor().apply {
                // BASIC não registra cabeçalhos, então a chave da API não aparece no log.
                level = HttpLoggingInterceptor.Level.BASIC
            }
            b.addInterceptor(logging)
        }
        return b.build()
    }

    /** RF-01: EmailJS (envio do código de recuperação de senha). */
    fun emailJsApi(logDebug: Boolean = false): com.autotech.android.data.remote.email.EmailJsApi =
        Retrofit.Builder()
            .baseUrl("https://api.emailjs.com/")
            .client(client(logDebug))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(com.autotech.android.data.remote.email.EmailJsApi::class.java)

    fun simplesApi(logDebug: Boolean = false): SimplesApi =
        Retrofit.Builder()
            .baseUrl("https://api.simplesapi.com.br/")
            .client(client(logDebug))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SimplesApi::class.java)
}
