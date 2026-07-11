package com.moham.taxi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moham.taxi.ui.components.AutoSizeText
import com.moham.taxi.ui.components.formatCurrency
import java.text.DateFormat
import java.util.Date
import kotlin.math.max

val StatsBackground = Color(0xFF0F172A)
val StatsCardBackground = Color(0xFF1E293B)
val StatsBorder = Color(0xFF334155)
val StatsTextPrimary = Color(0xFFF8FAFC)
val StatsTextSecondary = Color(0xFF94A3B8)
val StatsIncome = Color(0xFF34D399)
val StatsExpense = Color(0xFFF87171)
val StatsCommission = Color(0xFFFBBF24)
val StatsBlue = Color(0xFF3B82F6)
val StatsPurple = Color(0xFFA855F7)
val StatsAmber = Color(0xFFF59E0B)
val StatsSky = Color(0xFF38BDF8)
val StatsOrange = Color(0xFFF97316)
val StatsGray = Color(0xFF6B7280)

data class PaymentMethodItem(
    val name: String,
    val amount: Double,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val subItems: List<PaymentSubItem> = emptyList()
)

data class PaymentSubItem(
    val name: String,
    val amount: Double,
    val color: Color
)

data class PlatformItem(
    val name: String,
    val gross: Double,
    val commission: Double,
    val commissionPct: Double,
    val net: Double,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

data class TrendPoint(
    val label: String,
    val income: Double,
    val expenses: Double
)

data class StatsPeriodData(
    val period: String,
    val range: String,
    val gross: Double,
    val commissions: Double,
    val net: Double,
    val expenses: Double,
    val fuel: Double,
    val otherExpenses: Double,
    val paymentMethods: List<PaymentMethodItem>,
    val platforms: List<PlatformItem>,
    val meterTotal: Double,
    val fixedTotal: Double,
    val tipsTotal: Double,
    val tipsByMethod: Map<String, Double>,
    val trend: List<TrendPoint>,
    val trendTitle: String
)

fun buildTrendPoints(
    income: List<Pair<Date, Double>>,
    expenses: List<Pair<Date, Double>>,
    labelFormat: DateFormat
): List<TrendPoint> {
    if (income.isEmpty() && expenses.isEmpty()) return emptyList()
    val expensesByTime = expenses.associateBy { it.first.time }
    return income.map { (date, value) ->
        TrendPoint(
            label = labelFormat.format(date),
            income = value,
            expenses = expensesByTime[date.time]?.second ?: 0.0
        )
    }
}

@Composable
fun buildPaymentMethods(
    paymentMethodBreakdown: Map<String, Double>,
    appPlatformBreakdown: Map<String, Double>
): List<PaymentMethodItem> {
    if (paymentMethodBreakdown.isEmpty()) return emptyList()
    val cashLabel = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.payment_cash)
    val cardLabel = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.payment_card)
    val appLabel = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.payment_via_app)
    fun kindOfPaymentMethod(value: String): String {
        val lower = value.trim().lowercase()
        return when {
            lower == "efectivo" || lower == "cash" -> "cash"
            lower == "tarjeta" || lower == "card" -> "card"
            lower.replace(" ", "") == "viaapp" || lower == "via app" -> "app"
            else -> "other"
        }
    }
    fun displayPaymentMethodName(storedName: String): String {
        return when (kindOfPaymentMethod(storedName)) {
            "cash" -> cashLabel
            "card" -> cardLabel
            "app" -> appLabel
            else -> storedName
        }
    }
    return paymentMethodBreakdown.entries
        .sortedByDescending { it.value }
        .map { (method, amount) ->
            val kind = kindOfPaymentMethod(method)
            val color = when {
                kind == "cash" -> StatsIncome
                kind == "card" -> StatsBlue
                else -> StatsPurple
            }
            val icon = when {
                kind == "cash" -> Icons.Filled.Money
                kind == "card" -> Icons.Filled.CreditCard
                else -> Icons.Filled.Smartphone
            }
            val subItems = if (kind == "app" && appPlatformBreakdown.isNotEmpty()) {
                appPlatformBreakdown.entries.sortedByDescending { it.value }.map { (platform, platformAmount) ->
                    PaymentSubItem(
                        name = platform,
                        amount = platformAmount,
                        color = platformColor(platform)
                    )
                }
            } else {
                emptyList()
            }
            PaymentMethodItem(
                name = displayPaymentMethodName(method),
                amount = amount,
                color = color,
                icon = icon,
                subItems = subItems
            )
        }
}

