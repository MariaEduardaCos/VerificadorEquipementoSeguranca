package br.unirv.capsafe.data.model

/**
 * DTOs usados pelo Retrofit (RF6/RF7) — campos com nomes idênticos ao
 * payload de referência do enunciado (snake_case) para serialização Gson direta.
 */
data class DimensaoImagemDto(
    val largura_px: Int = 0,
    val altura_px: Int = 0
)

data class CaixaDto(
    val id_caixa: Int = 0,
    val rotulo_classe: String = "",
    val confianca: Float = 0f,
    val largura_px: Float = 0f,
    val altura_px: Float = 0f,
    val centroide_x: Float = 0f,
    val centroide_y: Float = 0f,
    val area_px2: Float = 0f
)

data class SessaoDto(
    val id_sessao: String = "",
    val data_hora: String = "",
    val nome_modelo: String = "",
    val tempo_execucao_ms: Long = 0L,
    val total_objetos: Int = 0,
    val confianca_media: Float = 0f,
    val dimensao_imagem: DimensaoImagemDto = DimensaoImagemDto(),
    val caixas_delimitadoras: List<CaixaDto> = emptyList(),
    val sincronizado: Boolean = true
)

data class RespostaPostDto(
    val mensagem: String? = null,
    val id_sessao: String? = null,
    val total_caixas: Int? = null
)
