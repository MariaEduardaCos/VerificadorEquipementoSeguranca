package br.unirv.capsafe.ui.detect

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.unirv.capsafe.ui.components.SectionCard
import br.unirv.capsafe.ui.components.StatusChip
import br.unirv.capsafe.ui.components.ChipTipo
import br.unirv.capsafe.ui.theme.ComplianceGreen
import br.unirv.capsafe.ui.theme.ViolationRed
import java.io.File

/**
 * TELA 1 (Configuração & Entrada):
 *  - Escolha do modelo ONNX (RF1 — embarcado ou do armazenamento)
 *  - Câmera / Galeria (RF2)
 *  - Slider de Limiar de Confiança + filtro de classes (RF3)
 *  - Botão "Executar Inferência" (RF4)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectScreen(
    viewModel: DetectViewModel,
    onInferenceCompleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(viewModel::onImagePicked) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { sucesso ->
        val uri = pendingCameraUri
        if (sucesso && uri != null) viewModel.onImagePicked(uri)
    }

    val modelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::onModelSelected) }

    // Navega automaticamente para a Tela 2 quando a inferência termina
    var ultimoNavegado by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.result?.sessionId) {
        val id = state.result?.sessionId
        if (id != null && id != ultimoNavegado) {
            ultimoNavegado = id
            onInferenceCompleted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CabecalhoCapSafe()

        // ---------- Modelo ONNX (RF1) ----------
        SectionCard(titulo = "Modelo YOLO (ONNX)", icone = Icons.Filled.Memory) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.modelName.ifEmpty { "Nenhum modelo carregado" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                when {
                    state.modelLoaded -> StatusChip("✓ Ativo", ChipTipo.SUCESSO)
                    state.modelError != null -> StatusChip("✗ Erro", ChipTipo.ERRO)
                    else -> StatusChip("Carregando…", ChipTipo.ATENCAO)
                }
            }
            if (state.modelError != null) {
                Text(
                    text = state.modelError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = ViolationRed
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        modelLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Selecionar modelo", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { showHelp = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Como exportar", maxLines = 1)
                }
            }
            OutlinedTextField(
                value = state.customLabelsText,
                onValueChange = viewModel::onCustomLabelsChange,
                label = { Text("Rótulos personalizados (separados por vírgula)") },
                placeholder = { Text(state.labels.joinToString(", ").ifEmpty { "capacete, cabeca, pessoa" }) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Ordem dos rótulos deve ser a MESMA do treinamento. Vazio = usar labels.txt.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---------- Conexão com o backend (RF6) ----------
        SectionCard(titulo = "Servidor Backend", icone = Icons.Filled.CheckCircle) {
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onServerUrlChange,
                label = { Text("URL da API REST") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
            Text(
                text = "No emulador use http://10.0.2.2:3030/ (localhost do host). " +
                    "Em dispositivo físico use o IP da rede local.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---------- Aquisição de imagem (RF2) + parâmetros (RF3) ----------
        SectionCard(titulo = "Capturar ou Selecionar Imagem", icone = Icons.Filled.CameraAlt) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                        val arquivo = File(dir, "captura_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", arquivo
                        )
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Tirar Foto")
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Galeria")
                }
            }

            // Slider de confiança (RF3)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Limiar de Confiança",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${(state.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = state.confidence,
                    onValueChange = viewModel::onConfidenceChange,
                    valueRange = 0.05f..0.95f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Filtro de classes (RF3)
            if (state.labels.isNotEmpty()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "  Filtro de classes (nenhum = todas)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedLabels.isEmpty(),
                                onClick = viewModel::selecionarTodasClasses,
                                label = { Text("Todas") }
                            )
                        }
                        items(state.labels) { rotulo ->
                            FilterChip(
                                selected = rotulo in state.selectedLabels,
                                onClick = { viewModel.toggleClass(rotulo) },
                                label = { Text(rotulo) }
                            )
                        }
                    }
                }
            }
        }

        // ---------- Pré-visualização ----------
        state.imageBitmap?.let { bitmap ->
            SectionCard(titulo = "Pré-visualização da imagem", icone = Icons.Filled.Engineering) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Imagem selecionada para inferência",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                Text(
                    text = "${bitmap.width} × ${bitmap.height} px",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------- Executar (RF4) ----------
        Button(
            onClick = viewModel::executarInferencia,
            enabled = state.modelLoaded && state.imageBitmap != null && !state.isRunning,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (state.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.size(10.dp))
                Text("Processando…")
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Executar Inferência", style = MaterialTheme.typography.labelLarge)
            }
        }

        state.errorMessage?.let { erro ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ViolationRed.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Error, contentDescription = null, tint = ViolationRed)
                    Spacer(Modifier.size(8.dp))
                    Text(erro, style = MaterialTheme.typography.bodySmall, color = ViolationRed)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    if (showHelp) {
        ExportHelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun CabecalhoCapSafe() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Engineering,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                "CapSafe",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Verificação de capacete · Inferência offline",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Dialog com as instruções de exportação ONNX (aula: best.pt → best.onnx). */
@Composable
private fun ExportHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exportar best.pt → ONNX") },
        text = {
            Column {
                Text(
                    "No ambiente Python (Ultralytics):",
                    style = MaterialTheme.typography.bodySmall
                )
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        "from ultralytics import YOLO\n" +
                            "model = YOLO('weights/best.pt')\n" +
                            "model.export(format='onnx', dynamic=True, simplify=True)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Text(
                    "\n1. Copie o best.onnx para app/src/main/assets/best.onnx; ou\n" +
                        "2. Use \"Selecionar modelo\" e aponte para o arquivo.\n" +
                        "3. Os rótulos seguem a ordem de classes do treino.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendi") }
        }
    )
}
