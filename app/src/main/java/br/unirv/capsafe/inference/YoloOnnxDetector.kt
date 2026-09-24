package br.unirv.capsafe.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import br.unirv.capsafe.data.model.BoundingBox
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

/** Detecção bruta do modelo — coordenadas já convertidas para a imagem ORIGINAL. */
data class RawDetection(
    val classIndex: Int,
    val label: String,
    val confidence: Float,
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float
)

/**
 * RF1 + RF4 — Pipeline de inferência LOCAL (on-device) para modelos YOLOv8 ONNX.
 *
 * Fluxo (aula "Visão Computacional e Aprendizado Profundo"):
 *   Imagem → Letterbox 640×640 → CHW normalizado → ONNX Runtime → pós-processamento
 *   (filtro de confiança + NMS) → caixas na resolução original.
 *
 * Suporta os dois layouts de saída do export ultralytics:
 *   [1, 4+nc, 8400] (padrão) e [1, 8400, 4+nc].
 *
 * Executa 100% no hardware móvel (CPU por padrão; NNAPI/NPU opcional),
 * sem nenhuma dependência de nuvem.
 */
class YoloOnnxDetector(
    labelsIniciais: List<String> = emptyList(),
    private val inputSize: Int = 640,
    private val useNnapi: Boolean = false
) {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null
    private var modelName: String = ""

    /** Rótulos usados para nomear as classes de saída (na ordem do treinamento). */
    @Volatile
    var labels: List<String> = labelsIniciais
        set(value) {
            field = value
        }

    val isModelLoaded: Boolean get() = session != null
    val loadedModelName: String get() = modelName

    /** RF1 — carrega modelo embarcado nos assets (bytes já lidos pelo chamador). */
    fun loadFromBytes(bytes: ByteArray, name: String) {
        session?.close()
        session = env.createSession(bytes, buildOptions())
        modelName = name
        Log.i(TAG, "Modelo ONNX carregado: $name")
    }

    fun close() {
        session?.close()
        session = null
    }

    private fun buildOptions(): OrtSession.SessionOptions =
        OrtSession.SessionOptions().apply {
            if (useNnapi) {
                // Aceleração por NPU/GPU via NNAPI quando disponível; fallback para CPU.
                try {
                    addNnapi()
                } catch (t: Throwable) {
                    Log.w(TAG, "NNAPI indisponível, usando CPU: ${t.message}")
                }
            }
            try {
                setIntraOpNumThreads(4)
            } catch (_: Throwable) {
            }
        }

    /**
     * Executa a detecção.
     * @param confidenceThreshold limiar de confiança ajustável por Slider (RF3)
     * @param allowedLabels filtro de rótulos/classes (RF3); vazio = todas
     * @param iouThreshold limiar de IoU para o Non-Maximum Suppression
     */
    fun detect(
        source: Bitmap,
        confidenceThreshold: Float,
        iouThreshold: Float = 0.45f,
        allowedLabels: Set<String> = emptySet()
    ): List<RawDetection> {
        val sess = session ?: error("Modelo ONNX não carregado.")

        // 1. Letterbox (mantém proporção, preenchimento cinza 114)
        val scale = min(inputSize.toFloat() / source.width, inputSize.toFloat() / source.height)
        val newW = (source.width * scale).toInt().coerceAtLeast(1)
        val newH = (source.height * scale).toInt().coerceAtLeast(1)
        val padX = (inputSize - newW) / 2f
        val padY = (inputSize - newH) / 2f
        val letterboxed = letterbox(source, newW, newH)

        // 2. Pré-processamento CHW normalizado [0..1]
        val input = BitmapUtils.bitmapToChw(letterboxed, inputSize)
        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())

        // 3. Forward pass no ONNX Runtime (pesos congelados — inferência)
        OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape).use { tensor ->
            val inputName = sess.inputNames.iterator().next()
            sess.run(mapOf(inputName to tensor)).use { output ->
                // 4. Pós-processamento
                val detections = parseOutput(
                    output, source.width, source.height,
                    confidenceThreshold, scale, padX, padY
                )
                val filtradas = if (allowedLabels.isEmpty()) detections
                else detections.filter { it.label.trim().lowercase() in allowedLabels }
                return nonMaxSuppression(filtradas, iouThreshold)
            }
        }
    }

    private fun letterbox(source: Bitmap, newW: Int, newH: Int): Bitmap {
        val resized = Bitmap.createScaledBitmap(source, newW, newH, true)
        val result = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.rgb(114, 114, 114))
        canvas.drawBitmap(resized, (inputSize - newW) / 2f, (inputSize - newH) / 2f, null)
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseOutput(
        output: OrtSession.Result,
        imageWidth: Int,
        imageHeight: Int,
        confidenceThreshold: Float,
        scale: Float,
        padX: Float,
        padY: Float
    ): List<RawDetection> {
        val batch = output[0].value as? Array<*> ?: return emptyList()
        val frame = batch[0] as? Array<*> ?: return emptyList()
        if (frame.isEmpty()) return emptyList()
        val primeiraLinha = frame[0] as? FloatArray ?: return emptyList()

        // Deduz o layout a partir das dimensões do tensor
        val innerSize = primeiraLinha.size
        val channelsFirst = frame.size <= innerSize   // [dim][N] vs [N][dim]
        val dim = if (channelsFirst) frame.size else innerSize
        val count = if (channelsFirst) innerSize else frame.size
        val numClasses = dim - 4
        if (dim < 5 || numClasses <= 0) {
            Log.w(TAG, "Saída do modelo inesperada: dim=$dim")
            return emptyList()
        }

        val detections = mutableListOf<RawDetection>()

        if (channelsFirst) {
            val ch = Array(dim) { i -> frame[i] as FloatArray }
            for (i in 0 until count) {
                var bestScore = 0f
                var bestClass = -1
                for (c in 0 until numClasses) {
                    val score = ch[4 + c][i]
                    if (score > bestScore) {
                        bestScore = score
                        bestClass = c
                    }
                }
                if (bestClass >= 0 && bestScore >= confidenceThreshold) {
                    toDetection(
                        bestClass, bestScore,
                        ch[0][i], ch[1][i], ch[2][i], ch[3][i],
                        scale, padX, padY, imageWidth, imageHeight
                    )?.let { detections.add(it) }
                }
            }
        } else {
            for (i in 0 until count) {
                val row = frame[i] as FloatArray
                var bestScore = 0f
                var bestClass = -1
                for (c in 0 until numClasses) {
                    val score = row[4 + c]
                    if (score > bestScore) {
                        bestScore = score
                        bestClass = c
                    }
                }
                if (bestClass >= 0 && bestScore >= confidenceThreshold) {
                    toDetection(
                        bestClass, bestScore,
                        row[0], row[1], row[2], row[3],
                        scale, padX, padY, imageWidth, imageHeight
                    )?.let { detections.add(it) }
                }
            }
        }
        return detections
    }

    /** Converte (cx, cy, w, h) do espaço 640 de volta para a imagem original (x1,y1,x2,y2). */
    private fun toDetection(
        classIndex: Int,
        confidence: Float,
        cx: Float, cy: Float, w: Float, h: Float,
        scale: Float, padX: Float, padY: Float,
        imageWidth: Int, imageHeight: Int
    ): RawDetection? {
        val x1 = ((cx - w / 2f) - padX) / scale
        val y1 = ((cy - h / 2f) - padY) / scale
        val x2 = ((cx + w / 2f) - padX) / scale
        val y2 = ((cy + h / 2f) - padY) / scale

        val left = x1.coerceIn(0f, imageWidth.toFloat())
        val top = y1.coerceIn(0f, imageHeight.toFloat())
        val right = x2.coerceIn(0f, imageWidth.toFloat())
        val bottom = y2.coerceIn(0f, imageHeight.toFloat())
        if (right - left <= 1f || bottom - top <= 1f) return null

        return RawDetection(
            classIndex = classIndex,
            label = labels.getOrNull(classIndex) ?: "classe_${classIndex + 1}",
            confidence = confidence,
            xMin = left, yMin = top, xMax = right, yMax = bottom
        )
    }

    /** Non-Maximum Suppression — remove caixas sobrepostas do mesmo objeto. */
    private fun nonMaxSuppression(
        detections: List<RawDetection>,
        iouThreshold: Float
    ): List<RawDetection> {
        val ordenadas = detections.sortedByDescending { it.confidence }.toMutableList()
        val mantidas = mutableListOf<RawDetection>()
        while (ordenadas.isNotEmpty()) {
            val atual = ordenadas.removeAt(0)
            mantidas.add(atual)
            ordenadas.removeAll { iou(atual, it) > iouThreshold }
        }
        return mantidas
    }

    private fun iou(a: RawDetection, b: RawDetection): Float {
        val interLeft = max(a.xMin, b.xMin)
        val interTop = max(a.yMin, b.yMin)
        val interRight = min(a.xMax, b.xMax)
        val interBottom = min(a.yMax, b.yMax)
        val interW = max(0f, interRight - interLeft)
        val interH = max(0f, interBottom - interTop)
        val inter = interW * interH
        val areaA = (a.xMax - a.xMin) * (a.yMax - a.yMin)
        val areaB = (b.xMax - b.xMin) * (b.yMax - b.yMin)
        val union = areaA + areaB - inter
        return if (union <= 0f) 0f else inter / union
    }

    companion object {
        private const val TAG = "YoloOnnxDetector"
    }
}

