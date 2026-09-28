package br.unirv.capsafe.data

import br.unirv.capsafe.data.local.AppDatabase
import br.unirv.capsafe.data.local.CaixaEntity
import br.unirv.capsafe.data.local.SessaoEntity
import br.unirv.capsafe.data.model.CaixaDto
import br.unirv.capsafe.data.model.InferenceResult
import br.unirv.capsafe.data.model.SessaoDto
import br.unirv.capsafe.data.remote.ApiClient
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Resultado do envio ao servidor (RF6). */
sealed class SyncOutcome {
    data object Sent : SyncOutcome()
    data class Failed(val motivo: String) : SyncOutcome()
}

/**
 * Repository — camada de domínio do MVVM (RF9).
 * Fluxo: salvar local (Room) → POST automático ao backend → atualizar flag de sync.
 */
class InferenceRepository(private val dao: AppDatabaseDao) {

    /** RF6 + RF7 — salva localmente e transmite via HTTP POST JSON. */
    suspend fun salvarInferencia(
        result: InferenceResult,
        imageUri: String?,
        baseUrl: String
    ): SyncOutcome {
        dao.inserirSessaoComCaixas(result.toSessaoEntity(imageUri), result.toCaixaEntities())

        return try {
            val body = result.toJsonPayload()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val response = ApiClient.api(baseUrl).enviarInferencia(body)
            if (response.isSuccessful) {
                dao.atualizarSincronizacao(result.sessionId, true)
                SyncOutcome.Sent
            } else {
                SyncOutcome.Failed("HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            SyncOutcome.Failed(e.message ?: "Falha de conexão")
        }
    }

    fun observarHistorico(): Flow<List<SessaoEntity>> = dao.observarHistorico()

    suspend fun detalhesDaSessao(id: String): Pair<SessaoEntity?, List<CaixaEntity>> =
        dao.sessaoPorId(id) to dao.caixasDaSessao(id)

    /** Reenvia sessões salvas localmente que falharam no POST anterior. */
    suspend fun sincronizarPendentes(baseUrl: String): Int {
        val pendentes = dao.sessoesNaoSincronizadas()
        var enviadas = 0
        for (sessao in pendentes) {
            val caixas = dao.caixasDaSessao(sessao.idSessao)
            try {
                val body = reconstruirPayload(sessao, caixas)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
                val response = ApiClient.api(baseUrl).enviarInferencia(body)
                if (response.isSuccessful) {
                    dao.atualizarSincronizacao(sessao.idSessao, true)
                    enviadas++
                }
            } catch (_: Exception) {
                // segue para a próxima; a sessão continua pendente
            }
        }
        return enviadas
    }

    /** Importa o histórico do servidor para o Room (consulta RF7 no app). */
    suspend fun importarDoServidor(baseUrl: String): Int {
        val remoto = ApiClient.api(baseUrl).historico()
        remoto.forEach { sessaoDto ->
            dao.inserirSessaoComCaixas(
                sessaoDto.toEntity(),
                sessaoDto.caixas_delimitadoras.map { it.toEntity(sessaoDto.id_sessao) }
            )
        }
        return remoto.size
    }

    private fun SessaoDto.toEntity() = SessaoEntity(
        idSessao = id_sessao,
        dataHoraMs = parseIso(data_hora),
        nomeModelo = nome_modelo,
        tempoExecucaoMs = tempo_execucao_ms,
        totalObjetos = total_objetos,
        confiancaMedia = confianca_media,
        larguraPx = dimensao_imagem.largura_px,
        alturaPx = dimensao_imagem.altura_px,
        sincronizado = true
    )

    private fun CaixaDto.toEntity(idSessao: String) = CaixaEntity(
        idCaixa = id_caixa,
        idSessao = idSessao,
        rotuloClasse = rotulo_classe,
        confianca = confianca,
        larguraPx = largura_px,
        alturaPx = altura_px,
        centroideX = centroide_x,
        centroideY = centroide_y,
        areaPx2 = area_px2
    )

    private fun parseIso(iso: String): Long = try {
        OffsetDateTime.parse(iso, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli()
    } catch (e: Exception) {
        try {
            OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        } catch (e2: Exception) {
            System.currentTimeMillis()
        }
    }

    /** Reconstrói o payload de referência a partir das entidades Room. */
    private fun reconstruirPayload(sessao: SessaoEntity, caixas: List<CaixaEntity>): String {
        val dataHoraIso = java.time.Instant.ofEpochMilli(sessao.dataHoraMs)
            .atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)

        return JSONObject().apply {
            put("id_sessao", sessao.idSessao)
            put("data_hora", dataHoraIso)
            put("nome_modelo", sessao.nomeModelo)
            put("tempo_execucao_ms", sessao.tempoExecucaoMs)
            put("total_objetos", sessao.totalObjetos)
            put("confianca_media", sessao.confiancaMedia)
            put(
                "dimensao_imagem",
                JSONObject().put("largura_px", sessao.larguraPx).put("altura_px", sessao.alturaPx)
            )
            put(
                "caixas_delimitadoras",
                JSONArray().apply {
                    caixas.forEach { caixa ->
                        put(
                            JSONObject().apply {
                                put("id_caixa", caixa.idCaixa)
                                put("rotulo_classe", caixa.rotuloClasse)
                                put("confianca", caixa.confianca)
                                put("largura_px", caixa.larguraPx)
                                put("altura_px", caixa.alturaPx)
                                put("centroide_x", caixa.centroideX)
                                put("centroide_y", caixa.centroideY)
                                put("area_px2", caixa.areaPx2)
                            }
                        )
                    }
                }
            )
        }.toString()
    }
}

/** Alias para o DAO usado pelo repository. */
typealias AppDatabaseDao = br.unirv.capsafe.data.local.InferenceDao

/** ServiceLocator simples para injeção sem framework. */
object ServiceLocator {
    fun repository(context: Context): InferenceRepository =
        InferenceRepository(AppDatabase.get(context).inferenceDao())
}
