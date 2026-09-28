package br.unirv.capsafe.ui.result

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.unirv.capsafe.data.model.InferenceResult
import br.unirv.capsafe.ui.components.ChipTipo
import br.unirv.capsafe.ui.components.SectionCard
import br.unirv.capsafe.ui.components.StatusChip
import br.unirv.capsafe.ui.components.corPorClasse
import br.unirv.capsafe.ui.detect.DetectViewModel
import br.unirv.capsafe.ui.theme.ComplianceGreen
import br.unirv.capsafe.ui.theme.ComplianceGreenDark
import br.unirv.capsafe.ui.theme.ComplianceGreenLight
import br.unirv.capsafe.ui.theme.TextPrimary
import br.unirv.capsafe.ui.theme.ViolationRed
import br.unirv.capsafe.ui.theme.ViolationRedDark
import br.unirv.capsafe.ui.theme.ViolationRedLight
import br.unirv.capsafe.util.AppUtils

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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CabecalhoResultado(resultado, state.syncMessage)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${resultado.totalObjects}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(resultado.averageConfidence * 100).toInt()}%",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }
        }

        Text(
            text = "Tempo de execução: ${resultado.executionTimeMs} ms (Local NPU/CPU)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        val contagens = resultado.classCounts()
        if (contagens.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                contagens.forEach { (rotulo, qtd) ->
                    val rotuloExibicao = br.unirv.capsafe.data.model.BoundingBox.friendlyLabel(rotulo)
                    val cor = corPorClasse(rotuloExibicao)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "$rotuloExibicao: $qtd",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        state.annotatedBitmap?.let { anotada ->
            var expandida by remember { mutableStateOf(false) }
            SectionCard(titulo = "Imagem Anotada (Toque para expandir)", icone = Icons.Filled.Expand) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expandida = true },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = anotada.asImageBitmap(),
                        contentDescription = "Imagem com caixas delimitadoras anotadas",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            if (expandida) {
                ZoomableImageDialog(bitmap = anotada.asImageBitmap()) { expandida = false }
            }
        }

        AlertaConformidade(resultado)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.prepararNovaAnalise()
                    onNovaAnalise()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Nova análise", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onVerHistorico,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Ver histórico", fontWeight = FontWeight.SemiBold)
            }
        }

        PayloadCard(resultado)

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CabecalhoResultado(resultado: InferenceResult, syncMessage: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "CapSafe — Detector de Infrações de EPI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Detecção #${resultado.sessionId.removePrefix("sessao_")} · ${AppUtils.dataHora(resultado.timestampMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Computer,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Local",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (syncMessage?.contains("Enviado") == true) {
                Spacer(Modifier.width(6.dp))
                StatusChip("✓ Servidor", ChipTipo.SUCESSO)
            }
        }
    }
}

private data class BannerInfo(
    val container: Color,
    val conteudo: Color,
    val icone: ImageVector,
    val texto: String
)

@Composable
private fun AlertaConformidade(resultado: InferenceResult) {
    val temCapacete = resultado.helmetCount > 0
    val temViolacao = resultado.violationCount > 0

    val info = when {
        temViolacao -> BannerInfo(
            ViolationRedLight, ViolationRedDark,
            Icons.Filled.Warning,
            "⚠ Alerta: ${resultado.violationCount} trabalhador(es) SEM capacete detectado(s)!"
        )
        temCapacete -> BannerInfo(
            ComplianceGreenLight, ComplianceGreenDark,
            Icons.Filled.CheckCircle,
            "✓ Conforme: Todos os ${resultado.helmetCount} trabalhadores com capacete!"
        )
        resultado.totalObjects == 0 -> BannerInfo(
            ComplianceGreenLight, ComplianceGreenDark,
            Icons.Filled.CheckCircle,
            "✓ Conforme: Nenhuma cabeça desprotegida detectada!"
        )
        else -> BannerInfo(
            MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Filled.DataObject,
            "Nenhuma infração detectada (${resultado.totalObjects} objeto(s) analisado(s))."
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = info.container),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(info.icone, contentDescription = null, tint = info.conteudo)
            Spacer(Modifier.size(10.dp))
            Text(
                info.texto,
                style = MaterialTheme.typography.bodyMedium,
                color = info.conteudo,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PayloadCard(resultado: InferenceResult) {
    val clipboard = LocalClipboardManager.current
    var expandido by remember { mutableStateOf(false) }
    SectionCard(
        titulo = "Payload JSON (RF6 — Auditoria REST)",
        icone = Icons.Filled.DataObject,
        acao = {
            IconButton(onClick = { clipboard.setText(AnnotatedString(resultado.toJsonPayload())) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar JSON")
            }
        }
    ) {
        Text(
            if (expandido) "Ocultar dados ▲" else "Exibir payload JSON ▼",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { expandido = !expandido }
        )
        if (expandido) {
            val scrollV = rememberScrollState()
            val scrollH = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(8.dp))
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
}

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
                "Pinça para zoom · Toque para fechar",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp)
            )
        }
    }
}
