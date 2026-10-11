package com.investmenttracker.app

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportSummary(
    val successCount: Int,
    val skippedCount: Int,
    val errors: List<String>,
    val parsedInvestments: List<Investment>
)

object ExcelCsvImporter {

    private val supportedDateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    )

    fun parseInvestmentsFromCsv(inputStream: InputStream): ImportSummary {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val lines = reader.readLines()

        val parsedList = mutableListOf<Investment>()
        val errors = mutableListOf<String>()
        var skipped = 0

        if (lines.isEmpty()) {
            return ImportSummary(0, 0, listOf("The selected file is empty."), emptyList())
        }

        // Find header line
        var headerIndex = -1
        var headers: List<String> = emptyList()
        var delimiter = ","

        for ((idx, line) in lines.withIndex()) {
            val clean = line.replace("\uFEFF", "").trim()
            if (clean.isBlank()) continue

            delimiter = detectDelimiter(clean)
            val tokens = splitCsvLine(clean, delimiter).map { it.trim().lowercase(Locale.ROOT) }

            if (tokens.any { it.contains("name") || it.contains("invest") || it.contains("amount") }) {
                headerIndex = idx
                headers = tokens
                break
            }
        }

        if (headerIndex == -1 || headers.isEmpty()) {
            return ImportSummary(0, 0, listOf("Could not detect valid CSV headers (Name, Amount, Date, etc.)"), emptyList())
        }

        // Determine column indexes
        var colName = headers.indexOfFirst { it.contains("name") || it == "investment" || it == "title" }
        var colCategory = headers.indexOfFirst { it.contains("category") || it.contains("type") }
        var colInvestAmount = headers.indexOfFirst {
            (it.contains("invest") && it.contains("amount")) || it == "amount" || it == "invested"
        }
        var colInvestDate = headers.indexOfFirst {
            (it.contains("invest") && it.contains("date")) || it == "start date" || it == "date"
        }
        var colMaturityAmount = headers.indexOfFirst {
            it.contains("maturity") && (it.contains("amount") || it.contains("value"))
        }
        var colMaturityDate = headers.indexOfFirst {
            it.contains("maturity") && it.contains("date") || it.contains("due date")
        }
        var colDetails = headers.indexOfFirst {
            it.contains("detail") || it.contains("note") || it.contains("remark") || it.contains("folio")
        }

        // Fallbacks if only generic names exist
        if (colName == -1 && headers.isNotEmpty()) colName = 0
        if (colInvestAmount == -1) {
            colInvestAmount = headers.indexOfFirst { it.contains("amount") }
        }

        val now = System.currentTimeMillis()

        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue

            try {
                val tokens = splitCsvLine(line, delimiter)
                if (tokens.isEmpty()) continue

                val name = if (colName in tokens.indices) tokens[colName].trim() else ""
                if (name.isBlank()) {
                    skipped++
                    continue
                }

                val category = if (colCategory in tokens.indices && tokens[colCategory].isNotBlank()) {
                    tokens[colCategory].trim()
                } else {
                    "Fixed Deposit"
                }

                val amountStr = if (colInvestAmount in tokens.indices) tokens[colInvestAmount] else "0"
                val investAmount = parseCurrencyAmount(amountStr)

                val maturityAmountStr = if (colMaturityAmount in tokens.indices) tokens[colMaturityAmount] else ""
                val maturityAmount = if (maturityAmountStr.isNotBlank()) {
                    parseCurrencyAmount(maturityAmountStr)
                } else {
                    investAmount * 1.10 // Default fallback 10% gain if omitted
                }

                val investDateStr = if (colInvestDate in tokens.indices) tokens[colInvestDate] else ""
                val investDate = parseDate(investDateStr) ?: (now - 86400_000L * 30)

                val maturityDateStr = if (colMaturityDate in tokens.indices) tokens[colMaturityDate] else ""
                val maturityDate = parseDate(maturityDateStr) ?: (now + 86400_000L * 365)

                val details = if (colDetails in tokens.indices) tokens[colDetails].trim() else "Imported from Excel"

                parsedList.add(
                    Investment(
                        name = name,
                        category = category,
                        investmentAmount = investAmount,
                        investmentDate = investDate,
                        maturityAmount = maturityAmount,
                        maturityDate = maturityDate,
                        details = details
                    )
                )
            } catch (e: Exception) {
                skipped++
                errors.add("Row ${i + 1}: ${e.message}")
            }
        }

        return ImportSummary(
            successCount = parsedList.size,
            skippedCount = skipped,
            errors = errors,
            parsedInvestments = parsedList
        )
    }

    private fun detectDelimiter(line: String): String {
        return when {
            line.count { it == ',' } >= line.count { it == ';' } && line.count { it == ',' } >= line.count { it == '\t' } -> ","
            line.count { it == ';' } > line.count { it == ',' } -> ";"
            line.count { it == '\t' } > line.count { it == ',' } -> "\t"
            else -> ","
        }
    }

    private fun splitCsvLine(line: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        val delimChar = delimiter[0]

        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimChar && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    private fun parseCurrencyAmount(text: String): Double {
        val cleaned = text.replace("$", "")
            .replace("₹", "")
            .replace("€", "")
            .replace("Rs", "", ignoreCase = true)
            .replace(",", "")
            .trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun parseDate(dateStr: String): Long? {
        if (dateStr.isBlank()) return null
        val clean = dateStr.trim()
        for (format in supportedDateFormats) {
            try {
                val parsed: Date? = format.parse(clean)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return null
    }
}
