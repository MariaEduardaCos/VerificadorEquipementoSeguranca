package br.unirv.capsafe.data.model

import org.json.JSONObject
import kotlin.math.roundToInt

private fun Float.round2(): Float = (this * 100).roundToInt() / 100f

/**
 * RF5 — Métricas geométricas de cada Bounding Box detectado:
 *  - Largura (Wpx) e Altura (Hpx) em pixels;
 *  - Centróide: Xc = Xmin + Wpx/2 ; Yc = Ymin + Hpx/2  (equação 1 do enunciado);
 *  - Área: Apx = Wpx × Hpx (pixels²).
 *
 * Todas as medidas são calculadas na RESOLUÇÃO ORIGINAL da imagem.
 */
data class BoundingBox(
    val boxId: Int,
    val classLabel: String,
    val confidence: Float,
    val xMinPx: Float,
    val yMinPx: Float,
    val widthPx: Float,
    val heightPx: Float
) {
    val centroidX: Float get() = xMinPx + widthPx / 2f
    val centroidY: Float get() = yMinPx + heightPx / 2f
    val areaPx2: Float get() = widthPx * heightPx

    /** Rótulos que indicam TRABALHADOR COM CAPACETE (conforme a classe do dataset). */
    val isHelmet: Boolean get() = classLabel.trim().lowercase() in HELMET_LABELS

    /** Rótulos que indicam VIOLAÇÃO (cabeça descoberta / sem capacete). */
    val isViolation: Boolean get() = classLabel.trim().lowercase() in VIOLATION_LABELS

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id_caixa", boxId)
        put("rotulo_classe", classLabel)
        put("confianca", confidence.round2())
        put("largura_px", widthPx.round2())
        put("altura_px", heightPx.round2())
        put("centroide_x", centroidX.round2())
        put("centroide_y", centroidY.round2())
        put("area_px2", areaPx2.round2())
    }

    companion object {
        // Ajuste conforme as classes do dataset escolhido no Roboflow.
        val HELMET_LABELS = setOf(
            "capacete", "helmet", "hardhat", "hard-hat", "hard_hat"
        )
        val VIOLATION_LABELS = setOf(
            "cabeca", "cabeça", "head", "sem-capacete", "no-helmet", "sem_capacete"
        )
    }
}
