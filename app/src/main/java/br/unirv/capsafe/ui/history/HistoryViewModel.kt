package br.unirv.capsafe.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.unirv.capsafe.data.AppPrefs
import br.unirv.capsafe.data.ServiceLocator
import br.unirv.capsafe.data.local.CaixaEntity
import br.unirv.capsafe.data.local.SessaoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * RF8 — ViewModel das Telas 3 (Histórico) e 4 (Detalhamento Geométrico).
 * MVVM + Coroutines + StateFlow (RF9).
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServiceLocator.repository(application)

    data class HistoryUiState(
        val sessoes: List<SessaoEntity> = emptyList(),
        val totalSessoes: Int = 0,
        val totalObjetos: Int = 0,
        val mediaTempoMs: Long = 0L,
        val pendentes: Int = 0,
        val filtro: String = "",
        val carregando: Boolean = true
    )

    private val filtro = MutableStateFlow("")
    private val mensagemInterna = MutableStateFlow<String?>(null)
    private val ocupado = MutableStateFlow(false)

    val uiState: StateFlow<HistoryUiState> =
        combine(repository.observarHistorico(), filtro, ocupado) { sessoes, textoFiltro, busy ->
            val texto = textoFiltro.trim().lowercase()
            val filtradas = if (texto.isEmpty()) sessoes else sessoes.filter { sessao ->
                sessao.idSessao.lowercase().contains(texto) ||
                    sessao.nomeModelo.lowercase().contains(texto) ||
                    br.unirv.capsafe.util.AppUtils.dataHora(sessao.dataHoraMs).contains(textoFiltro)
            }
            HistoryUiState(
                sessoes = filtradas,
                totalSessoes = sessoes.size,
                totalObjetos = sessoes.sumOf { it.totalObjetos },
                mediaTempoMs = if (sessoes.isEmpty()) 0L
                else (sessoes.sumOf { it.tempoExecucaoMs }.toDouble() / sessoes.size).toLong(),
                pendentes = sessoes.count { !it.sincronizado },
                filtro = textoFiltro,
                carregando = busy && sessoes.isEmpty()
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )

    val mensagem: StateFlow<String?> = mensagemInterna.asStateFlow()

    fun onFiltroChange(texto: String) {
        filtro.value = texto
    }

    /** RF7 — reenvia as sessões que não conseguiram ser enviadas ao servidor. */
    fun sincronizarPendentes() {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            ocupado.value = true
            try {
                val enviadas = repository.sincronizarPendentes(AppPrefs.serverUrl(context))
                mensagemInterna.value = if (enviadas > 0) "✓ $enviadas sessão(ões) enviada(s) ao servidor"
                else "Nenhuma sessão pôde ser enviada — verifique o servidor"
            } catch (e: Exception) {
                mensagemInterna.value = "Erro ao sincronizar: ${e.message}"
            } finally {
                ocupado.value = false
            }
        }
    }

    /** RF7 — importa o histórico persistido no servidor para exibição local. */
    fun importarDoServidor() {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            ocupado.value = true
            try {
                val importadas = repository.importarDoServidor(AppPrefs.serverUrl(context))
                mensagemInterna.value = "✓ $importadas sessão(ões) carregada(s) do servidor"
            } catch (e: Exception) {
                mensagemInterna.value = "Servidor indisponível: ${e.message}"
            } finally {
                ocupado.value = false
            }
        }
    }

    fun limparMensagem() {
        mensagemInterna.value = null
    }

    // ---------- Tela 4 (Detalhamento) ----------

    data class DetalhesUiState(
        val carregando: Boolean = true,
        val sessao: SessaoEntity? = null,
        val caixas: List<CaixaEntity> = emptyList()
    )

    private val _detalhes = MutableStateFlow(DetalhesUiState())
    val detalhes: StateFlow<DetalhesUiState> = _detalhes.asStateFlow()

    fun abrirDetalhes(idSessao: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _detalhes.value = DetalhesUiState(carregando = true)
            val (sessao, caixas) = repository.detalhesDaSessao(idSessao)
            _detalhes.value = DetalhesUiState(carregando = false, sessao = sessao, caixas = caixas)
        }
    }
}
