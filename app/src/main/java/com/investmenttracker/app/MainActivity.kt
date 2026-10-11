package com.investmenttracker.app

import android.app.DatePickerDialog
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.investmenttracker.app.databinding.ActivityMainBinding
import com.investmenttracker.app.databinding.DialogAddInvestmentBinding
import com.investmenttracker.app.databinding.DialogImportPreviewBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: InvestmentDatabaseHelper

    private lateinit var investmentAdapter: InvestmentAdapter
    private lateinit var yearlyMonthAdapter: YearlyMonthAdapter
    private lateinit var yearlyMaturingAdapter: InvestmentAdapter

    private var isGridView = false
    private var currentQuery = ""
    private var currentStatusFilter: InvestmentStatus? = null
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var isYearlyReportTab = false

    private val categories = listOf(
        "Fixed Deposit",
        "Mutual Funds",
        "Stocks / Equity",
        "Bonds",
        "Gold / SGB",
        "Real Estate / REIT",
        "Provident Fund (PPF/EPF)",
        "Other"
    )

    private val importFilePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleFileImport(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = InvestmentDatabaseHelper(this)
        dbHelper.seedSampleInvestmentsIfEmpty()

        setupAdapters()
        setupResponsiveLayout()
        setupSearchAndFilters()
        setupTabNavigation()
        setupYearNavigation()
        setupActionButtons()

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupResponsiveLayout()
    }

    private fun setupAdapters() {
        investmentAdapter = InvestmentAdapter(
            onEditClick = { investment -> showAddEditDialog(investment) },
            onDeleteClick = { investment -> confirmDelete(investment) }
        )

        yearlyMaturingAdapter = InvestmentAdapter(
            onEditClick = { investment -> showAddEditDialog(investment) },
            onDeleteClick = { investment -> confirmDelete(investment) }
        )

        yearlyMonthAdapter = YearlyMonthAdapter()
        binding.rvYearlyMonths.layoutManager = LinearLayoutManager(this)
        binding.rvYearlyMonths.adapter = yearlyMonthAdapter

        binding.rvYearlyMaturingList.layoutManager = LinearLayoutManager(this)
        binding.rvYearlyMaturingList.adapter = yearlyMaturingAdapter
    }

    private fun setupResponsiveLayout() {
        val widthDp = resources.configuration.screenWidthDp
        val isTablet = widthDp >= 600

        val spanCount = if (isGridView) {
            if (isTablet) (widthDp / 240).coerceAtLeast(3) else 2
        } else {
            if (isTablet) 2 else 1
        }

        binding.rvInvestments.layoutManager = GridLayoutManager(this, spanCount)
        investmentAdapter.isGridView = isGridView

        binding.btnToggleViewMode.setImageResource(
            if (isGridView) R.drawable.ic_list else R.drawable.ic_grid
        )
    }

    private fun setupSearchAndFilters() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility = if (currentQuery.isNotEmpty()) View.VISIBLE else View.GONE
                loadData()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }

        binding.btnToggleViewMode.setOnClickListener {
            isGridView = !isGridView
            setupResponsiveLayout()
        }

        binding.chipAll.setOnClickListener { selectFilter(null) }
        binding.chipOverdue.setOnClickListener { selectFilter(InvestmentStatus.OVERDUE) }
        binding.chipMaturingSoon.setOnClickListener { selectFilter(InvestmentStatus.MATURING_SOON) }
        binding.chipActive.setOnClickListener { selectFilter(InvestmentStatus.ACTIVE) }
        binding.chipClosed.setOnClickListener { selectFilter(InvestmentStatus.CLOSED) }
    }

    private fun selectFilter(status: InvestmentStatus?) {
        currentStatusFilter = status
        updateChipStyling()
        loadData()
    }

    private fun updateChipStyling() {
        val unselectedBg = ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected)
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
        val overdueSelectedBg = ContextCompat.getDrawable(this, R.drawable.bg_chip_overdue_selected)

        val textPrimary = ContextCompat.getColor(this, R.color.text_primary)
        val textMuted = ContextCompat.getColor(this, R.color.text_muted)
        val overdueColor = ContextCompat.getColor(this, R.color.status_overdue)
        val warningColor = ContextCompat.getColor(this, R.color.status_warning)
        val activeColor = ContextCompat.getColor(this, R.color.status_active)

        // Reset all
        binding.chipAll.background = unselectedBg
        binding.chipAll.setTextColor(textMuted)

        binding.chipOverdue.background = unselectedBg
        binding.chipOverdue.setTextColor(overdueColor)

        binding.chipMaturingSoon.background = unselectedBg
        binding.chipMaturingSoon.setTextColor(warningColor)

        binding.chipActive.background = unselectedBg
        binding.chipActive.setTextColor(activeColor)

        binding.chipClosed.background = unselectedBg
        binding.chipClosed.setTextColor(textMuted)

        // Highlight selected
        when (currentStatusFilter) {
            null -> {
                binding.chipAll.background = selectedBg
                binding.chipAll.setTextColor(textPrimary)
            }
            InvestmentStatus.OVERDUE -> {
                binding.chipOverdue.background = overdueSelectedBg
                binding.chipOverdue.setTextColor(overdueColor)
            }
            InvestmentStatus.MATURING_SOON -> {
                binding.chipMaturingSoon.background = selectedBg
                binding.chipMaturingSoon.setTextColor(warningColor)
            }
            InvestmentStatus.ACTIVE -> {
                binding.chipActive.background = selectedBg
                binding.chipActive.setTextColor(textPrimary)
            }
            InvestmentStatus.CLOSED -> {
                binding.chipClosed.background = selectedBg
                binding.chipClosed.setTextColor(textPrimary)
            }
        }
    }

    private fun setupTabNavigation() {
        binding.tabPortfolio.setOnClickListener {
            if (isYearlyReportTab) {
                isYearlyReportTab = false
                switchTabUi()
            }
        }

        binding.tabYearlyReport.setOnClickListener {
            if (!isYearlyReportTab) {
                isYearlyReportTab = true
                switchTabUi()
                renderYearlyReport()
            }
        }
    }

    private fun switchTabUi() {
        if (isYearlyReportTab) {
            binding.layoutPortfolioContainer.visibility = View.GONE
            binding.layoutYearlyReportContainer.visibility = View.VISIBLE

            binding.tabYearlyReport.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabYearlyReport.setTextColor(ContextCompat.getColor(this, R.color.white))

            binding.tabPortfolio.background = null
            binding.tabPortfolio.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            binding.layoutPortfolioContainer.visibility = View.VISIBLE
            binding.layoutYearlyReportContainer.visibility = View.GONE

            binding.tabPortfolio.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabPortfolio.setTextColor(ContextCompat.getColor(this, R.color.white))

            binding.tabYearlyReport.background = null
            binding.tabYearlyReport.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            loadData()
        }
    }

    private fun setupYearNavigation() {
        binding.btnYearPrev.setOnClickListener {
            currentYear--
            renderYearlyReport()
        }
        binding.btnYearNext.setOnClickListener {
            currentYear++
            renderYearlyReport()
        }
    }

    private fun setupActionButtons() {
        binding.fabAdd.setOnClickListener {
            showAddEditDialog(null)
        }

        binding.btnExportPdf.setOnClickListener {
            exportPdf()
        }

        binding.btnExportExcel.setOnClickListener {
            exportExcelCsv()
        }

        binding.btnImportExcel.setOnClickListener {
            importFilePicker.launch("*/*")
        }

        binding.btnDownloadTemplate.setOnClickListener {
            downloadTemplate()
        }
    }

    private fun loadData() {
        val summary = dbHelper.getPortfolioSummary()
        renderSummary(summary)

        val filteredList = dbHelper.getInvestmentsFiltered(
            query = currentQuery,
            statusFilter = currentStatusFilter
        )
        binding.rvInvestments.adapter = investmentAdapter
        investmentAdapter.submitList(filteredList)

        if (filteredList.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvInvestments.visibility = View.GONE
            if (currentQuery.isNotBlank() || currentStatusFilter != null) {
                binding.tvEmptyTitle.text = "No Matching Investments"
                binding.tvEmptySubtitle.text = "Try adjusting your search query or filter"
            } else {
                binding.tvEmptyTitle.text = getString(R.string.empty_title)
                binding.tvEmptySubtitle.text = getString(R.string.empty_subtitle)
            }
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvInvestments.visibility = View.VISIBLE
        }

        binding.tvHeaderSubtitle.text = "${summary.totalCount} investments • ${summary.formattedTotalInvested}"

        if (isYearlyReportTab) {
            renderYearlyReport()
        }
    }

    private fun renderSummary(summary: PortfolioSummary) {
        binding.tvSummaryInvested.text = summary.formattedTotalInvested
        binding.tvSummaryMaturity.text = summary.formattedTotalMaturity
        val prefix = if (summary.totalNetGain >= 0) "+" else ""
        binding.tvSummaryGain.text = "$prefix${summary.formattedTotalGain}"
        binding.tvSummaryRoi.text = "${if (summary.averageRoi >= 0) "+" else ""}${summary.formattedRoi} ROI"

        // Overdue Notification Alert Banner
        if (summary.overdueCount > 0) {
            binding.cardOverdueBanner.visibility = View.VISIBLE
            binding.tvOverdueBannerText.text = "${summary.overdueCount} investment${if (summary.overdueCount > 1) "s" else ""} overdue for maturity! Total: ${summary.formattedOverdueMaturity}"
            binding.btnViewOverdue.setOnClickListener {
                selectFilter(InvestmentStatus.OVERDUE)
            }
        } else {
            binding.cardOverdueBanner.visibility = View.GONE
        }
    }

    private fun renderYearlyReport() {
        binding.tvReportYear.text = "Year $currentYear"
        val report = dbHelper.getYearlyReport(currentYear)

        binding.tvYearlyInvested.text = report.formattedInvested
        binding.tvYearlyMaturing.text = report.formattedMaturing
        val prefix = if (report.netExpectedGainThisYear >= 0) "+" else ""
        binding.tvYearlyGain.text = "$prefix${report.formattedGain}"
        binding.tvYearlyRoi.text = "${if (report.roiPercentage >= 0) "+" else ""}${report.formattedRoi} ROI"

        yearlyMonthAdapter.submitList(report.monthlyData)

        // Maturing in Year List
        val cal = Calendar.getInstance()
        val all = dbHelper.getAllInvestments()
        val maturingInYear = all.filter {
            cal.timeInMillis = it.maturityDate
            cal.get(Calendar.YEAR) == currentYear
        }

        yearlyMaturingAdapter.submitList(maturingInYear)
        if (maturingInYear.isEmpty()) {
            binding.tvYearlyMaturingEmpty.visibility = View.VISIBLE
            binding.rvYearlyMaturingList.visibility = View.GONE
        } else {
            binding.tvYearlyMaturingEmpty.visibility = View.GONE
            binding.rvYearlyMaturingList.visibility = View.VISIBLE
        }
    }

    private fun showAddEditDialog(existing: Investment?) {
        val dialogBinding = DialogAddInvestmentBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        val isEdit = existing != null
        dialogBinding.tvDialogTitle.text = if (isEdit) getString(R.string.dialog_edit_title) else getString(R.string.dialog_add_title)

        // Category Spinner
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        dialogBinding.spinnerCategory.adapter = spinnerAdapter

        // Autocomplete suggestions for Name and Details
        val nameSuggestions = dbHelper.getDistinctInvestmentNames()
        val nameAutoAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nameSuggestions)
        dialogBinding.etInvestmentName.setAdapter(nameAutoAdapter)

        val detailSuggestions = dbHelper.getDistinctDetails()
        val detailsAutoAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, detailSuggestions)
        dialogBinding.etInvestmentDetails.setAdapter(detailsAutoAdapter)

        val cal = Calendar.getInstance()
        var selectedInvestDate: Long = existing?.investmentDate ?: cal.timeInMillis
        var selectedMaturityDate: Long = existing?.maturityDate ?: run {
            cal.add(Calendar.YEAR, 1)
            val d = cal.timeInMillis
            cal.timeInMillis = System.currentTimeMillis()
            d
        }

        fun updateDateButtons() {
            dialogBinding.btnPickInvestmentDate.text = Investment.formatDate(selectedInvestDate)
            dialogBinding.btnPickMaturityDate.text = Investment.formatDate(selectedMaturityDate)
        }
        updateDateButtons()

        dialogBinding.btnPickInvestmentDate.setOnClickListener {
            val c = Calendar.getInstance().apply { timeInMillis = selectedInvestDate }
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    c.set(y, m, d)
                    selectedInvestDate = c.timeInMillis
                    updateDateButtons()
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.btnPickMaturityDate.setOnClickListener {
            val c = Calendar.getInstance().apply { timeInMillis = selectedMaturityDate }
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    c.set(y, m, d)
                    selectedMaturityDate = c.timeInMillis
                    updateDateButtons()
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Pre-fill existing data
        if (existing != null) {
            dialogBinding.etInvestmentName.setText(existing.name)
            val catIdx = categories.indexOf(existing.category)
            if (catIdx >= 0) dialogBinding.spinnerCategory.setSelection(catIdx)
            dialogBinding.etInvestmentAmount.setText(existing.investmentAmount.toString())
            dialogBinding.etMaturityAmount.setText(existing.maturityAmount.toString())
            dialogBinding.etInvestmentDetails.setText(existing.details)
            dialogBinding.cbIsClosed.isChecked = existing.isClosed
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etInvestmentName.text.toString().trim()
            val category = dialogBinding.spinnerCategory.selectedItem?.toString() ?: "Fixed Deposit"
            val investAmountStr = dialogBinding.etInvestmentAmount.text.toString().trim()
            val maturityAmountStr = dialogBinding.etMaturityAmount.text.toString().trim()
            val details = dialogBinding.etInvestmentDetails.text.toString().trim()
            val isClosed = dialogBinding.cbIsClosed.isChecked

            if (name.isBlank()) {
                dialogBinding.etInvestmentName.error = "Investment name required"
                dialogBinding.etInvestmentName.requestFocus()
                return@setOnClickListener
            }

            val investAmount = investAmountStr.toDoubleOrNull()
            if (investAmount == null || investAmount <= 0.0) {
                dialogBinding.etInvestmentAmount.error = "Valid investment amount required"
                dialogBinding.etInvestmentAmount.requestFocus()
                return@setOnClickListener
            }

            val maturityAmount = maturityAmountStr.toDoubleOrNull()
            if (maturityAmount == null || maturityAmount < 0.0) {
                dialogBinding.etMaturityAmount.error = "Valid maturity amount required"
                dialogBinding.etMaturityAmount.requestFocus()
                return@setOnClickListener
            }

            val investment = Investment(
                id = existing?.id ?: 0,
                name = name,
                category = category,
                investmentAmount = investAmount,
                investmentDate = selectedInvestDate,
                maturityAmount = maturityAmount,
                maturityDate = selectedMaturityDate,
                details = details,
                isClosed = isClosed
            )

            if (isEdit) {
                dbHelper.updateInvestment(investment)
                Toast.makeText(this, "Updated investment: $name", Toast.LENGTH_SHORT).show()
            } else {
                dbHelper.insertInvestment(investment)
                Toast.makeText(this, "Added investment: $name", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
            loadData()
        }

        dialog.show()
    }

    private fun confirmDelete(investment: Investment) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage("Are you sure you want to delete '${investment.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteInvestment(investment.id)
                Toast.makeText(this, "Deleted ${investment.name}", Toast.LENGTH_SHORT).show()
                loadData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportPdf() {
        val investments = dbHelper.getAllInvestments()
        if (investments.isEmpty()) {
            Toast.makeText(this, "No investments to export", Toast.LENGTH_SHORT).show()
            return
        }
        val summary = dbHelper.getPortfolioSummary()
        try {
            val file = PdfReportExporter.exportPortfolioToPdf(
                this,
                "Investment Portfolio",
                investments,
                summary
            )
            Toast.makeText(this, "PDF saved: ${file.name}", Toast.LENGTH_SHORT).show()
            PdfReportExporter.openOrSharePdf(this, file)
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportExcelCsv() {
        val investments = dbHelper.getAllInvestments()
        if (investments.isEmpty()) {
            Toast.makeText(this, "No investments to export", Toast.LENGTH_SHORT).show()
            return
        }
        val summary = dbHelper.getPortfolioSummary()
        try {
            val file = ExcelCsvExporter.exportToExcelCsv(
                this,
                "Investment Portfolio",
                investments,
                summary
            )
            Toast.makeText(this, "Excel CSV saved: ${file.name}", Toast.LENGTH_SHORT).show()
            ExcelCsvExporter.openOrShareCsv(this, file)
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun downloadTemplate() {
        try {
            val file = ExcelCsvExporter.generateSampleTemplate(this)
            Toast.makeText(this, "Template ready: ${file.name}", Toast.LENGTH_SHORT).show()
            ExcelCsvExporter.openOrShareCsv(this, file)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed generating template: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleFileImport(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Toast.makeText(this, "Could not open selected file", Toast.LENGTH_SHORT).show()
                return
            }

            val result = ExcelCsvImporter.parseInvestmentsFromCsv(inputStream)
            if (result.parsedInvestments.isEmpty()) {
                val errorMsg = result.errors.firstOrNull() ?: "No valid investment rows found in file."
                AlertDialog.Builder(this)
                    .setTitle("Import Failed")
                    .setMessage(errorMsg)
                    .setPositiveButton("OK", null)
                    .show()
                return
            }

            showImportPreviewDialog(result)
        } catch (e: Exception) {
            Toast.makeText(this, "Import error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showImportPreviewDialog(summary: ImportSummary) {
        val previewBinding = DialogImportPreviewBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(previewBinding.root)
            .create()

        previewBinding.tvImportSubtitle.text = "Detected ${summary.parsedInvestments.size} investments ready to import"

        previewBinding.rvImportPreview.layoutManager = LinearLayoutManager(this)
        previewBinding.rvImportPreview.adapter = ImportPreviewAdapter(summary.parsedInvestments)

        previewBinding.btnImportCancel.setOnClickListener {
            dialog.dismiss()
        }

        previewBinding.btnImportConfirm.text = "Confirm Import (${summary.parsedInvestments.size})"
        previewBinding.btnImportConfirm.setOnClickListener {
            val insertedCount = dbHelper.insertBatch(summary.parsedInvestments)
            dialog.dismiss()
            Toast.makeText(this, "Successfully imported $insertedCount investments!", Toast.LENGTH_LONG).show()
            loadData()
        }

        dialog.show()
    }
}
