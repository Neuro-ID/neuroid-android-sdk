package com.neuroid.example

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.neuroid.example.databinding.AdvertisementCellBinding

class AdvertiserAdapter: RecyclerView.Adapter<AdvertiserAdapter.AdvertiserViewHolder>() {
    var bankNames: Array<String>? = null

    class AdvertiserViewHolder(binding: AdvertisementCellBinding ): RecyclerView.ViewHolder(binding.root) {
        val bank: TextView = binding.bankName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdvertiserViewHolder {
        val binding = AdvertisementCellBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return AdvertiserViewHolder(binding)

    }

    override fun getItemCount(): Int {
        this.bankNames?.size?.let {
            return it
        }
        return 0
    }

    override fun onBindViewHolder(holder: AdvertiserViewHolder, position: Int) {
        bankNames?.let {
            holder.bank.text = it[position]
        }
    }

    fun setBanks(banks: Array<String>?) {
        bankNames = banks
        notifyDataSetChanged()
    }

}