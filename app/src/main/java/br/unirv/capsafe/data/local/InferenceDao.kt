package br.unirv.capsafe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InferenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirSessao(sessao: SessaoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirCaixas(caixas: List<CaixaEntity>)

    @Transaction
    suspend fun inserirSessaoComCaixas(sessao: SessaoEntity, caixas: List<CaixaEntity>) {
        inserirSessao(sessao)
        inserirCaixas(caixas)
    }

    /** RF8 — listagem cronológica do histórico. */
    @Query("SELECT * FROM sessoes_inferencia ORDER BY dataHoraMs DESC")
    fun observarHistorico(): Flow<List<SessaoEntity>>

    @Query("SELECT * FROM sessoes_inferencia WHERE idSessao = :id LIMIT 1")
    suspend fun sessaoPorId(id: String): SessaoEntity?

    /** RF8 — caixas de uma sessão (Tela 4). */
    @Query("SELECT * FROM caixas_delimitadoras WHERE idSessao = :id ORDER BY idCaixa ASC")
    suspend fun caixasDaSessao(id: String): List<CaixaEntity>

    @Query("SELECT * FROM sessoes_inferencia WHERE sincronizado = 0 ORDER BY dataHoraMs ASC")
    suspend fun sessoesNaoSincronizadas(): List<SessaoEntity>

    @Query("UPDATE sessoes_inferencia SET sincronizado = :sincronizado WHERE idSessao = :id")
    suspend fun atualizarSincronizacao(id: String, sincronizado: Boolean)
}
