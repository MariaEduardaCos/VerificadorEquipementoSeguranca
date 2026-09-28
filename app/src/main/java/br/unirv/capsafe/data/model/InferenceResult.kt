package br.unirv.capsafe.data.model

import br.unirv.capsafe.data.local.CaixaEntity
import br.unirv.capsafe.data.local.SessaoEntity
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

    val helmetCount: Int
        get() = boundingBoxes.count { it.isHelmet }

    val violationCount: Int
        get() = boundingBoxes.count { it.isViolation }

    val complianceRate: Float?
        get() {
            val monitoradas = helmetCount + violationCount
            return if (monitoradas == 0) null else helmetCount.toFloat() / monitoradas
        }

    fun classCounts(): Map<String, Int> =
        boundingBoxes.groupingBy { it.classLabel }.eachCount()

    fun toJsonPayload(): String {
        val isoDate = Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        return JSONObject().apply {
            put("id_sessao", sessionId)
            put("data_hora", isoDate)
            put("nome_modelo", modelName)
            put("tempo_execucao_ms", executionTimeMs)
            put("total_objetos", totalObjects)
            put("confianca_media", (averageConfidence * 100).roundToInt() / 100.0)
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
        fun novoIdSessao(): String = "sessao_${System.currentTimeMillis()}"
    }
}