fun buildPlatformItems(
    totalPlatformBreakdown: Map<String, Double>,
    netPlatformBreakdown: Map<String, Double>
): List<PlatformItem> {
    if (totalPlatformBreakdown.isEmpty()) return emptyList()
    return totalPlatformBreakdown.entries
        .sortedByDescending { it.value }
        .map { (platform, gross) ->
            val net = netPlatformBreakdown[platform] ?: gross
            val commission = (gross - net).coerceAtLeast(0.0)
            val commissionPct = if (net > 0) (commission / net) * 100 else 0.0
            PlatformItem(
                name = platform,
                gross = gross,
                commission = commission,
                commissionPct = commissionPct,
                net = net,
                color = platformColor(platform),
                icon = platformIcon(platform)
            )
        }
}

@Composable
fun StatsHeroCard(
    net: Double,
    gross: Double,
    commissions: Double,
    expenses: Double,
    period: String,
    dates: String
) {
    val netColor = if (net >= 0) StatsIncome else StatsExpense
    val netPercent = if (gross > 0) (net / gross) * 100 else 0.0
    val commissionPercent = if (gross > 0) (commissions / gross) * 100 else 0.0
    val expensePercent = if (gross > 0) (expenses / gross) * 100 else 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                AutoSizeText(
                    text = formatCurrency(net),
                    modifier = Modifier.fillMaxWidth(),
                    maxFontSize = 44.sp,
                    minFontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = netColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Text(text = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.net), color = StatsTextSecondary, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            StatsHeroRow(
                title = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.label_gross),
                subtitle = null,
                amount = gross,
                color = StatsIncome,
                icon = null
            )
            StatsHeroDivider()
            if (commissions > 0) {
                StatsHeroRow(
                    title = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.commissions),
                    subtitle = null,
                    amount = commissions,
                    color = StatsCommission,
                    icon = null
                )
                StatsHeroDivider()
            }
            StatsHeroRow(
                title = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.expenses),
                subtitle = null,
                amount = expenses,
                color = StatsExpense,
                icon = null
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(StatsBorder, CircleShape)
            ) {
                if (gross > 0) {
                    val netWidth = max(0f, (net / gross).toFloat())
                    val commissionWidth = max(0f, (commissions / gross).toFloat())
                    val expenseWidth = max(0f, (expenses / gross).toFloat())
                    if (netWidth > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(netWidth)
                                .background(StatsIncome, CircleShape)
                        )
                    }
                    if (commissionWidth > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(commissionWidth)
                                .background(StatsCommission, CircleShape)
                        )
                    }
                    if (expenseWidth > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(expenseWidth)
                                .background(StatsExpense, CircleShape)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatsLegendItem(androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.net), StatsIncome, netPercent)
                StatsLegendItem(androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.commissions), StatsCommission, commissionPercent)
                StatsLegendItem(androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.expenses), StatsExpense, expensePercent)
            }
        }
    }
}

