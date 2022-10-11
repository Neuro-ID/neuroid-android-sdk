package com.sample.neuroid.us.activities.sandbox.adapter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sample.neuroid.us.R
import com.sample.neuroid.us.domain.network.Signal

class ScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val tvScoreLabel: TextView = itemView.findViewById(R.id.tvScoreLabel)
    val tvScore: TextView = itemView.findViewById(R.id.tvScore)

    fun attachViewHolder(signal: Signal) {
        tvScoreLabel.text = signal.model
        tvScore.text = "${signal.score}"
    }

}