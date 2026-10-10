package br.unirv.capsafe.domain.model

import android.graphics.Bitmap

/**
 * Modelos de domínio da detecção YOLO — estrutura portada do projeto de
 * referência AlessaApp (domain/model/DetectionResult.kt), adaptada ao
 * contexto do CapSafe (verificação de EPI / capacete de segurança).
 */

/** Caixa delimitadora no espaço da imagem ORIGINAL (pixels). */
data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
) {
    val width: Float get() = x2 - x1
    val height: Float get() = y2 - y1
}

data class Detection(
    val box: BoundingBox,
    val score: Float,
    val classId: Int,
    val label: String
)

/** Resumo de uma sessão de inferência (RF4/RF5) — inclui a imagem anotada. */
data class InferenceSummary(
    val totalCount: Int,
    val avgConfidence: Float,
    val detections: List<Detection>,
    val classCounts: Map<String, Int>,
    val inferenceTimeMs: Long,
    val annotatedBitmap: Bitmap
)

/** Origem do modelo ONNX em uso (RF1): embarcado nos assets ou do dispositivo. */
sealed interface ModelInfo {
    val name: String
    val isCustom: Boolean

    data class AssetModel(
        override val name: String,
        val assetPath: String
    ) : ModelInfo {
        override val isCustom: Boolean = false
    }

    data class CustomModel(
        override val name: String,
        val uri: android.net.Uri
    ) : ModelInfo {
        override val isCustom: Boolean = true
    }
}
