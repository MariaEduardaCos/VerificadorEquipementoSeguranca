package br.unirv.capsafe.domain.yolo

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.util.Log
import br.unirv.capsafe.domain.model.BoundingBox
import br.unirv.capsafe.domain.model.Detection
import br.unirv.capsafe.domain.model.InferenceSummary
import br.unirv.capsafe.util.ImagePainterUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

/**
 * RF1 + RF4 — Pipeline de inferência LOCAL (on-device) para modelos YOLO ONNX.
 */
class YoloDetector(
    private val context: Context,
    labelsIniciais: List<String> = emptyList(),
    private val useNnapi: Boolean = false
) {

    private val ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null
    private var currentModelName: String = ""

    /** Rótulos usados para nomear as classes de saída (na ordem do treinamento). */
    @Volatile
    private var customLabels: List<String>? = labelsIniciais

    val isModelLoaded: Boolean get() = ortSession != null
    val loadedModelName: String get() = currentModelName

    private fun getLabelForClass(classId: Int): String {
        val labels = customLabels
        val raw = if (!labels.isNullOrEmpty()) {
            if (labels.size == 1) labels[0]
            else if (classId in labels.indices) labels[classId]
            else "classe_${classId + 1}"
        } else {
            "classe_${classId + 1}"
        }
        return formatLabel(raw)
    }

    private fun formatLabel(raw: String): String {
        val clean = raw.trim().lowercase()
        return when {
            clean in setOf("head", "cabeca", "cabeça", "sem-capacete", "no-helmet") -> "cabeça"
            clean in setOf("helmet", "capacete", "hardhat", "hard-hat") -> "capacete"
            else -> raw
        }
    }

    fun setCustomLabels(labels: List<String>) {
        this.customLabels = labels
    }

    /** RF1 — carrega modelo ONNX embarcado nos assets. */
    suspend fun loadModelFromAsset(assetName: String) = withContext(Dispatchers.IO) {
        val assetManager = context.assets
        val inputStream = assetManager.open(assetName)
        val modelBytes = inputStream.readBytes()
        inputStream.close()

        ortSession?.close()
        ortSession = ortEnvironment.createSession(modelBytes, buildOptions())
        currentModelName = assetName
    }

    /** RF1 — carrega modelo ONNX escolhido do armazenamento do dispositivo. */
    suspend fun loadModelFromUri(uri: Uri, modelName: String) = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Não foi possível abrir o arquivo do modelo")
        val tempFile = File.createTempFile("yolo_model_", ".onnx", context.cacheDir)
        tempFile.deleteOnExit()

        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()

        ortSession?.close()
        ortSession = ortEnvironment.createSession(tempFile.absolutePath, buildOptions())
        currentModelName = modelName
    }

    private fun buildOptions(): OrtSession.SessionOptions =
        OrtSession.SessionOptions().apply {
            if (useNnapi) {
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
     * RF4 — executa a detecção local e devolve o resumo com a imagem anotada.
     */
    suspend fun detect(
        bitmap: Bitmap,
        confThreshold: Float = 0.20f,
        iouThreshold: Float = 0.45f,
        allowedLabels: Set<String> = emptySet(),
        targetWidth: Int = 640,
        targetHeight: Int = 640
    ): InferenceSummary = withContext(Dispatchers.Default) {
        val session = ortSession ?: throw IllegalStateException("Nenhum modelo ONNX carregado")
        val startTime = System.currentTimeMillis()

        // 1. Pré-processamento com Letterbox (mantém proporção original sem distorcer corpos/cabeças)
        val letterbox = letterbox(bitmap, targetWidth, targetHeight)
        val floatBuffer = bitmapToFloatBuffer(letterbox.bitmap, targetWidth, targetHeight)

        // 2. Tensor de entrada
        val inputName = session.inputNames.iterator().next()
        val inputShape = longArrayOf(1, 3, targetHeight.toLong(), targetWidth.toLong())

        // 3. Forward pass + pós-processamento
        val rawDetections = OnnxTensor.createTensor(ortEnvironment, floatBuffer, inputShape).use { inputTensor ->
            session.run(mapOf(inputName to inputTensor)).use { results ->
                val outputTensor = results[0] as OnnxTensor
                parseOutputTensor(
                    outputTensor = outputTensor,
                    origWidth = bitmap.width,
                    origHeight = bitmap.height,
                    scale = letterbox.scale,
                    padX = letterbox.padX,
                    padY = letterbox.padY,
                    confThreshold = confThreshold
                )
            }
        }

        // 4. Filtro de classes (RF3) aplicado ANTES do NMS
        val filtradas = if (allowedLabels.isEmpty()) rawDetections
        else rawDetections.filter { it.label.trim().lowercase() in allowedLabels }

        // 5. Non-Maximum Suppression (NMS) com eliminação de sobreposição
        val finalDetections = applyNMS(filtradas, iouThreshold)

        val inferenceTimeMs = System.currentTimeMillis() - startTime

        // 6. Imagem anotada com a paleta EPI do CapSafe (verde/vermelho/âmbar)
        val annotatedBitmap = ImagePainterUtils.drawDetections(bitmap, finalDetections)

        // 7. Estatísticas
        val classCounts = finalDetections.groupingBy { it.label }.eachCount()
        val avgConfidence = if (finalDetections.isNotEmpty()) {
            finalDetections.map { it.score }.average().toFloat()
        } else 0f

        InferenceSummary(
            totalCount = finalDetections.size,
            avgConfidence = avgConfidence,
            detections = finalDetections,
            classCounts = classCounts,
            inferenceTimeMs = inferenceTimeMs,
            annotatedBitmap = annotatedBitmap
        )
    }

    private fun letterbox(source: Bitmap, targetWidth: Int, targetHeight: Int): LetterboxParams {
        val scale = Math.min(targetWidth.toFloat() / source.width, targetHeight.toFloat() / source.height)
        val newW = Math.round(source.width * scale).coerceAtLeast(1)
        val newH = Math.round(source.height * scale).coerceAtLeast(1)
        val padX = (targetWidth - newW) / 2f
        val padY = (targetHeight - newH) / 2f

        val resized = Bitmap.createScaledBitmap(source, newW, newH, true)
        val letterboxed = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(letterboxed)
        canvas.drawColor(Color.rgb(114, 114, 114))
        canvas.drawBitmap(resized, padX, padY, null)

        return LetterboxParams(letterboxed, scale, padX, padY)
    }

    private data class LetterboxParams(
        val bitmap: Bitmap,
        val scale: Float,
        val padX: Float,
        val padY: Float
    )

    private fun bitmapToFloatBuffer(bitmap: Bitmap, width: Int, height: Int): FloatBuffer {
        val floatBuffer = FloatBuffer.allocate(1 * 3 * height * width)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val channelStride = width * height
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (Color.red(pixel) / 255.0f)
            val g = (Color.green(pixel) / 255.0f)
            val b = (Color.blue(pixel) / 255.0f)

            floatBuffer.put(i, r)
            floatBuffer.put(channelStride + i, g)
            floatBuffer.put(channelStride * 2 + i, b)
        }
        floatBuffer.rewind()
        return floatBuffer
    }

    private fun parseOutputTensor(
        outputTensor: OnnxTensor,
        origWidth: Int,
        origHeight: Int,
        scale: Float,
        padX: Float,
        padY: Float,
        confThreshold: Float
    ): List<Detection> {
        val shape = outputTensor.info.shape
        val detections = mutableListOf<Detection>()

        val floatArray = try {
            val buf = outputTensor.floatBuffer
            val arr = FloatArray(buf.remaining())
            buf.get(arr)
            arr
        } catch (_: Exception) {
            when (val data = outputTensor.value) {
                is FloatArray -> data
                else -> flattenFloatArray(data)
            }
        }

        if (shape.size < 3) return emptyList()

        val dim1 = shape[1].toInt()
        val dim2 = shape[2].toInt()

        if (dim1 < dim2) {
            // Layout padrão YOLOv8 / YOLOv11: [1, 4 + C, 8400]
            val numChannels = dim1
            val numAnchors = dim2
            val numClasses = numChannels - 4

            for (i in 0 until numAnchors) {
                var maxClassScore = 0f
                var maxClassId = -1

                for (c in 0 until numClasses) {
                    val score = floatArray[(4 + c) * numAnchors + i]
                    if (score > maxClassScore) {
                        maxClassScore = score
                        maxClassId = c
                    }
                }

                if (maxClassScore >= confThreshold) {
                    val cx = floatArray[0 * numAnchors + i]
                    val cy = floatArray[1 * numAnchors + i]
                    val w = floatArray[2 * numAnchors + i]
                    val h = floatArray[3 * numAnchors + i]

                    val x1 = (((cx - w / 2f) - padX) / scale).coerceIn(0f, origWidth.toFloat())
                    val y1 = (((cy - h / 2f) - padY) / scale).coerceIn(0f, origHeight.toFloat())
                    val x2 = (((cx + w / 2f) - padX) / scale).coerceIn(0f, origWidth.toFloat())
                    val y2 = (((cy + h / 2f) - padY) / scale).coerceIn(0f, origHeight.toFloat())

                    if (x2 - x1 >= 2f && y2 - y1 >= 2f) {
                        detections.add(
                            Detection(
                                box = BoundingBox(x1, y1, x2, y2),
                                score = maxClassScore,
                                classId = maxClassId,
                                label = getLabelForClass(maxClassId)
                            )
                        )
                    }
                }
            }
        } else {
            // Layout transposto: [1, 8400, 4 + C] ou YOLOv5: [1, 25200, 5 + C]
            val numAnchors = dim1
            val numChannels = dim2
            val isYolov5 = numChannels > 5 && (numChannels - 5) <= 1000

            for (i in 0 until numAnchors) {
                val offset = i * numChannels
                val cx = floatArray[offset + 0]
                val cy = floatArray[offset + 1]
                val w = floatArray[offset + 2]
                val h = floatArray[offset + 3]

                var maxClassScore = 0f
                var maxClassId = -1

                if (isYolov5) {
                    val objectness = floatArray[offset + 4]
                    val numClasses = numChannels - 5
                    for (c in 0 until numClasses) {
                        val score = objectness * floatArray[offset + 5 + c]
                        if (score > maxClassScore) {
                            maxClassScore = score
                            maxClassId = c
                        }
                    }
                } else {
                    val numClasses = numChannels - 4
                    for (c in 0 until numClasses) {
                        val score = floatArray[offset + 4 + c]
                        if (score > maxClassScore) {
                            maxClassScore = score
                            maxClassId = c
                        }
                    }
                }

                if (maxClassScore >= confThreshold) {
                    val x1 = (((cx - w / 2f) - padX) / scale).coerceIn(0f, origWidth.toFloat())
                    val y1 = (((cy - h / 2f) - padY) / scale).coerceIn(0f, origHeight.toFloat())
                    val x2 = (((cx + w / 2f) - padX) / scale).coerceIn(0f, origWidth.toFloat())
                    val y2 = (((cy + h / 2f) - padY) / scale).coerceIn(0f, origHeight.toFloat())

                    if (x2 - x1 >= 2f && y2 - y1 >= 2f) {
                        detections.add(
                            Detection(
                                box = BoundingBox(x1, y1, x2, y2),
                                score = maxClassScore,
                                classId = maxClassId,
                                label = getLabelForClass(maxClassId)
                            )
                        )
                    }
                }
            }
        }

        return detections
    }

    private fun flattenFloatArray(array: Any?): FloatArray {
        val result = mutableListOf<Float>()
        fun recurse(element: Any?) {
            when (element) {
                is FloatArray -> element.forEach { result.add(it) }
                is Array<*> -> element.forEach { recurse(it) }
            }
        }
        recurse(array)
        return result.toFloatArray()
    }

    private fun applyNMS(detections: List<Detection>, iouThreshold: Float): List<Detection> {
        val sortedDetections = detections.sortedByDescending { it.score }
        val selectedDetections = mutableListOf<Detection>()

        val active = BooleanArray(sortedDetections.size) { true }

        for (i in sortedDetections.indices) {
            if (!active[i]) continue
            val current = sortedDetections[i]
            selectedDetections.add(current)

            for (j in i + 1 until sortedDetections.size) {
                if (!active[j]) continue
                val candidate = sortedDetections[j]

                if (isOverlapping(current.box, candidate.box, iouThreshold)) {
                    active[j] = false
                }
            }
        }

        return selectedDetections
    }

    private fun isOverlapping(box1: BoundingBox, box2: BoundingBox, iouThreshold: Float): Boolean {
        val x1 = Math.max(box1.x1, box2.x1)
        val y1 = Math.max(box1.y1, box2.y1)
        val x2 = Math.min(box1.x2, box2.x2)
        val y2 = Math.min(box1.y2, box2.y2)

        val intersectionArea = Math.max(0f, x2 - x1) * Math.max(0f, y2 - y1)
        if (intersectionArea <= 0f) return false

        val box1Area = (box1.x2 - box1.x1) * (box1.y2 - box1.y1)
        val box2Area = (box2.x2 - box2.x1) * (box2.y2 - box2.y1)

        val unionArea = box1Area + box2Area - intersectionArea
        val minArea = Math.min(box1Area, box2Area)

        val iou = if (unionArea <= 0f) 0f else intersectionArea / unionArea
        val iom = if (minArea <= 0f) 0f else intersectionArea / minArea

        // Suprime caixa candidata se IoU > 0.35 ou se IoM > 0.35 (sobreposição forte do mesmo alvo)
        return iou > Math.min(iouThreshold, 0.35f) || iom > 0.35f
    }

    fun close() {
        ortSession?.close()
        ortSession = null
    }

    companion object {
        private const val TAG = "YoloDetector"
    }
}
