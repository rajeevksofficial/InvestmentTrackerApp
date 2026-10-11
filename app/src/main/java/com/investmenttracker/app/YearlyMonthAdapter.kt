package com.investmenttracker.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.investmenttracker.app.databinding.ItemYearlyMonthBinding
import java.text.NumberFormat
import java.util.Locale

class YearlyMonthAdapter(
    private var items: List<MonthlyReportItem> = emptyList(),
    private val onItemClick: ((MonthlyReportItem) -> Unit)? = null
) : RecyclerView.Adapter<YearlyMonthAdapter.MonthViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    private var maxMaturity: Double = 1.0

    fun submitList(newItems: List<MonthlyReportItem>) {
        items = newItems
        maxMaturity = (items.maxOfOrNull { it.totalMaturing } ?: 1.0).coerceAtLeast(1.0)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemYearlyMonthBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class MonthViewHolder(private val binding: ItemYearlyMonthBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MonthlyReportItem) {
            binding.tvYearlyMonthBadge.text = item.monthName.take(3).uppercase(Locale.US)
            binding.tvYearlyMonthName.text = item.monthName

            val maturingCount = item.count
            binding.tvYearlyMonthCount.text = when {
                maturingCount == 1 -> "1 Maturing"
                maturingCount > 1 -> "$maturingCount Maturing"
                else -> "No maturities"
            }

            binding.tvYearlyMonthInvested.text = "Invested: ${currencyFormat.format(item.totalInvested)}"
            binding.tvYearlyMonthMaturity.text = "Matures: ${currencyFormat.format(item.totalMaturing)}"

            val progressPercent = if (maxMaturity > 0) {
                ((item.totalMaturing / maxMaturity) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }
            binding.progressYearlyMonth.progress = progressPercent

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}
