package com.moham.taxi.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.moham.taxi.BillingData
import com.moham.taxi.R
import com.moham.taxi.data.model.QuoteData
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object PdfGenerator {
    private fun getCurrencyFormat(): NumberFormat {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    fun generateQuote(
        context: Context,
        billingData: BillingData,
        quoteData: QuoteData,
        origin: String,
        destination: String,
        dateTime: String
    ): Uri? {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val filePrefix = context.getString(R.string.pdf_quote_file_prefix)
            val fileName = "${filePrefix}_${System.currentTimeMillis()}.pdf"
            val file = File(downloadsDir, fileName)
            
            val fos = FileOutputStream(file)
            val writer = PdfWriter(fos)
            val pdf = PdfDocument(writer)
            val document = Document(pdf, PageSize.A4)
            
            val headerBackground = DeviceRgb(230, 235, 240)
            val currencyFormat = getCurrencyFormat()

                // Encabezado
                val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
                val leftCell = Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.LEFT)
                leftCell.add(Paragraph(context.getString(R.string.pdf_quote_header)).setBold().setFontSize(16f))
                leftCell.add(Paragraph(context.getString(R.string.pdf_quote_service_date, dateTime)).setFontSize(10f))
                leftCell.add(Paragraph(context.getString(R.string.pdf_quote_origin, origin)).setFontSize(10f))
                leftCell.add(Paragraph(context.getString(R.string.pdf_quote_destination, destination)).setFontSize(10f))
                
                val rightCell = Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                rightCell.add(Paragraph(context.getString(R.string.pdf_quote_driver_data)).setBold().setFontSize(10f))
                rightCell.add(Paragraph(billingData.name).setFontSize(9f))
                rightCell.add(Paragraph(context.getString(R.string.pdf_quote_nif, billingData.nif)).setFontSize(9f))
                if (billingData.license.isNotBlank()) rightCell.add(Paragraph(context.getString(R.string.pdf_quote_license, billingData.license)).setFontSize(9f))
                val zipText = context.getString(R.string.postal_code_short, billingData.postalCode)
                rightCell.add(Paragraph("${billingData.street}, $zipText ${billingData.city}").setFontSize(9f))
                
                headerTable.addCell(leftCell)
                headerTable.addCell(rightCell)
                document.add(headerTable)
                document.add(Paragraph("\n"))

                // Detalles del Presupuesto
                val table = Table(UnitValue.createPercentArray(floatArrayOf(4f, 1f, 2f, 2f))).useAllAvailableWidth()
                table.addHeaderCell(Cell().add(Paragraph(context.getString(R.string.pdf_quote_concept)).setBold()).setBackgroundColor(headerBackground))
                table.addHeaderCell(Cell().add(Paragraph(context.getString(R.string.pdf_quote_quantity)).setBold().setTextAlignment(TextAlignment.CENTER)).setBackgroundColor(headerBackground))
                table.addHeaderCell(Cell().add(Paragraph(context.getString(R.string.pdf_quote_unit_price)).setBold().setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(headerBackground))
                table.addHeaderCell(Cell().add(Paragraph(context.getString(R.string.pdf_quote_total)).setBold().setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(headerBackground))

                var subtotal = 0.0
                quoteData.items.forEach { item ->
                    table.addCell(Cell().add(Paragraph(item.description).setFontSize(10f)))
                    val qtyText = if (item.quantity != null) {
                        if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                    } else {
                        ""
                    }
                    table.addCell(Cell().add(Paragraph(qtyText).setFontSize(10f).setTextAlignment(TextAlignment.CENTER)))
                    table.addCell(Cell().add(Paragraph(currencyFormat.format(item.unitPrice)).setFontSize(10f).setTextAlignment(TextAlignment.RIGHT)))
                    table.addCell(Cell().add(Paragraph(currencyFormat.format(item.total)).setFontSize(10f).setTextAlignment(TextAlignment.RIGHT)))
                    subtotal += item.total
                }

                document.add(table)
                document.add(Paragraph("\n"))

                // IVA y Totales (Asumiendo 10% IVA para transporte de pasajeros en España)
                // Si el precio calculado ya incluye IVA, lo desglosamos.
                // Total = Base + IVA
                // Base = Total / 1.10
                val totalWithVat = quoteData.totalAmount
                val basePrice = totalWithVat / 1.10
                val vatAmount = totalWithVat - basePrice

                val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(7f, 2f))).useAllAvailableWidth()
                
                totalsTable.addCell(Cell().add(Paragraph(context.getString(R.string.pdf_quote_taxable_base))).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT))
                totalsTable.addCell(Cell().add(Paragraph(currencyFormat.format(basePrice))).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT))
                
                totalsTable.addCell(Cell().add(Paragraph(context.getString(R.string.pdf_quote_vat))).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT))
                totalsTable.addCell(Cell().add(Paragraph(currencyFormat.format(vatAmount))).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT))
                
                totalsTable.addCell(Cell().add(Paragraph(context.getString(R.string.pdf_quote_total_budget))).setBorder(Border.NO_BORDER).setBold().setTextAlignment(TextAlignment.RIGHT))
                totalsTable.addCell(Cell().add(Paragraph(currencyFormat.format(totalWithVat))).setBorder(Border.NO_BORDER).setBold().setTextAlignment(TextAlignment.RIGHT))
                
                document.add(totalsTable)

                document.add(Paragraph("\n"))
                document.add(Paragraph(context.getString(R.string.pdf_quote_disclaimer)).setFontSize(8f).setTextAlignment(TextAlignment.CENTER))

                document.close()
            
            // Usar FileProvider si el target es N o superior
            return try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (e: Exception) {
                // Fallback para versiones antiguas
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun sharePdf(context: Context, uri: Uri) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.pdf_quote_share_subject))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.pdf_quote_share_body))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.pdf_quote_share_title))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
