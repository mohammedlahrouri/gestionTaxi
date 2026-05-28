package com.moham.taxi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

import com.moham.taxi.utils.CurrencyUtils
import java.util.Currency

/**
 * Formatea un valor monetario en euros o dólares según la región.
 */
fun formatCurrency(amount: Double): String {
    val symbol = CurrencyUtils.getCurrencySymbol()
    val format = NumberFormat.getNumberInstance(Locale.getDefault())
    format.minimumFractionDigits = 2
    format.maximumFractionDigits = 2
    
    return if (CurrencyUtils.isEuCountry()) {
        "${format.format(amount)} $symbol"
    } else {
        "$symbol${format.format(amount)}"
    }
}

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit,
    minFontSize: TextUnit = 10.sp,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null
) {
    BoxWithConstraints(modifier = modifier) {
        val hasBoundedWidth = constraints.hasBoundedWidth
        val maxWidthPx = constraints.maxWidth
        val textMeasurer = rememberTextMeasurer()
        val resolvedColor = if (color == Color.Unspecified) style.color else color
        var mergedStyle = style.merge(
            TextStyle(
                color = resolvedColor,
                fontWeight = fontWeight,
                fontFamily = fontFamily
            )
        )
        if (textAlign != null) {
            mergedStyle = mergedStyle.merge(TextStyle(textAlign = textAlign))
        }

        val fittedFontSize = remember(text, hasBoundedWidth, maxWidthPx, maxFontSize, minFontSize, mergedStyle) {
            if (!hasBoundedWidth || maxWidthPx <= 0) {
                maxFontSize
            } else {
                val stepSp = 0.5f
                var current = maxFontSize.value
                val min = minFontSize.value
                while (current > min) {
                    val layout = textMeasurer.measure(
                        text = text,
                        style = mergedStyle.copy(fontSize = current.sp),
                        maxLines = 1
                    )
                    if (layout.size.width <= maxWidthPx) break
                    current -= stepSp
                }
                current.coerceAtLeast(min).sp
            }
        }

        Text(
            text = text,
            modifier = if (hasBoundedWidth) Modifier.fillMaxWidth() else Modifier,
            style = mergedStyle.copy(fontSize = fittedFontSize),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

/**
 * Componente que muestra un detalle financiero con etiqueta, valor e icono opcional.
 */
@Composable
fun FinancialDetail(
    label: String, 
    amount: Double,
    icon: ImageVector? = null,
    labelColor: Color = Color.White.copy(alpha = 0.9f),
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = label,
                fontSize = 16.sp,
                color = labelColor
            )
        }
        
        AutoSizeText(
            text = formatCurrency(amount),
            maxFontSize = 16.sp,
            minFontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            fontFamily = FontFamily.Monospace
        )
    }
} 
