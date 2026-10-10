package br.unirv.capsafe.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import br.unirv.capsafe.domain.model.Detection

/**
 * Anotação visual das detecções — estilo de desenho portado do AlessaApp
 * (utils/ImagePainterUtils.kt: caixas arredondadas, dimensionamento adaptativo,
 * rótulo "#N classe conf%"), ajustado ao contexto EPI do CapSafe:
 *  - capacete  → VERDE   (conformidade)
 *  - cabeça    → VERMELHO (violação — trabalhador sem capacete)
 *  - demais    → paleta por classe (âmbar/azul/ciano)
 */
object ImagePainterUtils {

    // Paleta de apoio para classes neutras (ex.: "pessoa")
    private val COLOR_PALETTE = intArrayOf(
        Color.parseColor("#D97706"), // Âmbar — pessoa / objeto neutro
        Color.parseColor("#3B82F6"), // Azul — informação
        Color.parseColor("#00B8D4"), // Ciano
        Color.parseColor("#0089A3"), // Ciano escuro
        Color.parseColor("#8B5CF6"), // Violeta
        Color.parseColor("#64748B")  // Cinza ardósia
    )

    private val HELMET_COLOR = Color.parseColor("#16A34A")     // verde — conformidade
    private val VIOLATION_COLOR = Color.parseColor("#DC2626")  // vermelho — violação

    /** Cor da caixa conforme a categoria EPI do rótulo (verde/vermelho/âmbar). */
    fun colorFor(label: String, classId: Int): Int {
        val limpo = label.trim().lowercase()
        return when {
            limpo in br.unirv.capsafe.data.model.BoundingBox.HELMET_LABELS -> HELMET_COLOR
            limpo in br.unirv.capsafe.data.model.BoundingBox.VIOLATION_LABELS -> VIOLATION_COLOR
            else -> COLOR_PALETTE[Math.abs(classId) % COLOR_PALETTE.size]
        }
    }

    fun drawDetections(originalBitmap: Bitmap, detections: List<Detection>): Bitmap {
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val imageWidth = mutableBitmap.width
        val imageHeight = mutableBitmap.height
        val minDim = Math.min(imageWidth, imageHeight).toFloat()

        // Dimensionamento adaptativo conforme a resolução da imagem
        val strokeWidth = (minDim * 0.005f).coerceAtLeast(3f)
        val textSize = (minDim * 0.035f).coerceAtLeast(24f)
        val cornerRadius = (minDim * 0.01f).coerceAtLeast(6f)

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }

        val textBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
            isFakeBoldText = true
        }

        detections.forEachIndexed { index, detection ->
            val color = colorFor(detection.label, detection.classId)
            boxPaint.color = color
            textBgPaint.color = color

            val rect = RectF(
                detection.box.x1.coerceIn(0f, imageWidth.toFloat()),
                detection.box.y1.coerceIn(0f, imageHeight.toFloat()),
                detection.box.x2.coerceIn(0f, imageWidth.toFloat()),
                detection.box.y2.coerceIn(0f, imageHeight.toFloat())
            )

            // Caixa delimitadora
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, boxPaint)

            // Texto do rótulo: "#1 capacete 95%"
            val confidencePct = (detection.score * 100).toInt()
            val labelText = "#${index + 1} ${detection.label} $confidencePct%"

            val textWidth = textPaint.measureText(labelText)
            val textMetrics = textPaint.fontMetrics
            val textHeight = textMetrics.descent - textMetrics.ascent
            val padding = strokeWidth * 2

            // Fundo do rótulo (acima da caixa ou no topo interno caso fique colado na borda superior)
            val bgTop = if (rect.top - textHeight - padding * 2 < 0) {
                rect.top
            } else {
                rect.top - textHeight - padding * 2
            }
            val bgBottom = (bgTop + textHeight + padding * 2).coerceAtMost(imageHeight.toFloat())
            val bgRight = (rect.left + textWidth + padding * 2).coerceAtMost(imageWidth.toFloat())

            val labelBgRect = RectF(rect.left, bgTop, bgRight, bgBottom)
            canvas.drawRoundRect(labelBgRect, cornerRadius / 2, cornerRadius / 2, textBgPaint)

            val textBaseline = bgTop + padding - textMetrics.ascent
            canvas.drawText(labelText, rect.left + padding, textBaseline, textPaint)
        }

        return mutableBitmap
    }
}