/**
 * Anotação visual e utilitários de imagem.
 */
object BitmapUtils {

    /** Converte Bitmap para layout CHW normalizado (RGB, 0..1). */
    fun bitmapToChw(bitmap: Bitmap, size: Int): FloatArray {
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        val area = size * size
        val chw = FloatArray(3 * area)
        for (i in 0 until area) {
            val p = pixels[i]
            chw[i] = ((p shr 16) and 0xFF) / 255f          // R
            chw[area + i] = ((p shr 8) and 0xFF) / 255f    // G
            chw[2 * area + i] = (p and 0xFF) / 255f        // B
        }
        return chw
    }

    /** Desenha as caixas delimitadoras + rótulos sobre uma cópia da imagem (Tela 2). */
    fun annotate(source: Bitmap, boxes: List<BoundingBox>): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val stroke = max(4f, output.width / 220f)
        val textSize = max(28f, output.width / 26f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
        }

        boxes.forEach { box ->
            val cor = boxColorInt(box.classLabel)
            val rect = RectF(
                box.xMinPx,
                box.yMinPx,
                box.xMinPx + box.widthPx,
                box.yMinPx + box.heightPx
            )
            // Contorno
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            paint.color = cor
            canvas.drawRect(rect, paint)
            // Fundo do rótulo
            val texto = "${box.classLabel} ${(box.confidence * 100).toInt()}%"
            val larguraTexto = textPaint.measureText(texto)
            val topTexto = (rect.top - textSize * 1.5f).coerceAtLeast(0f)
            val fundo = RectF(
                rect.left,
                topTexto,
                rect.left + larguraTexto + textSize * 0.6f,
                topTexto + textSize * 1.5f
            )
            paint.style = Paint.Style.FILL
            paint.color = cor
            canvas.drawRect(fundo, paint)
            canvas.drawText(texto, rect.left + textSize * 0.3f, fundo.bottom - textSize * 0.35f, textPaint)
        }
        return output
    }

    /** Cor da caixa conforme a categoria da classe (capacete verde / violação vermelha). */
    fun boxColorInt(label: String): Int {
        val limpo = label.trim().lowercase()
        return when {
            limpo in BoundingBox.HELMET_LABELS -> Color.rgb(22, 163, 74)
            limpo in BoundingBox.VIOLATION_LABELS -> Color.rgb(220, 38, 38)
            else -> Color.rgb(217, 119, 6)
        }
    }
}
