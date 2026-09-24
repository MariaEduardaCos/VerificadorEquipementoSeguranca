package br.unirv.capsafe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.unirv.capsafe.ui.theme.BoxHelmetColor
import br.unirv.capsafe.ui.theme.BoxOtherColor
import br.unirv.capsafe.ui.theme.BoxViolationColor
import br.unirv.capsafe.ui.theme.ComplianceGreen
import br.unirv.capsafe.ui.theme.ComplianceGreenLight
import br.unirv.capsafe.ui.theme.ComplianceGreenDark
import br.unirv.capsafe.ui.theme.HardHatYellow
import br.unirv.capsafe.ui.theme.HardHatYellowLight
import br.unirv.capsafe.ui.theme.ViolationRed
import br.unirv.capsafe.ui.theme.ViolationRedDark
import br.unirv.capsafe.ui.theme.ViolationRedLight

/** Cartão de estatística reutilizável (Telas 2 e 3). */
@Composable
fun StatCard(
    valor: String,
    titulo: String,
    modifier: Modifier = Modifier,
    corValor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineSmall,
                color = corValor
            )
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Seção padrão com cabeçalho (título + ícone). */
@Composable
fun SectionCard(
    titulo: String,
    icone: ImageVector? = null,
    acao: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icone != null) {
                    Icon(
                        imageVector = icone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(start = if (icone != null) 8.dp else 0.dp)
                        .weight(1f)
                )
                acao?.invoke(this)
            }
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

/** Chip de status semântico (Conforme / Violação / Sincronizado / Pendente). */
@Composable
fun StatusChip(
    texto: String,
    tipo: ChipTipo,
    modifier: Modifier = Modifier
) {
    val (container, conteudo) = when (tipo) {
        ChipTipo.SUCESSO -> ComplianceGreenLight to ComplianceGreenDark
        ChipTipo.ERRO -> ViolationRedLight to ViolationRedDark
        ChipTipo.ATENCAO -> HardHatYellowLight to Color(0xFF3F2D04)
        ChipTipo.NEUTRO -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = container),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = conteudo,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

enum class ChipTipo { SUCESSO, ERRO, ATENCAO, NEUTRO }

/** Cor do tema para a categoria da classe detectada. */
@Composable
fun corPorClasse(rotulo: String): Color {
    val limpo = rotulo.trim().lowercase()
    return when {
        limpo in br.unirv.capsafe.data.model.BoundingBox.HELMET_LABELS -> BoxHelmetColor
        limpo in br.unirv.capsafe.data.model.BoundingBox.VIOLATION_LABELS -> BoxViolationColor
        else -> BoxOtherColor
    }
}
