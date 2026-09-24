package br.unirv.capsafe.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Resultado de uma sessão de inferência local (RF4/RF5/RF6).
 * Espelha a estrutura InferenceResult do diagrama UML do enunciado.
 */
data class InferenceResult(
    val sessionId: String,
    val timestampMs: Long,
    val modelName: String,
    val executionTimeMs: Long,
    val imageWidthPx: Int,
    val imageHeightPx: Int,
    val boundingBoxes: List<BoundingBox>
) {
    val totalObjects: Int get() = boundingBoxes.size

    val averageConfidence: Float
        get() = if (boundingBoxes.isEmpty()) 0f
        else boundingBoxes.map { it.confidence }.average().toFloat()

    val helmetCount: Int get() = boundingBoxes.count { it.isHelmet }
    val violationCount: Int get() = boundingBoxes.count { it.isViolation }

    /** Índice de conformidade = capacetes / (capacetes + cabeças descobertas). */
    val complianceRate: Float?
        get() {
            val monitoradas = boundingBoxes.filter { it.isHelmet || it.isViolation }
            if (monitoradas.isEmpty()) return null
            return monitoradas.count { it.isHelmet }.toFloat() / monitoradas.size
        }

    fun classCounts(): Map<String, Int> = boundingBoxes.groupingBy { it.classLabel }.eachCount()

    /** RF6 — payload JSON exatamente na estrutura de referência do enunciado (seção 4). */
    fun toJsonPayload(): String {
        val dataHoraIso = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochMilli(timestampMs))

        return JSONObject().apply {
            put("id_sessao", sessionId)
            put("data_hora", dataHoraIso)
            put("nome_modelo", modelName)
            put("tempo_execucao_ms", executionTimeMs)
            put("total_objetos", totalObjects)
            put("confianca_media", (averageConfidence * 100).roundToInt() / 100f)
            put(
                "dimensao_imagem",
                JSONObject()
                    .put("largura_px", imageWidthPx)
                    .put("altura_px", imageHeightPx)
            )
            put(
                "caixas_delimitadoras",
                JSONArray().apply { boundingBoxes.forEach { put(it.toJsonObject()) } }
            )
        }.toString(2)
    }

    fun toSessaoEntity(uriImagem: String?): SessaoEntity = SessaoEntity(
        idSessao = sessionId,
        dataHoraMs = timestampMs,
        nomeModelo = modelName,
        tempoExecucaoMs = executionTimeMs,
        totalObjetos = totalObjects,
        confiancaMedia = averageConfidence,
        larguraPx = imageWidthPx,
        alturaPx = imageHeightPx,
        sincronizado = false,
        uriImagem = uriImagem
    )

    fun toCaixaEntities(): List<CaixaEntity> = boundingBoxes.map { box ->
        CaixaEntity(
            idCaixa = box.boxId,
            idSessao = sessionId,
            rotuloClasse = box.classLabel,
            confianca = box.confidence,
            larguraPx = box.widthPx,
            alturaPx = box.heightPx,
            centroideX = box.centroidX,
            centroideY = box.centroidY,
            areaPx2 = box.areaPx2
        )
    }

    companion object {
        /** Gera id no padrão "sessao_20260918_230601" (referência do enunciado). */
        fun novoIdSessao(): String {
            val formatter = DateTimeFormatter
                .ofPattern("yyyyMMdd_HHmmssSSS")
                .withZone(ZoneId.systemDefault())
            return "sessao_" + formatter.format(Instant.now())
        }
    }
}
