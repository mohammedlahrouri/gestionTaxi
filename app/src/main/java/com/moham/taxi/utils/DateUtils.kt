package com.moham.taxi.utils

import android.content.Context
import com.moham.taxi.GestionTaxiApplication
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utilidades para manejo de fechas en la aplicación
 */
object DateUtils {
    
    /**
     * Crea un rango de fechas para el día especificado (inicio y fin del día)
     */
    fun getDayRange(date: Date): Pair<Date, Date> {
        return Pair(getStartOfDay(date), getEndOfDay(date))
    }
    
    /**
     * Obtiene el rango de fechas para el mes que contiene la fecha especificada
     */
    fun getMonthRange(date: Date = Date()): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.time
        
        return Pair(startOfMonth, endOfMonth)
    }

    /**
     * Crea un rango de fechas para el mes actual (inicio y fin del mes)
     */
    fun getCurrentMonthRange(): Pair<Date, Date> {
        return getMonthRange()
    }
    
    fun getCurrentDayRange(): Pair<Date, Date> {
        return getDayRange(Date())
    }
    
    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    private fun getEndOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }
    
    /**
     * Obtiene el rango de la semana para una fecha dada según los requisitos:
     * - La semana comienza en el día configurado por el usuario
     * - La semana termina el día anterior al día configurado
     * - Las semanas no cruzan meses, se reinician al mes siguiente
     * - Cada vez que se alcanza el día configurado, se reinicia la semana
     * 
     * @param date Fecha para determinar la semana
     * @param firstDayOfWeek Primer día de la semana configurado (1-7, donde 1=domingo, 2=lunes, etc.)
     * @return Par con las fechas de inicio y fin de la semana
     */
    fun getWeekRange(date: Date, firstDayOfWeek: Int): Pair<Date, Date> {
        val calendar = Calendar.getInstance().apply { time = date }
        val dateMonth = calendar.get(Calendar.MONTH)
        val dateYear = calendar.get(Calendar.YEAR)

        // 1. Calcular el inicio de la semana
        val startCalendar = calendar.clone() as Calendar
        if (startCalendar.get(Calendar.DAY_OF_MONTH) == 1) {
            // El día 1 siempre inicia una nueva semana
            startCalendar.set(Calendar.HOUR_OF_DAY, 0)
            startCalendar.set(Calendar.MINUTE, 0)
            startCalendar.set(Calendar.SECOND, 0)
            startCalendar.set(Calendar.MILLISECOND, 0)
        } else {
            // Retroceder hasta el último inicio de semana o el día 1 del mes
            while (startCalendar.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek &&
                   startCalendar.get(Calendar.DAY_OF_MONTH) > 1 &&
                   startCalendar.get(Calendar.MONTH) == dateMonth &&
                   startCalendar.get(Calendar.YEAR) == dateYear) {
                startCalendar.add(Calendar.DAY_OF_MONTH, -1)
            }
            // Si retrocedimos hasta el mes anterior, volver al primer día del mes actual
            if (startCalendar.get(Calendar.MONTH) != dateMonth || 
                startCalendar.get(Calendar.YEAR) != dateYear) {
                startCalendar.set(Calendar.YEAR, dateYear)
                startCalendar.set(Calendar.MONTH, dateMonth)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)
            }
            startCalendar.set(Calendar.HOUR_OF_DAY, 0)
            startCalendar.set(Calendar.MINUTE, 0)
            startCalendar.set(Calendar.SECOND, 0)
            startCalendar.set(Calendar.MILLISECOND, 0)
        }

        // 2. Calcular el final de la semana
        val endCalendar = startCalendar.clone() as Calendar
        while (true) {
            endCalendar.add(Calendar.DAY_OF_MONTH, 1)
            // Si llegamos al día de inicio de semana, o cambiamos de mes, retroceder un día
            if (endCalendar.get(Calendar.DAY_OF_WEEK) == firstDayOfWeek ||
                endCalendar.get(Calendar.MONTH) != dateMonth ||
                endCalendar.get(Calendar.YEAR) != dateYear) {
                endCalendar.add(Calendar.DAY_OF_MONTH, -1)
                break
            }
        }
        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.set(Calendar.SECOND, 59)
        endCalendar.set(Calendar.MILLISECOND, 999)

        return Pair(startCalendar.time, endCalendar.time)
    }
    
    /**
     * Crea un rango de fechas para la semana actual basado en la configuración del usuario
     */
    suspend fun getCurrentWeekRange(context: Context): Pair<Date, Date> {
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        
        return getWeekRange(Date(), firstDayOfWeekValue)
    }
    
    /**
     * Formatea una fecha en el formato especificado
     * Si la fecha no es hoy y el patrón contiene HH:mm, reemplaza la hora con "SH" (sin hora)
     */
    fun formatDate(date: Date, pattern: String = "dd/MM/yyyy", locale: Locale = Locale("es", "ES")): String {
        // Si el patrón contiene formato de hora y la fecha no es hoy, mostrar "SH" en lugar de la hora
        if (pattern.contains("HH:mm") && !isToday(date)) {
            // Extraer solo la parte de la fecha del patrón (sin la hora)
            val dateOnlyPattern = pattern.replace("HH:mm", "")
                .replace(" ", "") // Eliminar espacios que pudieran quedar
            
            val dateFormatter = SimpleDateFormat(dateOnlyPattern, locale)
            return dateFormatter.format(date) + " SH"
        } else {
            val formatter = SimpleDateFormat(pattern, locale)
            return formatter.format(date)
        }
    }
    
    /**
     * Verifica si una fecha es hoy
     */
    fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val dateCalendar = Calendar.getInstance().apply { time = date }
        
        return today.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
               today.get(Calendar.MONTH) == dateCalendar.get(Calendar.MONTH) &&
               today.get(Calendar.DAY_OF_MONTH) == dateCalendar.get(Calendar.DAY_OF_MONTH)
    }
    
    /**
     * Asigna la fecha correcta según los requisitos:
     * - Si es el día actual, usa la fecha y hora exactas
     * - Si es un día específico, usa la medianoche de ese día
     * 
     * @param selectedDate La fecha seleccionada
     * @return Date con la fecha y hora correctas según los requisitos
     */
    fun assignProperDate(selectedDate: Date): Date {
        // Normalizar la fecha seleccionada para comparar solo año, mes y día
        val selectedCalendar = Calendar.getInstance().apply { time = selectedDate }
        val todayCalendar = Calendar.getInstance()
        
        val sameDay = selectedCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                      selectedCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                      selectedCalendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)
        
        return if (sameDay) {
            // Si es hoy, usa la fecha y hora exactas
            Date()
        } else {
            // Si es otro día, usa la medianoche de ese día
            selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
            selectedCalendar.set(Calendar.MINUTE, 0)
            selectedCalendar.set(Calendar.SECOND, 0)
            selectedCalendar.set(Calendar.MILLISECOND, 0)
            selectedCalendar.time
        }
    }
    
    /**
     * Obtiene el nombre del día de la semana para una fecha dada
     */
    fun getDayOfWeekName(date: Date, locale: Locale = Locale("es", "ES")): String {
        val dayFormat = SimpleDateFormat("EEEE", locale)
        return dayFormat.format(date).replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(locale) else it.toString() 
        }
    }

    /**
     * Determina en qué semana del mes cae una fecha específica
     * Las semanas comienzan en el día configurado y pueden tener menos de 7 días
     * 
     * @param date Fecha a analizar
     * @param firstDayOfWeek Primer día de la semana configurado (1-7, donde 1=domingo, 2=lunes, etc.)
     * @return Número de semana en el mes (empezando desde 0)
     */
    fun getWeekOfMonth(date: Date, firstDayOfWeek: Int): Int {
        val calendar = Calendar.getInstance().apply { time = date }
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        
        val monthStart = calendar.clone() as Calendar
        monthStart.set(Calendar.DAY_OF_MONTH, 1)
        
        var weekCount = 0
        val tempCal = monthStart.clone() as Calendar
        
        while (tempCal.get(Calendar.MONTH) == month && 
               tempCal.get(Calendar.YEAR) == year &&
               tempCal.get(Calendar.DAY_OF_MONTH) <= calendar.get(Calendar.DAY_OF_MONTH)) {
            
            if (tempCal.get(Calendar.DAY_OF_WEEK) == firstDayOfWeek && 
                tempCal.get(Calendar.DAY_OF_MONTH) != 1) {
                weekCount++
            }
            
            if (tempCal.get(Calendar.DAY_OF_MONTH) == 1) {
                weekCount = 0
            }
            
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return weekCount
    }

    /**
     * Calcula el número de semanas en un mes según los criterios especificados
     * @param date Fecha para determinar el mes
     * @param firstDayOfWeek Primer día de la semana configurado (1-7, donde 1=domingo, 2=lunes, etc.)
     */
    fun getMonthWeekCount(date: Date, firstDayOfWeek: Int): Int {
        val calendar = Calendar.getInstance().apply { time = date }
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        
        var weekCount = 1
        
        while (calendar.get(Calendar.MONTH) == month && 
               calendar.get(Calendar.YEAR) == year) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            
            if (calendar.get(Calendar.MONTH) != month || 
                calendar.get(Calendar.YEAR) != year) {
                break
            }
            
            if (calendar.get(Calendar.DAY_OF_WEEK) == firstDayOfWeek) {
                weekCount++
            }
        }
        
        return weekCount
    }

    /**
     * Obtiene el rango de una semana específica del mes
     * @param date Fecha para determinar el mes
     * @param weekNumber Número de semana en el mes (empezando desde 0)
     * @param firstDayOfWeek Primer día de la semana configurado (1-7, donde 1=domingo, 2=lunes, etc.)
     */
    fun getMonthWeekRange(date: Date, weekNumber: Int, firstDayOfWeek: Int): Pair<Date, Date> {
        val calendar = Calendar.getInstance().apply { time = date }
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        
        // Ir al primer día del mes
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        
        // Avanzar hasta encontrar el inicio de la semana especificada
        var currentWeek = 0
        
        // La primera semana siempre comienza el día 1
        if (weekNumber == 0) {
            // Ya estamos en el día 1, no hacer nada
        } else {
            while (currentWeek < weekNumber &&
                   calendar.get(Calendar.MONTH) == month &&
                   calendar.get(Calendar.YEAR) == year) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                
                // Si estamos en un nuevo mes, hemos ido demasiado lejos
                if (calendar.get(Calendar.MONTH) != month || 
                    calendar.get(Calendar.YEAR) != year) {
                    // Esto no debería ocurrir si weekNumber es válido
                    throw IllegalArgumentException("Número de semana inválido para el mes")
                }
                
                // Si encontramos el día configurado como inicio, aumentar el contador de semanas
                if (calendar.get(Calendar.DAY_OF_WEEK) == firstDayOfWeek) {
                    currentWeek++
                }
            }
        }
        
        // Ahora calendar está en el primer día de la semana solicitada
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.time
        
        // Avanzar hasta el día anterior al configurado como inicio o hasta el final del mes
        while (true) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            
            // Si llegamos al día configurado como inicio o salimos del mes, retroceder un día
            if (calendar.get(Calendar.DAY_OF_WEEK) == firstDayOfWeek ||
                calendar.get(Calendar.MONTH) != month ||
                calendar.get(Calendar.YEAR) != year) {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                break
            }
        }
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfWeek = calendar.time
        
        return Pair(startOfWeek, endOfWeek)
    }
}
