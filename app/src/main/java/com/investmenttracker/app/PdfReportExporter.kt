package com.investmenttracker.app

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportExporter {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val titleFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun exportPortfolioToPdf(
        context: Context,
        title: String,
        investments: List<Investment>,
        summary: PortfolioSummary
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595x842 pt)
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Banner
        paint.color = Color.parseColor("#0B0F19")
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // App Logo / Title
        paint.color = Color.parseColor("#10B981")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("InvestTrack Portfolio Report", 28f, 38f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        val nowStr = SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Report Scope: $title  |  Generated on: $nowStr", 28f, 58f, paint)

        // 2. Summary KPI Box
        paint.color = Color.parseColor("#131C2E")
        canvas.drawRoundRect(28f, 105f, 567f, 175f, 8f, 8f, paint)

        // KPI 1: Total Invested
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("TOTAL INVESTED", 42f, 126f, paint)
        paint.color = Color.parseColor("#FFFFFF")
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(String.format(Locale.US, "$%.2f", summary.totalInvested), 42f, 148f, paint)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("${summary.totalCount} investments", 42f, 164f, paint)

        // KPI 2: Maturity Value
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 9f
        canvas.drawText("MATURITY VALUE", 180f, 126f, paint)
        paint.color = Color.parseColor("#10B981")
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(String.format(Locale.US, "$%.2f", summary.totalMaturity), 180f, 148f, paint)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Expected Payout", 180f, 164f, paint)

        // KPI 3: Net Expected Gain
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 9f
        canvas.drawText("NET GAIN (ROI)", 318f, 126f, paint)
        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(String.format(Locale.US, "+$%.2f", summary.totalNetGain), 318f, 148f, paint)
        paint.color = Color.parseColor("#10B981")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(String.format(Locale.US, "%.1f%% ROI", summary.averageRoi), 318f, 164f, paint)

        // KPI 4: Overdue Alert
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 9f
        canvas.drawText("OVERDUE MATURITY", 450f, 126f, paint)
        paint.color = if (summary.overdueCount > 0) Color.parseColor("#EF4444") else Color.parseColor("#10B981")
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${summary.overdueCount} Items", 450f, 148f, paint)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(if (summary.overdueCount > 0) "Requires Attention!" else "All On Track", 450f, 164f, paint)

        // 3. Table Header
        var currentY = 205f
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(28f, currentY - 14f, 567f, currentY + 10f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NAME / DETAILS", 35f, currentY + 2f, paint)
        canvas.drawText("CATEGORY", 195f, currentY + 2f, paint)
        canvas.drawText("INVESTED", 270f, currentY + 2f, paint)
        canvas.drawText("MATURITY", 345f, currentY + 2f, paint)
        canvas.drawText("MATURITY DATE", 425f, currentY + 2f, paint)
        canvas.drawText("STATUS", 515f, currentY + 2f, paint)

        currentY += 22f
        val now = System.currentTimeMillis()

        // 4. Rows
        for ((index, inv) in investments.take(24).withIndex()) {
            if (currentY > 800f) break

            if (index % 2 == 1) {
                paint.color = Color.parseColor("#0F172A")
                canvas.drawRect(28f, currentY - 10f, 567f, currentY + 14f, paint)
            }

            val status = inv.getStatus(now)
            // Name
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val nameDisplay = if (inv.name.length > 24) inv.name.take(22) + ".." else inv.name
            canvas.drawText(nameDisplay, 35f, currentY + 1f, paint)

            // Category
            paint.color = Color.parseColor("#475569")
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(inv.category, 195f, currentY + 1f, paint)

            // Invested
            paint.color = Color.parseColor("#334155")
            canvas.drawText(String.format(Locale.US, "$%.2f", inv.investmentAmount), 270f, currentY + 1f, paint)

            // Maturity
            paint.color = Color.parseColor("#0F766E")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(String.format(Locale.US, "$%.2f", inv.maturityAmount), 345f, currentY + 1f, paint)

            // Maturity Date
            paint.color = Color.parseColor("#475569")
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(dateFormat.format(Date(inv.maturityDate)), 425f, currentY + 1f, paint)

            // Status Badge
            when (status) {
                InvestmentStatus.OVERDUE -> {
                    paint.color = Color.parseColor("#EF4444")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("OVERDUE", 515f, currentY + 1f, paint)
                }
                InvestmentStatus.MATURING_SOON -> {
                    paint.color = Color.parseColor("#D97706")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("SOON", 515f, currentY + 1f, paint)
                }
                InvestmentStatus.ACTIVE -> {
                    paint.color = Color.parseColor("#059669")
                    paint.typeface = Typeface.DEFAULT
                    canvas.drawText("ACTIVE", 515f, currentY + 1f, paint)
                }
                InvestmentStatus.CLOSED -> {
                    paint.color = Color.parseColor("#64748B")
                    paint.typeface = Typeface.DEFAULT
                    canvas.drawText("CLOSED", 515f, currentY + 1f, paint)
                }
            }

            currentY += 24f
        }

        // Footer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Generated by InvestTrack • Android Tablet & Mobile Certified", 28f, 825f, paint)

        pdfDoc.finishPage(page)

        val fileName = "InvestTrack_Report_${titleFormat.format(Date())}.pdf"
        val reportsDir = File(context.getExternalFilesDir(null), "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val file = File(reportsDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return file
    }

    fun openOrSharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Investment Portfolio Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Investment Report"))
    }
}
