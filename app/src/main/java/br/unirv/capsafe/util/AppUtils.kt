package br.unirv.capsafe.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

/** Utilitários de imagem (RF2) e formatação de data/hora (RF8). */
object AppUtils {

    private const val TAG = "AppUtils"

    /** Decodifica imagem do Uri com subamostragem para economizar RAM e acelerar o processamento. */
    fun decodificarImagem(context: Context, uri: Uri, maxDimensao: Int = 1280): Bitmap? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: run {
                    Log.e(TAG, "openInputStream retornou null para uri: $uri")
                    return null
                }

            if (bytes.isEmpty()) {
                Log.e(TAG, "Stream de bytes vazio para uri: $uri")
                return null
            }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                Log.e(TAG, "Dimensões inválidas: ${bounds.outWidth}x${bounds.outHeight}")
                return null
            }

            var amostra = 1
            val ladoMaior = max(bounds.outWidth, bounds.outHeight)
            while (ladoMaior / (amostra * 2) >= maxDimensao) {
                amostra *= 2
            }

            val opcoes = BitmapFactory.Options().apply {
                inSampleSize = amostra
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opcoes)
                ?: run {
                    Log.e(TAG, "Falha ao decodificar byte array para Bitmap")
                    return null
                }

            // Corrige orientação EXIF se necessário
            val orientacao = try {
                val exif = ExifInterface(ByteArrayInputStream(bytes))
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } catch (t: Throwable) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val matrix = Matrix()
            when (orientacao) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            val finalBitmap = if (!matrix.isIdentity) {
                val rotacionado = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                if (rotacionado != rawBitmap) {
                    rawBitmap.recycle()
                }
                rotacionado
            } else {
                rawBitmap
            }

            Log.i(TAG, "Imagem decodificada com sucesso: ${finalBitmap.width}x${finalBitmap.height}")
            finalBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao decodificar imagem: ${e.message}", e)
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
