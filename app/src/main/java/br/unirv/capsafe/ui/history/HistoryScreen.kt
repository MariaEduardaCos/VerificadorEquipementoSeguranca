package br.unirv.capsafe.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.unirv.capsafe.data.local.SessaoEntity
import br.unirv.capsafe.ui.components.ChipTipo
import br.unirv.capsafe.ui.components.StatCard
import br.unirv.capsafe.ui.components.StatusChip
import br.unirv.capsafe.ui.theme.ComplianceGreen
import br.unirv.capsafe.ui.theme.HardHatYellow
import br.unirv.capsafe.util.AppUtils

/**
 * TELA 3 (Histórico de Inferências) — RF8:
 * listagem cronológica com data/hora, modelo, quantidade de objetos,
 * tempo e indicador de envio ao servidor.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onAbrirDetalhes: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mensagem by viewModel.mensagem.collectAsStateWithLifecycle()

    LaunchedEffect(mensagem) {
        if (mensagem != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.limparMensagem()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text("Histórico de Inferências", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Persistido no Room + Sincronizado com o Servidor Backend",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Estatísticas (wireframe Tela 3)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                valor = "${state.totalSessoes}",
                titulo = "INFERÊNCIAS",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                valor = "${state.totalObjetos}",
                titulo = "OBJETOS CONTADOS",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                valor = "${state.mediaTempoMs}",
                titulo = "MÉDIA TEMPO (MS)",
                modifier = Modifier.weight(1f),
                corValor = MaterialTheme.colorScheme.secondary
            )
        }

        // Ações de sincronização (RF6/RF7)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::sincronizarPendentes, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Enviar pendentes (${state.pendentes})")
            }
            OutlinedButton(onClick = viewModel::importarDoServidor, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Importar do servidor")
            }
        }

        mensagem?.let { msg ->
            StatusChip(
                if (msg.startsWith("✓")) msg else msg,
                if (msg.startsWith("✓")) ChipTipo.SUCESSO else ChipTipo.ATENCAO
            )
        }

        OutlinedTextField(
            value = state.filtro,
            onValueChange = viewModel::onFiltroChange,
            placeholder = { Text("Filtrar por data, sessão ou modelo…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Text(
            "Registros Recentes (toque para detalhes)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state.carregando) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (state.sessoes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Nenhuma inferência registrada ainda.\nExecute uma análise na aba Detectar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.sessoes, key = { it.idSessao }) { sessao ->
                    SessaoCard(sessao = sessao, onClick = { onAbrirDetalhes(sessao.idSessao) })
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }
    }
}

@Composable
private fun SessaoCard(sessao: SessaoEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Sessão #${sessao.idSessao.removePrefix("sessao_")}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = AppUtils.dataHoraCurta(sessao.dataHoraMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(sessao.nomeModelo, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Memory, contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("Total: ${sessao.totalObjetos} objs") },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("${sessao.tempoExecucaoMs} ms") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Schedule, contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sessao.sincronizado) {
                    StatusChip("✓ Enviado ao Servidor", ChipTipo.SUCESSO)
                } else {
                    StatusChip("Pendente de envio", ChipTipo.ATENCAO)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClick) {
                    Text("Ver detalhes →", fontWeight = FontWeight.SemiBold, color = ComplianceGreen)
                }
            }
        }
    }
}
