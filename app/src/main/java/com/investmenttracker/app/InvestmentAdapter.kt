package com.investmenttracker.app

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.investmenttracker.app.databinding.ItemInvestmentGridBinding
import com.investmenttracker.app.databinding.ItemInvestmentListBinding

class InvestmentAdapter(
    private val onEditClick: (Investment) -> Unit,
    private val onDeleteClick: (Investment) -> Unit,
    private val onItemClick: ((Investment) -> Unit)? = null
) : ListAdapter<Investment, RecyclerView.ViewHolder>(InvestmentDiffCallback()) {

    var isGridView: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }
        }

    companion object {
        const val VIEW_TYPE_LIST = 0
        const val VIEW_TYPE_GRID = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (isGridView) VIEW_TYPE_GRID else VIEW_TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_GRID) {
            val binding = ItemInvestmentGridBinding.inflate(inflater, parent, false)
            GridViewHolder(binding)
        } else {
            val binding = ItemInvestmentListBinding.inflate(inflater, parent, false)
            ListViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is ListViewHolder) {
            holder.bind(item)
        } else if (holder is GridViewHolder) {
            holder.bind(item)
        }
    }

    inner class ListViewHolder(private val binding: ItemInvestmentListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(investment: Investment) {
            val context = binding.root.context

            binding.tvInvestmentName.text = investment.name
            binding.tvCategory.text = investment.category
            binding.tvInvestedAmount.text = investment.formattedInvestedAmount
            binding.tvInvestedDate.text = investment.formattedInvestmentDate
            binding.tvMaturityAmount.text = investment.formattedMaturityAmount
            binding.tvMaturityDate.text = investment.formattedMaturityDate

            // Gain and ROI
            val gain = investment.netGain
            val prefix = if (gain >= 0) "+" else ""
            binding.tvGainAmount.text = "$prefix${investment.formattedGain}"
            binding.tvRoiPercent.text = "${investment.formattedRoi} ROI"

            // Details/Notes
            if (investment.details.isNotBlank()) {
                binding.tvDetails.visibility = View.VISIBLE
                binding.tvDetails.text = investment.details
            } else {
                binding.tvDetails.visibility = View.GONE
            }

            // Apply Visual Overdue / Status Styling
            applyStatusStyling(context, investment)

            // Listeners
            binding.btnEdit.setOnClickListener { onEditClick(investment) }
            binding.btnDelete.setOnClickListener { onDeleteClick(investment) }
            binding.root.setOnClickListener { onItemClick?.invoke(investment) ?: onEditClick(investment) }
        }

        private fun applyStatusStyling(context: Context, investment: Investment) {
            when (investment.status) {
                InvestmentStatus.OVERDUE -> {
                    binding.cardInvestment.setBackgroundResource(R.drawable.bg_card_overdue)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_overdue)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_overdue))
                    val days = -investment.daysUntilMaturity
                    binding.tvStatusBadge.text = "${days}d OVERDUE"

                    binding.tvMaturityCountdown.setTextColor(ContextCompat.getColor(context, R.color.status_overdue))
                    binding.tvMaturityCountdown.text = "⚠️ Matured $days days ago (Action Required)"
                }
                InvestmentStatus.MATURING_SOON -> {
                    binding.cardInvestment.setBackgroundResource(R.drawable.bg_card)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_warning)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                    val days = investment.daysUntilMaturity
                    binding.tvStatusBadge.text = "${days}d LEFT"

                    binding.tvMaturityCountdown.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                    binding.tvMaturityCountdown.text = "⏳ Matures in $days days"
                }
                InvestmentStatus.ACTIVE -> {
                    binding.cardInvestment.setBackgroundResource(R.drawable.bg_card)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_active))
                    binding.tvStatusBadge.text = "ACTIVE"

                    binding.tvMaturityCountdown.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    val days = investment.daysUntilMaturity
                    binding.tvMaturityCountdown.text = "Matures in $days days"
                }
                InvestmentStatus.CLOSED -> {
                    binding.cardInvestment.setBackgroundResource(R.drawable.bg_card)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_closed)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_closed))
                    binding.tvStatusBadge.text = "CLOSED"

                    binding.tvMaturityCountdown.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    binding.tvMaturityCountdown.text = "✓ Redeemed / Closed"
                }
            }
        }
    }

    inner class GridViewHolder(private val binding: ItemInvestmentGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(investment: Investment) {
            val context = binding.root.context

            binding.tvGridName.text = investment.name
            binding.tvGridCategory.text = investment.category
            binding.tvGridInvestedAmount.text = investment.formattedInvestedAmount
            binding.tvGridMaturityAmount.text = investment.formattedMaturityAmount

            val gain = investment.netGain
            val prefix = if (gain >= 0) "+" else ""
            binding.tvGridGain.text = "$prefix${investment.formattedGain}"
            binding.tvGridRoi.text = "${investment.formattedRoi} ROI"

            // Details preview
            if (investment.details.isNotBlank()) {
                binding.tvGridDetails.visibility = View.VISIBLE
                binding.tvGridDetails.text = investment.details
            } else {
                binding.tvGridDetails.visibility = View.GONE
            }

            // Apply Visual Status Styling
            when (investment.status) {
                InvestmentStatus.OVERDUE -> {
                    binding.cardInvestmentGrid.setBackgroundResource(R.drawable.bg_card_overdue)
                    binding.tvGridStatusBadge.setBackgroundResource(R.drawable.bg_badge_overdue)
                    binding.tvGridStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_overdue))
                    val days = -investment.daysUntilMaturity
                    binding.tvGridStatusBadge.text = "${days}d OVERDUE"

                    binding.tvGridCountdown.setTextColor(ContextCompat.getColor(context, R.color.status_overdue))
                    binding.tvGridCountdown.text = "⚠️ Overdue ${days}d"
                }
                InvestmentStatus.MATURING_SOON -> {
                    binding.cardInvestmentGrid.setBackgroundResource(R.drawable.bg_card)
                    binding.tvGridStatusBadge.setBackgroundResource(R.drawable.bg_badge_warning)
                    binding.tvGridStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                    val days = investment.daysUntilMaturity
                    binding.tvGridStatusBadge.text = "${days}d LEFT"

                    binding.tvGridCountdown.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                    binding.tvGridCountdown.text = "⏳ Matures in ${days}d"
                }
                InvestmentStatus.ACTIVE -> {
                    binding.cardInvestmentGrid.setBackgroundResource(R.drawable.bg_card)
                    binding.tvGridStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
                    binding.tvGridStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_active))
                    binding.tvGridStatusBadge.text = "ACTIVE"

                    binding.tvGridCountdown.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    binding.tvGridCountdown.text = "Due: ${investment.formattedMaturityDate}"
                }
                InvestmentStatus.CLOSED -> {
                    binding.cardInvestmentGrid.setBackgroundResource(R.drawable.bg_card)
                    binding.tvGridStatusBadge.setBackgroundResource(R.drawable.bg_badge_closed)
                    binding.tvGridStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_closed))
                    binding.tvGridStatusBadge.text = "CLOSED"

                    binding.tvGridCountdown.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    binding.tvGridCountdown.text = "✓ Redeemed"
                }
            }

            binding.btnGridEdit.setOnClickListener { onEditClick(investment) }
            binding.btnGridDelete.setOnClickListener { onDeleteClick(investment) }
            binding.root.setOnClickListener { onItemClick?.invoke(investment) ?: onEditClick(investment) }
        }
    }

    class InvestmentDiffCallback : DiffUtil.ItemCallback<Investment>() {
        override fun areItemsTheSame(oldItem: Investment, newItem: Investment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Investment, newItem: Investment): Boolean {
            return oldItem == newItem
        }
    }
}
