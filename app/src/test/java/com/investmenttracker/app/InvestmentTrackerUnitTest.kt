package com.investmenttracker.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class InvestmentTrackerUnitTest {

    @Test
    fun testInvestmentCalculations() {
        val now = System.currentTimeMillis()
        val day = 86400000L

        val inv = Investment(
            id = 1,
            name = "Test FD",
            category = "Fixed Deposit",
            investmentAmount = 10000.0,
            investmentDate = now - (365 * day),
            maturityAmount = 12500.0,
            maturityDate = now + (60 * day),
            details = "FD 7.5%"
        )

        assertEquals(2500.0, inv.gainAmount, 0.01)
        assertEquals(25.0, inv.roiPercentage, 0.01)
        assertEquals(InvestmentStatus.ACTIVE, inv.getStatus(now))
    }

    @Test
    fun testOverdueStatus() {
        val now = System.currentTimeMillis()
        val day = 86400000L

        // Matured 5 days ago
        val overdueInv = Investment(
            id = 2,
            name = "Overdue Bond",
            category = "Bonds",
            investmentAmount = 5000.0,
            investmentDate = now - (400 * day),
            maturityAmount = 6000.0,
            maturityDate = now - (5 * day),
            details = "Bond matured"
        )

        assertEquals(InvestmentStatus.OVERDUE, overdueInv.getStatus(now))
        assertTrue(overdueInv.getDaysDifference(now) < 0)

        // If marked closed, status should be CLOSED regardless of date
        val closedInv = overdueInv.copy(isClosed = true)
        assertEquals(InvestmentStatus.CLOSED, closedInv.getStatus(now))
    }

    @Test
    fun testMaturingSoonStatus() {
        val now = System.currentTimeMillis()
        val day = 86400000L

        // Matures in 15 days (within 30 days)
        val soonInv = Investment(
            id = 3,
            name = "Maturing Soon Fund",
            category = "Mutual Funds",
            investmentAmount = 8000.0,
            investmentDate = now - (300 * day),
            maturityAmount = 10000.0,
            maturityDate = now + (15 * day),
            details = "Target met"
        )

        assertEquals(InvestmentStatus.MATURING_SOON, soonInv.getStatus(now))
    }

    @Test
    fun testCsvImportParsing() {
        val csvContent = """
            Investment Name,Category,Investment Amount,Investment Date,Maturity Amount,Maturity Date,Details
            HDFC Fixed Deposit,Fixed Deposit,"10,000.00",2024-01-15,"12,500.00",2026-01-15,A/C 1234
            Vanguard Growth,Mutual Funds,5000.00,2024-06-01,7200.00,2027-06-01,Folio 9988
        """.trimIndent()

        val inputStream = ByteArrayInputStream(csvContent.toByteArray(StandardCharsets.UTF_8))
        val result = ExcelCsvImporter.parseInvestmentsFromCsv(inputStream)

        assertEquals(2, result.parsedInvestments.size)
        assertEquals("HDFC Fixed Deposit", result.parsedInvestments[0].name)
        assertEquals(10000.0, result.parsedInvestments[0].investmentAmount, 0.01)
        assertEquals(12500.0, result.parsedInvestments[0].maturityAmount, 0.01)

        assertEquals("Vanguard Growth", result.parsedInvestments[1].name)
        assertEquals(5000.0, result.parsedInvestments[1].investmentAmount, 0.01)
        assertEquals(7200.0, result.parsedInvestments[1].maturityAmount, 0.01)
    }

    @Test
    fun testMonthlyReportMetrics() {
        val item = MonthlyReportItem(
            monthIndex = 0,
            monthName = "January",
            monthShortName = "JAN",
            investedAmount = 10000.0,
            maturityAmount = 15000.0,
            count = 2
        )

        assertEquals(10000.0, item.totalInvested, 0.01)
        assertEquals(15000.0, item.totalMaturing, 0.01)
        assertEquals(2, item.count)
    }
}
