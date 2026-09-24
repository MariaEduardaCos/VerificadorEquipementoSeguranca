package br.unirv.capsafe.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistência local Room — espelha o diagrama ER do enunciado (Figura 2):
 *   SESSAO_INFERENCIA (1) —— possui ——> (N) CAIXA_DELIMITADORA
 */

@Entity(tableName = "sessoes_inferencia")
data class SessaoEntity(
    @PrimaryKey val idSessao: String,
    val dataHoraMs: Long,
    val nomeModelo: String,
    val tempoExecucaoMs: Long,
    val totalObjetos: Int,
    val confiancaMedia: Float,
    val larguraPx: Int,
    val alturaPx: Int,
    val sincronizado: Boolean = false,
    val uriImagem: String? = null
)

@Entity(
    tableName = "caixas_delimitadoras",
    primaryKeys = ["idCaixa", "idSessao"],
    foreignKeys = [
        ForeignKey(
            entity = SessaoEntity::class,
            parentColumns = ["idSessao"],
            childColumns = ["idSessao"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idSessao")]
)
data class CaixaEntity(
    val idCaixa: Int,
    val idSessao: String,
    val rotuloClasse: String,
    val confianca: Float,
    val larguraPx: Float,
    val alturaPx: Float,
    val centroideX: Float,
    val centroideY: Float,
    val areaPx2: Float
)
