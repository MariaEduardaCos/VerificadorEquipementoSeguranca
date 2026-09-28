package br.unirv.capsafe.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.unirv.capsafe.ui.detect.DetectScreen
import br.unirv.capsafe.ui.detect.DetectViewModel
import br.unirv.capsafe.ui.details.BoxDetailsScreen
import br.unirv.capsafe.ui.history.HistoryScreen
import br.unirv.capsafe.ui.history.HistoryViewModel
import br.unirv.capsafe.ui.result.ResultScreen

/**
 * Estrutura de navegação do enunciado (seção 7):
 * Bottom Navigation Bar com "Detectar" e "Histórico" + 4 telas.
 */
@Composable
fun CapSafeApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rotaAtual = backStackEntry?.destination?.route

    // ViewModels escopados à Activity — compartilhados entre as telas
    val detectViewModel: DetectViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = rotaAtual == Rota.DETECTAR,
                    onClick = {
                        navController.navigate(Rota.DETECTAR) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Radar, contentDescription = "Aba Detectar") },
                    label = { Text("Detectar", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = rotaAtual == Rota.HISTORICO,
                    onClick = {
                        navController.navigate(Rota.HISTORICO) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.History, contentDescription = "Aba Histórico") },
                    label = { Text("Histórico", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Rota.DETECTAR,
            modifier = Modifier.padding(padding)
        ) {
            // Tela 1 — Configuração & Entrada
            composable(Rota.DETECTAR) {
                DetectScreen(
                    viewModel = detectViewModel,
                    onInferenceCompleted = {
                        navController.navigate(Rota.RESULTADO) { launchSingleTop = true }
                    }
                )
            }
            // Tela 2 — Resultado Gráfico
            composable(Rota.RESULTADO) {
                ResultScreen(
                    viewModel = detectViewModel,
                    onNovaAnalise = {
                        navController.popBackStack(Rota.DETECTAR, inclusive = false)
                    },
                    onVerHistorico = {
                        navController.navigate(Rota.HISTORICO) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            // Tela 3 — Histórico de Inferências
            composable(Rota.HISTORICO) {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onAbrirDetalhes = { idSessao ->
                        navController.navigate("${Rota.DETALHES}/$idSessao")
                    }
                )
            }
            // Tela 4 — Detalhamento Geométrico das Caixas
            composable("${Rota.DETALHES}/{sessaoId}") { entry ->
                BoxDetailsScreen(
                    sessaoId = entry.arguments?.getString("sessaoId").orEmpty(),
                    viewModel = historyViewModel,
                    onVoltar = { navController.popBackStack() }
                )
            }
        }
    }
}

private object Rota {
    const val DETECTAR = "detectar"
    const val RESULTADO = "resultado"
    const val HISTORICO = "historico"
    const val DETALHES = "detalhes"
}
