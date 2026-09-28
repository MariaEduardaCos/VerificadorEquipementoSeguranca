package br.unirv.capsafe.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.unirv.capsafe.data.local.CaixaEntity
import br.unirv.capsafe.ui.components.ChipTipo
import br.unirv.capsafe.ui.components.StatusChip
import br.unirv.capsafe.ui.components.corPorClasse
import br.unirv.capsafe.ui.history.HistoryViewModel
import br.unirv.capsafe.util.AppUtils

/**
 * TELA 4 (Detalhamento Geométrico das Caixas) — RF8:
 * auditoria individual de cada Bounding Box com dimensões exatas
 * (Wpx, Hpx), centróide (Xc, Yc) e área em pixels² (RF5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxDetailsScreen(
    sessaoId: String,
    viewModel: HistoryViewModel,
    onVoltar: () -> Unit
) {
    LaunchedEffect(sessaoId) {
        viewModel.abrirDetalhes(sessaoId)
    }

    val detalhes by viewModel.detalhes.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Detecção #${sessaoId.removePrefix("sessao_")}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        detalhes.sessao?.let {
                            Text(
                                AppUtils.dataHora(it.dataHoraMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        when {
            detalhes.carregando -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.size(10.dp))
                    Text("Carregando telemetria…")
                }
            }

            detalhes.sessao == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sessão não encontrada.", style = MaterialTheme.typography.bodyLarge)
                }
            }

            else -> {
                val sessao = detalhes.sessao!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Resumo da sessão
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                LinhaInfo(
                                    icone = { Icon(Icons.Filled.Memory, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
                                    rotulo = "MODELO", valor = sessao.nomeModelo
                                )
                                LinhaInfo(
                                    icone = { Icon(Icons.Filled.Storage, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
                                    rotulo = "TOTAL CAIXAS", valor = "${sessao.totalObjetos} caixa(s) delimitadora(s)"
                                )
                                LinhaInfo(
                                    icone = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
                                    rotulo = "TEMPO INFERÊNCIA", valor = "${sessao.tempoExecucaoMs} ms"
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "PERSISTÊNCIA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (sessao.sincronizado) {
                                        StatusChip("✓ BD Servidor", ChipTipo.SUCESSO)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.CloudOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            StatusChip("Somente local", ChipTipo.ATENCAO)
                                        }
                                    }
                                }
                                LinhaInfo(
                                    icone = null,
                                    rotulo = "RESOLUÇÃO DA IMAGEM",
                                    valor = "${sessao.larguraPx} × ${sessao.alturaPx} px"
                                )
                            }
                        }
                    }

                    // Cabeçalho da telemetria
                    item {
                        Column {
                            Text(
                                "Telemetria das Caixas Delimitadoras",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Exibindo ${detalhes.caixas.size} de ${sessao.totalObjetos} — RF5: dimensões, centróide e área",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Auditoria individual de cada caixa
                    items(detalhes.caixas, key = { it.idCaixa }) { caixa ->
                        CaixaCard(caixa)
                    }

                    item { Spacer(Modifier.size(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LinhaInfo(
    icone: @Composable (() -> Unit)?,
    rotulo: String,
    valor: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icone != null) icone()
        Text(
            rotulo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = if (icone != null) 8.dp else 0.dp)
                .weight(1f)
        )
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Card de auditoria de uma caixa delimitadora (Tela 4 do wireframe). */
@Composable
private fun CaixaCard(caixa: CaixaEntity) {
    val corClasse = corPorClasse(caixa.rotuloClasse)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Caixa #${"%02d".format(caixa.idCaixa)} (${caixa.rotuloClasse})",
                    style = MaterialTheme.typography.titleSmall,
                    color = corClasse,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(
                    texto = "Confiança ${(caixa.confianca * 100).toInt()}%",
                    tipo = if (caixa.confianca >= 0.7f) ChipTipo.SUCESSO else ChipTipo.ATENCAO
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "LARGURA (W_px)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${caixa.larguraPx} px",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "ALTURA (H_px)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${caixa.alturaPx} px",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Centróide (equação 1 do enunciado)
            Card(colors = CardDefaults.cardColors(containerColor = corClasse.copy(alpha = 0.10f))) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CENTRÓIDE (Xc, Yc):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "(${caixa.centroideX}, ${caixa.centroideY}) px",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = corClasse
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ÁREA (A_px = W × H):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${caixa.areaPx2} px²",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
