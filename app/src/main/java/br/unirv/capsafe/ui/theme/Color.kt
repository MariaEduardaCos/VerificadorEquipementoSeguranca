package br.unirv.capsafe.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * DESIGN SYSTEM "OBRA SEGURA" — CapSafe
 * Paleta inspirada em sinalização de segurança do trabalho:
 * laranja de sinalização + cinza aço + verde de conformidade.
 * Tema Light Mode obrigatório (enunciado, seção 7).
 */

// Primária — Laranja de Sinalização (capacete/obra)
val SafetyOrange = Color(0xFFEA580C)
val SafetyOrangeDark = Color(0xFFC2410C)
val SafetyOrangeLight = Color(0xFFFFEDD5)

// Secundária — Cinza Aço (estruturas metálicas)
val SteelBlue = Color(0xFF334155)
val SteelGray = Color(0xFF64748B)
val SteelLight = Color(0xFFE2E8F0)

// Destaque — Amarelo Capacete
val HardHatYellow = Color(0xFFF59E0B)
val HardHatYellowLight = Color(0xFFFEF3C7)

// Neutros
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val OutlineGray = Color(0xFFCBD5E1)

// Semânticas — Conformidade x Violação (uso de capacete)
val ComplianceGreen = Color(0xFF16A34A)
val ComplianceGreenLight = Color(0xFFDCFCE7)
val ComplianceGreenDark = Color(0xFF166534)
val ViolationRed = Color(0xFFDC2626)
val ViolationRedLight = Color(0xFFFEE2E2)
val ViolationRedDark = Color(0xFF991B1B)

// Cores das caixas delimitadoras por categoria de classe
val BoxHelmetColor = Color(0xFF16A34A)   // capacete — verde
val BoxViolationColor = Color(0xFFDC2626) // cabeça descoberta — vermelho
val BoxOtherColor = Color(0xFFD97706)     // pessoa/outros — âmbar
