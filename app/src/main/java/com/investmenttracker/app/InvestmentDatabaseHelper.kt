package com.investmenttracker.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InvestmentDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "investments.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_INVESTMENTS = "investments"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_CATEGORY = "category"
        const val COL_INVESTMENT_AMOUNT = "investment_amount"
        const val COL_INVESTMENT_DATE = "investment_date"
        const val COL_MATURITY_AMOUNT = "maturity_amount"
        const val COL_MATURITY_DATE = "maturity_date"
        const val COL_DETAILS = "details"
        const val COL_IS_CLOSED = "is_closed"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createQuery = """
            CREATE TABLE $TABLE_INVESTMENTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_INVESTMENT_AMOUNT REAL NOT NULL,
                $COL_INVESTMENT_DATE INTEGER NOT NULL,
                $COL_MATURITY_AMOUNT REAL NOT NULL,
                $COL_MATURITY_DATE INTEGER NOT NULL,
                $COL_DETAILS TEXT,
                $COL_IS_CLOSED INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INVESTMENTS")
        onCreate(db)
    }

    fun insertInvestment(investment: Investment): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, investment.name)
            put(COL_CATEGORY, investment.category)
            put(COL_INVESTMENT_AMOUNT, investment.investmentAmount)
            put(COL_INVESTMENT_DATE, investment.investmentDate)
            put(COL_MATURITY_AMOUNT, investment.maturityAmount)
            put(COL_MATURITY_DATE, investment.maturityDate)
            put(COL_DETAILS, investment.details)
            put(COL_IS_CLOSED, if (investment.isClosed) 1 else 0)
        }
        return db.insert(TABLE_INVESTMENTS, null, values)
    }

    fun updateInvestment(investment: Investment): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, investment.name)
            put(COL_CATEGORY, investment.category)
            put(COL_INVESTMENT_AMOUNT, investment.investmentAmount)
            put(COL_INVESTMENT_DATE, investment.investmentDate)
            put(COL_MATURITY_AMOUNT, investment.maturityAmount)
            put(COL_MATURITY_DATE, investment.maturityDate)
            put(COL_DETAILS, investment.details)
            put(COL_IS_CLOSED, if (investment.isClosed) 1 else 0)
        }
        return db.update(TABLE_INVESTMENTS, values, "$COL_ID = ?", arrayOf(investment.id.toString())) > 0
    }

    fun deleteInvestment(id: Long): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_INVESTMENTS, "$COL_ID = ?", arrayOf(id.toString())) > 0
    }

    fun getAllInvestments(): List<Investment> {
        val list = mutableListOf<Investment>()
        val db = readableDatabase
        val cursor = db.query(TABLE_INVESTMENTS, null, null, null, null, null, "$COL_MATURITY_DATE ASC")
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(cursorToInvestment(it))
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun getInvestmentsFiltered(
        query: String = "",
        statusFilter: InvestmentStatus? = null,
        sortOption: SortOption = SortOption.MATURITY_DATE_ASC
    ): List<Investment> {
        val all = getAllInvestments()
        val now = System.currentTimeMillis()

        return all.filter { inv ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase(Locale.ROOT)
                inv.name.lowercase(Locale.ROOT).contains(q) ||
                    inv.category.lowercase(Locale.ROOT).contains(q) ||
                    inv.details.lowercase(Locale.ROOT).contains(q)
            }

            val matchesStatus = when (statusFilter) {
                null -> true
                InvestmentStatus.OVERDUE -> inv.getStatus(now) == InvestmentStatus.OVERDUE
                InvestmentStatus.MATURING_SOON -> inv.getStatus(now) == InvestmentStatus.MATURING_SOON
                InvestmentStatus.ACTIVE -> inv.getStatus(now) == InvestmentStatus.ACTIVE
                InvestmentStatus.CLOSED -> inv.isClosed
            }

            matchesQuery && matchesStatus
        }.sortedWith { a, b ->
            when (sortOption) {
                SortOption.MATURITY_DATE_ASC -> a.maturityDate.compareTo(b.maturityDate)
                SortOption.MATURITY_DATE_DESC -> b.maturityDate.compareTo(a.maturityDate)
                SortOption.INVESTMENT_DATE_DESC -> b.investmentDate.compareTo(a.investmentDate)
                SortOption.INVESTMENT_DATE_ASC -> a.investmentDate.compareTo(b.investmentDate)
                SortOption.AMOUNT_DESC -> b.investmentAmount.compareTo(a.investmentAmount)
                SortOption.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
            }
        }
    }

    fun getPortfolioSummary(): PortfolioSummary {
        val all = getAllInvestments()
        val now = System.currentTimeMillis()

        val totalInvested = all.sumOf { it.investmentAmount }
        val totalMaturity = all.sumOf { it.maturityAmount }
        val totalGain = totalMaturity - totalInvested
        val avgRoi = if (totalInvested > 0.0) ((totalGain / totalInvested) * 100.0) else 0.0

        var overdue = 0
        var overdueMaturityTotal = 0.0
        var soon = 0
        var active = 0
        var closed = 0

        for (inv in all) {
            when (inv.getStatus(now)) {
                InvestmentStatus.OVERDUE -> {
                    overdue++
                    overdueMaturityTotal += inv.maturityAmount
                }
                InvestmentStatus.MATURING_SOON -> soon++
                InvestmentStatus.ACTIVE -> active++
                InvestmentStatus.CLOSED -> closed++
            }
        }

        return PortfolioSummary(
            totalInvested = totalInvested,
            totalMaturity = totalMaturity,
            totalNetGain = totalGain,
            averageRoi = avgRoi,
            totalCount = all.size,
            overdueCount = overdue,
            maturingSoonCount = soon,
            activeCount = active,
            closedCount = closed,
            overdueMaturityTotal = overdueMaturityTotal
        )
    }

    fun getDistinctInvestmentNames(): List<String> {
        val names = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT $COL_NAME FROM $TABLE_INVESTMENTS WHERE $COL_NAME IS NOT NULL AND $COL_NAME != '' ORDER BY $COL_NAME ASC", null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    names.add(it.getString(0))
                } while (it.moveToNext())
            }
        }
        return names
    }

    fun getDistinctDetails(): List<String> {
        val details = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT $COL_DETAILS FROM $TABLE_INVESTMENTS WHERE $COL_DETAILS IS NOT NULL AND $COL_DETAILS != '' ORDER BY $COL_DETAILS ASC", null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    details.add(it.getString(0))
                } while (it.moveToNext())
            }
        }
        return details
    }

    fun getYearlyReport(year: Int): YearlyReport {
        val all = getAllInvestments()
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        // Investments maturing or made in this year
        val maturingThisYear = all.filter {
            cal.timeInMillis = it.maturityDate
            cal.get(Calendar.YEAR) == year
        }

        val investedThisYear = all.filter {
            cal.timeInMillis = it.investmentDate
            cal.get(Calendar.YEAR) == year
        }

        val totalInvestedThisYear = investedThisYear.sumOf { it.investmentAmount }
        val totalMaturingThisYear = maturingThisYear.sumOf { it.maturityAmount }
        val netGain = totalMaturingThisYear - totalInvestedThisYear

        val overdueInYear = maturingThisYear.count { it.getStatus(now) == InvestmentStatus.OVERDUE }

        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val shortFormat = SimpleDateFormat("MMM", Locale.getDefault())

        val monthlyItems = mutableListOf<MonthlyReportItem>()
        for (m in 0..11) {
            cal.set(Calendar.MONTH, m)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthName = monthFormat.format(cal.time)
            val shortName = shortFormat.format(cal.time).uppercase(Locale.getDefault())

            val inMonthInvested = investedThisYear.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.investmentDate }
                c.get(Calendar.MONTH) == m
            }.sumOf { it.investmentAmount }

            val inMonthMatured = maturingThisYear.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.maturityDate }
                c.get(Calendar.MONTH) == m
            }

            monthlyItems.add(
                MonthlyReportItem(
                    monthIndex = m,
                    monthName = monthName,
                    monthShortName = shortName,
                    investedAmount = inMonthInvested,
                    maturityAmount = inMonthMatured.sumOf { it.maturityAmount },
                    count = inMonthMatured.size
                )
            )
        }

        return YearlyReport(
            year = year,
            totalInvestedThisYear = totalInvestedThisYear,
            totalMaturingThisYear = totalMaturingThisYear,
            netExpectedGainThisYear = netGain,
            investmentsCount = investedThisYear.size,
            maturingCount = maturingThisYear.size,
            overdueCount = overdueInYear,
            monthlyData = monthlyItems
        )
    }

    fun getAvailableYears(): List<Int> {
        val all = getAllInvestments()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val cal = Calendar.getInstance()

        val years = mutableSetOf(currentYear, currentYear + 1, currentYear - 1)
        for (inv in all) {
            cal.timeInMillis = inv.investmentDate
            years.add(cal.get(Calendar.YEAR))
            cal.timeInMillis = inv.maturityDate
            years.add(cal.get(Calendar.YEAR))
        }
        return years.sorted()
    }

    fun insertBatch(investments: List<Investment>): Int {
        val db = writableDatabase
        var count = 0
        db.beginTransaction()
        try {
            for (inv in investments) {
                val values = ContentValues().apply {
                    put(COL_NAME, inv.name)
                    put(COL_CATEGORY, inv.category)
                    put(COL_INVESTMENT_AMOUNT, inv.investmentAmount)
                    put(COL_INVESTMENT_DATE, inv.investmentDate)
                    put(COL_MATURITY_AMOUNT, inv.maturityAmount)
                    put(COL_MATURITY_DATE, inv.maturityDate)
                    put(COL_DETAILS, inv.details)
                    put(COL_IS_CLOSED, if (inv.isClosed) 1 else 0)
                }
                if (db.insert(TABLE_INVESTMENTS, null, values) > 0) {
                    count++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return count
    }

    fun seedSampleInvestmentsIfEmpty() {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_INVESTMENTS", null)
        var count = 0
        cursor.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }

        if (count == 0) {
            val now = System.currentTimeMillis()
            val dayMillis = 86400_000L

            // 1. OVERDUE Investment (Matured 14 days ago - RED alert)
            insertInvestment(
                Investment(
                    name = "HDFC 3-Yr High Yield FD",
                    category = "Fixed Deposit",
                    investmentAmount = 10000.0,
                    investmentDate = now - (365L * 3 * dayMillis) - (14 * dayMillis),
                    maturityAmount = 12450.0,
                    maturityDate = now - (14 * dayMillis),
                    details = "A/C #98321045 • 7.5% p.a. • Pending collection/renewal"
                )
            )

            // 2. OVERDUE Investment (Matured 35 days ago - RED alert)
            insertInvestment(
                Investment(
                    name = "Corporate Infra Bond Series IV",
                    category = "Bonds",
                    investmentAmount = 15000.0,
                    investmentDate = now - (365L * 2 * dayMillis) - (35 * dayMillis),
                    maturityAmount = 18200.0,
                    maturityDate = now - (35 * dayMillis),
                    details = "Folio #INFRA-883 • 9.2% Coupon • Principal redemption due"
                )
            )

            // 3. MATURING SOON (Matures in 9 days - AMBER warning)
            insertInvestment(
                Investment(
                    name = "Vanguard Growth Mutual Fund",
                    category = "Mutual Funds",
                    investmentAmount = 8500.0,
                    investmentDate = now - (365L * dayMillis),
                    maturityAmount = 10800.0,
                    maturityDate = now + (9 * dayMillis),
                    details = "Target return achieved • 27.1% Gain • Prepare redemption"
                )
            )

            // 4. MATURING SOON (Matures in 22 days - AMBER warning)
            insertInvestment(
                Investment(
                    name = "Sovereign Gold Bond 2021",
                    category = "Gold",
                    investmentAmount = 5000.0,
                    investmentDate = now - (365L * 5 * dayMillis),
                    maturityAmount = 7900.0,
                    maturityDate = now + (22 * dayMillis),
                    details = "RBI SGB-Series 3 • 2.5% semi-annual interest credited"
                )
            )

            // 5. ACTIVE (Matures in 180 days - BLUE/GREEN)
            insertInvestment(
                Investment(
                    name = "National Pension Scheme Tier 1",
                    category = "Pension",
                    investmentAmount = 25000.0,
                    investmentDate = now - (180 * dayMillis),
                    maturityAmount = 31500.0,
                    maturityDate = now + (180 * dayMillis),
                    details = "PRAN #110098721 • Auto-rebalance growth plan"
                )
            )

            // 6. ACTIVE (Matures in 280 days - BLUE/GREEN)
            insertInvestment(
                Investment(
                    name = "Apple Inc. Equity Holdings",
                    category = "Stocks",
                    investmentAmount = 12000.0,
                    investmentDate = now - (90 * dayMillis),
                    maturityAmount = 15600.0,
                    maturityDate = now + (280 * dayMillis),
                    details = "Broker: Zerodha • Target exit price: \$250/share"
                )
            )

            // 7. CLOSED / REDEEMED
            insertInvestment(
                Investment(
                    name = "Commercial Paper Short Term",
                    category = "Bonds",
                    investmentAmount = 7000.0,
                    investmentDate = now - (300 * dayMillis),
                    maturityAmount = 7450.0,
                    maturityDate = now - (100 * dayMillis),
                    details = "Matured & transferred to Primary Savings A/C",
                    isClosed = true
                )
            )
        }
    }

    private fun cursorToInvestment(cursor: android.database.Cursor): Investment {
        return Investment(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
            investmentAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_INVESTMENT_AMOUNT)),
            investmentDate = cursor.getLong(cursor.getColumnIndexOrThrow(COL_INVESTMENT_DATE)),
            maturityAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_MATURITY_AMOUNT)),
            maturityDate = cursor.getLong(cursor.getColumnIndexOrThrow(COL_MATURITY_DATE)),
            details = cursor.getString(cursor.getColumnIndexOrThrow(COL_DETAILS)) ?: "",
            isClosed = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_CLOSED)) == 1
        )
    }
}
