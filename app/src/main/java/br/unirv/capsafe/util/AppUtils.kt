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

    /** Decodifica imagem do Uri com subamostragem e correção de rotação EXIF. */
    fun decodificarImagem(context: Context, uri: Uri, maxDimensao: Int = 1920): Bitmap? {
        return try {
            val resolver = context.contentResolver

            // Leitura dos bytes uma única vez para garantir estabilidade em qualquer ContentProvider/FileProvider
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            if (bytes.isEmpty()) return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var amostra = 1
            val ladoMaior = max(bounds.outWidth, bounds.outHeight)
            if (ladoMaior > maxDimensao) {
                while (ladoMaior / (amostra * 2) >= maxDimensao) amostra *= 2
            }
            val opcoes = BitmapFactory.Options().apply {
                inSampleSize = amostra
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }
            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opcoes)
                ?: return null

            val bitmapPronto = if (rawBitmap.config == Bitmap.Config.HARDWARE) {
                rawBitmap.copy(Bitmap.Config.ARGB_8888, true) ?: rawBitmap
            } else {
                rawBitmap
            }

            rotacionarConformeExif(bytes, bitmapPronto)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotacionarConformeExif(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = java.io.ByteArrayInputStream(bytes)
            val exif = android.media.ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            val degrees = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees != 0f) {
                val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            bitmap
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
