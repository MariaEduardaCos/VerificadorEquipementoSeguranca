package br.unirv.capsafe.ui.result

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.unirv.capsafe.data.model.InferenceResult
import br.unirv.capsafe.ui.components.ChipTipo
import br.unirv.capsafe.ui.components.SectionCard
import br.unirv.capsafe.ui.components.StatCard
import br.unirv.capsafe.ui.components.StatusChip
import br.unirv.capsafe.ui.components.corPorClasse
import br.unirv.capsafe.ui.detect.DetectViewModel
import br.unirv.capsafe.ui.theme.BoxHelmetColor
import br.unirv.capsafe.ui.theme.BoxOtherColor
import br.unirv.capsafe.ui.theme.BoxViolationColor
import br.unirv.capsafe.ui.theme.ComplianceGreen
import br.unirv.capsafe.ui.theme.ComplianceGreenDark
import br.unirv.capsafe.ui.theme.ComplianceGreenLight
import br.unirv.capsafe.ui.theme.TextPrimary
import br.unirv.capsafe.ui.theme.ViolationRed
import br.unirv.capsafe.ui.theme.ViolationRedDark
import br.unirv.capsafe.ui.theme.ViolationRedLight
import br.unirv.capsafe.util.AppUtils

/**
 * TELA 2 (Resultado Gráfico): contagem total, tempo em ms, confiança média,
 * índice de conformidade de capacete e imagem anotada com as caixas (RF5).
 */
@Composable
fun ResultScreen(
    viewModel: DetectViewModel,
    onNovaAnalise: () -> Unit,
    onVerHistorico: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resultado = state.result

    if (resultado == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Nenhuma inferência executada ainda.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onNovaAnalise) { Text("Ir para Detecção") }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CabecalhoResultado(resultado, state.syncMessage)

        AlertaConformidade(resultado)

        // Estatísticas principais (RF5/RF6)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                valor = "${resultado.totalObjects}",
                titulo = "OBJETOS",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                valor = "${resultado.executionTimeMs}",
                titulo = "TEMPO (MS)",
                modifier = Modifier.weight(1f),
                corValor = MaterialTheme.colorScheme.secondary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                valor = "${(resultado.averageConfidence * 100).toInt()}%",
                titulo = "CONFIANÇA MÉDIA",
                modifier = Modifier.weight(1f),
                corValor = MaterialTheme.colorScheme.tertiary
            )
            val conformidade = resultado.complianceRate
            StatCard(
                valor = if (conformidade == null) "N/A" else "${(conformidade * 100).toInt()}%",
                titulo = "COM CAPACETE",
                modifier = Modifier.weight(1f),
                corValor = if (conformidade == null) TextPrimary
                else if (conformidade >= 1f) ComplianceGreen else ViolationRed
            )
        }

        // Contagem por classe
        val contagens = resultado.classCounts()
        if (contagens.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                contagens.forEach { (rotulo, qtd) ->
                    val cor = corPorClasse(rotulo)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.12f)),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "$rotulo: $qtd",
                            style = MaterialTheme.typography.labelLarge,
                            color = cor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Imagem anotada (toque para expandir)
        state.annotatedBitmap?.let { anotada ->
            var expandida by remember { mutableStateOf(false) }
            SectionCard(titulo = "Imagem Anotada (toque para expandir)", icone = Icons.Filled.Expand) {
                Image(
                    bitmap = anotada.asImageBitmap(),
                    contentDescription = "Imagem com caixas delimitadoras anotadas",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { expandida = true }
                )
            }
            if (expandida) {
                ZoomableImageDialog(bitmap = anotada) { expandida = false }
            }
        }

        // Payload JSON enviado ao servidor (RF6) — auditoria
        PayloadCard(resultado)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    viewModel.prepararNovaAnalise()
                    onNovaAnalise()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Nova análise") }
            Button(
                onClick = onVerHistorico,
                modifier = Modifier.weight(1f)
            ) { Text("Ver histórico") }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CabecalhoResultado(resultado: InferenceResult, syncMessage: String?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Detecção #${resultado.sessionId.removePrefix("sessao_")}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = AppUtils.dataHora(resultado.timestampMs) + " · " + resultado.modelName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Imagem: ${resultado.imageWidthPx} × ${resultado.imageHeightPx} px",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (syncMessage?.contains("Enviado") == true) {
                    StatusChip(syncMessage, ChipTipo.SUCESSO)
                } else if (syncMessage != null) {
                    StatusChip("Salvo localmente", ChipTipo.ATENCAO)
                }
            }
        }
    }
}

/** Banner de conformidade — núcleo do caso de uso "uso de capacete". */
@Composable
private fun AlertaConformidade(resultado: InferenceResult) {
    val temCapacete = resultado.helmetCount > 0
    val temViolacao = resultado.violationCount > 0
    val monitoradas = resultado.helmetCount + resultado.violationCount

    val (container, conteudo, icone, texto) = when {
        monitoradas == 0 -> Four(
            MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Filled.DataObject,
            "Nenhuma classe de capacete/cabeça entre as detecções " +
                "(${resultado.totalObjects} objetos)."
        )
        temViolacao -> Four(
            ViolationRedLight, ViolationRedDark,
            Icons.Filled.Warning,
            "⚠ ${resultado.violationCount} trabalhador(es) SEM capacete detectado(s)!"
        )
        else -> Four(
            ComplianceGreenLight, ComplianceGreenDark,
            Icons.Filled.CheckCircle,
            "✓ Todos os ${resultado.helmetCount} trabalhadores com capacete!"
        )
    }

    Card(colors = CardDefaults.cardColors(containerColor = container)) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icone, contentDescription = null, tint = conteudo)
            Spacer(Modifier.size(10.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = conteudo, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class Four<C, I, S>(val primeiro: C, val segundo: I, val terceiro: I, val quarto: S)

@Composable
private fun PayloadCard(resultado: InferenceResult) {
    val clipboard = LocalClipboardManager.current
    var expandido by remember { mutableStateOf(false) }
    SectionCard(
        titulo = "Payload JSON (RF6 — POST enviado ao backend)",
        icone = Icons.Filled.DataObject,
        acao = {
            IconButton(onClick = { clipboard.setText(AnnotatedString(resultado.toJsonPayload())) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar JSON")
            }
        }
    ) {
        Text(
            if (expandido) "contraído ▲" else "expandido ▼",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { expandido = !expandido }
        )
        val scrollV = rememberScrollState()
        val scrollH = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (expandido) 360.dp else 140.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = resultado.toJsonPayload(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                modifier = Modifier
                    .padding(10.dp)
                    .verticalScroll(scrollV)
                    .horizontalScroll(scrollH)
            )
        }
    }
}

/** Dialog fullscreen com pinch-zoom para inspecionar a imagem anotada. */
@Composable
private fun ZoomableImageDialog(bitmap: androidx.compose.ui.graphics.ImageBitmap, onDismiss: () -> Unit) {
    var escala by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        escala = (escala * zoom).coerceIn(1f, 6f)
                        offset = if (escala > 1f) offset + pan else Offset.Zero
                    }
                }
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Imagem anotada ampliada",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = escala, scaleY = escala,
                        translationX = offset.x, translationY = offset.y
                    )
            )
            Text(
                "Pinça para zoom · toque para fechar",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp)
            )
        }
    }
}
