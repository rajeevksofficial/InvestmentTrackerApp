package com.investmenttracker.app

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class InvestmentStatus {
    OVERDUE,       // Maturity date has passed and not closed (RED alert)
    MATURING_SOON, // Maturity within 30 days (AMBER warning)
    ACTIVE,        // Maturing in future (BLUE/GREEN)
    CLOSED         // Redeemed / Collected (PURPLE/MUTED)
}

data class Investment(
    val id: Long = 0,
    val name: String,
    val category: String = "Fixed Deposit",
    val investmentAmount: Double,
    val investmentDate: Long, // timestamp millis
    val maturityAmount: Double,
    val maturityDate: Long,   // timestamp millis
    val details: String = "", // account #, folio, interest rate, notes
    val isClosed: Boolean = false
) {
    val gainAmount: Double
        get() = maturityAmount - investmentAmount

    val netGain: Double
        get() = gainAmount

    val roiPercentage: Double
        get() = if (investmentAmount > 0.0) ((gainAmount / investmentAmount) * 100.0) else 0.0

    val status: InvestmentStatus
        get() = getStatus()

    val daysUntilMaturity: Long
        get() = getDaysDifference()

    val formattedInvestedAmount: String
        get() = formatCurrency(investmentAmount)

    val formattedMaturityAmount: String
        get() = formatCurrency(maturityAmount)

    val formattedGain: String
        get() = formatCurrency(gainAmount)

    val formattedRoi: String
        get() = String.format(Locale.US, "%.1f%%", roiPercentage)

    val formattedInvestmentDate: String
        get() = formatDate(investmentDate)

    val formattedMaturityDate: String
        get() = formatDate(maturityDate)

    fun getStatus(currentTime: Long = System.currentTimeMillis()): InvestmentStatus {
        if (isClosed) return InvestmentStatus.CLOSED

        return when {
            maturityDate < currentTime -> InvestmentStatus.OVERDUE
            getDaysDifference(currentTime) in 0..30 -> InvestmentStatus.MATURING_SOON
            else -> InvestmentStatus.ACTIVE
        }
    }

    fun getDaysDifference(currentTime: Long = System.currentTimeMillis()): Long {
        val diffMillis = maturityDate - currentTime
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    companion object {
        private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
        private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun formatCurrency(amount: Double): String = currencyFormatter.format(amount)
        fun formatDate(timestamp: Long): String = dateFormatter.format(Date(timestamp))
        fun formatIsoDate(timestamp: Long): String = isoDateFormatter.format(Date(timestamp))
    }
}

data class PortfolioSummary(
    val totalInvested: Double,
    val totalMaturity: Double,
    val totalNetGain: Double,
    val averageRoi: Double,
    val totalCount: Int,
    val overdueCount: Int,
    val maturingSoonCount: Int,
    val activeCount: Int,
    val closedCount: Int,
    val overdueMaturityTotal: Double = 0.0
) {
    val formattedTotalInvested: String
        get() = Investment.formatCurrency(totalInvested)

    val formattedTotalMaturity: String
        get() = Investment.formatCurrency(totalMaturity)

    val formattedTotalGain: String
        get() = Investment.formatCurrency(totalNetGain)

    val formattedRoi: String
        get() = String.format(Locale.US, "%.1f%%", averageRoi)

    val formattedOverdueMaturity: String
        get() = Investment.formatCurrency(overdueMaturityTotal)
}

data class YearlyReport(
    val year: Int,
    val totalInvestedThisYear: Double,
    val totalMaturingThisYear: Double,
    val netExpectedGainThisYear: Double,
    val investmentsCount: Int,
    val maturingCount: Int,
    val overdueCount: Int,
    val monthlyData: List<MonthlyReportItem>
) {
    val roiPercentage: Double
        get() = if (totalInvestedThisYear > 0.0) ((netExpectedGainThisYear / totalInvestedThisYear) * 100.0) else 0.0

    val formattedInvested: String
        get() = Investment.formatCurrency(totalInvestedThisYear)

    val formattedMaturing: String
        get() = Investment.formatCurrency(totalMaturingThisYear)

    val formattedGain: String
        get() = Investment.formatCurrency(netExpectedGainThisYear)

    val formattedRoi: String
        get() = String.format(Locale.US, "%.1f%%", roiPercentage)
}

data class MonthlyReportItem(
    val monthIndex: Int, // 0..11
    val monthName: String,
    val monthShortName: String,
    val investedAmount: Double,
    val maturityAmount: Double,
    val count: Int
) {
    val totalInvested: Double get() = investedAmount
    val totalMaturing: Double get() = maturityAmount
}
