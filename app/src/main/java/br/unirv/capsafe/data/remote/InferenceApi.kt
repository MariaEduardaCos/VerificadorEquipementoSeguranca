package br.unirv.capsafe.data.remote

import br.unirv.capsafe.data.model.CaixaDto
import br.unirv.capsafe.data.model.SessaoDto
import com.google.gson.JsonElement
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * RF6/RF7 — cliente HTTP do backend (InferenceApiClient do diagrama UML).
 *
 * Endpoints (InferenceController do backend):
 *   POST /api/inferencia            → envia o payload da inferência
 *   GET  /api/inferencia/historico  → lista sessões (histórico no servidor)
 *   GET  /api/inferencia/{id}/caixas → caixas de uma sessão
 */
interface InferenceApi {

    @POST("api/inferencia")
    suspend fun enviarInferencia(@Body payload: RequestBody): Response<JsonElement>

    @GET("api/inferencia/historico")
    suspend fun historico(): List<SessaoDto>

    @GET("api/inferencia/{id}/caixas")
    suspend fun caixasDaSessao(@Path("id") idSessao: String): List<CaixaDto>

    @DELETE("api/inferencia/{id}")
    suspend fun removerSessao(@Path("id") idSessao: String): Response<JsonElement>
}
