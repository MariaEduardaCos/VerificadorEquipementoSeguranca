package br.unirv.capsafe.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Preferências simples do app (URL do servidor, rótulos customizados,
 * limiar de confiança padrão).
 */
object AppPrefs {

    private const val ARQUIVO = "capsafe_prefs"
    private const val KEY_SERVER = "server_url"
    private const val KEY_LABELS = "custom_labels"
    private const val KEY_CONFIDENCE = "confidence"

    /** 10.0.2.2 é o localhost do host visto pelo emulador Android. */
    const val DEFAULT_SERVER = "http://10.0.2.2:3030/"
    const val DEFAULT_CONFIDENCE = 0.25f

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    fun serverUrl(context: Context): String =
        prefs(context).getString(KEY_SERVER, DEFAULT_SERVER) ?: DEFAULT_SERVER

    fun setServerUrl(context: Context, value: String) =
        prefs(context).edit().putString(KEY_SERVER, value).apply()

    fun customLabels(context: Context): String? =
        prefs(context).getString(KEY_LABELS, null)

    fun setCustomLabels(context: Context, value: String) =
        prefs(context).edit().putString(KEY_LABELS, value).apply()

    fun confidence(context: Context): Float =
        prefs(context).getFloat(KEY_CONFIDENCE, DEFAULT_CONFIDENCE)

    fun setConfidence(context: Context, value: Float) =
        prefs(context).edit().putFloat(KEY_CONFIDENCE, value).apply()
}
