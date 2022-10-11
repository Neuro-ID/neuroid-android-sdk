package com.sample.neuroid.us.activities.sandbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.sample.neuroid.us.activities.sandbox.adapter.ScoreAdapter
import com.sample.neuroid.us.databinding.ScoreFragmentBinding
import com.sample.neuroid.us.utils.collect

class ScoreFragment : Fragment() {
    private val viewModel: SandBoxViewModel by activityViewModels()
    private lateinit var binding: ScoreFragmentBinding
    private val scoreAdapter = ScoreAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ScoreFragmentBinding.inflate(inflater)
        binding.apply {
            rvScores.adapter = scoreAdapter
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collect(viewModel.error) { error ->
            with(binding) {
                if (error.isNotEmpty()) {
                    rvScores.visibility = View.GONE
                    tvError.text = error
                }
            }
        }
        collect(viewModel.score) { scores ->
            with(binding) {
                scoreAdapter.updateAdapter(scores)
            }
        }
    }
}