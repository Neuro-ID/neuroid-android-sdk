package com.sample.neuroid.us.activities.sandbox.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sample.neuroid.us.R
import com.sample.neuroid.us.domain.network.Signal

class ScoreAdapter : RecyclerView.Adapter<ScoreViewHolder>() {

    private var signalList = emptyList<Signal>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.score_item_view, parent, false)

        return ScoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        holder.attachViewHolder(signalList[position])
    }

    override fun getItemCount(): Int = signalList.size

    fun updateAdapter(list: List<Signal>) {
        signalList = list
        notifyDataSetChanged()
    }
}