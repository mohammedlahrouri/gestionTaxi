package com.moham.taxi.utils

import android.content.Context
import android.os.Environment
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
        origin: String,
        destination: String,
        totalAmount: Double,
        baseAmount: Double,
        ivaAmount: Double,
        date: String,
        time: String
    ): File {
        // Crear directorio si no existe
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val facturasDir = File(downloadsDir, "Facturas")
        if (!facturasDir.exists()) {
            facturasDir.mkdirs()
        }

        // Crear nombre del archivo con fecha y hora
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Factura_${timestamp}.pdf"
        val file = File(facturasDir, fileName)

        // Crear PDF
        PdfWriter(file).use { writer ->
            val pdf = PdfDocument(writer)
            Document(pdf).use { document ->
                // Título
                val title = Paragraph("FACTURA")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20f)
                document.add(title)
                document.add(Paragraph("\n"))

                // Fecha y hora
                document.add(Paragraph("Fecha: $date"))
                document.add(Paragraph("Hora: $time"))
                document.add(Paragraph("\n"))

                // Datos del emisor
                document.add(Paragraph("DATOS DEL EMISOR"))
                document.add(Paragraph("Nombre: ${billingData.name}"))
                document.add(Paragraph("NIF: ${billingData.nif}"))
                document.add(Paragraph("Licencia: ${billingData.license}"))
                document.add(Paragraph("Dirección: ${billingData.street}, ${billingData.city}, CP: ${billingData.postalCode}"))
                document.add(Paragraph("\n"))

                // Datos del cliente
                document.add(Paragraph("DATOS DEL CLIENTE"))
                document.add(Paragraph("Nombre: $clientName"))
                document.add(Paragraph("NIF/CIF: $clientNif"))
                document.add(Paragraph("\n"))

                // Detalles del servicio
                document.add(Paragraph("DETALLES DEL SERVICIO"))
                document.add(Paragraph("Origen: $origin"))
                document.add(Paragraph("Destino: $destination"))
                document.add(Paragraph("\n"))

                // Tabla de importes
                val table = Table(UnitValue.createPercentArray(2))
                table.setWidth(UnitValue.createPercentValue(100f))
                
                // Encabezados
                table.addCell(Cell().add(Paragraph("Concepto")))
                table.addCell(Cell().add(Paragraph("Importe")))
                
                // Base imponible
                table.addCell(Cell().add(Paragraph("Base Imponible")))
                table.addCell(Cell().add(Paragraph(String.format("%.2f €", baseAmount))))
                
                // IVA
                table.addCell(Cell().add(Paragraph("IVA (10%)")))
                table.addCell(Cell().add(Paragraph(String.format("%.2f €", ivaAmount))))
                
                // Total
                table.addCell(Cell().add(Paragraph("Total")))
                table.addCell(Cell().add(Paragraph(String.format("%.2f €", totalAmount))))
                
                document.add(table)
            }
        }

        return file
    }
} 