
package com.moham.taxi.utils

import android.content.Context
import android.os.Environment
import com.moham.taxi.R
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PdfGenerator(private val context: Context) {
    
    fun generateInvoice(
        billingData: com.moham.taxi.BillingData,
        clientName: String,
        clientNif: String,
        clientAddress: String,
        origin: String,
        destination: String,
        invoiceNumber: Int,
        totalAmount: Double,
        baseAmount: Double,
        ivaAmount: Double,
        date: String,
        time: String
    ): File {
        // Usar el directorio de archivos de la aplicación para evitar problemas de permisos
        val folderName = context.getString(R.string.invoice_folder_name)
        val facturasDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), folderName)
        if (!facturasDir.exists()) {
            facturasDir.mkdirs()
        }

        val currencySymbol = CurrencyUtils.getCurrencySymbol()

        // Crear nombre del archivo con fecha y hora
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filePrefix = context.getString(R.string.invoice_file_prefix)
        val fileName = "${filePrefix}_${timestamp}.pdf"
        val file = File(facturasDir, fileName)

        // Crear PDF
        PdfWriter(file).use { writer ->
            val pdf = PdfDocument(writer)
            Document(pdf).use { document ->
                // Título
                val title = Paragraph(context.getString(R.string.invoice_title_upper))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20f)
                document.add(title)

                // Fecha y hora
                document.add(Paragraph("${context.getString(R.string.invoice_number_pdf)}: $invoiceNumber"))
                document.add(Paragraph("${context.getString(R.string.date)}: $date"))
                document.add(Paragraph("${context.getString(R.string.time)}: $time"))
                document.add(Paragraph("\n"))

                // Datos del emisor
                document.add(Paragraph(context.getString(R.string.issuer_data_title)))
                document.add(Paragraph("${context.getString(R.string.name_label)}: ${billingData.name}"))
                document.add(Paragraph("${context.getString(R.string.nif_label)}: ${billingData.nif}"))
                document.add(Paragraph("${context.getString(R.string.license_label_pdf)}: ${billingData.license}"))
                val zipText = context.getString(R.string.postal_code_short, billingData.postalCode)
                document.add(Paragraph("${context.getString(R.string.address_label_pdf)}: ${billingData.street}, ${billingData.city}, $zipText"))
                document.add(Paragraph("\n"))

                // Datos del cliente
                document.add(Paragraph(context.getString(R.string.client_data_title)))
                document.add(Paragraph("${context.getString(R.string.name_label)}: $clientName"))
                document.add(Paragraph("${context.getString(R.string.nif_cif_label)}: $clientNif"))
                document.add(Paragraph("${context.getString(R.string.address_label_pdf)}: $clientAddress"))
                document.add(Paragraph("\n"))

                // Detalles del servicio
                document.add(Paragraph(context.getString(R.string.service_details_title)))
                document.add(Paragraph("${context.getString(R.string.origin_label)}: $origin"))
                document.add(Paragraph("${context.getString(R.string.destination_label)}: $destination"))
                document.add(Paragraph("\n"))

                // Tabla de importes
                val table = Table(UnitValue.createPercentArray(floatArrayOf(3f, 1f)))
                table.setWidth(UnitValue.createPercentValue(100f))

                // Encabezados
                table.addHeaderCell(Cell().add(Paragraph(context.getString(R.string.concept_header))))
                table.addHeaderCell(Cell().add(Paragraph(context.getString(R.string.amount_header))))

                // Fila de servicio
                table.addCell(Cell().add(Paragraph(context.getString(R.string.taxi_service_concept))))
                table.addCell(Cell().add(Paragraph(""))) // Dejar importe vacío

                // Base imponible
                table.addCell(Cell().add(Paragraph(context.getString(R.string.base_amount_row))))
                table.addCell(Cell().add(Paragraph(String.format("%.2f %s", baseAmount, currencySymbol))))

                // IVA
                table.addCell(Cell().add(Paragraph(context.getString(R.string.vat_row))))
                table.addCell(Cell().add(Paragraph(String.format("%.2f %s", ivaAmount, currencySymbol))))

                // Total
                table.addCell(Cell().add(Paragraph(context.getString(R.string.total_row))))
                table.addCell(Cell().add(Paragraph(String.format("%.2f %s", totalAmount, currencySymbol))))

                document.add(table)
                
                document.add(Paragraph("\n"))
                
                document.add(Paragraph(context.getString(R.string.invoice_footer_note))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10f))
            }
        }

        return file
    }
}