@Composable
private fun StatsHeroRow(
    title: String,
    subtitle: String?,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(text = title, color = StatsTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(text = subtitle, color = StatsTextSecondary, fontSize = 11.sp)
                }
            }
        }
        AutoSizeText(
            text = formatCurrency(amount),
            modifier = Modifier.widthIn(max = 140.dp),
            maxFontSize = 16.sp,
            minFontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = color,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun StatsHeroDivider() {
    androidx.compose.material3.HorizontalDivider(
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun StatsLegendItem(label: String, color: Color, percent: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label ${percent.toInt()}%",
            color = StatsTextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
fun StatsPaymentMethods(methods: List<PaymentMethodItem>) {
    val total = methods.sumOf { it.amount }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(text = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.breakdown_payment_methods), color = StatsTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        if (total > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(StatsBorder, CircleShape)
            ) {
                methods.forEach { method ->
                    val weight = (method.amount / total).toFloat()
                    if (weight > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(weight)
                                .background(method.color, CircleShape)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            methods.forEach { method ->
                val percent = if (total > 0) ((method.amount / total) * 100).toInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(method.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(method.icon, contentDescription = null, tint = method.color, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = method.name, color = StatsTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(text = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.percent_of_total, percent), color = StatsTextSecondary, fontSize = 11.sp)
                        }
                    }
                    AutoSizeText(
                        text = formatCurrency(method.amount),
                        modifier = Modifier.widthIn(max = 140.dp),
                        maxFontSize = 14.sp,
                        minFontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = method.color,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                if (method.subItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .padding(start = 24.dp, top = 4.dp)
                            .fillMaxWidth()
                    ) {
                        method.subItems.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(sub.color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Smartphone, contentDescription = null, tint = sub.color, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = sub.name, color = StatsTextSecondary, fontSize = 13.sp)
                                }
                                AutoSizeText(
                                    text = formatCurrency(sub.amount),
                                    modifier = Modifier.widthIn(max = 140.dp),
                                    maxFontSize = 13.sp,
                                    minFontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    color = sub.color,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsPlatformBreakdown(platforms: List<PlatformItem>, meterTotal: Double, fixedTotal: Double) {
    val totalGross = platforms.sumOf { it.gross }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(text = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.breakdown_platform), color = StatsTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        if (totalGross > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(StatsBorder, CircleShape)
            ) {
                platforms.forEach { platform ->
                    val weight = (platform.gross / totalGross).toFloat()
                    if (weight > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(weight)
                                .background(platform.color, CircleShape)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            platforms.forEach { platform ->
                val percent = if (totalGross > 0) ((platform.gross / totalGross) * 100).toInt() else 0
                val hasCommission = platform.commission > 0.01
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(platform.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(platform.icon, contentDescription = null, tint = platform.color, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = platform.name, color = StatsTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.percent_of_total, percent), color = StatsTextSecondary, fontSize = 11.sp)
                                if (hasCommission) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.commission_percent_label, platform.commissionPct.toInt()),
                                        color = StatsCommission,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    AutoSizeText(
                        text = formatCurrency(platform.net),
                        modifier = Modifier.widthIn(max = 140.dp),
                        maxFontSize = 14.sp,
                        minFontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = platform.color,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                if (hasCommission && platform.gross > 0) {
                    Column(
                        modifier = Modifier
                            .padding(start = 48.dp, top = 6.dp)
                            .background(StatsBorder.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(StatsBorder, CircleShape)
                        ) {
                            val netWeight = max(0f, (platform.net / platform.gross).toFloat())
                            val commissionWeight = max(0f, (platform.commission / platform.gross).toFloat())
                            if (netWeight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(netWeight)
                                        .background(platform.color, CircleShape)
                                )
                            }
                            if (commissionWeight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(commissionWeight)
                                        .background(StatsCommission, CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        StatsMiniRow(androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.label_gross), formatCurrency(platform.gross), StatsTextSecondary, StatsTextPrimary)
                        StatsMiniRow(androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.label_commission), "-${formatCurrency(platform.commission)}", StatsCommission, StatsCommission)
                        StatsMiniRow(androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.label_net), formatCurrency(platform.net), StatsTextPrimary, platform.color, true)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StatsBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            StatsMiniRow(androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.total_taximeter), formatCurrency(meterTotal), StatsTextSecondary, StatsTextPrimary)
            StatsMiniRow(androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.total_fixed_price), formatCurrency(fixedTotal), StatsTextSecondary, StatsTextPrimary)
        }
    }
}

@Composable
private fun StatsMiniRow(label: String, value: String, labelColor: Color, valueColor: Color, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = labelColor, fontSize = 11.sp)
        AutoSizeText(
            text = value,
            modifier = Modifier.widthIn(max = 160.dp),
            maxFontSize = 11.sp,
            minFontSize = 9.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = valueColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun StatsExpenseDetails(fuel: Double, otherExpenses: Double) {
    val total = fuel + otherExpenses
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.expense_details), color = StatsTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            AutoSizeText(
                text = formatCurrency(total),
                modifier = Modifier.widthIn(max = 160.dp),
                maxFontSize = 13.sp,
                minFontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = StatsExpense,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (total == 0.0) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.no_expenses_recorded),
                color = StatsTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            StatsExpenseItem(
                name = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.expense_type_fuel),
                amount = fuel,
                color = StatsAmber,
                total = total
            )
            Spacer(modifier = Modifier.height(10.dp))
            StatsExpenseItem(
                name = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.other_expenses),
                amount = otherExpenses,
                color = StatsSky,
                total = total
            )
        }
    }
}

@Composable
fun StatsTipsDetails(total: Double, byMethod: Map<String, Double>) {
    val directLabel = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.platform_direct)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.tips),
                color = StatsTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            AutoSizeText(
                text = formatCurrency(total),
                modifier = Modifier.widthIn(max = 160.dp),
                maxFontSize = 13.sp,
                minFontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = StatsIncome,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (total == 0.0) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.no_tips_recorded),
                color = StatsTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            byMethod.entries.sortedByDescending { it.value }.forEach { (method, amount) ->
                val displayMethod = if (
                    method.equals(directLabel, ignoreCase = true) ||
                    method.equals("Directo", ignoreCase = true) ||
                    method.equals("Direct", ignoreCase = true)
                ) {
                    directLabel
                } else {
                    method
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(StatsIncome.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Money, contentDescription = null, tint = StatsIncome, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.tip_method_format, displayMethod),
                            color = StatsTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    AutoSizeText(
                        text = formatCurrency(amount),
                        modifier = Modifier.widthIn(max = 160.dp),
                        maxFontSize = 13.sp,
                        minFontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = StatsIncome,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsExpenseItem(name: String, amount: Double, color: Color, total: Double) {
    val percentage = if (total > 0) (amount / total) * 100 else 0.0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Remove, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = name, color = StatsTextPrimary, fontSize = 13.sp)
        }
        AutoSizeText(
            text = formatCurrency(amount),
            modifier = Modifier.widthIn(max = 160.dp),
            maxFontSize = 13.sp,
            minFontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = StatsTextPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(StatsBorder, CircleShape)
    ) {
        if (percentage > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(percentage.toFloat())
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun StatsTrendChart(points: List<TrendPoint>, title: String) {
    val maxValue = max(1.0, points.maxOfOrNull { max(it.income, it.expenses) } ?: 1.0)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(text = title, color = StatsTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatsLegendItem(androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.income), StatsIncome, 0.0)
            StatsLegendItem(androidx.compose.ui.res.stringResource(com.moham.taxi.R.string.expenses), StatsExpense, 0.0)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.isEmpty()) return@Canvas
                val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
                val incomePoints = points.mapIndexed { index, point ->
                    Offset(stepX * index, size.height - (point.income / maxValue).toFloat() * size.height)
                }
                val expensePoints = points.mapIndexed { index, point ->
                    Offset(stepX * index, size.height - (point.expenses / maxValue).toFloat() * size.height)
                }

                val incomePath = Path().apply {
                    moveTo(incomePoints.first().x, incomePoints.first().y)
                    incomePoints.drop(1).forEach { lineTo(it.x, it.y) }
                }
                val expensePath = Path().apply {
                    moveTo(expensePoints.first().x, expensePoints.first().y)
                    expensePoints.drop(1).forEach { lineTo(it.x, it.y) }
                }

                val incomeFill = Path().apply {
                    addPath(incomePath)
                    lineTo(incomePoints.last().x, size.height)
                    lineTo(incomePoints.first().x, size.height)
                    close()
                }
                val expenseFill = Path().apply {
                    addPath(expensePath)
                    lineTo(expensePoints.last().x, size.height)
                    lineTo(expensePoints.first().x, size.height)
                    close()
                }

                drawPath(
                    path = expenseFill,
                    brush = Brush.verticalGradient(listOf(StatsExpense.copy(alpha = 0.35f), Color.Transparent))
                )
                drawPath(
                    path = incomeFill,
                    brush = Brush.verticalGradient(listOf(StatsIncome.copy(alpha = 0.35f), Color.Transparent))
                )
                drawPath(
                    path = expensePath,
                    color = StatsExpense,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = incomePath,
                    color = StatsIncome,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                fun platformColor(name: String) = when {
                    name.contains("directo", true) || name.equals("direct", true) -> StatsIncome
                    name.contains("freenow", true) -> StatsOrange
                    name.contains("uber", true) -> StatsGray
                    name.contains("cabify", true) -> StatsPurple
                    name.contains("bolt", true) -> Color(0xFF22C55E)
                    else -> StatsBlue
                }

            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { point ->
                Text(text = point.label, color = StatsTextSecondary, fontSize = 10.sp)
            }
        }
    }
}

fun platformIcon(name: String) = when {
    name.contains("directo", true) || name.equals("direct", true) -> Icons.Filled.LocationOn
    name.contains("uber", true) -> Icons.Filled.DirectionsCar
    else -> Icons.Filled.Smartphone
}

fun platformColor(name: String) = when {
    name.contains("directo", true) || name.equals("direct", true) -> StatsIncome
    name.contains("freenow", true) -> StatsOrange
    name.contains("uber", true) -> StatsGray
    name.contains("cabify", true) -> StatsPurple
    name.contains("bolt", true) -> Color(0xFF22C55E)
    else -> StatsBlue
}
