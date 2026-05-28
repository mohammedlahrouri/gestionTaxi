package com.moham.taxi.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Colores principales - Paleta profesional sobria
var PrimaryBlue by mutableStateOf(Color(0xFF1565C0))  // Azul profesional más oscuro para navegación/neutro principal
val SecondaryPurple = Color(0xFF8B5CF6)  // Violeta suave (mantener para compatibilidad)
val AccentGreen = Color(0xFF2E7D32)  // Verde profesional más oscuro para ingresos/positivo
val AccentRed = Color(0xFFD32F2F)  // Rojo profesional más oscuro para gastos/negativo

// Colores de fondo y superficie - Gris muy oscuro profesional
val DarkBackground = Color(0xFF121212)  // Gris muy oscuro como fondo principal
val DarkSurface = Color(0xFF1E1E1E)  // Superficie ligeramente más clara
val DarkCard = Color(0xFF2C2C2C)  // Tarjetas con contraste sutil

// Colores de texto
val TextPrimary = Color(0xFFFFFFFF)  // Texto principal blanco
val TextSecondary = Color(0xFF9E9E9E)  // Iconos secundarios y divisores - gris medio
val TextTertiary = Color(0xFF757575)  // Texto terciario

// Colores de estado - Paleta profesional sobria
val Success = Color(0xFF2E7D32)  // Verde profesional para elementos positivos
val Warning = Color(0xFFFF9800)  // Naranja para advertencias
val Error = Color(0xFFD32F2F)  // Rojo profesional para elementos negativos
val Info = Color(0xFF1565C0)  // Azul profesional para información

// Colores para gráficos - Paleta profesional sobria
val ChartIncomeColor = Color(0xFF2E7D32)  // Verde profesional para ingresos
val ChartExpenseColor = Color(0xFFD32F2F)  // Rojo profesional para gastos
val ChartIncomeAltColor = Color(0xFF2E7D32)  // Verde profesional alternativo
val ChartExpenseAltColor = Color(0xFFD32F2F)  // Rojo profesional alternativo
val ChartGridColor = Color(0xFF9E9E9E)  // Gris medio para líneas de cuadrícula
val ChartBackgroundColor = Color(0xFF121212)  // Fondo gris muy oscuro para gráficas

// Colores de acento específicos - Paleta profesional sobria
var BlueAccent by mutableStateOf(Color(0xFF1565C0))  // Azul profesional para navegación/acciones
var PurpleAccent by mutableStateOf(Color(0xFF1565C0))  // Azul profesional para estadísticas
val GreenAccent = Color(0xFF2E7D32)  // Verde profesional exclusivo para ingresos
val RedAccent = Color(0xFFD32F2F)  // Rojo profesional exclusivo para gastos
var YellowAccent by mutableStateOf(Color(0xFF1565C0))  // Azul profesional para precio
val TealAccent = Color(0xFF009688)  // Verde azulado para elementos especiales
var OrangeAccent by mutableStateOf(Color(0xFF1565C0))  // Azul profesional para acciones

// Colores específicos para botones del menú principal - Paleta profesional sobria
val IncomeButtonColor = Color(0xFF2E7D32)  // Verde profesional más oscuro para ingresos
val ExpenseButtonColor = Color(0xFFD32F2F)  // Rojo profesional más oscuro para gastos
var RidesButtonColor by mutableStateOf(Color(0xFF1565C0))  // Azul profesional más oscuro para navegación
var StatsButtonColor by mutableStateOf(Color(0xFF1565C0))  // Azul profesional más oscuro para acciones principales
var InvoiceButtonColor by mutableStateOf(Color(0xFF1565C0))  // Azul profesional más oscuro para navegación
var PriceButtonColor by mutableStateOf(Color(0xFF1565C0))  // Azul profesional más oscuro para acciones principales

// Colores para widgets/tarjetas - Fondo similar al calendario con iconos de colores
val IncomeWidgetBackground = Color(0xFF1E1E1E)  // Fondo similar al calendario
val ExpenseWidgetBackground = Color(0xFF1E1E1E)  // Fondo similar al calendario
val IncomeIconColor = Color(0xFF2E7D32)  // Verde profesional para iconos de ingresos
val ExpenseIconColor = Color(0xFFD32F2F)  // Rojo profesional para iconos de gastos

