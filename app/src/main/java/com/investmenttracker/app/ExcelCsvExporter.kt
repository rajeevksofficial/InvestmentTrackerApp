package com.investmenttracker.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelCsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun exportToExcelCsv(
        context: Context,
        title: String,
        investments: List<Investment>,
        summary: PortfolioSummary
    ): File {
        val fileName = "InvestTrack_Portfolio_${timestampFormat.format(Date())}.csv"
        val exportsDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()
        val file = File(exportsDir, fileName)

        val now = System.currentTimeMillis()

        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Write UTF-8 BOM for Microsoft Excel auto-detection
                writer.write("\uFEFF")

                // Title and Summary Block
                writer.write("InvestTrack Portfolio Export - $title\n")
                writer.write("Generated on,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                writer.write("Total Invested,${String.format(Locale.US, "%.2f", summary.totalInvested)}\n")
                writer.write("Maturity Value,${String.format(Locale.US, "%.2f", summary.totalMaturity)}\n")
                writer.write("Net Expected Gain,${String.format(Locale.US, "%.2f", summary.totalNetGain)}\n")
                writer.write("Average ROI (%),${String.format(Locale.US, "%.2f", summary.averageRoi)}%\n")
                writer.write("Total Investments,${summary.totalCount}\n")
                writer.write("Overdue Items,${summary.overdueCount}\n")
                writer.write("\n")

                // Column Headers
                writer.write("ID,Investment Name,Category,Investment Amount,Investment Date,Maturity Amount,Maturity Date,Expected Gain,ROI (%),Status,Days Overdue / Left,Details / Notes\n")

                // Data Rows
                for (inv in investments) {
                    val status = inv.getStatus(now)
                    val daysDiff = inv.getDaysDifference(now)
                    val daysText = when {
                        status == InvestmentStatus.CLOSED -> "Closed"
                        daysDiff < 0 -> "${-daysDiff} days OVERDUE"
                        daysDiff == 0L -> "Matures Today"
                        else -> "$daysDiff days left"
                    }

                    val row = listOf(
                        inv.id.toString(),
                        escapeCsv(inv.name),
                        escapeCsv(inv.category),
                        String.format(Locale.US, "%.2f", inv.investmentAmount),
                        dateFormat.format(Date(inv.investmentDate)),
                        String.format(Locale.US, "%.2f", inv.maturityAmount),
                        dateFormat.format(Date(inv.maturityDate)),
                        String.format(Locale.US, "%.2f", inv.gainAmount),
                        String.format(Locale.US, "%.2f%%", inv.roiPercentage),
                        status.name,
                        escapeCsv(daysText),
                        escapeCsv(inv.details)
                    )
                    writer.write(row.joinToString(",") + "\n")
                }
            }
        }
        return file
    }

    fun generateSampleTemplate(context: Context): File {
        val fileName = "InvestTrack_Sample_Template.csv"
        val exportsDir = File(context.getExternalFilesDir(null), "templates")
        if (!exportsDir.exists()) exportsDir.mkdirs()
        val file = File(exportsDir, fileName)

        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write("\uFEFF")
                writer.write("Investment Name,Category,Investment Amount,Investment Date,Maturity Amount,Maturity Date,Details\n")
                writer.write("HDFC 3-Yr High Yield FD,Fixed Deposit,10000.00,2023-01-15,12450.00,2026-01-15,A/C #98321045 7.5% p.a.\n")
                writer.write("Vanguard Growth Fund,Mutual Funds,5000.00,2024-05-10,6800.00,2027-05-10,Folio #VF-9012\n")
                writer.write("Sovereign Gold Bond,Gold,8000.00,2021-11-20,11500.00,2026-11-20,RBI Series 2021\n")
                writer.write("Tesla Growth Equity,Stocks,15000.00,2025-02-01,21000.00,2028-02-01,100 Shares Broker Zerodha\n")
            }
        }
        return file
    }

    fun openOrShareCsv(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Investment Portfolio Excel CSV")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Investment CSV"))
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
