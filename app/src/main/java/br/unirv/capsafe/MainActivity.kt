package br.unirv.capsafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.unirv.capsafe.navigation.CapSafeApp
import br.unirv.capsafe.ui.theme.CapSafeTheme

/**
 * CapSafe — Aplicativo Android Full-Stack em Kotlin (Jetpack Compose).
 * Trabalho Prático N2 — Desenvolvimento de Software para Dispositivos Móveis (UniRV).
 *
 * Tema do grupo: verificação do uso de capacete de segurança (EPI)
 * via Visão Computacional com inferência local ONNX (YOLOv8).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CapSafeTheme {
                CapSafeApp()
            }
        }
    }
}