// Colores para el calendario - Paleta profesional funcional
val CalendarBackground = Color(0xFF1E1E1E)  // Consistente con DarkSurface
var CalendarAccent by mutableStateOf(Color(0xFF1565C0))  // Azul profesional para acciones principales
val CalendarText = Color(0xFFFFFFFF)  // Texto blanco para mejor legibilidad
val CalendarDayBackground = Color(0xFF9E9E9E)  // Gris medio para días
val CalendarDayText = Color(0xFFFFFFFF)  // Texto blanco para días

// Colores del tema original (mantener para compatibilidad)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Colores personalizados para la UI
val TextWhite = Color.White

// Colores de tema claro
val md_theme_light_primary = PrimaryBlue
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFD1E4FF)
val md_theme_light_onPrimaryContainer = Color(0xFF001D36)
val md_theme_light_secondary = SecondaryPurple
val md_theme_light_onSecondary = Color.White
val md_theme_light_secondaryContainer = Color(0xFFEFDDFF)
val md_theme_light_onSecondaryContainer = Color(0xFF251A58)
val md_theme_light_background = Color.White
val md_theme_light_surface = Color.White

// Colores de tema oscuro - Paleta profesional funcional
val md_theme_dark_primary = PrimaryBlue  // Azul brillante
val md_theme_dark_onPrimary = Color.White
val md_theme_dark_primaryContainer = Color(0xFF1565C0)  // Azul más oscuro
val md_theme_dark_onPrimaryContainer = Color.White
val md_theme_dark_secondary = SecondaryPurple
val md_theme_dark_onSecondary = Color.White
val md_theme_dark_secondaryContainer = Color(0xFF3A2A71)
val md_theme_dark_onSecondaryContainer = Color(0xFFEFDDFF)
val md_theme_dark_background = DarkBackground  // Gris muy oscuro
val md_theme_dark_surface = DarkCard  // Gris oscuro para superficies

// New Design Colors (Statistics Redesign)
val StatsBackground = Color(0xFF121212) // Keeping pure black/dark grey for consistency with app theme, or use new design:
// From Oklch(0.12 0.005 285) ~ Dark Purple Grey
val NewStatsBackground = Color(0xFF1E1B24)
// From Oklch(0.16 0.005 285)
val NewStatsCardBackground = Color(0xFF26232E)
// From Oklch(0.72 0.19 160) - Emerald Green
val NewStatsIncome = Color(0xFF00D29F)
// From Oklch(0.65 0.22 25) - Coral Red
val NewStatsExpense = Color(0xFFFF525E)
// From Oklch(0.97 0 0)
val NewStatsTextSecondary = Color(0xFFF7F7F7).copy(alpha = 0.6f)
val NewStatsTextPrimary = Color.White
val NewStatsBorder = Color(0xFFFFFFFF).copy(alpha = 0.1f)

// Icon Backgrounds
val NewStatsFuelBg = Color(0xFFF59E0B).copy(alpha = 0.1f) // Amber/Orange
val NewStatsFuelIcon = Color(0xFFF59E0B)
val NewStatsOtherBg = Color(0xFF0EA5E9).copy(alpha = 0.1f) // Sky/Info
val NewStatsOtherIcon = Color(0xFF0EA5E9)

// Payment Methods
val NewStatsCashBg = Color(0xFF2E7D32).copy(alpha = 0.1f) // Primary/Green ish
val NewStatsCashIcon = Color(0xFF2E7D32)
val NewStatsCardBg = Color(0xFF0EA5E9).copy(alpha = 0.1f) // Info
val NewStatsCardIcon = Color(0xFF0EA5E9)
val NewStatsAppBg = Color(0xFF8B5CF6).copy(alpha = 0.1f) // Accent/Purple
val NewStatsAppIcon = Color(0xFF8B5CF6)

// Others Screen Gradients
val PriceGradientStart = Color(0xFF2563EB) // Blue 600
val PriceGradientEnd = Color(0xFF06B6D4) // Cyan 500
val InvoiceGradientStart = Color(0xFF4F46E5) // Indigo 600
val InvoiceGradientEnd = Color(0xFF8B5CF6) // Purple 500
val TipBackground = Color(0xFF8B5CF6).copy(alpha = 0.1f) // Secondary/30 approx
val TipIconBg = Color(0xFF1565C0).copy(alpha = 0.1f) // Primary/10 approx
