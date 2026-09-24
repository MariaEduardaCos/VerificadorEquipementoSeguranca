package br.unirv.capsafe.data.remote

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Fábrica de Retrofit com cache por baseUrl (a URL é configurável na Tela 1). */
object ApiClient {

    @Volatile
    private var baseUrlEmCache: String? = null

    @Volatile
    private var apiEmCache: InferenceApi? = null

    fun api(baseUrl: String): InferenceApi {
        val normalizada = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
        val existente = apiEmCache
        if (existente != null && baseUrlEmCache == normalizada) return existente

        synchronized(this) {
            val existenteSync = apiEmCache
            if (existenteSync != null && baseUrlEmCache == normalizada) return existenteSync

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            val nova = Retrofit.Builder()
                .baseUrl(normalizada)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()
                .create(InferenceApi::class.java)

            baseUrlEmCache = normalizada
            apiEmCache = nova
            return nova
        }
    }
}
