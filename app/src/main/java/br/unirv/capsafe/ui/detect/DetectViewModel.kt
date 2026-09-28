package br.unirv.capsafe.ui.detect

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.unirv.capsafe.data.AppPrefs
import br.unirv.capsafe.data.ServiceLocator
import br.unirv.capsafe.data.model.BoundingBox
import br.unirv.capsafe.data.model.InferenceResult
import br.unirv.capsafe.data.SyncOutcome
import br.unirv.capsafe.inference.BitmapUtils
import br.unirv.capsafe.inference.YoloOnnxDetector
import br.unirv.capsafe.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * RF9 — ViewModel da Tela 1/2 com MVVM, Coroutines e StateFlow.
 * Orquestra: modelo ONNX (RF1), aquisição de imagem (RF2), parâmetros (RF3),
 * inferência local (RF4), métricas geométricas (RF5) e envio ao servidor (RF6).
 */
class DetectViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServiceLocator.repository(application)
    private var detector: YoloOnnxDetector? = null

    data class DetectUiState(
        val modelLoaded: Boolean = false,
        val modelName: String = "",
        val modelError: String? = null,
        val labels: List<String> = emptyList(),
        val customLabelsText: String = "",
        val imageUri: Uri? = null,
        val imageBitmap: Bitmap? = null,
        val confidence: Float = AppPrefs.DEFAULT_CONFIDENCE,
        val selectedLabels: Set<String> = emptySet(),
        val serverUrl: String = AppPrefs.DEFAULT_SERVER,
        val isRunning: Boolean = false,
        val errorMessage: String? = null,
        val result: InferenceResult? = null,
        val annotatedBitmap: Bitmap? = null,
        val syncMessage: String? = null
    )

    private val _uiState = MutableStateFlow(
        DetectUiState(
            confidence = AppPrefs.confidence(application),
            serverUrl = AppPrefs.serverUrl(application),
            customLabelsText = AppPrefs.customLabels(application) ?: ""
        )
    )
    val uiState: StateFlow<DetectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            carregarModeloEmbarcado()
        }
    }

    /** RF1 — carrega o modelo embarcado em assets/best.onnx (se existir). */
    private suspend fun carregarModeloEmbarcado() {
        val context = getApplication<Application>()
        try {
            val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
            val rotulos = resolverRotulos(context)
            val novoDetector = YoloOnnxDetector(rotulos).apply { loadFromBytes(bytes, MODEL_ASSET) }
            detector = novoDetector
            _uiState.update {
                it.copy(
                    modelLoaded = true,
                    modelName = MODEL_ASSET,
                    modelError = null,
                    labels = rotulos
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    modelLoaded = false,
                    modelName = "",
                    modelError = "Modelo ONNX não encontrado em assets/$MODEL_ASSET. " +
                        "Coloque o arquivo ou use \"Selecionar modelo\".",
                    labels = resolverRotulos(context)
                )
            }
        }
    }

    /** Ordem de prioridade: rótulos digitados → labels.txt → padrão do CapSafe. */
    private fun resolverRotulos(context: Application): List<String> {
        val custom = AppPrefs.customLabels(context)
        if (!custom.isNullOrBlank()) {
            val lista = custom.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (lista.isNotEmpty()) return lista
        }
        try {
            val txt = context.assets.open(LABELS_ASSET).bufferedReader().use { it.readText() }
            val lista = txt.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lista.isNotEmpty()) return lista
        } catch (_: Exception) {
        }
        return DEFAULT_LABELS
    }

    /** RF1 — usuário seleciona um .onnx do armazenamento do dispositivo. */
    fun onModelSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                val destino = java.io.File(context.cacheDir, "modelos/${System.currentTimeMillis()}_modelo.onnx")
                destino.parentFile?.mkdirs()
                context.contentResolver.openInputStream(uri)?.use { entrada ->
                    destino.outputStream().use { saida -> entrada.copyTo(saida) }
                } ?: error("Não foi possível ler o arquivo selecionado.")
                val bytes = destino.readBytes()
                val rotulos = resolverRotulos(context)
                val novoDetector = (detector ?: YoloOnnxDetector(rotulos)).apply {
                    labels = rotulos
                    loadFromBytes(bytes, destino.name)
                }
                detector = novoDetector
                _uiState.update {
                    it.copy(
                        modelLoaded = true,
                        modelName = destino.name,
                        modelError = null,
                        labels = rotulos
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(modelError = "Falha ao carregar modelo: ${e.message}") }
            }
        }
    }

    /** RF2 — imagem da câmera ou galeria. */
    fun onImagePicked(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val bitmap = AppUtils.decodificarImagem(context, uri)
            withContext(Dispatchers.Main) {
                if (bitmap == null) {
                    _uiState.update { it.copy(errorMessage = "Não foi possível abrir a imagem.") }
                } else {
                    _uiState.update {
                        it.copy(
                            imageUri = uri,
                            imageBitmap = bitmap,
                            errorMessage = null,
                            result = null,
                            annotatedBitmap = null,
                            syncMessage = null
                        )
                    }
                }
            }
        }
    }

    /** RF3 — Slider de limiar de confiança. */
    fun onConfidenceChange(valor: Float) {
        AppPrefs.setConfidence(getApplication(), valor)
        _uiState.update { it.copy(confidence = valor) }
    }

    /** RF3 — filtro de rótulos/classes (conjunto vazio = todas). */
    fun toggleClass(label: String) {
        _uiState.update { estado ->
            val novo = if (label in estado.selectedLabels) {
                estado.selectedLabels - label
            } else {
                estado.selectedLabels + label
            }
            estado.copy(selectedLabels = novo)
        }
    }

    fun selecionarTodasClasses() {
        _uiState.update { it.copy(selectedLabels = emptySet()) }
    }

    fun onServerUrlChange(url: String) {
        AppPrefs.setServerUrl(getApplication(), url)
        _uiState.update { it.copy(serverUrl = url) }
    }

    /** Rótulos personalizados separados por vírgula (Tela 1). */
    fun onCustomLabelsChange(texto: String) {
        AppPrefs.setCustomLabels(getApplication(), texto)
        val context = getApplication<Application>()
        val rotulos = if (texto.isBlank()) resolverRotulosCustom()
        else texto.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        detector?.labels = rotulos
        _uiState.update { it.copy(customLabelsText = texto, labels = rotulos) }
    }

    private fun resolverRotulosCustom(): List<String> {
        val custom = AppPrefs.customLabels(getApplication())
        return if (custom.isNullOrBlank()) _uiState.value.labels
        else custom.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** RF4 — pipeline de inferência local + RF5 métricas + RF6 envio. */
    fun executarInferencia() {
        val estado = _uiState.value
        val bitmap = estado.imageBitmap
        if (!estado.modelLoaded || detector == null) {
            _uiState.update { it.copy(errorMessage = "Carregue um modelo ONNX antes de executar.") }
            return
        }
        if (bitmap == null) {
            _uiState.update { it.copy(errorMessage = "Selecione uma imagem (câmera ou galeria).") }
            return
        }
        _uiState.update { it.copy(isRunning = true, errorMessage = null, syncMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val det = detector ?: error("Modelo não carregado.")
                val inicio = SystemClock.elapsedRealtime()
                val detections = det.detect(
                    source = bitmap,
                    confidenceThreshold = estado.confidence,
                    iouThreshold = 0.45f,
                    allowedLabels = estado.selectedLabels
                )
                val tempoMs = SystemClock.elapsedRealtime() - inicio

                // RF5 — caixas na resolução original, ordenadas por confiança
                val boxes = detections
                    .sortedByDescending { it.confidence }
                    .mapIndexed { indice, raw ->
                        BoundingBox(
                            boxId = indice + 1,
                            classLabel = raw.label,
                            confidence = raw.confidence,
                            xMinPx = raw.xMin,
                            yMinPx = raw.yMin,
                            widthPx = raw.xMax - raw.xMin,
                            heightPx = raw.yMax - raw.yMin
                        )
                    }

                val resultado = InferenceResult(
                    sessionId = InferenceResult.novoIdSessao(),
                    timestampMs = System.currentTimeMillis(),
                    modelName = det.loadedModelName.ifEmpty { estado.modelName },
                    executionTimeMs = tempoMs,
                    imageWidthPx = bitmap.width,
                    imageHeightPx = bitmap.height,
                    boundingBoxes = boxes
                )

                val anotada = BitmapUtils.annotate(bitmap, boxes)

                // RF6 — envio automático POST JSON ao backend
                val sync = repository.salvarInferencia(
                    resultado,
                    estado.imageUri?.toString(),
                    estado.serverUrl
                )
                val mensagemSync = when (sync) {
                    is SyncOutcome.Sent -> "✓ Enviado ao servidor backend"
                    is SyncOutcome.Failed -> "Servidor indisponível (${sync.motivo}) — salvo localmente"
                }

                _uiState.update {
                    it.copy(
                        isRunning = false,
                        result = resultado,
                        annotatedBitmap = anotada,
                        syncMessage = mensagemSync
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isRunning = false, errorMessage = e.message ?: "Erro na inferência.")
                }
            }
        }
    }

    /** Prepara a Tela 1 para uma nova análise. */
    fun prepararNovaAnalise() {
        _uiState.update {
            it.copy(result = null, annotatedBitmap = null, syncMessage = null)
        }
    }

    fun limparErro() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MODEL_ASSET = "best.onnx"
        const val LABELS_ASSET = "labels.txt"
        val DEFAULT_LABELS = listOf("capacete", "cabeca", "pessoa")
    }
}
