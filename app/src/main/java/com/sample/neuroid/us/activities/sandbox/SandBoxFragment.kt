package com.sample.neuroid.us.activities.sandbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.neuroid.tracker.NeuroID
import com.sample.neuroid.us.R
import com.sample.neuroid.us.databinding.SandboxFragmentBinding
import com.sample.neuroid.us.utils.collect

class SandBoxFragment : Fragment() {
    private val viewModel: SandBoxViewModel by activityViewModels()
    private var binding: SandboxFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SandboxFragmentBinding.inflate(inflater)
        binding?.apply {
            NeuroID.getInstance()?.let {
                if (it.isStopped()) {
                    it.start()
                }
                it.setScreenName("PERSONAL_DETAILS")
            }

            val yearList = resources.getStringArray(R.array.nid_app_array_years)
            val monthList = resources.getStringArray(R.array.nid_app_array_months)

            val adapterYear = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                yearList
            )

            editTextBirthYear.apply {
                setAdapter(adapterYear)
            }

            val adapterMonth = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                monthList
            )

            dobMonth.apply {
                setAdapter(adapterMonth)
                setOnItemClickListener { _, _, position, _ ->
                    editTextBirthDay.setText("")
                    editTextBirthDay.isEnabled = true
                    editTextBirthDay.setAdapter(getAdapter(position))
                }
            }

            buttonContinue.setOnClickListener {
                viewModel.checkScore()
            }
            buttonContinue.visibility = VISIBLE
        }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collect(viewModel.navigationState) {
            when (it) {
                is NavigationState.NavigateToScore -> findNavController().navigate(R.id.action_to_score_fragment)
                else -> {} //Nothing to do
            }
        }
    }

    private fun getAdapter(position: Int): ArrayAdapter<Int> {
        return ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            getDays(position)
        )
    }

    private fun getDays(position: Int): List<Int> {
        return when (position) {
            0, 2, 4, 6, 7, 9, 11 -> (1..31).toList()
            3, 5, 8, 10 -> (1..30).toList()
            else -> (1..28).toList()
        }
    }

}