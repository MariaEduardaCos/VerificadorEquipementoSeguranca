package br.unirv.capsafe.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

/** Utilitários de imagem (RF2) e formatação de data/hora (RF8). */
object AppUtils {

    /** Decodifica imagem do Uri com subamostragem para controlar memória. */
    fun decodificarImagem(context: Context, uri: Uri, maxDimensao: Int = 1920): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return null

            var amostra = 1
            val ladoMaior = max(bounds.outWidth, bounds.outHeight)
            if (ladoMaior > maxDimensao) {
                while (ladoMaior / (amostra * 2) >= maxDimensao) amostra *= 2
            }
            val opcoes = BitmapFactory.Options().apply {
                inSampleSize = amostra
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opcoes)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** "18/09/2026 às 23:06:12" (RF8 — listagem cronológica). */
    fun dataHora(ms: Long): String = Instant.ofEpochMilli(ms)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss"))

    /** "18/09/2026 23:06" (versão curta). */
    fun dataHoraCurta(ms: Long): String = Instant.ofEpochMilli(ms)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

    /** "18/09/2026" apenas data. */
    fun data(ms: Long): String = Instant.ofEpochMilli(ms)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}
