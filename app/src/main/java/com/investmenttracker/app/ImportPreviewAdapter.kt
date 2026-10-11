package com.investmenttracker.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.investmenttracker.app.databinding.ItemImportPreviewBinding

class ImportPreviewAdapter(
    private val items: List<Investment>
) : RecyclerView.Adapter<ImportPreviewAdapter.PreviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val binding = ItemImportPreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvPreviewName.text = item.name
        holder.binding.tvPreviewCategory.text = "${item.category} • Matures: ${item.maturityDate}"
        holder.binding.tvPreviewAmount.text = item.formattedInvestedAmount
        holder.binding.tvPreviewMaturity.text = "→ ${item.formattedMaturityAmount}"
    }

    override fun getItemCount(): Int = items.size

    class PreviewViewHolder(val binding: ItemImportPreviewBinding) :
        RecyclerView.ViewHolder(binding.root)
}
